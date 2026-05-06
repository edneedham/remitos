package handlers

import (
	"net/http"

	"server/internal/billing"
	"server/internal/middleware"
)

type planLimitPayload struct {
	MaxWarehouses           *int `json:"max_warehouses,omitempty"`
	MaxUsers                *int `json:"max_users,omitempty"`
	DocumentsMonthlyLimit   *int `json:"documents_monthly_limit,omitempty"`
}

// GetMePlanCatalogLimits returns enforced caps per catalog plan id from billing.PlanLimitsByID.
// Used so the website downgrade preview matches server validation without duplicating numbers.
func (h *AuthHandler) GetMePlanCatalogLimits(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" || claims.CompanyID == "" {
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	if !canAccessWebManagement(claims.Role) {
		RespondWithError(w, r, ErrCodeForbidden, "Este rol no tiene acceso a la administración web", http.StatusForbidden)
		return
	}

	plans := make(map[string]planLimitPayload)
	for _, id := range []string{"pyme", "empresa", "corporativo"} {
		mw, mu, md := billing.PlanLimitsByID(id)
		plans[id] = planLimitPayload{
			MaxWarehouses:         mw,
			MaxUsers:              mu,
			DocumentsMonthlyLimit: md,
		}
	}

	RespondWithJSON(w, http.StatusOK, map[string]any{
		"plans": plans,
	})
}
