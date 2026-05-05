package handlers

import (
	"encoding/json"
	"net/http"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"server/internal/jwt"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	"server/internal/repository"
	"server/internal/validation"
)

type WarehouseHandler struct {
	warehouseRepo *repository.WarehouseRepository
	companyRepo   *repository.CompanyRepository
	deviceRepo    *repository.DeviceRepository
	jwtSvc        *jwt.Service
}

func NewWarehouseHandler(
	warehouseRepo *repository.WarehouseRepository,
	companyRepo *repository.CompanyRepository,
	deviceRepo *repository.DeviceRepository,
	jwtSvc *jwt.Service,
) *WarehouseHandler {
	return &WarehouseHandler{
		warehouseRepo: warehouseRepo,
		companyRepo:   companyRepo,
		deviceRepo:    deviceRepo,
		jwtSvc:        jwtSvc,
	}
}

func (h *WarehouseHandler) List(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	var companyID uuid.UUID
	var err error

	// Check for company_code query param (used during device setup before auth)
	if companyCode := r.URL.Query().Get("company_code"); companyCode != "" {
		// Look up company by code
		company, err := h.warehouseRepo.GetCompanyByCode(ctx, companyCode)
		if err != nil || company == nil {
			RespondWithError(w, ErrCodeInvalidRequest, "Company not found", http.StatusBadRequest)
			return
		}
		companyID = company.ID
	} else {
		// Use auth context
		userClaims := middleware.GetUserClaims(r)
		if userClaims.UserID == "" {
			RespondWithError(w, ErrCodeUnauthorized, "Unauthorized", http.StatusUnauthorized)
			return
		}
		companyID, err = uuid.Parse(userClaims.CompanyID)
		if err != nil {
			RespondWithError(w, ErrCodeInvalidRequest, "Company ID inválido", http.StatusBadRequest)
			return
		}
	}

	warehouses, err := h.warehouseRepo.GetByCompanyID(ctx, companyID)
	if err != nil {
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	if warehouses == nil {
		warehouses = []repository.Warehouse{}
	}

	RespondWithJSON(w, http.StatusOK, warehouses)
}

type warehouseWriteRequest struct {
	Name    string `json:"name" validate:"required,min=1,max=100"`
	Address string `json:"address" validate:"omitempty,max=500"`
}

// reachedWarehouseCap returns true when the company has reached its plan-defined
// max_warehouses. Returns false when max is nil (unlimited / unset).
func reachedWarehouseCap(currentCount int64, max *int) bool {
	if max == nil {
		return false
	}
	return currentCount >= int64(*max)
}

// archiveGuard captures the inputs needed to refuse archiving a warehouse.
type archiveGuard struct {
	totalActive   int64
	activeDevices int64
}

// canArchiveWarehouse refuses the last active warehouse (no orphaned company) and
// refuses while devices are still actively syncing under it.
func canArchiveWarehouse(c archiveGuard) (bool, string) {
	if c.totalActive <= 1 {
		return false, "last_warehouse"
	}
	if c.activeDevices > 0 {
		return false, "active_devices"
	}
	return true, ""
}

func normalizeWarehouseWriteRequest(req *warehouseWriteRequest) {
	req.Name = strings.TrimSpace(req.Name)
	req.Address = strings.TrimSpace(req.Address)
}

// Create adds a new warehouse for the authenticated user's company. Refuses with 403
// when the company has reached its plan-defined max_warehouses.
func (h *WarehouseHandler) Create(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
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

	var req warehouseWriteRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}
	normalizeWarehouseWriteRequest(&req)
	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, "Revisá los datos del depósito.", fields, http.StatusBadRequest)
		return
	}

	company, err := h.companyRepo.GetByIDForBilling(ctx, companyID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("warehouse create: company")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if company == nil {
		RespondWithError(w, ErrCodeNotFound, "Empresa no encontrada", http.StatusNotFound)
		return
	}

	if company.MaxWarehouses != nil {
		count, cerr := h.warehouseRepo.CountByCompanyID(ctx, companyID)
		if cerr != nil {
			logger.Log.Error().Err(cerr).Msg("warehouse create: count")
			RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
			return
		}
		if reachedWarehouseCap(count, company.MaxWarehouses) {
			RespondWithError(
				w,
				ErrCodeForbidden,
				"Alcanzaste el límite de depósitos de tu plan.",
				http.StatusForbidden,
			)
			return
		}
	}

	now := time.Now()
	warehouse := &repository.Warehouse{
		ID:        uuid.New(),
		CompanyID: companyID,
		Name:      req.Name,
		Address:   req.Address,
		CreatedAt: now,
		UpdatedAt: now,
	}
	if err := h.warehouseRepo.Create(ctx, warehouse); err != nil {
		logger.Log.Error().Err(err).Msg("warehouse create: insert")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	RespondWithJSON(w, http.StatusCreated, warehouse)
}

// Update renames or changes the address of an existing (non-archived) warehouse owned by
// the authenticated user's company.
func (h *WarehouseHandler) Update(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
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

	id, err := uuid.Parse(chi.URLParam(r, "id"))
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "ID de depósito inválido", http.StatusBadRequest)
		return
	}

	var req warehouseWriteRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}
	normalizeWarehouseWriteRequest(&req)
	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, "Revisá los datos del depósito.", fields, http.StatusBadRequest)
		return
	}

	updated, err := h.warehouseRepo.Update(ctx, id, companyID, req.Name, req.Address)
	if err != nil {
		logger.Log.Error().Err(err).Msg("warehouse update")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if !updated {
		RespondWithError(w, ErrCodeNotFound, "Depósito no encontrado", http.StatusNotFound)
		return
	}

	RespondWithJSON(w, http.StatusOK, map[string]string{
		"message": "Depósito actualizado correctamente",
	})
}

