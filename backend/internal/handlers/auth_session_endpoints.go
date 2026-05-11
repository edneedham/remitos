package handlers

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"net/http"
	"strings"
	"time"

	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
	"server/internal/httputil"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	"server/internal/notifications/inapp"
	"server/internal/repository"
	"server/internal/validation"
)

func (h *AuthHandler) Register(w http.ResponseWriter, r *http.Request) {
	var req models.CreateUserRequest
	if !decodeJSONBody(w, r, httputil.MaxJSONAuthBody, &req) {
		return
	}

	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, r, "Revisá los datos del formulario.", fields, http.StatusBadRequest)
		return
	}

	ctx := r.Context()

	// Check for existing user by email if email provided
	if req.Email != nil && *req.Email != "" {
		existing, err := h.userRepo.GetByEmail(ctx, *req.Email)
		if err != nil {
			RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
			return
		}
		if existing != nil {
			RespondWithError(w, r, ErrCodeConflict, "El usuario ya existe", http.StatusConflict)
			return
		}
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	role := req.Role
	if role == "" {
		role = "operator"
	}
	if role != "company_owner" && role != "warehouse_admin" && role != "operator" {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Rol inválido", http.StatusBadRequest)
		return
	}

	// Email required for company_owner and warehouse_admin roles
	if (role == "company_owner" || role == "warehouse_admin") && (req.Email == nil || *req.Email == "") {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Email es requerido para este rol", http.StatusBadRequest)
		return
	}

	// Look up role ID
	var roleID *uuid.UUID
	var foundRoleID uuid.UUID
	row := h.db.QueryRow(context.Background(), "SELECT id FROM roles WHERE name = $1", role)
	err = row.Scan(&foundRoleID)
	if err != nil && err.Error() != "no rows in result set" {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
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
			RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
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
			RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
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
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
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
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
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
	var req models.LoginRequest
	if !decodeJSONBody(w, r, httputil.MaxJSONAuthBody, &req) {
		return
	}

	validation.NormalizeLoginRequest(&req)

	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, r, "Revisá los datos del formulario.", fields, http.StatusBadRequest)
		return
	}

	ctx := r.Context()

	company, err := h.companyRepo.GetByCode(ctx, req.CompanyCode)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	if company == nil {
		RespondWithError(w, r, ErrCodeUnauthorized, "Código de empresa inválido", http.StatusUnauthorized)
		return
	}

	var user *models.User

	user, err = h.userRepo.GetByEmailAndCompanyID(ctx, req.Username, company.ID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	if user == nil {
		user, err = h.userRepo.GetByUsernameAndCompanyID(ctx, req.Username, company.ID)
		if err != nil {
			RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
			return
		}
	}

	if user == nil || bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.Password)) != nil {
		RespondWithError(w, r, ErrCodeUnauthorized, "Credenciales inválidas", http.StatusUnauthorized)
		return
	}

	token, err := h.jwtSvc.GenerateToken(user.ID, user.CompanyID, user.Role, 15*time.Minute)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	refreshToken := generateRefreshToken()
	refreshTokenHash, err := bcrypt.GenerateFromPassword([]byte(refreshToken), bcrypt.DefaultCost)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
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
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	logger.Log.Info().Str("user_id", user.ID.String()).Msg("User logged in")

	secure := middleware.RequestIsHTTPS(r)
	if wantsWebCookies(r) {
		middleware.SetWebSessionCookies(w, token, refreshToken, secure)
		RespondWithJSON(w, http.StatusOK, LoginResponse{
			ExpiresIn: 900,
			Role:      user.Role,
			Session:   "cookie",
		})
		return
	}

	RespondWithJSON(w, http.StatusOK, LoginResponse{
		Token:        token,
		RefreshToken: refreshToken,
		ExpiresIn:    900,
		Role:         user.Role,
	})
}

