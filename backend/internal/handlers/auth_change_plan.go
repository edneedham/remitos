package handlers

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/google/uuid"
	"server/internal/billing"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	"server/internal/payments/mercadopago"
	"server/internal/validation"
)

type changePlanRequest struct {
	PlanID         string `json:"plan_id" validate:"required,oneof=pyme empresa"`
	CardToken      string `json:"card_token"`
	UseMockPayment bool   `json:"use_mock_payment"`
	// ConfirmDowngrade must be true to schedule a downgrade — guards against accidental
	// switches while the panel preview is still loading.
	ConfirmDowngrade bool `json:"confirm_downgrade"`
}

type changePlanResponse struct {
	Message               string `json:"message"`
	PlanID                string `json:"plan_id"`
	PendingPlan           string `json:"pending_plan,omitempty"`
	SubscriptionExpiresAt string `json:"subscription_expires_at,omitempty"`
	ChargedAmountMinor    int64  `json:"charged_amount_minor,omitempty"`
	Currency              string `json:"currency,omitempty"`
}

// planTier maps a plan id to a numeric tier for upgrade vs downgrade detection.
//
//	0 = trial / unknown / corporativo (corporativo is sales-only here)
//	1 = pyme
//	2 = empresa
func planTier(plan string) int {
	switch strings.ToLower(strings.TrimSpace(plan)) {
	case "pyme":
		return 1
	case "empresa":
		return 2
	default:
		return 0
	}
}

// PostMeChangePlan switches the company between paid catalog plans during an active paid
// period. Upgrades charge a prorated diff via Mercado Pago and apply immediately; downgrades
// are recorded as pending_plan and applied at the next renewal.
func (h *AuthHandler) PostMeChangePlan(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" || claims.CompanyID == "" {
		RespondWithError(w, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	if !canAccessWebManagement(claims.Role) {
		RespondWithError(w, ErrCodeForbidden, "Este rol no tiene acceso a la administración web", http.StatusForbidden)
		return
	}
	if !canManageBillingSubscriptions(claims.Role) {
		RespondWithError(w, ErrCodeForbidden, "Tu rol no puede cambiar el plan ni los cobros.", http.StatusForbidden)
		return
	}

	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}

	var req changePlanRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}
	req.PlanID = strings.ToLower(strings.TrimSpace(req.PlanID))
	req.CardToken = strings.TrimSpace(req.CardToken)
	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, "Revisá los datos del cambio de plan.", fields, http.StatusBadRequest)
		return
	}

	ctx := r.Context()
	company, err := h.companyRepo.GetByIDForBilling(ctx, companyID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("change plan: company")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if company == nil {
		RespondWithError(w, ErrCodeNotFound, "Empresa no encontrada", http.StatusNotFound)
		return
	}

	now := time.Now()
	currentPlan := strings.ToLower(strings.TrimSpace(company.SubscriptionPlan))
	currentTier := planTier(currentPlan)
	newTier := planTier(req.PlanID)
	if newTier == 0 {
		RespondWithError(w, ErrCodeInvalidRequest, "Plan no válido para cambio en línea", http.StatusBadRequest)
		return
	}
	if currentTier == newTier {
		RespondWithError(w, ErrCodeConflict, "Ya estás en ese plan.", http.StatusConflict)
		return
	}

	// Self-serve plan switch is only available for users on a paid period (subscription_expires_at
	// in the future). Trial / lapsed users go through /auth/me/activate-subscription instead.
	if !billing.IsPaidPlan(currentPlan) {
		RespondWithError(
			w,
			ErrCodeConflict,
			"Activá una suscripción paga antes de cambiar de plan.",
			http.StatusConflict,
		)
		return
	}
	if company.SubscriptionExpiresAt == nil || !company.SubscriptionExpiresAt.After(now) {
		RespondWithError(
			w,
			ErrCodeConflict,
			"Tu período pago venció. Reactivá la suscripción antes de cambiar de plan.",
			http.StatusConflict,
		)
		return
	}

	maxWarehouses, maxUsers, documentsMonthlyLimit := planLimits(req.PlanID)

	if newTier < currentTier {
		h.handleDowngrade(w, r, company, req, maxWarehouses, maxUsers, documentsMonthlyLimit)
		return
	}
	h.handleUpgrade(w, r, company, req, now, maxWarehouses, maxUsers, documentsMonthlyLimit)
}

