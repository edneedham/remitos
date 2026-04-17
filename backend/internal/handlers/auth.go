package handlers

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/bcrypt"
	"server/internal/jwt"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	"server/internal/payments/mercadopago"
	"server/internal/repository"
	"server/internal/validation"
)

type AuthHandler struct {
	userRepo         *repository.UserRepository
	companyRepo      *repository.CompanyRepository
	warehouseRepo    *repository.WarehouseRepository
	deviceRepo       *repository.DeviceRepository
	refreshTokenRepo *repository.RefreshTokenRepository
	subscriptionRepo *repository.SubscriptionRepository
	db               *pgxpool.Pool
	jwtSvc           *jwt.Service
	mp               *mercadopago.Client
	signupAllowMock  bool
}

func NewAuthHandler(userRepo *repository.UserRepository, companyRepo *repository.CompanyRepository, warehouseRepo *repository.WarehouseRepository, deviceRepo *repository.DeviceRepository, refreshTokenRepo *repository.RefreshTokenRepository, subscriptionRepo *repository.SubscriptionRepository, db *pgxpool.Pool, jwtSvc *jwt.Service, mp *mercadopago.Client, signupAllowMock bool) *AuthHandler {
	return &AuthHandler{
		userRepo:         userRepo,
		companyRepo:      companyRepo,
		warehouseRepo:    warehouseRepo,
		deviceRepo:       deviceRepo,
		refreshTokenRepo: refreshTokenRepo,
		subscriptionRepo: subscriptionRepo,
		db:               db,
		jwtSvc:           jwtSvc,
		mp:               mp,
		signupAllowMock:  signupAllowMock,
	}
}

type LoginRequest struct {
	CompanyCode string `json:"company_code" validate:"required"`
	Username    string `json:"username" validate:"required"`
	Password    string `json:"password" validate:"required"`
	DeviceName  string `json:"device_name" validate:"omitempty"`
}

type RegisterDeviceRequest struct {
	DeviceUUID  string  `json:"device_uuid" validate:"required"`
	Platform    string  `json:"platform" validate:"required"`
	WarehouseID string  `json:"warehouse_id" validate:"required"`
	Model       *string `json:"model,omitempty"`
	OSVersion   *string `json:"os_version,omitempty"`
	AppVersion  *string `json:"app_version,omitempty"`
	DeviceName  string  `json:"device_name" validate:"omitempty"`
	Username    string  `json:"username" validate:"omitempty"`
	Password    string  `json:"password" validate:"omitempty"`
}

type DeviceRegistrationResponse struct {
	DeviceID     string `json:"device_id"`
	AccessToken  string `json:"access_token,omitempty"`
	RefreshToken string `json:"refresh_token,omitempty"`
	ExpiresIn    int    `json:"expires_in,omitempty"`
}

type LoginResponse struct {
	Token        string `json:"token"`
	RefreshToken string `json:"refresh_token"`
	ExpiresIn    int    `json:"expires_in"`
	Role         string `json:"role,omitempty"`
}

type RefreshRequest struct {
	RefreshToken string `json:"refresh_token" validate:"required"`
}

