package handlers

import (
	"net/http"
	"path"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
	"server/internal/billing"
	"server/internal/httputil"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	"server/internal/notifications/inapp"
	"server/internal/releases"
	"server/internal/repository"
	"server/internal/validation"
)

type meEntitlementResponse struct {
	CanDownloadApp               bool                                  `json:"can_download_app"`
	SubscriptionPlan             string                                `json:"subscription_plan"`
	PendingPlan                  *string                               `json:"pending_plan,omitempty"`
	TrialEndsAt                  *time.Time                            `json:"trial_ends_at,omitempty"`
	SubscriptionExpiresAt        *time.Time                            `json:"subscription_expires_at,omitempty"`
	CompanyStatus                string                                `json:"company_status"`
	ArchivedAt                   *time.Time                            `json:"archived_at,omitempty"`
	WarehouseCount               int64                                 `json:"warehouse_count"`
	MaxWarehouses                *int                                  `json:"max_warehouses,omitempty"`
	DeviceCount                  int64                                 `json:"device_count"`
	UserCount                    int64                                 `json:"user_count"`
	MaxUsers                     *int                                  `json:"max_users,omitempty"`
	RemitosProcessedLast30Days   int64                                 `json:"remitos_processed_last_30_days"`
	FirstScanCompleted           bool                                  `json:"first_scan_completed"`
	FirstScanCompletedAt         *time.Time                            `json:"first_scan_completed_at,omitempty"`
	WarehouseUsageLast30Days     []repository.WarehouseInboundUsageRow `json:"warehouse_usage_last_30_days"`
	DocumentsMonthlyLimit        *int                                  `json:"documents_monthly_limit,omitempty"`
	DocumentsUsageMTD            int64                                 `json:"documents_usage_mtd"`
	DocumentsUsageSeries         []repository.DocumentUsageSeriesPoint `json:"documents_usage_series"`
	DocumentsUsageByWarehouseMTD []repository.WarehouseInboundUsageRow `json:"documents_usage_by_warehouse_mtd"`
}

type meProfileResponse struct {
	ID              string  `json:"id"`
	Username        string  `json:"username"`
	Email           *string `json:"email,omitempty"`
	CompanyID       string  `json:"company_id"`
	CompanyName     string  `json:"company_name"`
	CompanyCode     string  `json:"company_code"`
	Role            string  `json:"role"`
	Cuit            string  `json:"cuit,omitempty"`
	RazonSocial     string  `json:"razon_social,omitempty"`
	CondicionIVA    string  `json:"condicion_iva,omitempty"`
	DomicilioFiscal string  `json:"domicilio_fiscal,omitempty"`
	CuitEstado      string  `json:"cuit_estado,omitempty"`
}

func canAccessWebManagement(role string) bool {
	switch role {
	case models.RoleCompanyOwner, models.RoleWarehouseAdmin, models.RoleReadOnly, "admin":
		return true
	default:
		return false
	}
}

// canManageBillingSubscriptions allows charging cards, activating or changing paid plans, and
// updating saved payment methods. read_only may view invoices/entitlement but must not mutate billing.
func canManageBillingSubscriptions(role string) bool {
	switch role {
	case models.RoleCompanyOwner, models.RoleWarehouseAdmin, "admin":
		return true
	default:
		return false
	}
}

