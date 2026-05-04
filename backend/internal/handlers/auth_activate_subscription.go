package handlers

import (
	"encoding/json"
	"net/http"
	"strings"
	"time"

	"github.com/google/uuid"
	"server/internal/billing"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/payments/mercadopago"
	"server/internal/validation"
)

const activateSubscriptionPaidMonths = 1

type activateSubscriptionRequest struct {
	PlanID         string `json:"plan_id" validate:"required,oneof=pyme empresa"`
	CardToken      string `json:"card_token"`
	UseMockPayment bool   `json:"use_mock_payment"`
}

// PostMeActivateSubscription saves a payment method and starts the first paid subscription period
// after trial (or when the company otherwise has no app download entitlement).
func (h *AuthHandler) PostMeActivateSubscription(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" || claims.CompanyID == "" {
		RespondWithError(w, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	if !canAccessWebManagement(claims.Role) {
		RespondWithError(w, ErrCodeForbidden, "Este rol no tiene acceso a la administración web", http.StatusForbidden)
		return
	}

	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}
	userID, err := uuid.Parse(claims.UserID)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Usuario inválido", http.StatusBadRequest)
		return
	}

	var req activateSubscriptionRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}
	req.PlanID = strings.TrimSpace(req.PlanID)
	req.CardToken = strings.TrimSpace(req.CardToken)
	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, "Revisá los datos del plan.", fields, http.StatusBadRequest)
		return
	}

	ctx := r.Context()
	company, err := h.companyRepo.GetByIDForBilling(ctx, companyID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("activate subscription: company")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if company == nil {
		RespondWithError(w, ErrCodeNotFound, "Empresa no encontrada", http.StatusNotFound)
		return
	}

	now := time.Now()
	if billing.CompanyHasAppDownloadAccess(now, company) {
		RespondWithError(w, ErrCodeConflict, "La suscripción ya está activa.", http.StatusConflict)
		return
	}

	if req.UseMockPayment && !h.signupAllowMock {
		RespondWithError(w, ErrCodeInvalidRequest, "Pago simulado no habilitado en este servidor.", http.StatusBadRequest)
		return
	}
	if !req.UseMockPayment && req.CardToken == "" {
		RespondWithError(w, ErrCodeInvalidRequest, "Falta el token de la tarjeta.", http.StatusBadRequest)
		return
	}

	user, err := h.userRepo.GetByID(ctx, userID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("activate subscription: user")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if user == nil {
		RespondWithError(w, ErrCodeNotFound, "Usuario no encontrado", http.StatusNotFound)
		return
	}
	email := ""
	if user.Email != nil {
		email = strings.TrimSpace(*user.Email)
	}
	if email == "" && user.Username != nil {
		email = strings.TrimSpace(*user.Username)
	}
	if email == "" && !req.UseMockPayment && h.mp.HasAccessToken() {
		RespondWithError(w, ErrCodeInvalidRequest, "Falta un email de titular para Mercado Pago.", http.StatusBadRequest)
		return
	}

	maxWarehouses, maxUsers, documentsMonthlyLimit := planLimits(req.PlanID)

	var mpCust, mpCard string
	switch {
	case req.UseMockPayment && h.signupAllowMock:
		mpCust, mpCard = mercadopago.StubCustomerID, mercadopago.StubCardID
	case h.mp.HasAccessToken():
		existingCust := ""
		if company.MpCustomerID != nil {
			existingCust = strings.TrimSpace(*company.MpCustomerID)
		}
		if existingCust != "" && existingCust != mercadopago.StubCustomerID {
			cardID, aerr := h.mp.AttachCardToCustomer(ctx, existingCust, req.CardToken)
			if aerr != nil {
				logger.Log.Error().Err(aerr).Msg("activate subscription: attach card")
				RespondWithError(w, ErrCodeInvalidRequest, "No pudimos guardar la tarjeta. Revisá los datos e intentá de nuevo.", http.StatusBadRequest)
				return
			}
			mpCust, mpCard = existingCust, cardID
		} else {
			custID, cardID, serr := h.mp.SaveCard(ctx, email, req.CardToken)
			if serr != nil {
				logger.Log.Error().Err(serr).Msg("activate subscription: save card")
				RespondWithError(w, ErrCodeInvalidRequest, "No pudimos guardar la tarjeta. Revisá los datos e intentá de nuevo.", http.StatusBadRequest)
				return
			}
			mpCust, mpCard = custID, cardID
		}
	case h.signupAllowMock:
		mpCust, mpCard = mercadopago.StubCustomerID, mercadopago.StubCardID
	default:
		RespondWithError(w, ErrCodeInternalError, "Medios de pago no configurados en el servidor.", http.StatusServiceUnavailable)
		return
	}

	paidUntil := now.UTC().AddDate(0, activateSubscriptionPaidMonths, 0)
	if err := h.companyRepo.ActivateSubscription(
		ctx,
		companyID,
		mpCust,
		mpCard,
		req.PlanID,
		maxWarehouses,
		maxUsers,
		documentsMonthlyLimit,
		paidUntil,
	); err != nil {
		logger.Log.Error().Err(err).Msg("activate subscription: update company")
		RespondWithError(w, ErrCodeInternalError, "No se pudo activar la suscripción.", http.StatusInternalServerError)
		return
	}

	RespondWithJSON(w, http.StatusOK, map[string]any{
		"message":                 "Suscripción activada",
		"plan_id":                 req.PlanID,
		"subscription_expires_at": paidUntil.Format(time.RFC3339),
	})
}
