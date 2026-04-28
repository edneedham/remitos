package handlers

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"time"

	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
	"server/internal/logger"
	"server/internal/models"
	notifymail "server/internal/notifications/email"
	"server/internal/payments/mercadopago"
	"server/internal/repository"
	"server/internal/validation"
)

const signupTrialDays = 7

var errCardTokenRequired = errors.New("card token required")

func resolveSignupCardToken(cardToken string, allowMock bool) (string, error) {
	if cardToken != "" {
		return cardToken, nil
	}
	if !allowMock {
		return "", errCardTokenRequired
	}
	return "mock_card_token", nil
}

// SignupTrial creates one company (trial), one warehouse, the first user (company_owner), subscription row,
// and stores Mercado Pago customer + saved card for charging after the trial. No payment is taken.
func (h *AuthHandler) SignupTrial(w http.ResponseWriter, r *http.Request) {
	var req models.SignupTrialRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}

	validation.NormalizeSignupTrialRequest(&req)

	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, "Revisá los datos del formulario.", fields, http.StatusBadRequest)
		return
	}

	cardToken, err := resolveSignupCardToken(req.CardToken, h.signupAllowMock)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Token de tarjeta requerido", http.StatusBadRequest)
		return
	}

	ctx := r.Context()
	email := req.Email
	companyCode := req.CompanyCode
	companyName := req.CompanyName

	if existing, err := h.userRepo.GetByEmail(ctx, email); err != nil {
		logger.Log.Error().Err(err).Msg("signup trial: email check")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	} else if existing != nil {
		RespondWithError(w, ErrCodeConflict, "El correo ya está registrado", http.StatusConflict)
		return
	}

	if taken, err := h.companyRepo.GetByCode(ctx, companyCode); err != nil {
		logger.Log.Error().Err(err).Msg("signup trial: company code check")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	} else if taken != nil {
		RespondWithError(w, ErrCodeConflict, "El código de empresa ya existe", http.StatusConflict)
		return
	}

	var customerID, cardID string
	if h.signupAllowMock && (cardToken == "" || cardToken == "mock_card_token") {
		customerID = mercadopago.StubCustomerID
		cardID = mercadopago.StubCardID
	} else {
		customerID, cardID, err = h.mp.SaveCard(ctx, email, cardToken)
		if err != nil {
			logger.Log.Error().Err(err).Msg("signup trial: mercado pago")
			RespondWithError(w, ErrCodeInvalidRequest, "No pudimos validar la tarjeta. Revisá los datos o probá otra tarjeta.", http.StatusBadRequest)
			return
		}
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		logger.Log.Error().Err(err).Msg("signup trial: hash password")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	role := "company_owner"
	var roleID *uuid.UUID
	var foundRoleID uuid.UUID
	row := h.db.QueryRow(ctx, "SELECT id FROM roles WHERE name = $1", role)
	if err := row.Scan(&foundRoleID); err == nil {
		roleID = &foundRoleID
	} else {
		logger.Log.Error().Err(err).Msg("signup trial: role company_owner")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	companyID := uuid.New()
	warehouseID := uuid.New()
	userID := uuid.New()
	trialEnd := time.Now().UTC().Add(signupTrialDays * 24 * time.Hour)
	one := 1
	two := 2
	docLimit := 3000

	company := &models.Company{
		ID:                    companyID,
		Code:                  companyCode,
		Name:                  companyName,
		TrialEndsAt:           &trialEnd,
		MaxWarehouses:         &one,
		MaxUsers:              &two,
		DocumentsMonthlyLimit: &docLimit,
		MpCustomerID:          &customerID,
		MpCardID:              &cardID,
		CreatedAt:             time.Now(),
		UpdatedAt:             time.Now(),
	}

	warehouse := &repository.Warehouse{
		ID:        warehouseID,
		CompanyID: companyID,
		Name:      "Depósito Central",
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}

	emailPtr := email
	user := &models.User{
		ID:           userID,
		CompanyID:    companyID,
		WarehouseID:  &warehouseID,
		Email:        &emailPtr,
		Username:     &emailPtr,
		PasswordHash: string(hash),
		RoleID:       roleID,
		Role:         role,
		Status:       "active",
		IsVerified:   false,
		CreatedAt:    time.Now(),
		UpdatedAt:    time.Now(),
	}

	subscription := &models.Subscription{
		ID:              uuid.New(),
		UserID:          userID,
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

	if err := h.companyRepo.CreateTrial(ctx, company); err != nil {
		logger.Log.Error().Err(err).Msg("signup trial: create company")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if err := h.warehouseRepo.Create(ctx, warehouse); err != nil {
		logger.Log.Error().Err(err).Msg("signup trial: create warehouse")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if err := h.userRepo.Create(ctx, user); err != nil {
		logger.Log.Error().Err(err).Msg("signup trial: create user")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}
	if err := h.subscriptionRepo.Create(ctx, subscription); err != nil {
		logger.Log.Error().Err(err).Msg("signup trial: subscription")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	token, err := h.jwtSvc.GenerateToken(user.ID, user.CompanyID, user.Role, 15*time.Minute)
	if err != nil {
		logger.Log.Error().Err(err).Msg("signup trial: jwt")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	refreshToken := generateRefreshToken()
	refreshTokenHash, err := bcrypt.GenerateFromPassword([]byte(refreshToken), bcrypt.DefaultCost)
	if err != nil {
		logger.Log.Error().Err(err).Msg("signup trial: refresh hash")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	refreshTokenModel := &models.RefreshToken{
		ID:         uuid.New(),
		UserID:     user.ID,
		TokenHash:  string(refreshTokenHash),
		DeviceName: "web-signup",
		ExpiresAt:  time.Now().Add(30 * 24 * time.Hour),
		CreatedAt:  time.Now(),
	}
	if err := h.refreshTokenRepo.Create(ctx, refreshTokenModel); err != nil {
		logger.Log.Error().Err(err).Msg("signup trial: refresh token")
		RespondWithError(w, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError)
		return
	}

	type signupTrialResponse struct {
		Message       string `json:"message"`
		UserID        string `json:"user_id"`
		CompanyID     string `json:"company_id"`
		CompanyCode   string `json:"company_code"`
		TrialEndsAt   string `json:"trial_ends_at"`
		MaxWarehouses int    `json:"max_warehouses"`
		MaxUsers      int    `json:"max_users"`
		Token         string `json:"token"`
		RefreshToken  string `json:"refresh_token"`
		ExpiresIn     int    `json:"expires_in"`
		Role          string `json:"role"`
	}

	h.queueSignupWelcomeEmail(email, companyName, companyCode, trialEnd)

	RespondWithJSON(w, http.StatusCreated, signupTrialResponse{
		Message:       fmt.Sprintf("Cuenta creada. Tenés %d días de prueba.", signupTrialDays),
		UserID:        user.ID.String(),
		CompanyID:     companyID.String(),
		CompanyCode:   companyCode,
		TrialEndsAt:   trialEnd.Format(time.RFC3339),
		MaxWarehouses: one,
		MaxUsers:      two,
		Token:         token,
		RefreshToken:  refreshToken,
		ExpiresIn:     900,
		Role:          user.Role,
	})
}

func (h *AuthHandler) queueSignupWelcomeEmail(recipient, companyName, companyCode string, trialEnd time.Time) {
	msg := notifymail.SignupTrialWelcome(recipient, companyCode, companyName, trialEnd, h.publicSiteURL)
	go func(m notifymail.Message) {
		ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()
		if err := h.mailer.Send(ctx, m); err != nil {
			logger.Log.Error().Err(err).Str("to", recipient).Msg("signup welcome email failed")
		}
	}(msg)
}
