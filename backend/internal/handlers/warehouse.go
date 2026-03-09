package handlers

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"server/internal/middleware"
	"server/internal/repository"
)

type WarehouseHandler struct {
	warehouseRepo *repository.WarehouseRepository
}

func NewWarehouseHandler(warehouseRepo *repository.WarehouseRepository) *WarehouseHandler {
	return &WarehouseHandler{
		warehouseRepo: warehouseRepo,
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

func (h *WarehouseHandler) Routes() *chi.Mux {
	r := chi.NewRouter()
	r.Get("/", h.List)
	return r
}