func (h *AuthHandler) Register(w http.ResponseWriter, r *http.Request) {
	var req models.CreateUserRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}

	if errs := validation.Struct(req); errs != nil {
		for _, err := range errs {
			RespondWithError(w, ErrCodeInvalidRequest, err, http.StatusBadRequest)
			return
		}
	}

	ctx := r.Context()

	// Check for existing user by email if email provided
	if req.Email != nil && *req.Email != "" {
		existing, err := h.userRepo.GetByEmail(ctx, *req.Email)
		if err != nil {
			logger.Log.Error().Err(err).Msg("Error checking user")
			RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
			return
		}
		if existing != nil {
			RespondWithError(w, ErrCodeConflict, "El usuario ya existe", http.StatusConflict)
			return
		}
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Error hashing password")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	role := req.Role
	if role == "" {
		role = "operator"
	}
	if role != "company_owner" && role != "warehouse_admin" && role != "operator" {
		RespondWithError(w, ErrCodeInvalidRequest, "Rol inválido", http.StatusBadRequest)
		return
	}

	// Email required for company_owner and warehouse_admin roles
	if (role == "company_owner" || role == "warehouse_admin") && (req.Email == nil || *req.Email == "") {
		RespondWithError(w, ErrCodeInvalidRequest, "Email es requerido para este rol", http.StatusBadRequest)
		return
	}

	// Look up role ID
	var roleID *uuid.UUID
	var foundRoleID uuid.UUID
	row := h.db.QueryRow(context.Background(), "SELECT id FROM roles WHERE name = $1", role)
	err = row.Scan(&foundRoleID)
	if err != nil && err.Error() != "no rows in result set" {
		logger.Log.Error().Err(err).Msg("Error looking up role")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if err == nil {
		roleID = &foundRoleID
	}

	companyID := uuid.Nil
	warehouseID := uuid.Nil

	if role == "company_owner" {
		companyCode := strings.ToUpper(req.CompanyCode)
		if companyCode == "" {
			if req.Username != nil {
				companyCode = strings.ToUpper(*req.Username)
			} else {
				companyCode = "COMPANY"
			}
		}
		company := &models.Company{
			ID:        uuid.New(),
			Code:      companyCode,
			Name:      req.CompanyName,
			CreatedAt: time.Now(),
			UpdatedAt: time.Now(),
		}
		if company.Name == "" {
			company.Name = companyCode + " S.A."
		}

		if err := h.companyRepo.Create(ctx, company); err != nil {
			logger.Log.Error().Err(err).Msg("Error creating company")
			RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
			return
		}
		companyID = company.ID

		warehouse := &repository.Warehouse{
			ID:        uuid.New(),
			CompanyID: companyID,
			Name:      "Depósito Central",
			CreatedAt: time.Now(),
			UpdatedAt: time.Now(),
		}
		if err := h.warehouseRepo.Create(ctx, warehouse); err != nil {
			logger.Log.Error().Err(err).Msg("Error creating warehouse")
			RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
			return
		}
		warehouseID = warehouse.ID
		logger.Log.Info().Str("company_id", companyID.String()).Str("warehouse_id", warehouseID.String()).Msg("Created company and warehouse")
	}

	user := &models.User{
		ID:           uuid.New(),
		CompanyID:    companyID,
		WarehouseID:  &warehouseID,
		Email:        req.Email,
		Username:     req.Username,
		PasswordHash: string(hash),
		RoleID:       roleID,
		Role:         role,
		Status:       "active",
		IsVerified:   false,
		CreatedAt:    time.Now(),
	}

	if err := h.userRepo.Create(ctx, user); err != nil {
		logger.Log.Error().Err(err).Msg("Error creating user")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	subscription := &models.Subscription{
		ID:              uuid.New(),
		UserID:          user.ID,
		Status:          "trialing",
		DeviceConnected: false,
		Features: models.SubscriptionFeatures{
			OfflineMode:     true,
			ConnectedMode:   true,
			PremiumFeatures: true,
		},
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}
	if err := h.subscriptionRepo.Create(ctx, subscription); err != nil {
		logger.Log.Error().Err(err).Msg("Error creating subscription")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	response := map[string]string{
		"message": "Usuario creado exitosamente",
		"id":      user.ID.String(),
	}
	if companyID != uuid.Nil {
		if req.Username != nil {
			response["company_code"] = strings.ToUpper(*req.Username)
		}
	}
	RespondWithJSON(w, http.StatusCreated, response)
}

func (h *AuthHandler) Login(w http.ResponseWriter, r *http.Request) {
	var req LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}

	if errs := validation.Struct(req); errs != nil {
		for _, err := range errs {
			RespondWithError(w, ErrCodeInvalidRequest, err, http.StatusBadRequest)
			return
		}
	}

	ctx := r.Context()

	company, err := h.companyRepo.GetByCode(ctx, req.CompanyCode)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Error fetching company")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	if company == nil {
		RespondWithError(w, ErrCodeUnauthorized, "Código de empresa inválido", http.StatusUnauthorized)
		return
	}

	var user *models.User

	user, err = h.userRepo.GetByEmailAndCompanyID(ctx, req.Username, company.ID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Error fetching user by email")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	if user == nil {
		user, err = h.userRepo.GetByUsernameAndCompanyID(ctx, req.Username, company.ID)
		if err != nil {
			logger.Log.Error().Err(err).Msg("Error fetching user by username")
			RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
			return
		}
	}

	if user == nil || bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.Password)) != nil {
		RespondWithError(w, ErrCodeUnauthorized, "Credenciales inválidas", http.StatusUnauthorized)
		return
	}

	token, err := h.jwtSvc.GenerateToken(user.ID, user.CompanyID, user.Role, 15*time.Minute)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Error generating token")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	refreshToken := generateRefreshToken()
	refreshTokenHash, err := bcrypt.GenerateFromPassword([]byte(refreshToken), bcrypt.DefaultCost)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Error hashing refresh token")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	refreshTokenModel := &models.RefreshToken{
		ID:         uuid.New(),
		UserID:     user.ID,
		TokenHash:  string(refreshTokenHash),
		DeviceName: req.DeviceName,
		ExpiresAt:  time.Now().Add(30 * 24 * time.Hour),
		CreatedAt:  time.Now(),
	}

	if err := h.refreshTokenRepo.Create(ctx, refreshTokenModel); err != nil {
		logger.Log.Error().Err(err).Msg("Error creating refresh token")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	logger.Log.Info().Str("user_id", user.ID.String()).Msg("User logged in")

	RespondWithJSON(w, http.StatusOK, LoginResponse{
		Token:        token,
		RefreshToken: refreshToken,
		ExpiresIn:    900,
		Role:         user.Role,
	})
}

