package handlers

import (
	"encoding/json"
	"net/http"
	"strings"

	"github.com/google/uuid"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/payments/mercadopago"
)

type updatePaymentMethodRequest struct {
	CardToken      string `json:"card_token"`
	UseMockPayment bool   `json:"use_mock_payment"`
}

// PostMeUpdatePaymentMethod attaches a new Mercado Pago card and updates the company record.
// Does not change subscription_plan or subscription_expires_at.
func (h *AuthHandler) PostMeUpdatePaymentMethod(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" || claims.CompanyID == "" {
		RespondWithError(w, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	if !canManageBillingSubscriptions(claims.Role) {
		RespondWithError(w, ErrCodeForbidden, "Tu rol no puede actualizar medios de pago.", http.StatusForbidden)
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

	var req updatePaymentMethodRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}
	req.CardToken = strings.TrimSpace(req.CardToken)

	if req.UseMockPayment && !h.signupAllowMock {
		RespondWithError(w, ErrCodeInvalidRequest, "Pago simulado no habilitado en este servidor.", http.StatusBadRequest)
		return
	}
	if !req.UseMockPayment && req.CardToken == "" {
		RespondWithError(w, ErrCodeInvalidRequest, "Falta el token de la tarjeta.", http.StatusBadRequest)
		return
	}

	ctx := r.Context()
	company, err := h.companyRepo.GetByIDForBilling(ctx, companyID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("update payment method: company")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if company == nil {
		RespondWithError(w, ErrCodeNotFound, "Empresa no encontrada", http.StatusNotFound)
		return
	}

	user, err := h.userRepo.GetByID(ctx, userID)
	if err != nil || user == nil {
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
				logger.Log.Error().Err(aerr).Msg("update payment method: attach card")
				RespondWithError(w, ErrCodeInvalidRequest, "No pudimos guardar la tarjeta. Revisá los datos e intentá de nuevo.", http.StatusBadRequest)
				return
			}
			mpCust, mpCard = existingCust, cardID
		} else {
			custID, cardID, serr := h.mp.SaveCard(ctx, email, req.CardToken)
			if serr != nil {
				logger.Log.Error().Err(serr).Msg("update payment method: save card")
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

	if err := h.companyRepo.UpdateMercadoPagoPaymentMethod(ctx, companyID, mpCust, mpCard); err != nil {
		logger.Log.Error().Err(err).Msg("update payment method: persist")
		RespondWithError(w, ErrCodeInternalError, "No se pudo guardar el medio de pago.", http.StatusInternalServerError)
		return
	}

	RespondWithJSON(w, http.StatusOK, map[string]string{
		"message": "Medio de pago actualizado correctamente.",
	})
}
