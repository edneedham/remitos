package handlers

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"server/internal/jwt"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	"server/internal/repository"
)

// DeviceHandler exposes panel-only device management routes (list / revoke / reactivate).
// Mobile device registration lives on the AuthHandler (POST /auth/device).
type DeviceHandler struct {
	deviceRepo *repository.DeviceRepository
	jwtSvc     *jwt.Service
}

func NewDeviceHandler(deviceRepo *repository.DeviceRepository, jwtSvc *jwt.Service) *DeviceHandler {
	return &DeviceHandler{
		deviceRepo: deviceRepo,
		jwtSvc:     jwtSvc,
	}
}

// List returns all devices for the authenticated user's company, joined to warehouse name.
func (h *DeviceHandler) List(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.CompanyID == "" {
		RespondWithError(w, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}

	devices, err := h.deviceRepo.ListByCompanyWithWarehouseName(r.Context(), companyID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("device list")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if devices == nil {
		devices = []repository.DeviceWithWarehouse{}
	}
	RespondWithJSON(w, http.StatusOK, devices)
}

// Revoke marks the device as revoked. The Android app polls /auth/user/status and signs out
// the operator when its device is revoked.
func (h *DeviceHandler) Revoke(w http.ResponseWriter, r *http.Request) {
	h.setStatus(w, r, "revoked", "Dispositivo revocado correctamente")
}

// Reactivate flips a previously revoked device back to active so it can sync again.
func (h *DeviceHandler) Reactivate(w http.ResponseWriter, r *http.Request) {
	h.setStatus(w, r, "active", "Dispositivo reactivado correctamente")
}

func (h *DeviceHandler) setStatus(w http.ResponseWriter, r *http.Request, status, okMessage string) {
	claims := middleware.GetUserClaims(r)
	if claims.CompanyID == "" {
		RespondWithError(w, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}
	deviceID, err := uuid.Parse(chi.URLParam(r, "id"))
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "ID de dispositivo inválido", http.StatusBadRequest)
		return
	}

	updated, err := h.deviceRepo.SetDeviceStatusForCompany(r.Context(), companyID, deviceID, status)
	if err != nil {
		logger.Log.Error().Err(err).Msg("device status update")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if !updated {
		RespondWithError(w, ErrCodeNotFound, "Dispositivo no encontrado", http.StatusNotFound)
		return
	}
	RespondWithJSON(w, http.StatusOK, map[string]string{
		"message": okMessage,
	})
}

func (h *DeviceHandler) Routes() *chi.Mux {
	r := chi.NewRouter()
	r.Use(middleware.Auth(middleware.AuthDeps{JwtSvc: h.jwtSvc, DeviceRepo: h.deviceRepo}))
	r.Use(middleware.RequireRoles(models.RoleCompanyOwner, models.RoleWarehouseAdmin))
	r.Get("/", h.List)
	r.Patch("/{id}/revoke", h.Revoke)
	r.Patch("/{id}/reactivate", h.Reactivate)
	return r
}