func (h *AuthHandler) Refresh(w http.ResponseWriter, r *http.Request) {
	var req RefreshRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}

	if errs := validation.Struct(req); errs != nil {
		for _, err := range errs {
			RespondWithError(w, ErrCodeInvalidRequest, err, http.StatusBadRequest)
			return
		}
	}

	ctx := r.Context()

	refreshTokenModel, err := h.refreshTokenRepo.GetByHash(ctx, req.RefreshToken)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Error fetching refresh token")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	if refreshTokenModel == nil {
		RespondWithError(w, ErrCodeUnauthorized, "Token de refresh inválido o expirado", http.StatusUnauthorized)
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(refreshTokenModel.TokenHash), []byte(req.RefreshToken)); err != nil {
		RespondWithError(w, ErrCodeUnauthorized, "Token de refresh inválido", http.StatusUnauthorized)
		return
	}

	user, err := h.userRepo.GetByID(ctx, refreshTokenModel.UserID)
	if err != nil || user == nil {
		logger.Log.Error().Err(err).Msg("Error fetching user")
		RespondWithError(w, ErrCodeUnauthorized, "Usuario no encontrado", http.StatusUnauthorized)
		return
	}

	if err := h.refreshTokenRepo.Revoke(ctx, refreshTokenModel.ID); err != nil {
		logger.Log.Error().Err(err).Msg("Error revoking refresh token")
	}

	newToken, err := h.jwtSvc.GenerateToken(user.ID, user.CompanyID, user.Role, 15*time.Minute)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Error generating token")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	newRefreshToken := generateRefreshToken()
	newRefreshTokenHash, err := bcrypt.GenerateFromPassword([]byte(newRefreshToken), bcrypt.DefaultCost)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Error hashing refresh token")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	newRefreshTokenModel := &models.RefreshToken{
		ID:         uuid.New(),
		UserID:     user.ID,
		TokenHash:  string(newRefreshTokenHash),
		DeviceName: refreshTokenModel.DeviceName,
		ExpiresAt:  time.Now().Add(30 * 24 * time.Hour),
		CreatedAt:  time.Now(),
	}

	if err := h.refreshTokenRepo.Create(ctx, newRefreshTokenModel); err != nil {
		logger.Log.Error().Err(err).Msg("Error creating refresh token")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	logger.Log.Info().Str("user_id", user.ID.String()).Msg("Token refreshed")

	RespondWithJSON(w, http.StatusOK, LoginResponse{
		Token:        newToken,
		RefreshToken: newRefreshToken,
		ExpiresIn:    900,
	})
}

func generateRefreshToken() string {
	bytes := make([]byte, 32)
	rand.Read(bytes)
	return hex.EncodeToString(bytes)
}

func (h *AuthHandler) Logout(w http.ResponseWriter, r *http.Request) {
	userClaims := middleware.GetUserClaims(r)
	logger.Log.Info().Str("user_id", userClaims.UserID).Msg("User logged out")

	RespondWithJSON(w, http.StatusOK, map[string]string{
		"message": "Logout exitoso",
	})
}

