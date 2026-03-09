package handlers

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/google/uuid"
	"server/internal/middleware"
	"server/internal/repository"
)

type mockWarehouseRepository struct {
	warehouses []repository.Warehouse
}

func newMockWarehouseRepository() *mockWarehouseRepository {
	companyID := uuid.MustParse("11111111-1111-1111-1111-111111111111")
	warehouse1ID := uuid.MustParse("22222222-2222-2222-2222-222222222222")
	warehouse2ID := uuid.MustParse("33333333-3333-3333-3333-333333333333")

	return &mockWarehouseRepository{
		warehouses: []repository.Warehouse{
			{
				ID:        warehouse1ID,
				CompanyID: companyID,
				Name:      "Main Warehouse",
				Address:   "123 Main St",
			},
			{
				ID:        warehouse2ID,
				CompanyID: companyID,
				Name:      "Secondary Warehouse",
				Address:   "456 Secondary St",
			},
		},
	}
}

func (m *mockWarehouseRepository) GetByCompanyID(ctx context.Context, companyID uuid.UUID) ([]repository.Warehouse, error) {
	return m.warehouses, nil
}

type WarehouseHandlerTestable struct {
	warehouseRepo *mockWarehouseRepository
}

func NewWarehouseHandlerTestable() *WarehouseHandlerTestable {
	return &WarehouseHandlerTestable{warehouseRepo: newMockWarehouseRepository()}
}

func (h *WarehouseHandlerTestable) List(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	userClaims := middleware.GetUserClaims(r)

	companyID, err := uuid.Parse(userClaims.CompanyID)
	if err != nil {
		http.Error(w, "Invalid company ID", http.StatusBadRequest)
		return
	}

	warehouses, err := h.warehouseRepo.GetByCompanyID(ctx, companyID)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(warehouses)
}

func TestWarehouseList_Success(t *testing.T) {
	handler := NewWarehouseHandlerTestable()

	req := httptest.NewRequest(http.MethodGet, "/warehouses", nil)
	ctx := context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{
		UserID:    "123-456",
		CompanyID: "11111111-1111-1111-1111-111111111111",
	})
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	handler.List(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", w.Code)
	}

	var warehouses []repository.Warehouse
	if err := json.Unmarshal(w.Body.Bytes(), &warehouses); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if len(warehouses) != 2 {
		t.Errorf("expected 2 warehouses, got %d", len(warehouses))
	}

	if warehouses[0].Name != "Main Warehouse" {
		t.Errorf("expected first warehouse name 'Main Warehouse', got '%s'", warehouses[0].Name)
	}
}

func TestWarehouseList_InvalidCompanyID(t *testing.T) {
	handler := NewWarehouseHandlerTestable()

	req := httptest.NewRequest(http.MethodGet, "/warehouses", nil)
	ctx := context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{
		UserID:    "123-456",
		CompanyID: "invalid-uuid",
	})
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	handler.List(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected status 400, got %d", w.Code)
	}
}

func TestWarehouseList_EmptyWarehouses(t *testing.T) {
	handler := &WarehouseHandlerTestable{
		warehouseRepo: &mockWarehouseRepository{warehouses: []repository.Warehouse{}},
	}

	req := httptest.NewRequest(http.MethodGet, "/warehouses", nil)
	ctx := context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{
		UserID:    "123-456",
		CompanyID: "11111111-1111-1111-1111-111111111111",
	})
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	handler.List(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", w.Code)
	}

	var warehouses []repository.Warehouse
	if err := json.Unmarshal(w.Body.Bytes(), &warehouses); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if len(warehouses) != 0 {
		t.Errorf("expected 0 warehouses, got %d", len(warehouses))
	}
}
