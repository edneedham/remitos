package server

import (
	"context"
	"net/http"
	"strings"
	"time"

	"cloud.google.com/go/storage"
	"github.com/go-chi/chi/v5"
	"server/config"
	"server/db"
	"server/internal/billing"
	"server/internal/handlers"
	"server/internal/jobs"
	"server/internal/jwt"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	notifymail "server/internal/notifications/email"
	"server/internal/notifications/inapp"
	"server/internal/payments/afip"
	"server/internal/payments/mercadopago"
	"server/internal/repository"
)

// Build wires repositories, optional background jobs, and the HTTP router.
// jobCancel stops email/billing ticker loops; call it before closing the DB during shutdown.
func Build(cfg *config.Config) (http.Handler, context.CancelFunc, error) {
	jobCtx, jobCancel := context.WithCancel(context.Background())

	userRepo := repository.NewUserRepository(db.Pool)
	companyRepo := repository.NewCompanyRepository(db.Pool)
	warehouseRepo := repository.NewWarehouseRepository(db.Pool)
	userWarehouseRepo := repository.NewUserWarehouseRepository(db.Pool)
	deviceRepo := repository.NewDeviceRepository(db.Pool)
	refreshTokenRepo := repository.NewRefreshTokenRepository(db.Pool)
	passwordResetTokenRepo := repository.NewPasswordResetTokenRepository(db.Pool)
	transferRepo := repository.NewWebSessionTransferRepository(db.Pool)
	subscriptionRepo := repository.NewSubscriptionRepository(db.Pool)
	imageRepo := repository.NewImageRepository(db.Pool)
	jwtSvc := jwt.NewService(cfg.JWTSecret)
	mpClient := mercadopago.New(cfg.MercadoPagoAccessToken)

	afipClient, afipBillingActive, afipPadronActive := initAfipClient(cfg)
	logger.Log.Info().
		Bool("afip_billing_configured", afipBillingActive).
		Bool("afip_padron_configured", afipPadronActive).
		Msg("AFIP startup flags (from config; client may still be nil if init failed)")

	var authReleases *handlers.AuthReleasesConfig
	if cfg.GCSReleasesBucket != "" && cfg.AndroidReleaseObject != "" {
		ctx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
		releaseClient, err := storage.NewClient(ctx)
		cancel()
		if err != nil {
			logger.Log.Warn().Err(err).Msg("GCS releases storage unavailable; /auth/downloads/android disabled")
		} else {
			expiry := cfg.ReleasesSignedURLExpiry
			if expiry <= 0 {
				expiry = 15 * time.Minute
			}
			authReleases = &handlers.AuthReleasesConfig{
				Storage: releaseClient,
				Bucket:  cfg.GCSReleasesBucket,
				Object:  cfg.AndroidReleaseObject,
				Expiry:  expiry,
			}
			logger.Log.Info().
				Str("bucket", cfg.GCSReleasesBucket).
				Str("object", cfg.AndroidReleaseObject).
				Msg("Android APK releases (signed URLs) enabled")
		}
	}

	mailSender := notifymail.ConfigureSender(cfg.EmailEnabled, cfg.ResendAPIKey, cfg.EmailFrom, cfg.EmailReplyTo)

	syncRepo := repository.NewSyncRepository(db.Pool)
	invoiceRepo := repository.NewInvoiceRepository(db.Pool)
	notificationRepo := repository.NewUserNotificationRepository(db.Pool)
	panelBroadcaster := inapp.NewBroadcaster(notificationRepo, userRepo, cfg.PublicSiteURL)
	afipTicketRepo := repository.NewAfipTicketRepository(db.Pool)

	var afipTAManager *afip.TAManager
	if afipClient != nil {
		afipTAManager = newAfipTAManager(afipClient, afipTicketRepo)
		logger.Log.Info().Msg("AFIP TA manager initialized (afip_tickets cache backed)")
	}
	var facturaEmitter *billing.FacturaEmitter
	if afipClient != nil && afipTAManager != nil {
		var padronCacheTTL time.Duration
		if cfg.AfipPadronCacheHours > 0 {
			padronCacheTTL = time.Duration(cfg.AfipPadronCacheHours) * time.Hour
		}
		facturaEmitter = &billing.FacturaEmitter{
			Afip:           afipClient,
			TAMgr:          afipTAManager,
			Invoices:       invoiceRepo,
			Companies:      companyRepo,
			BillingEnabled: cfg.AfipBillingEnabled,
			PadronEnabled:  cfg.AfipPadronEnabled,
			PadronCacheTTL: padronCacheTTL,
		}
		if panelBroadcaster != nil {
			facturaEmitter.InApp = panelBroadcaster
		}
	}
	if facturaEmitter != nil && cfg.AfipBillingEnabled {
		jobs.StartBillingFacturaEmitLoop(jobCtx, facturaEmitter, invoiceRepo, 10*time.Minute)
		logger.Log.Info().Msg("Billing AFIP factura retry loop enabled (10m)")
	}

	billingFx := &billing.MEPWithFallback{
		HTTP: &http.Client{
			Timeout: 20 * time.Second,
		},
		BolsaURL:          cfg.BillingMEPBolsaURL,
		FallbackARSPerUSD: cfg.BillingUSDToARSRate,
	}

	if cfg.EmailEnabled && strings.TrimSpace(cfg.ResendAPIKey) != "" && strings.TrimSpace(cfg.EmailFrom) != "" {
		go func() {
			jobs.StartTrialOnboardingNudgeLoop(jobCtx, companyRepo, mailSender, cfg.PublicSiteURL)
		}()
		logger.Log.Info().Msg("Trial onboarding email nudges enabled (5m ticker)")
		go func() {
			jobs.StartSubscriptionRenewalReminderLoop(
				jobCtx,
				companyRepo,
				mailSender,
				billingFx,
				cfg.BillingFXBufferFraction,
				cfg.PublicSiteURL,
				panelBroadcaster,
			)
		}()
		logger.Log.Info().Msg("Subscription renewal reminder emails enabled (1h ticker)")
		go func() {
			jobs.StartTrialEndingNoticeLoop(jobCtx, companyRepo, mailSender, cfg.PublicSiteURL, panelBroadcaster)
		}()
		logger.Log.Info().Msg("Trial ending notice emails enabled (1h ticker)")
		go func() {
			jobs.StartSubscriptionLapseNoticeLoop(jobCtx, companyRepo, mailSender, cfg.PublicSiteURL, panelBroadcaster)
		}()
		logger.Log.Info().Msg("Subscription lapse notice emails enabled (1h ticker)")
	}

	waitlistRepo := repository.NewWaitlistRepository(db.Pool)
	publicHandler := handlers.NewPublicHandler(handlers.PublicHandlerConfig{
		WaitlistRepo:  waitlistRepo,
		Mailer:        mailSender,
		PublicSiteURL: cfg.PublicSiteURL,
	})

	authHandler := handlers.NewAuthHandlerFromConfig(handlers.AuthHandlerConfig{
		UserRepo:                userRepo,
		CompanyRepo:             companyRepo,
		WarehouseRepo:           warehouseRepo,
		SyncRepo:                syncRepo,
		InvoiceRepo:             invoiceRepo,
		DeviceRepo:              deviceRepo,
		UserWarehouseRepo:       userWarehouseRepo,
		RefreshTokenRepo:        refreshTokenRepo,
		PasswordResetTokenRepo:  passwordResetTokenRepo,
		TransferRepo:            transferRepo,
		SubscriptionRepo:        subscriptionRepo,
		NotificationRepo:        notificationRepo,
		DB:                      db.Pool,
		JwtSvc:                  jwtSvc,
		MercadoPago:             mpClient,
		SignupAllowMockPayment:  cfg.SignupAllowMockPayment,
		Releases:                authReleases,
		Mailer:                  mailSender,
		PublicSiteURL:           cfg.PublicSiteURL,
		BillingRateQuoter:       billingFx,
		BillingFXBufferFraction: cfg.BillingFXBufferFraction,
		FacturaEmitter:          facturaEmitter,
		AfipClient:              afipClient,
		WaitlistOnly:            cfg.WaitlistOnly,
	})

	mpWebhookHandler := handlers.NewMercadoPagoWebhookHandler(
		db.Pool,
		invoiceRepo,
		companyRepo,
		userRepo,
		mpClient,
		mailSender,
		cfg.PublicSiteURL,
		cfg.BillingFXBufferFraction,
		cfg.MercadoPagoWebhookSecret,
		facturaEmitter,
		panelBroadcaster,
	)
	warehouseHandler := handlers.NewWarehouseHandler(warehouseRepo, companyRepo, deviceRepo, userWarehouseRepo, jwtSvc)
	deviceHandler := handlers.NewDeviceHandler(deviceRepo, userWarehouseRepo, jwtSvc, panelBroadcaster)
	adminHandler := handlers.NewAdminHandler(userRepo, companyRepo, deviceRepo, jwtSvc, panelBroadcaster)
	scanHandler, err := handlers.NewScanHandler()
	if err != nil {
		logger.Log.Warn().Err(err).Msg("Failed to initialize scan handler, /scan endpoint will not be available")
	}

	imageHandler, err := handlers.NewImageHandler(imageRepo, deviceRepo)
	if err != nil {
		logger.Log.Warn().Err(err).Msg("Failed to initialize image handler, image upload will not be available")
		imageHandler = nil
	}

	syncHandler := handlers.NewSyncHandler(syncRepo, companyRepo, notificationRepo, panelBroadcaster)

	h := chi.NewRouter()

	h.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("ok"))
	})

	h.Get("/health/ready", func(w http.ResponseWriter, r *http.Request) {
		ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
		defer cancel()
		if err := db.Pool.Ping(ctx); err != nil {
			logger.Log.Warn().Err(err).Msg("health ready: database ping failed")
			w.WriteHeader(http.StatusServiceUnavailable)
			_, _ = w.Write([]byte("db unavailable"))
			return
		}
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("ok"))
	})

	for _, p := range []string{
		"/webhooks/mercadopago",
		"/webhooks/mercadopago/",
		"/api/webhooks/mercadopago",
		"/api/webhooks/mercadopago/",
	} {
		h.Get(p, mpWebhookHandler.Ping)
		h.Post(p, mpWebhookHandler.PostNotification)
	}

	h.Mount("/auth", authHandler.Routes())
	h.Mount("/public", publicHandler.Routes())
	var renewalSvc *billing.RenewalService
	if cfg.BillingRenewalSecret != "" || cfg.BillingAutomaticRenewalEnabled {
		renewalSvc = billing.NewRenewalService(
			db.Pool,
			companyRepo,
			invoiceRepo,
			userRepo,
			mpClient,
			cfg.BillingStubAutoCharge,
			billingFx,
			cfg.BillingFXBufferFraction,
			mailSender,
			cfg.PublicSiteURL,
			facturaEmitter,
			panelBroadcaster,
		)
	}
	if cfg.BillingRenewalSecret != "" && renewalSvc != nil {
		billingRenewalHandler := handlers.NewBillingRenewalHandler(renewalSvc)
		billingFacturaHandler := handlers.NewBillingFacturaHandler(facturaEmitter)
		h.Route("/internal/billing", func(r chi.Router) {
			r.Use(middleware.BillingRenewalSecret(cfg.BillingRenewalSecret))
			r.Post("/trigger-renewal", billingRenewalHandler.PostTriggerRenewal)
			if facturaEmitter != nil {
				r.Post("/invoices/{invoiceID}/emit-factura", billingFacturaHandler.PostEmitFactura)
			}
		})
		logger.Log.Info().Msg("Billing renewal endpoint enabled at POST /internal/billing/trigger-renewal")
	}
	if cfg.BillingAutomaticRenewalEnabled && renewalSvc != nil {
		poll := time.Duration(cfg.BillingRenewalPollMinutes) * time.Minute
		if cfg.BillingRenewalPollMinutes <= 0 {
			poll = time.Hour
		}
		jobs.StartBillingRenewalSweep(jobCtx, renewalSvc, companyRepo, poll)
	}
	h.Mount("/warehouses", warehouseHandler.Routes())
	h.Mount("/devices", deviceHandler.Routes())
	if scanHandler != nil {
		h.Group(func(r chi.Router) {
			r.Use(middleware.Auth(middleware.AuthDeps{JwtSvc: jwtSvc, DeviceRepo: deviceRepo, UserWarehouseRepo: userWarehouseRepo}))
			r.Mount("/scan", scanHandler.Routes())
		})
	}
	h.Group(func(r chi.Router) {
		r.Use(middleware.Auth(middleware.AuthDeps{JwtSvc: jwtSvc, DeviceRepo: deviceRepo, UserWarehouseRepo: userWarehouseRepo}))
		r.Use(middleware.RequireRoles(models.RoleCompanyOwner, models.RoleWarehouseAdmin))
		r.Mount("/admin", adminHandler.Routes())
	})
	if imageHandler != nil {
		h.Group(func(r chi.Router) {
			r.Use(middleware.Auth(middleware.AuthDeps{JwtSvc: jwtSvc, DeviceRepo: deviceRepo, UserWarehouseRepo: userWarehouseRepo}))
			r.Mount("/images", imageHandler.Routes())
		})
		logger.Log.Info().Msg("Image upload endpoint registered at /images")
	}
	h.Group(func(r chi.Router) {
		r.Use(middleware.Auth(middleware.AuthDeps{JwtSvc: jwtSvc, DeviceRepo: deviceRepo, UserWarehouseRepo: userWarehouseRepo}))
		r.Mount("/sync", syncHandler.Routes())
	})
	logger.Log.Info().Msg("Sync endpoint registered at /sync")

	r := middleware.Router(h, cfg.CorsAllowedOrigins)
	return r, jobCancel, nil
}