// Archive soft-deletes a warehouse. Refuses when this would leave the company with no
// warehouses, or when the warehouse still has active devices.
func (h *WarehouseHandler) Archive(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
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

	id, err := uuid.Parse(chi.URLParam(r, "id"))
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "ID de depósito inválido", http.StatusBadRequest)
		return
	}

	count, err := h.warehouseRepo.CountByCompanyID(ctx, companyID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("warehouse archive: count")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	activeDevices, err := h.warehouseRepo.CountActiveDevicesByWarehouseID(ctx, id)
	if err != nil {
		logger.Log.Error().Err(err).Msg("warehouse archive: active devices")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if ok, reason := canArchiveWarehouse(archiveGuard{totalActive: count, activeDevices: activeDevices}); !ok {
		switch reason {
		case "last_warehouse":
			RespondWithError(
				w,
				ErrCodeConflict,
				"No podés archivar el último depósito activo. Crea otro depósito antes de archivar este.",
				http.StatusConflict,
			)
		default:
			RespondWithError(
				w,
				ErrCodeConflict,
				"Este depósito tiene dispositivos activos. Revocá los dispositivos antes de archivar el depósito.",
				http.StatusConflict,
			)
		}
		return
	}

	archived, err := h.warehouseRepo.Archive(ctx, id, companyID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("warehouse archive")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if !archived {
		RespondWithError(w, ErrCodeNotFound, "Depósito no encontrado", http.StatusNotFound)
		return
	}

	RespondWithJSON(w, http.StatusOK, map[string]string{
		"message": "Depósito archivado correctamente",
	})
}

func (h *WarehouseHandler) Routes() *chi.Mux {
	r := chi.NewRouter()
	r.Get("/", h.List)
	r.Group(func(r chi.Router) {
		r.Use(middleware.Auth(middleware.AuthDeps{JwtSvc: h.jwtSvc, DeviceRepo: h.deviceRepo}))
		r.Use(middleware.RequireRoles(models.RoleCompanyOwner, models.RoleWarehouseAdmin))
		r.Post("/", h.Create)
		r.Patch("/{id}", h.Update)
		r.Delete("/{id}", h.Archive)
	})
	return r
}
