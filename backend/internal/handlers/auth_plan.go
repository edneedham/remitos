package handlers

import (
	"net/http"

	"github.com/google/uuid"
	"server/internal/billing"
	"server/internal/httputil"
	"server/internal/middleware"
	"server/internal/validation"
)

type SignupPlanSelectionRequest struct {
	PlanID       string `json:"plan_id" validate:"required,oneof=pyme empresa corporativo"`
	PlanName     string `json:"plan_name"`
	MonthlyPrice string `json:"monthly_price"`
	BillingCycle string `json:"billing_cycle"`
	TrialDays    int    `json:"trial_days"`
}

// planLimits delegates to billing.PlanLimitsByID so handlers and the renewal sweep agree.
func planLimits(planID string) (maxWarehouses *int, maxUsers *int, documentsMonthlyLimit *int) {
	return billing.PlanLimitsByID(planID)
}

func (h *AuthHandler) SelectMyPlan(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.CompanyID == "" {
		RespondWithError(w, r, ErrCodeUnauthorized, "Unauthorized", http.StatusUnauthorized)
		return
	}

	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "ID de empresa inválido", http.StatusBadRequest)
		return
	}

	var req SignupPlanSelectionRequest
	if !decodeJSONBody(w, r, httputil.MaxJSONAuthBody, &req) {
		return
	}
	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, r, "Revisá los datos del plan.", fields, http.StatusBadRequest)
		return
	}

	maxWarehouses, maxUsers, documentsMonthlyLimit := planLimits(req.PlanID)
	if err := h.companyRepo.UpdateSignupPlan(
		r.Context(),
		companyID,
		req.PlanID,
		maxWarehouses,
		maxUsers,
		documentsMonthlyLimit,
	); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "No se pudo guardar el plan", http.StatusInternalServerError, err)
		return
	}

	RespondWithJSON(w, http.StatusOK, map[string]any{
		"message":                 "Plan guardado correctamente",
		"plan_id":                 req.PlanID,
		"plan_name":               req.PlanName,
		"monthly_price":           req.MonthlyPrice,
		"billing_cycle":           req.BillingCycle,
		"trial_days":              req.TrialDays,
		"max_warehouses":          maxWarehouses,
		"max_users":               maxUsers,
		"documents_monthly_limit": documentsMonthlyLimit,
	})
}