func (h *AuthHandler) Refresh(w http.ResponseWriter, r *http.Request) {
	var req RefreshRequest
	if !decodeJSONBody(w, r, httputil.MaxJSONAuthBody, &req) {
		return
	}

	req.RefreshToken = strings.TrimSpace(req.RefreshToken)
	if req.RefreshToken == "" {
		if c, err := r.Cookie(middleware.CookieWebRefresh); err == nil {
			req.RefreshToken = strings.TrimSpace(c.Value)
		}
	}
	if req.RefreshToken == "" {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Token de refresh requerido", http.StatusBadRequest)
		return
	}

	ctx := r.Context()

	refreshTokenModel, err := h.refreshTokenRepo.GetValidByRawToken(ctx, req.RefreshToken)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	if refreshTokenModel == nil {
		RespondWithError(w, r, ErrCodeUnauthorized, "Token de refresh inválido o expirado", http.StatusUnauthorized)
		return
	}

	user, err := h.userRepo.GetByID(ctx, refreshTokenModel.UserID)
	if err != nil || user == nil {
		RespondWithError(w, r, ErrCodeUnauthorized, "Usuario no encontrado", http.StatusUnauthorized, err)
		return
	}

	if err := h.refreshTokenRepo.Revoke(ctx, refreshTokenModel.ID); err != nil {
		logger.Log.Error().Err(err).Msg("Error revoking refresh token")
	}

	newToken, err := h.jwtSvc.GenerateToken(user.ID, user.CompanyID, user.Role, 15*time.Minute)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	newRefreshToken := generateRefreshToken()
	newRefreshTokenHash, err := bcrypt.GenerateFromPassword([]byte(newRefreshToken), bcrypt.DefaultCost)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
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
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	logger.Log.Info().Str("user_id", user.ID.String()).Msg("Token refreshed")

	secure := middleware.RequestIsHTTPS(r)
	if wantsWebCookies(r) {
		middleware.SetWebSessionCookies(w, newToken, newRefreshToken, secure)
		RespondWithJSON(w, http.StatusOK, LoginResponse{
			ExpiresIn: 900,
			Session:   "cookie",
		})
		return
	}

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

func hashTransferToken(raw string) string {
	sum := sha256.Sum256([]byte(raw))
	return hex.EncodeToString(sum[:])
}

func (h *AuthHandler) StartSessionTransfer(w http.ResponseWriter, r *http.Request) {
	var req TransferStartRequest
	if !decodeJSONBody(w, r, httputil.MaxJSONAuthBody, &req) {
		return
	}
	req.RefreshToken = strings.TrimSpace(req.RefreshToken)
	if req.RefreshToken == "" {
		if c, err := r.Cookie(middleware.CookieWebRefresh); err == nil {
			req.RefreshToken = strings.TrimSpace(c.Value)
		}
	}
	if req.RefreshToken == "" {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Token de refresh requerido", http.StatusBadRequest)
		return
	}

	claims := middleware.GetUserClaims(r)
	userID, err := uuid.Parse(claims.UserID)
	if err != nil {
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}

	refreshTokenModel, err := h.refreshTokenRepo.GetValidByRawToken(r.Context(), req.RefreshToken)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if refreshTokenModel == nil || refreshTokenModel.UserID != userID {
		RespondWithError(w, r, ErrCodeUnauthorized, "Token de refresh inválido", http.StatusUnauthorized)
		return
	}

	rawToken := generateRefreshToken()
	now := time.Now().UTC()
	expiresAt := now.Add(60 * time.Second)
	transfer := &models.WebSessionTransfer{
		ID:                    uuid.New(),
		TokenHash:             hashTransferToken(rawToken),
		UserID:                userID,
		DesktopRefreshTokenID: refreshTokenModel.ID,
		ExpiresAt:             expiresAt,
		CreatedAt:             now,
	}
	if err := h.transferRepo.Create(r.Context(), transfer); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	RespondWithJSON(w, http.StatusOK, TransferStartResponse{
		Token:     rawToken,
		ExpiresAt: expiresAt.Format(time.RFC3339),
	})
}

func (h *AuthHandler) ClaimSessionTransfer(w http.ResponseWriter, r *http.Request) {
	var req TransferClaimRequest
	if !decodeJSONBody(w, r, httputil.MaxJSONAuthBody, &req) {
		return
	}
	req.Token = strings.TrimSpace(req.Token)
	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, r, "Revisá los datos del formulario.", fields, http.StatusBadRequest)
		return
	}

	transfer, err := h.transferRepo.ConsumeByTokenHash(r.Context(), hashTransferToken(req.Token))
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if transfer == nil {
		RespondWithError(w, r, ErrCodeUnauthorized, "Token de transferencia inválido o expirado", http.StatusUnauthorized)
		return
	}

	user, err := h.userRepo.GetByID(r.Context(), transfer.UserID)
	if err != nil || user == nil {
		RespondWithError(w, r, ErrCodeUnauthorized, "Usuario no encontrado", http.StatusUnauthorized, err)
		return
	}

	token, err := h.jwtSvc.GenerateToken(user.ID, user.CompanyID, user.Role, 15*time.Minute)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	refreshToken := generateRefreshToken()
	refreshTokenHash, err := bcrypt.GenerateFromPassword([]byte(refreshToken), bcrypt.DefaultCost)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	refreshTokenModel := &models.RefreshToken{
		ID:         uuid.New(),
		UserID:     user.ID,
		TokenHash:  string(refreshTokenHash),
		DeviceName: "web_transfer_phone",
		ExpiresAt:  time.Now().Add(30 * 24 * time.Hour),
		CreatedAt:  time.Now(),
	}
	if err := h.refreshTokenRepo.Create(r.Context(), refreshTokenModel); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if err := h.transferRepo.AttachPhoneRefreshToken(r.Context(), transfer.ID, refreshTokenModel.ID); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	if bc := inapp.NewBroadcaster(h.notificationRepo, h.userRepo, h.publicSiteURL); bc != nil {
		bc.SessionTransferCompleted(r.Context(), user.CompanyID)
	}

	secure := middleware.RequestIsHTTPS(r)
	if wantsWebCookies(r) {
		middleware.SetWebSessionCookies(w, token, refreshToken, secure)
		RespondWithJSON(w, http.StatusOK, LoginResponse{
			ExpiresIn: 900,
			Role:      user.Role,
			Session:   "cookie",
		})
		return
	}

	RespondWithJSON(w, http.StatusOK, LoginResponse{
		Token:        token,
		RefreshToken: refreshToken,
		ExpiresIn:    900,
		Role:         user.Role,
	})
}

