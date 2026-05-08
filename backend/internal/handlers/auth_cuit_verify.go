package handlers

import (
	"encoding/json"
	"net/http"
	"strings"

	"github.com/google/uuid"
	"server/internal/billing"
	"server/internal/middleware"
)

type postMeVerifyCUITRequest struct {
	CUIT string `json:"cuit"`
}

// PostMeVerifyCUIT resolves the company CUIT against AFIP padrón and updates companies tax fields.
func (h *AuthHandler) PostMeVerifyCUIT(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" || claims.CompanyID == "" {
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	if !canManageBillingSubscriptions(claims.Role) {
		RespondWithError(w, r, ErrCodeForbidden, "Solo administradores pueden verificar el CUIT", http.StatusForbidden)
		return
	}
	if h.facturaEmitter == nil {
		RespondWithError(w, r, ErrCodeInternalError, "Verificación CUIT no disponible en este servidor", http.StatusServiceUnavailable)
		return
	}

	var req postMeVerifyCUITRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Cuerpo inválido", http.StatusBadRequest)
		return
	}
	cuit := strings.TrimSpace(req.CUIT)
	if cuit == "" {
		RespondWithError(w, r, ErrCodeInvalidRequest, "CUIT requerido", http.StatusBadRequest)
		return
	}

	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}

	co, err := billing.VerifyCompanyCUIT(r.Context(), h.facturaEmitter, companyID, cuit)
	if err != nil {
		msg := err.Error()
		if strings.Contains(msg, "checksum") {
			msg = "El dígito verificador del CUIT no es válido."
		}
		RespondWithError(w, r, ErrCodeInvalidRequest, msg, http.StatusBadRequest, err)
		return
	}
	RespondWithJSON(w, http.StatusOK, map[string]any{
		"cuit":             co.Cuit,
		"razon_social":     co.RazonSocial,
		"condicion_iva":    co.CondicionIVA,
		"domicilio_fiscal": co.DomicilioFiscal,
		"cuit_estado":      co.CuitEstado,
		"cuit_verified_at": co.CuitVerifiedAt,
		"padron_synced_at": co.PadronSyncedAt,
	})
}
