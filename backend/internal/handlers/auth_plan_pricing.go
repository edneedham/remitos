package handlers

import (
	"net/http"
	"strings"

	"server/internal/billing"
	"server/internal/logger"
	"server/internal/middleware"
)

type planPricingResponse struct {
	PlanID            string  `json:"plan_id"`
	Currency          string  `json:"currency"`
	AmountMinor       int64   `json:"amount_minor"`
	MonthlyListUSD    float64 `json:"monthly_list_usd"`
	ARSPerUSD         float64 `json:"ars_per_usd"` // ARS per 1 USD (MEP venta or fallback)
	FxSource          string  `json:"fx_source"`
	FxEffectiveDate   string  `json:"fx_effective_date,omitempty"`
	LegalNoticeAR     string  `json:"legal_notice_ar"`
}

// GetMePlanPricing returns the ARS invoice amount (centavos) for a catalog plan using the MEP (bolsa) rate.
func (h *AuthHandler) GetMePlanPricing(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" || claims.CompanyID == "" {
		RespondWithError(w, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	if !canAccessWebManagement(claims.Role) {
		RespondWithError(w, ErrCodeForbidden, "Este rol no tiene acceso a la administración web", http.StatusForbidden)
		return
	}

	if h.billingRateQuoter == nil {
		RespondWithError(
			w,
			ErrCodeInternalError,
			"Cotización MEP no disponible (configurá BILLING_USD_ARS_RATE como respaldo o revisá la conectividad).",
			http.StatusServiceUnavailable,
		)
		return
	}

	planID := strings.ToLower(strings.TrimSpace(r.URL.Query().Get("plan_id")))
	if planID != "pyme" && planID != "empresa" {
		RespondWithError(w, ErrCodeInvalidRequest, "plan_id debe ser pyme o empresa", http.StatusBadRequest)
		return
	}

	usd, ok := billing.MonthlyListUSD(planID)
	if !ok {
		logger.Log.Error().Str("plan_id", planID).Msg("plan pricing: unknown plan")
		RespondWithError(w, ErrCodeInternalError, "Plan no facturable", http.StatusInternalServerError)
		return
	}

	q, err := h.billingRateQuoter.Quote(r.Context())
	if err != nil {
		logger.Log.Error().Err(err).Msg("plan pricing: fx quote")
		RespondWithError(
			w,
			ErrCodeInternalError,
			"No se pudo obtener la cotización MEP. Probá más tarde o configurá BILLING_USD_ARS_RATE como respaldo.",
			http.StatusServiceUnavailable,
		)
		return
	}

	minor, err := billing.PlanMonthlyAmountMinorARS(planID, q.SellPerUSD)
	if err != nil {
		logger.Log.Error().Err(err).Str("plan_id", planID).Msg("plan pricing: compute")
		RespondWithError(w, ErrCodeInternalError, "No se pudo calcular el importe", http.StatusInternalServerError)
		return
	}

	fxDate := ""
	if !q.EffectiveDate.IsZero() {
		fxDate = q.EffectiveDate.Format("2006-01-02")
	}

	RespondWithJSON(w, http.StatusOK, planPricingResponse{
		PlanID:          planID,
		Currency:        "ARS",
		AmountMinor:     minor,
		MonthlyListUSD:  usd,
		ARSPerUSD:       q.SellPerUSD,
		FxSource:        q.Source,
		FxEffectiveDate: fxDate,
		LegalNoticeAR:   billing.LegalNoticeContractUSDARSMEPTemplate,
	})
}