func (h *AuthHandler) RegisterDevice(w http.ResponseWriter, r *http.Request) {
	var req RegisterDeviceRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}

	if errs := validation.Struct(req); errs != nil {
		for _, err := range errs {
			RespondWithError(w, ErrCodeInvalidRequest, err, http.StatusBadRequest)
		}
		return
	}

	ctx := r.Context()

	// Get company ID from warehouse (no auth required for device setup)
	warehouseID, err := uuid.Parse(req.WarehouseID)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "ID de warehouse inválido", http.StatusBadRequest)
		return
	}

	// Get warehouse to find company
	warehouse, err := h.warehouseRepo.GetByID(ctx, warehouseID)
	if err != nil || warehouse == nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Warehouse no encontrado", http.StatusBadRequest)
		return
	}
	companyID := warehouse.CompanyID

	device := &models.Device{
		ID:           uuid.New(),
		CompanyID:    companyID,
		WarehouseID:  warehouseID,
		DeviceUUID:   req.DeviceUUID,
		Platform:     req.Platform,
		Model:        req.Model,
		OSVersion:    req.OSVersion,
		AppVersion:   req.AppVersion,
		Status:       "active",
		RegisteredAt: time.Now(),
		LastSeenAt:   func() *time.Time { t := time.Now(); return &t }(),
	}

	if err := h.deviceRepo.Create(ctx, device); err != nil {
		logger.Log.Error().Err(err).Msg("Error creating device")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	logger.Log.Info().Str("device_id", device.ID.String()).Str("warehouse_id", warehouseID.String()).Msg("Device registered")

	response := DeviceRegistrationResponse{
		DeviceID: device.ID.String(),
	}

	// If username/password provided, authenticate and return tokens
	if req.Username != "" && req.Password != "" {
		user, err := h.userRepo.GetByUsernameAndCompanyID(ctx, req.Username, companyID)
		if err == nil && user != nil {
			if bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.Password)) == nil {
				// Generate tokens
				token, _ := h.jwtSvc.GenerateToken(user.ID, user.CompanyID, user.Role, 15*time.Minute)
				refreshToken := generateRefreshToken()
				refreshTokenHash, _ := bcrypt.GenerateFromPassword([]byte(refreshToken), bcrypt.DefaultCost)

				refreshTokenModel := &models.RefreshToken{
					ID:         uuid.New(),
					UserID:     user.ID,
					TokenHash:  string(refreshTokenHash),
					DeviceName: req.DeviceName,
					ExpiresAt:  time.Now().Add(30 * 24 * time.Hour),
					CreatedAt:  time.Now(),
				}
				h.refreshTokenRepo.Create(ctx, refreshTokenModel)

				response.AccessToken = token
				response.RefreshToken = refreshToken
				response.ExpiresIn = 3600

				logger.Log.Info().Str("user_id", user.ID.String()).Msg("Device registered with user session")
			}
		}
	}

	RespondWithJSON(w, http.StatusCreated, response)
}

type UserStatusResponse struct {
	UserStatus   string `json:"user_status"`
	DeviceStatus string `json:"device_status"`
	Message      string `json:"message,omitempty"`
}

func (h *AuthHandler) GetUserStatus(w http.ResponseWriter, r *http.Request) {
	userID, err := uuid.Parse(r.Context().Value("user_id").(string))
	if err != nil {
		RespondWithError(w, ErrCodeUnauthorized, "Token inválido", http.StatusUnauthorized)
		return
	}

	ctx := r.Context()

	user, err := h.userRepo.GetByID(ctx, userID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Error fetching user")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	if user == nil {
		RespondWithError(w, ErrCodeNotFound, "Usuario no encontrado", http.StatusNotFound)
		return
	}

	deviceID, err := uuid.Parse(r.Context().Value("device_id").(string))
	if err != nil {
		RespondWithError(w, ErrCodeUnauthorized, "Dispositivo inválido", http.StatusUnauthorized)
		return
	}

	device, err := h.deviceRepo.GetByID(ctx, deviceID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Error fetching device")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	response := UserStatusResponse{
		UserStatus:   user.Status,
		DeviceStatus: "active",
	}

	if user.Status != "active" {
		response.Message = "Tu cuenta ha sido desactivada. Por favor contacta al administrador."
	}

	if device != nil && device.Status == "revoked" {
		response.DeviceStatus = "revoked"
		response.Message = "Tu dispositivo ha sido revocado. Por favor contacta al administrador."
	}

	RespondWithJSON(w, http.StatusOK, response)
}

func (h *AuthHandler) Routes() *chi.Mux {
	r := chi.NewRouter()
	r.Post("/registrarse", h.Register)
	r.Post("/signup/trial", h.SignupTrial)
	r.Post("/login", h.Login)
	r.Post("/device", h.RegisterDevice)
	r.Post("/refresh", h.Refresh)
	r.Group(func(r chi.Router) {
		r.Use(middleware.Auth(middleware.AuthDeps{JwtSvc: h.jwtSvc, DeviceRepo: h.deviceRepo}))
		r.Post("/logout", h.Logout)
		r.Get("/user/status", h.GetUserStatus)
	})
	return r
}