// handleDowngrade validates the target plan can absorb current usage and schedules the
// switch as pending_plan to be applied at the next renewal.
func (h *AuthHandler) handleDowngrade(
	w http.ResponseWriter,
	r *http.Request,
	company *models.Company,
	req changePlanRequest,
	maxWarehouses, maxUsers, documentsMonthlyLimit *int,
) {
	if !req.ConfirmDowngrade {
		RespondWithError(
			w,
			ErrCodeInvalidRequest,
			"Confirmá el cambio antes de programar el descenso de plan.",
			http.StatusBadRequest,
		)
		return
	}
	if err := h.validateDowngradeFits(r.Context(), company.ID, maxWarehouses, maxUsers, documentsMonthlyLimit); err != nil {
		RespondWithError(w, ErrCodeConflict, err.Error(), http.StatusConflict)
		return
	}
	if err := h.companyRepo.SetPendingPlan(r.Context(), company.ID, req.PlanID); err != nil {
		logger.Log.Error().Err(err).Msg("change plan: set pending")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	expiresStr := ""
	if company.SubscriptionExpiresAt != nil {
		expiresStr = company.SubscriptionExpiresAt.UTC().Format(time.RFC3339)
	}
	RespondWithJSON(w, http.StatusOK, changePlanResponse{
		Message:               "Cambio programado. Se aplicará en el próximo período.",
		PlanID:                strings.ToLower(strings.TrimSpace(company.SubscriptionPlan)),
		PendingPlan:           req.PlanID,
		SubscriptionExpiresAt: expiresStr,
	})
}

// validateDowngradeFits returns a 409-friendly error when current usage exceeds the
// caps that would apply on the target plan.
func (h *AuthHandler) validateDowngradeFits(
	ctx context.Context,
	companyID uuid.UUID,
	maxWarehouses, maxUsers, documentsMonthlyLimit *int,
) error {
	if maxWarehouses != nil {
		count, err := h.warehouseRepo.CountByCompanyID(ctx, companyID)
		if err != nil {
			return errors.New("No se pudo validar el uso actual de depósitos.")
		}
		if count > int64(*maxWarehouses) {
			return fmt.Errorf("Tenés %d depósitos activos y el plan elegido permite %d. Archivá depósitos antes de bajar de plan.", count, *maxWarehouses)
		}
	}
	if maxUsers != nil {
		count, err := h.userRepo.CountByCompanyID(ctx, companyID)
		if err != nil {
			return errors.New("No se pudo validar el uso actual de usuarios.")
		}
		if count > int64(*maxUsers) {
			return fmt.Errorf("Tenés %d usuarios y el plan elegido permite %d. Suspendé operadores antes de bajar de plan.", count, *maxUsers)
		}
	}
	if documentsMonthlyLimit != nil {
		mtd, _, err := h.syncRepo.InboundNotesMTDCumulativeSeries(ctx, companyID)
		if err != nil {
			return errors.New("No se pudo validar el uso actual de documentos.")
		}
		if mtd > int64(*documentsMonthlyLimit) {
			return fmt.Errorf("Ya procesaste %d documentos este mes; el plan elegido permite %d/mes. Vas a poder bajar el mes próximo.", mtd, *documentsMonthlyLimit)
		}
	}
	return nil
}

// handleUpgrade computes the prorated diff, charges Mercado Pago, and applies the new
// plan keeping the current subscription_expires_at intact.
func (h *AuthHandler) handleUpgrade(
	w http.ResponseWriter,
	r *http.Request,
	company *models.Company,
	req changePlanRequest,
	now time.Time,
	maxWarehouses, maxUsers, documentsMonthlyLimit *int,
) {
	ctx := r.Context()

	if h.billingRateQuoter == nil {
		RespondWithError(
			w,
			ErrCodeInternalError,
			"Cotización MEP no disponible. Probá más tarde o configurá BILLING_USD_ARS_RATE.",
			http.StatusServiceUnavailable,
		)
		return
	}
	q, err := h.billingRateQuoter.Quote(ctx)
	if err != nil {
		logger.Log.Error().Err(err).Msg("change plan: fx quote")
		RespondWithError(
			w,
			ErrCodeInternalError,
			"No se pudo obtener la cotización MEP. Probá más tarde.",
			http.StatusServiceUnavailable,
		)
		return
	}
	charged := billing.ChargedARSPerUSD(q.SellPerUSD, h.billingFXBufferFraction)
	currentPlan := strings.ToLower(strings.TrimSpace(company.SubscriptionPlan))
	currentMonthlyMinor, err := billing.PlanMonthlyAmountMinorARS(currentPlan, charged)
	if err != nil {
		logger.Log.Error().Err(err).Msg("change plan: current pricing")
		RespondWithError(w, ErrCodeInternalError, "No se pudo calcular el ajuste prorrateado.", http.StatusInternalServerError)
		return
	}
	newMonthlyMinor, err := billing.PlanMonthlyAmountMinorARS(req.PlanID, charged)
	if err != nil {
		logger.Log.Error().Err(err).Msg("change plan: new pricing")
		RespondWithError(w, ErrCodeInternalError, "No se pudo calcular el ajuste prorrateado.", http.StatusInternalServerError)
		return
	}

	breakdown, err := billing.ComputeUpgradeProrationDueMinor(now, *company.SubscriptionExpiresAt, currentMonthlyMinor, newMonthlyMinor)
	if err != nil {
		logger.Log.Error().Err(err).Msg("change plan: proration")
		RespondWithError(w, ErrCodeInternalError, "No se pudo calcular el ajuste prorrateado.", http.StatusInternalServerError)
		return
	}
	if breakdown == nil || breakdown.DueNowMinor <= 0 {
		// Period nearly over or fraction collapses to zero: skip the charge but still apply
		// the new plan immediately for the remainder of the period.
		if err := h.companyRepo.ChangePlan(ctx, company.ID, req.PlanID, maxWarehouses, maxUsers, documentsMonthlyLimit); err != nil {
			logger.Log.Error().Err(err).Msg("change plan: update without charge")
			RespondWithError(w, ErrCodeInternalError, "No se pudo cambiar el plan.", http.StatusInternalServerError)
			return
		}
		RespondWithJSON(w, http.StatusOK, changePlanResponse{
			Message: "Plan cambiado correctamente (sin ajuste prorrateado).",
			PlanID:  req.PlanID,
		})
		return
	}

	custID, cardID, err := h.resolveChangePlanCard(ctx, company, req)
	if err != nil {
		logger.Log.Error().Err(err).Msg("change plan: resolve card")
		RespondWithError(w, ErrCodeInvalidRequest, err.Error(), http.StatusBadRequest)
		return
	}

	payerEmail, err := h.userRepo.GetCompanyOwnerPrimaryEmail(ctx, company.ID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("change plan: payer email")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if payerEmail == "" {
		RespondWithError(w, ErrCodeInvalidRequest, "Falta un email de titular para Mercado Pago.", http.StatusBadRequest)
		return
	}

	description := fmt.Sprintf("Cambio de plan: %s → %s", currentPlan, req.PlanID)

	tx1, err := h.db.Begin(ctx)
	if err != nil {
		logger.Log.Error().Err(err).Msg("change plan: tx begin")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	invoiceID, err := h.invoiceRepo.InsertPending(ctx, tx1, company.ID, breakdown.DueNowMinor, "ARS", description)
	if err != nil {
		_ = tx1.Rollback(ctx)
		logger.Log.Error().Err(err).Msg("change plan: insert invoice")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if err := tx1.Commit(ctx); err != nil {
		logger.Log.Error().Err(err).Msg("change plan: tx commit")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	stub := req.UseMockPayment && h.signupAllowMock
	chargeOut, err := h.mp.ChargeRenewal(ctx, mercadopago.RenewalChargeInput{
		PayerEmail:        payerEmail,
		CustomerID:        custID,
		CardID:            cardID,
		CompanyID:         company.ID.String(),
		AmountARS:         float64(breakdown.DueNowMinor) / 100.0,
		Description:       description,
		ExternalReference: invoiceID.String(),
	}, stub)
	if err != nil || !chargeOut.Approved || chargeOut.PaymentID == "" {
		logger.Log.Error().Err(err).Msg("change plan: charge")
		RespondWithError(
			w,
			ErrCodePaymentRequired,
			"No se pudo cobrar el ajuste con la tarjeta registrada. Probá con una tarjeta nueva.",
			http.StatusPaymentRequired,
		)
		return
	}

	tx2, err := h.db.Begin(ctx)
	if err != nil {
		logger.Log.Error().Err(err).Msg("change plan: tx2 begin")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if _, err := h.invoiceRepo.MarkPaid(ctx, tx2, invoiceID, company.ID, chargeOut.PaymentID); err != nil {
		_ = tx2.Rollback(ctx)
		logger.Log.Error().Err(err).Msg("change plan: mark paid")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if err := tx2.Commit(ctx); err != nil {
		logger.Log.Error().Err(err).Msg("change plan: tx2 commit")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	if err := h.companyRepo.ChangePlan(ctx, company.ID, req.PlanID, maxWarehouses, maxUsers, documentsMonthlyLimit); err != nil {
		logger.Log.Error().Err(err).Msg("change plan: apply")
		RespondWithError(w, ErrCodeInternalError, "El cobro fue exitoso pero no pudimos aplicar el plan. Contactá a soporte.", http.StatusInternalServerError)
		return
	}

	RespondWithJSON(w, http.StatusOK, changePlanResponse{
		Message:               "Plan actualizado. Cobramos el ajuste prorrateado.",
		PlanID:                req.PlanID,
		ChargedAmountMinor:    breakdown.DueNowMinor,
		Currency:              "ARS",
		SubscriptionExpiresAt: company.SubscriptionExpiresAt.UTC().Format(time.RFC3339),
	})
}

// resolveChangePlanCard returns the Mercado Pago customer + card ids to use for the charge.
// Prefers the card already on file; otherwise attaches the freshly tokenized card to the
// existing customer (or returns the stub pair in mock mode).
func (h *AuthHandler) resolveChangePlanCard(
	ctx context.Context,
	company *models.Company,
	req changePlanRequest,
) (string, string, error) {
	mpCust := ""
	mpCard := ""
	if company.MpCustomerID != nil {
		mpCust = strings.TrimSpace(*company.MpCustomerID)
	}
	if company.MpCardID != nil {
		mpCard = strings.TrimSpace(*company.MpCardID)
	}

	stub := req.UseMockPayment && h.signupAllowMock
	switch {
	case stub:
		return mercadopago.StubCustomerID, mercadopago.StubCardID, nil
	case mpCust != "" && mpCust != mercadopago.StubCustomerID && mpCard != "" && req.CardToken == "":
		return mpCust, mpCard, nil
	case h.mp.HasAccessToken() && req.CardToken != "":
		if mpCust == "" || mpCust == mercadopago.StubCustomerID {
			return "", "", errors.New("No tenés un cliente de Mercado Pago asociado. Probá activar la suscripción primero.")
		}
		newCard, err := h.mp.AttachCardToCustomer(ctx, mpCust, req.CardToken)
		if err != nil {
			return "", "", err
		}
		return mpCust, newCard, nil
	default:
		return "", "", errors.New("Falta un medio de pago. Cargá una tarjeta antes de cambiar de plan.")
	}
}
