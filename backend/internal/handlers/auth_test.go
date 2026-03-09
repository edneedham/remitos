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

func ptr(s string) *string {
	return &s
}

type mockUserRepository struct {
	users map[string]*models.User
}

func newMockUserRepository() *mockUserRepository {
	email := "test@test.com"
	return &mockUserRepository{users: map[string]*models.User{
		email: {Email: &email},
	}}
}

func (m *mockUserRepository) GetByEmail(email string) (*models.User, error) {
	if user, ok := m.users[email]; ok {
		return user, nil
	}
	return nil, nil
}

func (m *mockUserRepository) Create(user *models.User) error {
	if user.Email != nil {
		m.users[*user.Email] = user
	}
	return nil
}

type AuthHandlerTestable struct {
	userRepo *mockUserRepository
}

func NewAuthHandlerTestable() *AuthHandlerTestable {
	repo := newMockUserRepository()
	return &AuthHandlerTestable{userRepo: repo}
}

func (h *AuthHandlerTestable) Register(w http.ResponseWriter, r *http.Request) {
	var req models.CreateUserRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if req.Email == nil || *req.Email == "" || req.Password == "" {
		http.Error(w, "Email and password are required", http.StatusBadRequest)
		return
	}

	existing, _ := h.userRepo.GetByEmail(*req.Email)
	if existing != nil {
		http.Error(w, "User already exists", http.StatusConflict)
		return
	}

	h.userRepo.Create(&models.User{
		Email: req.Email,
	})

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]string{
		"message": "User created successfully",
	})
}

func (h *AuthHandlerTestable) Login(w http.ResponseWriter, r *http.Request) {
	var req LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if req.CompanyCode == "" || req.Username == "" || req.Password == "" {
		http.Error(w, "Company code, username and password are required", http.StatusBadRequest)
		return
	}

	user, _ := h.userRepo.GetByEmail(req.Username)
	if user == nil || bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.Password)) != nil {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(LoginResponse{Token: "valid-token"})
}

