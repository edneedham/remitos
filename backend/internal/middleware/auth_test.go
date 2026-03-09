package middleware

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	gjwt "github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	srvjwt "server/internal/jwt"
	"server/internal/models"
)

type mockDeviceRepo struct {
	device *models.Device
	err    error
}

func (m *mockDeviceRepo) GetByID(ctx context.Context, deviceID uuid.UUID) (*models.Device, error) {
	if m.err != nil {
		return nil, m.err
	}
	return m.device, nil
}

func generateTestToken(userID uuid.UUID, companyID uuid.UUID, role string, secret string) string {
	claims := srvjwt.Claims{
		UserID:    userID,
		CompanyID: companyID,
		Role:      role,
		RegisteredClaims: gjwt.RegisteredClaims{
			ExpiresAt: gjwt.NewNumericDate(time.Now().Add(time.Hour)),
			IssuedAt:  gjwt.NewNumericDate(time.Now()),
		},
	}
	token := gjwt.NewWithClaims(gjwt.SigningMethodHS256, claims)
	tokenString, _ := token.SignedString([]byte(secret))
	return tokenString
}

func TestAuth_MissingToken(t *testing.T) {
	jwtSvc := srvjwt.NewService("test-secret")
	handler := Auth(AuthDeps{JwtSvc: jwtSvc})(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected status 401, got %d", w.Code)
	}
}

func TestAuth_InvalidToken(t *testing.T) {
	jwtSvc := srvjwt.NewService("test-secret")
	handler := Auth(AuthDeps{JwtSvc: jwtSvc})(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("Authorization", "Bearer invalid-token")
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected status 401, got %d", w.Code)
	}
}

func TestAuth_ValidToken(t *testing.T) {
	jwtSvc := srvjwt.NewService("test-secret")
	userID := uuid.New()
	companyID := uuid.New()
	tokenString := generateTestToken(userID, companyID, "admin", "test-secret")

	handler := Auth(AuthDeps{JwtSvc: jwtSvc})(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		claims := r.Context().Value(UserContextKey).(UserClaims)
		if claims.UserID != userID.String() {
			t.Errorf("expected user_id %s, got %s", userID, claims.UserID)
		}
		if claims.Role != "admin" {
			t.Errorf("expected role admin, got %s", claims.Role)
		}
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("Authorization", "Bearer "+tokenString)
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", w.Code)
	}
}

func TestRequireRole_Forbidden(t *testing.T) {
	handler := RequireRole("admin")(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	ctx := context.WithValue(req.Context(), UserContextKey, UserClaims{UserID: "123", Role: "operator"})
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusForbidden {
		t.Errorf("expected status 403, got %d", w.Code)
	}
}

func TestRequireRole_Allowed(t *testing.T) {
	handler := RequireRole("admin")(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	ctx := context.WithValue(req.Context(), UserContextKey, UserClaims{UserID: "123", Role: "admin"})
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", w.Code)
	}
}

func TestGetUserClaims(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/", nil)
	ctx := context.WithValue(req.Context(), UserContextKey, UserClaims{UserID: "123", CompanyID: "456", Role: "admin"})
	req = req.WithContext(ctx)

	claims := GetUserClaims(req)

	if claims.UserID != "123" {
		t.Errorf("expected user_id 123, got %s", claims.UserID)
	}
	if claims.CompanyID != "456" {
		t.Errorf("expected company_id 456, got %s", claims.CompanyID)
	}
	if claims.Role != "admin" {
		t.Errorf("expected role admin, got %s", claims.Role)
	}
}

func TestGetUserClaims_Empty(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/", nil)
	claims := GetUserClaims(req)

	if claims.UserID != "" {
		t.Errorf("expected empty user_id, got %s", claims.UserID)
	}
}

func TestAuth_DeviceID_InvalidFormat(t *testing.T) {
	jwtSvc := srvjwt.NewService("test-secret")
	userID := uuid.New()
	companyID := uuid.New()
	tokenString := generateTestTokenWithCompany(userID, "admin", companyID, "test-secret")

	handler := Auth(AuthDeps{JwtSvc: jwtSvc})(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("Authorization", "Bearer "+tokenString)
	req.Header.Set("X-Device-ID", "not-a-valid-uuid")
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("expected status 200 (invalid device ID ignored), got %d", w.Code)
	}
}

func TestAuth_DeviceID_ValidMatchingCompany(t *testing.T) {
	jwtSvc := srvjwt.NewService("test-secret")
	userID := uuid.New()
	companyID := uuid.New()
	deviceID := uuid.New()
	tokenString := generateTestTokenWithCompany(userID, "admin", companyID, "test-secret")

	deviceRepo := &mockDeviceRepo{
		device: &models.Device{
			ID:          deviceID,
			CompanyID:   companyID,
			DeviceUUID:  "test-device-uuid",
			Platform:    "android",
			Status:      "active",
			WarehouseID: uuid.New(),
		},
	}

	handler := Auth(AuthDeps{JwtSvc: jwtSvc, DeviceRepo: deviceRepo})(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		claims := r.Context().Value(UserContextKey).(UserClaims)
		if claims.DeviceID != deviceID.String() {
			t.Errorf("expected device_id %s, got %s", deviceID, claims.DeviceID)
		}
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("Authorization", "Bearer "+tokenString)
	req.Header.Set("X-Device-ID", deviceID.String())
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", w.Code)
	}
}

func TestAuth_DeviceID_CompanyMismatch(t *testing.T) {
	jwtSvc := srvjwt.NewService("test-secret")
	userID := uuid.New()
	userCompanyID := uuid.New()
	deviceCompanyID := uuid.New()
	deviceID := uuid.New()
	tokenString := generateTestTokenWithCompany(userID, "admin", userCompanyID, "test-secret")

	deviceRepo := &mockDeviceRepo{
		device: &models.Device{
			ID:          deviceID,
			CompanyID:   deviceCompanyID,
			DeviceUUID:  "test-device-uuid",
			Platform:    "android",
			Status:      "active",
			WarehouseID: uuid.New(),
		},
	}

	handler := Auth(AuthDeps{JwtSvc: jwtSvc, DeviceRepo: deviceRepo})(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	req := httptest.NewRequest(http.MethodGet, "/", nil)
	req.Header.Set("Authorization", "Bearer "+tokenString)
	req.Header.Set("X-Device-ID", deviceID.String())
	w := httptest.NewRecorder()

	handler.ServeHTTP(w, req)

	if w.Code != http.StatusForbidden {
		t.Errorf("expected status 403, got %d", w.Code)
	}
}

func generateTestTokenWithCompany(userID uuid.UUID, role string, companyID uuid.UUID, secret string) string {
	claims := srvjwt.Claims{
		UserID:    userID,
		CompanyID: companyID,
		Role:      role,
		RegisteredClaims: gjwt.RegisteredClaims{
			ExpiresAt: gjwt.NewNumericDate(time.Now().Add(time.Hour)),
			IssuedAt:  gjwt.NewNumericDate(time.Now()),
		},
	}
	token := gjwt.NewWithClaims(gjwt.SigningMethodHS256, claims)
	tokenString, _ := token.SignedString([]byte(secret))
	return tokenString
}
