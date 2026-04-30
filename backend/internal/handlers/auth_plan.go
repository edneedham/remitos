package handlers

import (
	"encoding/json"
	"net/http"

	"github.com/google/uuid"
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

func planLimits(planID string) (maxWarehouses *int, maxUsers *int, documentsMonthlyLimit *int) {
	switch planID {
	case "pyme":
		w, u, d := 2, 3, 500
		return &w, &u, &d
	case "empresa":
		w, u, d := 3, 10, 10000
		return &w, &u, &d
	case "corporativo":
		return nil, nil, nil
	default:
		return nil, nil, nil
	}
}

func (h *AuthHandler) SelectMyPlan(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.CompanyID == "" {
		RespondWithError(w, ErrCodeUnauthorized, "Unauthorized", http.StatusUnauthorized)
		return
	}

	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "ID de empresa inválido", http.StatusBadRequest)
		return
	}

	var req SignupPlanSelectionRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}
	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, "Revisá los datos del plan.", fields, http.StatusBadRequest)
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
		RespondWithError(w, ErrCodeInternalError, "No se pudo guardar el plan", http.StatusInternalServerError)
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