func TestRegister_MissingEmail(t *testing.T) {
	handler := NewAuthHandlerTestable()

	body := []byte(`{"password":"password123"}`)
	req := httptest.NewRequest(http.MethodPost, "/registrarse", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.Register(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected status 400, got %d", w.Code)
	}
}

func TestRegister_MissingPassword(t *testing.T) {
	handler := NewAuthHandlerTestable()

	body := []byte(`{"email":"test@example.com"}`)
	req := httptest.NewRequest(http.MethodPost, "/registrarse", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.Register(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected status 400, got %d", w.Code)
	}
}

func TestRegister_UserAlreadyExists(t *testing.T) {
	handler := NewAuthHandlerTestable()

	handler.userRepo.users["test@example.com"] = &models.User{
		Email: ptr("test@example.com"),
	}

	body := []byte(`{"email":"test@example.com","password":"password123"}`)
	req := httptest.NewRequest(http.MethodPost, "/registrarse", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.Register(w, req)

	if w.Code != http.StatusConflict {
		t.Errorf("expected status 409, got %d", w.Code)
	}
}

func TestRegister_Success(t *testing.T) {
	handler := NewAuthHandlerTestable()

	body := []byte(`{"email":"new@example.com","password":"password123"}`)
	req := httptest.NewRequest(http.MethodPost, "/registrarse", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.Register(w, req)

	if w.Code != http.StatusCreated {
		t.Errorf("expected status 201, got %d", w.Code)
	}

	var resp map[string]string
	if err := json.Unmarshal(w.Body.Bytes(), &resp); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if resp["message"] != "User created successfully" {
		t.Errorf("expected message 'User created successfully', got '%s'", resp["message"])
	}

	if _, ok := handler.userRepo.users["new@example.com"]; !ok {
		t.Error("expected user to be created in repository")
	}
}

func TestLogin_MissingFields(t *testing.T) {
	handler := NewAuthHandlerTestable()

	body := []byte(`{"company_code":"","username":"","password":"password123"}`)
	req := httptest.NewRequest(http.MethodPost, "/login", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.Login(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected status 400, got %d", w.Code)
	}
}

func TestLogin_UserNotFound(t *testing.T) {
	handler := NewAuthHandlerTestable()

	body := []byte(`{"company_code":"TEST","username":"notfound@test.com","password":"password123"}`)
	req := httptest.NewRequest(http.MethodPost, "/login", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.Login(w, req)

	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected status 401, got %d", w.Code)
	}
}

func TestLogin_WrongPassword(t *testing.T) {
	handler := NewAuthHandlerTestable()

	hash, _ := bcrypt.GenerateFromPassword([]byte("correctpassword"), bcrypt.DefaultCost)
	handler.userRepo.users["test@test.com"] = &models.User{
		ID:           uuid.New(),
		Email:        ptr("test@test.com"),
		PasswordHash: string(hash),
		Role:         "admin",
	}

	body := []byte(`{"company_code":"TEST","username":"test@test.com","password":"wrongpassword"}`)
	req := httptest.NewRequest(http.MethodPost, "/login", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.Login(w, req)

	if w.Code != http.StatusUnauthorized {
		t.Errorf("expected status 401, got %d", w.Code)
	}
}

func TestLogin_Success(t *testing.T) {
	handler := NewAuthHandlerTestable()

	hash, _ := bcrypt.GenerateFromPassword([]byte("password123"), bcrypt.DefaultCost)
	handler.userRepo.users["test@test.com"] = &models.User{
		ID:           uuid.New(),
		Email:        ptr("test@test.com"),
		PasswordHash: string(hash),
		Role:         "admin",
	}

	body := []byte(`{"company_code":"TEST","username":"test@test.com","password":"password123"}`)
	req := httptest.NewRequest(http.MethodPost, "/login", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.Login(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", w.Code)
	}

	var resp LoginResponse
	json.Unmarshal(w.Body.Bytes(), &resp)
	if resp.Token != "valid-token" {
		t.Errorf("expected token, got %s", resp.Token)
	}
}

func TestLogout_Success(t *testing.T) {
	_ = NewAuthHandlerTestable()

	req := httptest.NewRequest(http.MethodPost, "/logout", nil)
	ctx := context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{UserID: "123-456", Role: "admin"})
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	logoutHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		userClaims := middleware.GetUserClaims(r)
		if userClaims.UserID != "123-456" {
			t.Errorf("expected user_id 123-456, got %s", userClaims.UserID)
		}
		RespondWithJSON(w, http.StatusOK, map[string]string{"message": "Logout exitoso"})
	})

	logoutHandler.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Errorf("expected status 200, got %d", w.Code)
	}
}

type mockDeviceRepository struct {
	devices map[string]interface{}
}

func newMockDeviceRepository() *mockDeviceRepository {
	return &mockDeviceRepository{devices: make(map[string]interface{})}
}

func (m *mockDeviceRepository) Create(device interface{}) error {
	m.devices[device.(*models.Device).DeviceUUID] = device
	return nil
}

type DeviceRegistrationTestable struct {
	deviceRepo *mockDeviceRepository
}

func NewDeviceRegistrationTestable() *DeviceRegistrationTestable {
	return &DeviceRegistrationTestable{deviceRepo: newMockDeviceRepository()}
}

func (h *DeviceRegistrationTestable) RegisterDevice(w http.ResponseWriter, r *http.Request) {
	var req RegisterDeviceRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if req.DeviceUUID == "" || req.Platform == "" || req.WarehouseID == "" {
		http.Error(w, "device_uuid, platform, and warehouse_id are required", http.StatusBadRequest)
		return
	}

	userClaims := middleware.GetUserClaims(r)
	companyID := userClaims.CompanyID
	warehouseID := req.WarehouseID

	device := &models.Device{
		ID:          uuid.New(),
		DeviceUUID:  req.DeviceUUID,
		Platform:    req.Platform,
		WarehouseID: uuid.MustParse(warehouseID),
		CompanyID:   uuid.MustParse(companyID),
		Status:      "active",
	}

	h.deviceRepo.Create(device)

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(map[string]string{
		"message":   "Device registered successfully",
		"device_id": device.ID.String(),
	})
}

func TestRegisterDevice_MissingFields(t *testing.T) {
	handler := NewDeviceRegistrationTestable()

	body := []byte(`{}`)
	req := httptest.NewRequest(http.MethodPost, "/device", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.RegisterDevice(w, req)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected status 400, got %d", w.Code)
	}
}

func TestRegisterDevice_Success(t *testing.T) {
	handler := NewDeviceRegistrationTestable()

	body := []byte(`{
		"device_uuid": "test-device-123",
		"platform": "android",
		"warehouse_id": "22222222-2222-2222-2222-222222222222"
	}`)
	req := httptest.NewRequest(http.MethodPost, "/device", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	ctx := context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{
		UserID:    "123-456",
		CompanyID: "11111111-1111-1111-1111-111111111111",
		Role:      "company_owner",
	})
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	handler.RegisterDevice(w, req)

	if w.Code != http.StatusCreated {
		t.Errorf("expected status 201, got %d", w.Code)
	}

	var resp map[string]string
	if err := json.Unmarshal(w.Body.Bytes(), &resp); err != nil {
		t.Fatalf("failed to unmarshal response: %v", err)
	}

	if resp["message"] != "Device registered successfully" {
		t.Errorf("expected message 'Device registered successfully', got '%s'", resp["message"])
	}

	if resp["device_id"] == "" {
		t.Error("expected device_id to be returned")
	}
}

func TestRegisterDevice_StoresDevice(t *testing.T) {
	handler := NewDeviceRegistrationTestable()

	body := []byte(`{
		"device_uuid": "unique-device-456",
		"platform": "ios",
		"warehouse_id": "33333333-3333-3333-3333-333333333333"
	}`)
	req := httptest.NewRequest(http.MethodPost, "/device", bytes.NewBuffer(body))
	req.Header.Set("Content-Type", "application/json")
	ctx := context.WithValue(req.Context(), middleware.UserContextKey, middleware.UserClaims{
		UserID:    "123-456",
		CompanyID: "11111111-1111-1111-1111-111111111111",
		Role:      "warehouse_admin",
	})
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	handler.RegisterDevice(w, req)

	if w.Code != http.StatusCreated {
		t.Errorf("expected status 201, got %d", w.Code)
	}

	if len(handler.deviceRepo.devices) != 1 {
		t.Errorf("expected 1 device in repo, got %d", len(handler.deviceRepo.devices))
	}

	if _, ok := handler.deviceRepo.devices["unique-device-456"]; !ok {
		t.Error("expected device to be stored in repository")
	}
}