// GetMe returns minimal profile details for the authenticated web session.
func (h *AuthHandler) GetMe(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" || claims.CompanyID == "" {
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	if !canAccessWebManagement(claims.Role) {
		RespondWithError(w, r, ErrCodeForbidden, "Este rol no tiene acceso a la administración web", http.StatusForbidden)
		return
	}

	userID, err := uuid.Parse(claims.UserID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Usuario inválido", http.StatusBadRequest)
		return
	}
	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}

	user, err := h.userRepo.GetByID(r.Context(), userID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if user == nil {
		RespondWithError(w, r, ErrCodeNotFound, "Usuario no encontrado", http.StatusNotFound)
		return
	}

	company, err := h.companyRepo.GetByIDForBilling(r.Context(), companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if company == nil {
		RespondWithError(w, r, ErrCodeNotFound, "Empresa no encontrada", http.StatusNotFound)
		return
	}

	username := ""
	if user.Username != nil {
		username = strings.TrimSpace(*user.Username)
	}
	if username == "" && user.Email != nil {
		username = strings.TrimSpace(*user.Email)
	}

	RespondWithJSON(w, http.StatusOK, meProfileResponse{
		ID:              user.ID.String(),
		Username:        username,
		Email:           user.Email,
		CompanyID:       companyID.String(),
		CompanyName:     company.Name,
		CompanyCode:     company.Code,
		Role:            claims.Role,
		Cuit:            strings.TrimSpace(company.Cuit),
		RazonSocial:     strings.TrimSpace(company.RazonSocial),
		CondicionIVA:    strings.TrimSpace(company.CondicionIVA),
		DomicilioFiscal: strings.TrimSpace(company.DomicilioFiscal),
		CuitEstado:      strings.TrimSpace(company.CuitEstado),
	})
}

// GetMeEntitlement returns whether the user's company may download the Android app (trial or paid).
func (h *AuthHandler) GetMeEntitlement(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" || claims.CompanyID == "" {
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	if !canAccessWebManagement(claims.Role) {
		RespondWithError(w, r, ErrCodeForbidden, "Este rol no tiene acceso a la administración web", http.StatusForbidden)
		return
	}
	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}
	company, err := h.companyRepo.GetByIDForBilling(r.Context(), companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if company == nil {
		RespondWithError(w, r, ErrCodeNotFound, "Empresa no encontrada", http.StatusNotFound)
		return
	}
	warehouseCount, err := h.warehouseRepo.CountByCompanyID(r.Context(), companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	remitos30d, _, firstScanAt, err := h.syncRepo.InboundNoteEntitlementMetrics(r.Context(), companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	firstScanDone := firstScanAt != nil
	warehouseUsage, err := h.syncRepo.ListInboundNotesByWarehouseLast30Days(r.Context(), companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	deviceCount, err := h.deviceRepo.CountByCompanyID(r.Context(), companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	userCount, err := h.userRepo.CountByCompanyID(r.Context(), companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	mtdTotal, usageSeries, err := h.syncRepo.InboundNotesMTDCumulativeSeries(r.Context(), companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	documentsByWarehouseMTD, err := h.syncRepo.ListInboundNotesByWarehouseMTD(r.Context(), companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	now := time.Now()
	RespondWithJSON(w, http.StatusOK, meEntitlementResponse{
		CanDownloadApp:               billing.CompanyHasAppDownloadAccess(now, company),
		SubscriptionPlan:             company.SubscriptionPlan,
		PendingPlan:                  company.PendingPlan,
		TrialEndsAt:                  company.TrialEndsAt,
		SubscriptionExpiresAt:        company.SubscriptionExpiresAt,
		CompanyStatus:                company.Status,
		ArchivedAt:                   company.ArchivedAt,
		WarehouseCount:               warehouseCount,
		MaxWarehouses:                company.MaxWarehouses,
		DeviceCount:                  deviceCount,
		UserCount:                    userCount,
		MaxUsers:                     company.MaxUsers,
		RemitosProcessedLast30Days:   remitos30d,
		FirstScanCompleted:           firstScanDone,
		FirstScanCompletedAt:         firstScanAt,
		WarehouseUsageLast30Days:     warehouseUsage,
		DocumentsMonthlyLimit:        company.DocumentsMonthlyLimit,
		DocumentsUsageMTD:            mtdTotal,
		DocumentsUsageSeries:         usageSeries,
		DocumentsUsageByWarehouseMTD: documentsByWarehouseMTD,
	})
}

type invoiceListItem struct {
	ID          uuid.UUID `json:"id"`
	AmountMinor int64     `json:"amount_minor"`
	Currency    string    `json:"currency"`
	Status      string    `json:"status"`
	Description string    `json:"description,omitempty"`
	IssuedAt    time.Time `json:"issued_at"`
	MpPaymentID *string   `json:"mp_payment_id,omitempty"`
	// AFIP factura (optional until emitted).
	FacturaTipo      *int32  `json:"factura_tipo,omitempty"`
	FacturaPtoVta    *int32  `json:"factura_pto_vta,omitempty"`
	FacturaNumero    *int64  `json:"factura_numero,omitempty"`
	FacturaCAE       *string `json:"factura_cae,omitempty"`
	FacturaCAEVto    *string `json:"factura_cae_vto,omitempty"`
	FacturaEmittedAt *string `json:"factura_emitted_at,omitempty"`
	FacturaPending   bool    `json:"factura_pending"`
	FacturaLastError *string `json:"factura_last_error,omitempty"`
}

// GetMeInvoices lists billing invoices for the authenticated user's company (newest first).
func (h *AuthHandler) GetMeInvoices(w http.ResponseWriter, r *http.Request) {
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" || claims.CompanyID == "" {
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	if !canAccessWebManagement(claims.Role) {
		RespondWithError(w, r, ErrCodeForbidden, "Este rol no tiene acceso a la administración web", http.StatusForbidden)
		return
	}
	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}
	rows, err := h.invoiceRepo.ListByCompanyID(r.Context(), companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	out := make([]invoiceListItem, len(rows))
	for i, inv := range rows {
		item := invoiceListItem{
			ID:          inv.ID,
			AmountMinor: inv.AmountMinor,
			Currency:    inv.Currency,
			Status:      inv.Status,
			Description: inv.Description,
			IssuedAt:    inv.IssuedAt,
			MpPaymentID: inv.MpPaymentID,
		}
		if inv.FacturaTipo.Valid {
			v := inv.FacturaTipo.Int32
			item.FacturaTipo = &v
		}
		if inv.FacturaPtoVta.Valid {
			v := inv.FacturaPtoVta.Int32
			item.FacturaPtoVta = &v
		}
		if inv.FacturaNumero.Valid {
			v := inv.FacturaNumero.Int64
			item.FacturaNumero = &v
		}
		if inv.FacturaCAE.Valid {
			s := inv.FacturaCAE.String
			item.FacturaCAE = &s
		}
		if inv.FacturaCAEVto.Valid {
			s := inv.FacturaCAEVto.Time.Format("2006-01-02")
			item.FacturaCAEVto = &s
		}
		if inv.FacturaEmittedAt.Valid {
			s := inv.FacturaEmittedAt.Time.UTC().Format(time.RFC3339)
			item.FacturaEmittedAt = &s
		}
		if inv.FacturaLastError.Valid {
			s := inv.FacturaLastError.String
			item.FacturaLastError = &s
		}
		item.FacturaPending = inv.Status == "paid" && inv.MpPaymentID != nil &&
			!inv.FacturaEmittedAt.Valid && inv.FacturaAttempts < repository.MaxFacturaAttempts
		out[i] = item
	}
	RespondWithJSON(w, http.StatusOK, out)
}

type androidDownloadResponse struct {
	SignedURL string `json:"signed_url"`
	ExpiresAt string `json:"expires_at"`
	Filename  string `json:"filename"`
}

// GetAndroidDownloadURL returns a short-lived signed GCS URL to the release APK (entitled companies only).
func (h *AuthHandler) GetAndroidDownloadURL(w http.ResponseWriter, r *http.Request) {
	if h.releases == nil || h.releases.Storage == nil || h.releases.Bucket == "" || h.releases.Object == "" {
		RespondWithError(w, r, ErrCodeInternalError, "Descarga de la aplicación no disponible en este servidor", http.StatusServiceUnavailable)
		return
	}
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" || claims.CompanyID == "" {
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}
	companyID, err := uuid.Parse(claims.CompanyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}
	company, err := h.companyRepo.GetByIDForBilling(r.Context(), companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if company == nil {
		RespondWithError(w, r, ErrCodeNotFound, "Empresa no encontrada", http.StatusNotFound)
		return
	}
	if !billing.CompanyHasAppDownloadAccess(time.Now(), company) {
		RespondWithError(w, r, ErrCodeForbidden, "Tu plan no incluye descargar la aplicación en este momento.", http.StatusForbidden)
		return
	}
	expiry := h.releases.Expiry
	if expiry <= 0 {
		expiry = 15 * time.Minute
	}
	urlStr, expiresAt, err := releases.SignedGETURL(r.Context(), h.releases.Storage, h.releases.Bucket, h.releases.Object, expiry)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error al generar enlace de descarga", http.StatusInternalServerError, err)
		return
	}
	filename := path.Base(h.releases.Object)
	if filename == "." || filename == "/" || filename == "" {
		filename = "app-release.apk"
	}
	RespondWithJSON(w, http.StatusOK, androidDownloadResponse{
		SignedURL: urlStr,
		ExpiresAt: expiresAt.Format(time.RFC3339),
		Filename:  filename,
	})
}

func (h *AuthHandler) RegisterDevice(w http.ResponseWriter, r *http.Request) {
	var req RegisterDeviceRequest
	if !decodeJSONBody(w, r, httputil.MaxJSONAuthBody, &req) {
		return
	}

	normalizeRegisterDeviceRequest(&req)

	if fields := validation.StructFieldErrors(req); len(fields) > 0 {
		RespondWithValidationError(w, r, "Revisá los datos del formulario.", fields, http.StatusBadRequest)
		return
	}

	ctx := r.Context()

	// Get company ID from warehouse (no auth required for device setup)
	warehouseID, err := uuid.Parse(req.WarehouseID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "ID de warehouse inválido", http.StatusBadRequest)
		return
	}

	// Get warehouse to find company
	warehouse, err := h.warehouseRepo.GetByID(ctx, warehouseID)
	if err != nil || warehouse == nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Warehouse no encontrado", http.StatusBadRequest)
		return
	}
	companyID := warehouse.CompanyID

	company, err := h.companyRepo.GetByIDForBilling(ctx, companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if company == nil {
		RespondWithError(w, r, ErrCodeNotFound, "Empresa no encontrada", http.StatusNotFound)
		return
	}

	existingDevice, err := h.deviceRepo.GetByUUID(ctx, companyID, req.DeviceUUID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}
	if existingDevice == nil {
		activeInWarehouse, err := h.deviceRepo.CountActiveByWarehouseID(ctx, warehouseID)
		if err != nil {
			RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
			return
		}
		if block, msg := blockNewDeviceForWarehousePlan(company.SubscriptionPlan, activeInWarehouse); block {
			RespondWithError(w, r, ErrCodeForbidden, msg, http.StatusForbidden)
			return
		}
	}

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
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	if bc := inapp.NewBroadcaster(h.notificationRepo, h.userRepo, h.publicSiteURL); bc != nil {
		bc.DeviceRegistered(ctx, companyID, warehouse.Name)
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

// blockNewDeviceForWarehousePlan enforces one active handset per warehouse on trial and PyME.
// Other paid tiers may register multiple devices per warehouse.
func blockNewDeviceForWarehousePlan(subscriptionPlan string, activeDevicesInWarehouse int64) (block bool, message string) {
	if activeDevicesInWarehouse < 1 {
		return false, ""
	}
	switch strings.ToLower(strings.TrimSpace(subscriptionPlan)) {
	case "trial":
		return true, "La prueba permite 1 dispositivo por depósito."
	case "pyme":
		return true, "Tu plan permite 1 dispositivo activo por depósito. Revocá un dispositivo en el panel para registrar otro."
	default:
		return false, ""
	}
}

func (h *AuthHandler) GetUserStatus(w http.ResponseWriter, r *http.Request) {
	userID, err := uuid.Parse(r.Context().Value("user_id").(string))
	if err != nil {
		RespondWithError(w, r, ErrCodeUnauthorized, "Token inválido", http.StatusUnauthorized)
		return
	}

	ctx := r.Context()

	user, err := h.userRepo.GetByID(ctx, userID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
		return
	}

	if user == nil {
		RespondWithError(w, r, ErrCodeNotFound, "Usuario no encontrado", http.StatusNotFound)
		return
	}

	deviceID, err := uuid.Parse(r.Context().Value("device_id").(string))
	if err != nil {
		RespondWithError(w, r, ErrCodeUnauthorized, "Dispositivo inválido", http.StatusUnauthorized)
		return
	}

	device, err := h.deviceRepo.GetByID(ctx, deviceID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error interno del servidor", http.StatusInternalServerError, err)
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
	r.Group(func(r chi.Router) {
		r.Use(middleware.AuthEndpointsRateLimit())
		r.Post("/registrarse", h.Register)
		r.Post("/signup", h.Signup)
		r.Post("/signup/trial", h.Signup)
		r.Post("/login", h.Login)
		r.Post("/forgot-password", h.ForgotPassword)
		r.Post("/reset-password", h.ResetPassword)
		r.Post("/device", h.RegisterDevice)
		r.Post("/refresh", h.Refresh)
		r.Post("/transfer/claim", h.ClaimSessionTransfer)
	})
	r.Group(func(r chi.Router) {
		r.Use(middleware.Auth(middleware.AuthDeps{JwtSvc: h.jwtSvc, DeviceRepo: h.deviceRepo, UserWarehouseRepo: h.userWarehouseRepo}))
		r.Post("/logout", h.Logout)
		r.Post("/change-password", h.ChangePassword)
		r.Post("/me/plan", h.SelectMyPlan)
		r.Post("/me/plan/change", h.PostMeChangePlan)
		r.Post("/me/payment-method", h.PostMeUpdatePaymentMethod)
		r.Post("/me/activate-subscription", h.PostMeActivateSubscription)
		r.Post("/transfer/start", h.StartSessionTransfer)
		r.Get("/me", h.GetMe)
		r.Get("/user/status", h.GetUserStatus)
		r.Get("/me/entitlement", h.GetMeEntitlement)
		r.Get("/me/invoices", h.GetMeInvoices)
		r.Get("/me/invoices/{invoiceID}/factura.pdf", h.GetMeInvoiceFacturaPDF)
		r.Post("/me/cuit/verify", h.PostMeVerifyCUIT)
		r.Get("/me/plan-catalog-limits", h.GetMePlanCatalogLimits)
		r.Get("/me/plan-pricing", h.GetMePlanPricing)
		r.Get("/me/notifications", h.GetMeNotifications)
		r.Patch("/me/notifications/{notificationID}/read", h.PatchMeNotificationRead)
		r.Get("/downloads/android", h.GetAndroidDownloadURL)
	})
	return r
}
