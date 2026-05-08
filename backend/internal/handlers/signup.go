package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
	"server/internal/billing"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	notifymail "server/internal/notifications/email"
	"server/internal/payments/mercadopago"
	"server/internal/repository"
	"server/internal/validation"
)

// Limits for the initial trial period granted at signup (subscription_plan=trial).
const (
	trialPeriodDays    = 7
	trialMaxWarehouses = 2
	trialMaxUsers      = 2
	trialDocsLimit     = 500
)

// Signup creates one company (trial tier), one warehouse, the first user (company_owner), and subscription row.
func (h *AuthHandler) Signup(w http.ResponseWriter, r *http.Request) {
	var req models.SignupRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
		return
	}

	validation.NormalizeSignupRequest(&req)

	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, r, "Revisá los datos del formulario.", fields, http.StatusBadRequest)
		return
	}

	ctx := r.Context()
	email := req.Email
	companyCode := req.CompanyCode
	companyName := req.CompanyName

	if existing, err := h.userRepo.GetByEmail(ctx, email); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	} else if existing != nil {
		RespondWithError(w, r, ErrCodeConflict, "El correo ya está registrado", http.StatusConflict)
		return
	}

	if taken, err := h.companyRepo.GetByCode(ctx, companyCode); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	} else if taken != nil {
		RespondWithError(w, r, ErrCodeConflict, "El código de empresa ya existe", http.StatusConflict)
		return
	}

	if h.facturaEmitter == nil || !h.facturaEmitter.PadronEnabled {
		RespondWithError(w, r, ErrCodeInternalError, "No podemos verificar el CUIT con AFIP en este momento. Intentá de nuevo más tarde.", http.StatusServiceUnavailable)
		return
	}
	persona, err := billing.LookupPadronPersona(ctx, h.facturaEmitter, req.CompanyCUIT)
	if err != nil {
		logger.Log.Warn().Err(err).Str("cuit", req.CompanyCUIT).Msg("signup padron lookup failed")
		msg := "No pudimos validar el CUIT con AFIP. Revisá el número o probá más tarde."
		if strings.Contains(err.Error(), "checksum") {
			msg = "El dígito verificador del CUIT no es válido."
		}
		RespondWithError(w, r, ErrCodeInvalidRequest, msg, http.StatusBadRequest, err)
		return
	}
	estado := strings.TrimSpace(persona.EstadoClave)
	if strings.EqualFold(estado, "INACTIVO") {
		RespondWithError(w, r, ErrCodeInvalidRequest, "El CUIT no está activo en AFIP (clave inactiva). No podemos crear la cuenta con este número.", http.StatusBadRequest)
		return
	}
	stamp := time.Now().UTC()
	cuitStr := req.CompanyCUIT
	razonSocial := persona.RazonSocial
	condicionIVA := string(persona.CondicionIVA)
	domicilio := persona.DomicilioFiscal
	cuitVerifiedAt := &stamp

	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	role := "company_owner"
	var roleID *uuid.UUID
	var foundRoleID uuid.UUID
	row := h.db.QueryRow(ctx, "SELECT id FROM roles WHERE name = $1", role)
	if err := row.Scan(&foundRoleID); err == nil {
		roleID = &foundRoleID
	} else {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	companyID := uuid.New()
	warehouseID := uuid.New()
	userID := uuid.New()
	trialEnd := time.Now().UTC().Add(trialPeriodDays * 24 * time.Hour)
	maxWarehouses := trialMaxWarehouses
	maxUsers := trialMaxUsers
	docLimit := trialDocsLimit

	company := &models.Company{
		ID:                    companyID,
		Code:                  companyCode,
		Name:                  companyName,
		Cuit:                  cuitStr,
		RazonSocial:           razonSocial,
		CondicionIVA:          condicionIVA,
		DomicilioFiscal:       domicilio,
		CuitEstado:            estado,
		CuitVerifiedAt:        cuitVerifiedAt,
		PadronSyncedAt:        cuitVerifiedAt,
		TrialEndsAt:           &trialEnd,
		MaxWarehouses:         &maxWarehouses,
		MaxUsers:              &maxUsers,
		DocumentsMonthlyLimit: &docLimit,
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

	cardTok := strings.TrimSpace(req.CardToken)
	if cardTok != "" {
		switch {
		case h.mp.HasAccessToken():
			custID, mpCardID, err := h.mp.SaveCard(ctx, email, cardTok)
			if err != nil {
				RespondWithError(w, r, ErrCodeInvalidRequest, "No pudimos validar la tarjeta. Revisá los datos e intentá de nuevo.", http.StatusBadRequest, err)
				return
			}
			company.MpCustomerID = &custID
			company.MpCardID = &mpCardID
		case h.signupAllowMock:
			custID := mercadopago.StubCustomerID
			mpCardID := mercadopago.StubCardID
			company.MpCustomerID = &custID
			company.MpCardID = &mpCardID
		default:
			RespondWithError(w, r, ErrCodeInternalError, "Medios de pago no configurados en el servidor.", http.StatusServiceUnavailable)
			return
		}
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
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if err := h.warehouseRepo.Create(ctx, warehouse); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if err := h.userRepo.Create(ctx, user); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if err := h.subscriptionRepo.Create(ctx, subscription); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
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
		DeviceName: "web-signup",
		ExpiresAt:  time.Now().Add(30 * 24 * time.Hour),
		CreatedAt:  time.Now(),
	}
	if err := h.refreshTokenRepo.Create(ctx, refreshTokenModel); err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	type signupResponse struct {
		Message       string `json:"message"`
		UserID        string `json:"user_id"`
		CompanyID     string `json:"company_id"`
		CompanyCode   string `json:"company_code"`
		TrialEndsAt   string `json:"trial_ends_at"`
		MaxWarehouses int    `json:"max_warehouses"`
		MaxUsers      int    `json:"max_users"`
		Token         string `json:"token,omitempty"`
		RefreshToken  string `json:"refresh_token,omitempty"`
		ExpiresIn     int    `json:"expires_in"`
		Role          string `json:"role"`
		Session       string `json:"session,omitempty"`
	}

	h.queueSignupWelcomeEmail(email, companyName, companyCode, trialEnd)

	if wantsWebCookies(r) {
		middleware.SetWebSessionCookies(w, token, refreshToken, middleware.RequestIsHTTPS(r))
		RespondWithJSON(w, http.StatusCreated, signupResponse{
			Message:       fmt.Sprintf("Cuenta creada. Tenés %d días de prueba.", trialPeriodDays),
			UserID:        user.ID.String(),
			CompanyID:     companyID.String(),
			CompanyCode:   companyCode,
			TrialEndsAt:   trialEnd.Format(time.RFC3339),
			MaxWarehouses: maxWarehouses,
			MaxUsers:      maxUsers,
			ExpiresIn:     900,
			Role:          user.Role,
			Session:       "cookie",
		})
		return
	}

	RespondWithJSON(w, http.StatusCreated, signupResponse{
		Message:       fmt.Sprintf("Cuenta creada. Tenés %d días de prueba.", trialPeriodDays),
		UserID:        user.ID.String(),
		CompanyID:     companyID.String(),
		CompanyCode:   companyCode,
		TrialEndsAt:   trialEnd.Format(time.RFC3339),
		MaxWarehouses: maxWarehouses,
		MaxUsers:      maxUsers,
		Token:         token,
		RefreshToken:  refreshToken,
		ExpiresIn:     900,
		Role:          user.Role,
	})
}

func (h *AuthHandler) queueSignupWelcomeEmail(recipient, companyName, companyCode string, trialEnd time.Time) {
	msg := notifymail.SignupWelcome(recipient, companyCode, companyName, trialEnd, h.publicSiteURL)
	go func(m notifymail.Message) {
		ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()
		if err := h.mailer.Send(ctx, m); err != nil {
			logger.Log.Error().Err(err).Str("to", recipient).Msg("signup welcome email failed")
		}
	}(msg)
}
