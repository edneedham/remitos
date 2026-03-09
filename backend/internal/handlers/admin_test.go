package handlers

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
	"server/internal/middleware"
	"server/internal/models"
)

func ptrToString(s string) *string {
	return &s
}

type mockUserRepoForAdmin struct {
	users map[string]*models.User
}

func newMockUserRepoForAdmin() *mockUserRepoForAdmin {
	return &mockUserRepoForAdmin{users: make(map[string]*models.User)}
}

func (m *mockUserRepoForAdmin) GetByEmail(email string) (*models.User, error) {
	if user, ok := m.users[email]; ok {
		return user, nil
	}
	return nil, nil
}

func (m *mockUserRepoForAdmin) Create(user *models.User) error {
	if user.Email != nil {
		m.users[*user.Email] = user
	}
	return nil
}

type mockDeviceRepoForAdmin struct{}

func newMockDeviceRepoForAdmin() *mockDeviceRepoForAdmin {
	return &mockDeviceRepoForAdmin{}
}

func TestAdminCreateOperator_MissingEmail(t *testing.T) {
	repo := newMockUserRepoForAdmin()

	body := []byte(`{"password":"password123"}`)
	req := httptest.NewRequest(http.MethodPost, "/operadores", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var req struct {
			Email    string `json:"email"`
			Password string `json:"password"`
		}
		json.NewDecoder(r.Body).Decode(&req)

		if req.Email == "" || req.Password == "" {
			RespondWithError(w, ErrCodeInvalidRequest, "El email y la contraseña son requeridos", http.StatusBadRequest)
			return
		}

		existing, _ := repo.GetByEmail(req.Email)
		if existing != nil {
			RespondWithError(w, ErrCodeConflict, "El usuario ya existe", http.StatusConflict)
			return
		}
	})

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected status 400, got %d", w.Code)
	}
}

func TestAdminCreateOperator_UserAlreadyExists(t *testing.T) {
	repo := newMockUserRepoForAdmin()
	repo.users["existing@test.com"] = &models.User{
		Email: ptrToString("existing@test.com"),
	}

	body := []byte(`{"email":"existing@test.com","password":"password123"}`)
	req := httptest.NewRequest(http.MethodPost, "/operadores", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var req struct {
			Email    string `json:"email"`
			Password string `json:"password"`
		}
		json.NewDecoder(r.Body).Decode(&req)

		existing, _ := repo.GetByEmail(req.Email)
		if existing != nil {
			RespondWithError(w, ErrCodeConflict, "El usuario ya existe", http.StatusConflict)
			return
		}
	})

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusConflict {
		t.Errorf("expected status 409, got %d", w.Code)
	}
}

func TestAdminCreateOperator_Success(t *testing.T) {
	repo := newMockUserRepoForAdmin()

	body := []byte(`{"email":"newoperator@test.com","password":"password123"}`)
	req := httptest.NewRequest(http.MethodPost, "/operadores", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var req struct {
			Email    string `json:"email"`
			Password string `json:"password"`
		}
		json.NewDecoder(r.Body).Decode(&req)

		existing, _ := repo.GetByEmail(req.Email)
		if existing != nil {
			RespondWithError(w, ErrCodeConflict, "El usuario ya existe", http.StatusConflict)
			return
		}

		hash, _ := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
		user := &models.User{
			ID:           uuid.New(),
			Email:        ptrToString(req.Email),
			PasswordHash: string(hash),
			Role:         "operator",
		}
		repo.Create(user)

		RespondWithJSON(w, http.StatusCreated, map[string]string{
			"message": "Operador creado exitosamente",
			"id":      user.ID.String(),
		})
	})

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusCreated {
		t.Errorf("expected status 201, got %d", w.Code)
	}

	var resp map[string]string
	json.Unmarshal(w.Body.Bytes(), &resp)
	if resp["message"] != "Operador creado exitosamente" {
		t.Errorf("expected message, got %s", resp["message"])
	}

	if _, ok := repo.users["newoperator@test.com"]; !ok {
		t.Error("expected operator to be created")
	}
}

func TestAdminCreateOperator_WithoutAuth(t *testing.T) {
	req := httptest.NewRequest(http.MethodPost, "/operadores", nil)
	w := httptest.NewRecorder()

	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, ok := r.Context().Value(middleware.UserContextKey).(middleware.UserClaims)
		if !ok {
			http.Error(w, "Unauthorized", http.StatusUnauthorized)
			return
		}
		w.WriteHeader(http.StatusOK)
	})

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected status 401, got %d", w.Code)
	}
}

func TestAdminCreateOperator_WithAuth(t *testing.T) {
	req := httptest.NewRequest(http.MethodPost, "/operadores", nil)
	ctx := context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{UserID: "123", Role: "admin"})
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		claims, ok := r.Context().Value(middleware.UserContextKey).(middleware.UserClaims)
		if !ok {
			http.Error(w, "Unauthorized", http.StatusUnauthorized)
			return
		}
		if claims.Role != "admin" {
			http.Error(w, "Forbidden", http.StatusForbidden)
			return
		}
		w.WriteHeader(http.StatusOK)
	})

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", w.Code)
	}
}

func TestAdminCreateOperator_ForbiddenRole(t *testing.T) {
	req := httptest.NewRequest(http.MethodPost, "/operadores", nil)
	ctx := context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{UserID: "123", Role: "operator"})
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		claims, ok := r.Context().Value(middleware.UserContextKey).(middleware.UserClaims)
		if !ok {
			http.Error(w, "Unauthorized", http.StatusUnauthorized)
			return
		}
		if claims.Role != "admin" {
			http.Error(w, "Forbidden", http.StatusForbidden)
			return
		}
		w.WriteHeader(http.StatusOK)
	})

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusForbidden {
		t.Errorf("expected status 403, got %d", w.Code)
	}
}