func normalizeRegisterDeviceRequest(req *RegisterDeviceRequest) {
	req.DeviceUUID = strings.TrimSpace(req.DeviceUUID)
	req.Platform = strings.TrimSpace(req.Platform)
	req.WarehouseID = strings.TrimSpace(req.WarehouseID)
	req.DeviceName = strings.TrimSpace(req.DeviceName)
	req.Username = strings.TrimSpace(req.Username)
	if req.Model != nil {
		v := strings.TrimSpace(*req.Model)
		req.Model = &v
	}
	if req.OSVersion != nil {
		v := strings.TrimSpace(*req.OSVersion)
		req.OSVersion = &v
	}
	if req.AppVersion != nil {
		v := strings.TrimSpace(*req.AppVersion)
		req.AppVersion = &v
	}
}

func (h *AuthHandler) Logout(w http.ResponseWriter, r *http.Request) {
	userClaims := middleware.GetUserClaims(r)
	logger.Log.Info().Str("user_id", userClaims.UserID).Msg("User logged out")

	if wantsWebCookies(r) {
		middleware.ClearWebSessionCookies(w, middleware.RequestIsHTTPS(r))
	}

	RespondWithJSON(w, http.StatusOK, map[string]string{
		"message": "Logout exitoso",
	})
}

func wantsWebCookies(r *http.Request) bool {
	return r.Header.Get("X-Enpunto-Web") == "1"
}
