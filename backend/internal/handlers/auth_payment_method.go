package handlers

import (
	"net/http"
	"strings"

	"github.com/google/uuid"
	"server/internal/httputil"
	"server/internal/middleware"
	"server/internal/notifications/inapp"
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
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	if !canManageBillingSubscriptions(claims.Role) {
		RespondWithError(w, r, ErrCodeForbidden, "Tu rol no puede actualizar medios de pago.", http.StatusForbidden)
		return
	}

	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}
	userID, err := uuid.Parse(claims.UserID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Usuario inválido", http.StatusBadRequest)
		return
	}

	var req updatePaymentMethodRequest
	if !decodeJSONBody(w, r, httputil.MaxJSONAuthBody, &req) {
		return
	}
	req.CardToken = strings.TrimSpace(req.CardToken)

	if req.UseMockPayment && !h.signupAllowMock {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Pago simulado no habilitado en este servidor.", http.StatusBadRequest)
		return
	}
	if !req.UseMockPayment && req.CardToken == "" {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Falta el token de la tarjeta.", http.StatusBadRequest)
		return
	}

	ctx := r.Context()
	company, err := h.companyRepo.GetByIDForBilling(ctx, companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if company == nil {
		RespondWithError(w, r, ErrCodeNotFound, "Empresa no encontrada", http.StatusNotFound)
		return
	}

	user, err := h.userRepo.GetByID(ctx, userID)
	if err != nil || user == nil {
		RespondWithError(w, r, ErrCodeNotFound, "Usuario no encontrado", http.StatusNotFound)
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
		RespondWithError(w, r, ErrCodeInvalidRequest, "Falta un email de titular para Mercado Pago.", http.StatusBadRequest)
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
				RespondWithError(w, r, ErrCodeInvalidRequest, "No pudimos guardar la tarjeta. Revisá los datos e intentá de nuevo.", http.StatusBadRequest, aerr)
				return
			}
			mpCust, mpCard = existingCust, cardID
		} else {
			custID, cardID, serr := h.mp.SaveCard(ctx, email, req.CardToken)
			if serr != nil {
				RespondWithError(w, r, ErrCodeInvalidRequest, "No pudimos guardar la tarjeta. Revisá los datos e intentá de nuevo.", http.StatusBadRequest, serr)
				return
			}
			mpCust, mpCard = custID, cardID
		}
	case h.signupAllowMock:
		mpCust, mpCard = mercadopago.StubCustomerID, mercadopago.StubCardID
	default:
		RespondWithError(w, r, ErrCodeInternalError, "Medios de pago no configurados en el servidor.", http.StatusServiceUnavailable)
		return
	}

	if err := h.companyRepo.UpdateMercadoPagoPaymentMethod(ctx, companyID, mpCust, mpCard); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "No se pudo guardar el medio de pago.", http.StatusInternalServerError, err)
		return
	}

	if bc := inapp.NewBroadcaster(h.notificationRepo, h.userRepo, h.publicSiteURL); bc != nil {
		bc.PaymentMethodUpdated(ctx, companyID)
	}

	RespondWithJSON(w, http.StatusOK, map[string]string{
		"message": "Medio de pago actualizado correctamente.",
	})
}
