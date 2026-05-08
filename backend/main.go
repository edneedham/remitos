package main

import (
	"context"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"

	"cloud.google.com/go/storage"
	"github.com/go-chi/chi/v5"
	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"github.com/joho/godotenv"
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
	"server/internal/payments/afip"
	"server/internal/payments/afip/certprovider"
	"server/internal/payments/afip/wsaa"
	"server/internal/payments/mercadopago"
	"server/internal/repository"
)

func main() {
	if err := godotenv.Load(); err != nil {
		// Logging not configured yet; env vars may still be set by the process environment.
	}
	cfg := config.Load()
	logger.Init(cfg.LogLevel)
	logger.Log.Info().Str("version", Version).Msg("Starting server")

	if strings.TrimSpace(cfg.JWTSecret) == "" {
		logger.Log.Fatal().Msg("JWT_SECRET must be set to a non-empty value")
	}
	if strings.TrimSpace(cfg.MercadoPagoAccessToken) != "" && strings.TrimSpace(cfg.MercadoPagoWebhookSecret) == "" {
		logger.Log.Warn().Msg("MERCADOPAGO_WEBHOOK_SECRET is empty; webhook x-signature verification is disabled. Set the secret from Mercado Pago → Your integrations → Webhooks.")
	}

	if err := db.Connect(cfg); err != nil {
		logger.Log.Fatal().Err(err).Msg("Failed to connect to database")
	}
	defer db.Close()

	logger.Log.Info().Msg("Connected to database successfully")

	if err := runMigrations(cfg); err != nil {
		logger.Log.Fatal().Err(err).Msg("Failed to run migrations")
	}

	if cfg.SeedLocalDevUsers {
		if err := seedLocalDevUsers(context.Background()); err != nil {
			logger.Log.Fatal().Err(err).Msg("Failed to seed local dev users")
		}
	}

	logger.Log.Info().Msg("Database setup complete!")

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

	var authReleases *handlers.AuthReleasesConfig
	if cfg.GCSReleasesBucket != "" && cfg.AndroidReleaseObject != "" {
		releaseClient, err := storage.NewClient(context.Background())
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
	}
	if facturaEmitter != nil && cfg.AfipBillingEnabled {
		jobs.StartBillingFacturaEmitLoop(context.Background(), facturaEmitter, invoiceRepo, 10*time.Minute)
		logger.Log.Info().Msg("Billing AFIP factura retry loop enabled (10m)")
	}
	_ = afipBillingActive
	_ = afipPadronActive
	billingFx := &billing.MEPWithFallback{
		HTTP: &http.Client{
			Timeout: 20 * time.Second,
		},
		BolsaURL:          cfg.BillingMEPBolsaURL,
		FallbackARSPerUSD: cfg.BillingUSDToARSRate,
	}

	if cfg.EmailEnabled && strings.TrimSpace(cfg.ResendAPIKey) != "" && strings.TrimSpace(cfg.EmailFrom) != "" {
		go func() {
			jobs.StartTrialOnboardingNudgeLoop(context.Background(), companyRepo, mailSender, cfg.PublicSiteURL)
		}()
		logger.Log.Info().Msg("Trial onboarding email nudges enabled (5m ticker)")
		go func() {
			jobs.StartSubscriptionRenewalReminderLoop(
				context.Background(),
				companyRepo,
				mailSender,
				billingFx,
				cfg.BillingFXBufferFraction,
				cfg.PublicSiteURL,
			)
		}()
		logger.Log.Info().Msg("Subscription renewal reminder emails enabled (1h ticker)")
		go func() {
			jobs.StartTrialEndingNoticeLoop(context.Background(), companyRepo, mailSender, cfg.PublicSiteURL)
		}()
		logger.Log.Info().Msg("Trial ending notice emails enabled (1h ticker)")
		go func() {
			jobs.StartSubscriptionLapseNoticeLoop(context.Background(), companyRepo, mailSender, cfg.PublicSiteURL)
		}()
		logger.Log.Info().Msg("Subscription lapse notice emails enabled (1h ticker)")
	}
	authHandler := handlers.NewAuthHandler(userRepo, companyRepo, warehouseRepo, syncRepo, invoiceRepo, deviceRepo, userWarehouseRepo, refreshTokenRepo, passwordResetTokenRepo, transferRepo, subscriptionRepo, db.Pool, jwtSvc, mpClient, cfg.SignupAllowMockPayment, authReleases, mailSender, cfg.PublicSiteURL, billingFx, cfg.BillingFXBufferFraction, facturaEmitter, afipClient)
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
	)
	warehouseHandler := handlers.NewWarehouseHandler(warehouseRepo, companyRepo, deviceRepo, userWarehouseRepo, jwtSvc)
	deviceHandler := handlers.NewDeviceHandler(deviceRepo, userWarehouseRepo, jwtSvc)
	adminHandler := handlers.NewAdminHandler(userRepo, companyRepo, deviceRepo, jwtSvc)
	scanHandler, err := handlers.NewScanHandler()
	if err != nil {
		logger.Log.Warn().Err(err).Msg("Failed to initialize scan handler, /scan endpoint will not be available")
	}

	// Initialize image handler (optional - won't fail startup if GCS is not configured)
	imageHandler, err := handlers.NewImageHandler(imageRepo, deviceRepo)
	if err != nil {
		logger.Log.Warn().Err(err).Msg("Failed to initialize image handler, image upload will not be available")
		imageHandler = nil
	}

	syncHandler := handlers.NewSyncHandler(syncRepo, companyRepo)

	h := chi.NewRouter()

	h.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("ok"))
	})

	// Readiness: includes Postgres ping. Use for load balancers / uptime checks that should fail when DB is down.
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
		w.Write([]byte("ok"))
	})

	// Mercado Pago may POST with a trailing slash or via proxies that add /api; aliases avoid false 404s.
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
		jobs.StartBillingRenewalSweep(context.Background(), renewalSvc, companyRepo, poll)
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

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	logger.Log.Info().Str("port", port).Msg("Server starting")
	if err := http.ListenAndServe(":"+port, r); err != nil {
		logger.Log.Fatal().Err(err).Msg("Failed to start server")
	}
}

func runMigrations(cfg *config.Config) error {
	m, err := migrate.New(
		"file://db/migrations",
		fmt.Sprintf("postgres://%s:%s@%s:%d/%s?sslmode=%s",
			cfg.DBUser, cfg.DBPassword, cfg.DBHost, cfg.DBPort, cfg.DBName, cfg.DBSSLMode),
	)
	if err != nil {
		return fmt.Errorf("failed to create migrate instance: %w", err)
	}

	if err := m.Up(); err != nil && err != migrate.ErrNoChange {
		return fmt.Errorf("failed to run migrations: %w", err)
	}

	logger.Log.Info().Msg("Migrations completed successfully")
	return nil
}

// initAfipClient builds the AFIP/ARCA client when AFIP_BILLING_ENABLED or AFIP_PADRON_ENABLED is set.
// Returns nil when neither flag is enabled or when required emisor/cert configuration is missing
// (server keeps booting; the dependent features simply stay off).
func initAfipClient(cfg *config.Config) (*afip.Client, bool, bool) {
	billingOn := cfg.AfipBillingEnabled
	padronOn := cfg.AfipPadronEnabled
	if !billingOn && !padronOn {
		return nil, false, false
	}
	if cfg.AfipCUIT == "" || cfg.AfipPuntoVenta <= 0 {
		logger.Log.Warn().Msg("AFIP feature flag set but AFIP_CUIT / AFIP_PUNTO_VENTA missing; AFIP integration disabled")
		return nil, false, false
	}

	var provider certprovider.CertProvider
	if cfg.AfipCertSecretName != "" && cfg.AfipKeySecretName != "" {
		provider = certprovider.NewGCPSecret(cfg.AfipCertSecretName, cfg.AfipKeySecretName)
	} else {
		provider = certprovider.NewEnv(cfg.AfipCertPEM, cfg.AfipCertPath, cfg.AfipKeyPEM, cfg.AfipKeyPath)
	}

	client, err := afip.New(afip.Config{
		Env:             cfg.AfipEnv,
		CUIT:            cfg.AfipCUIT,
		PuntoVenta:      cfg.AfipPuntoVenta,
		IssuerCondicion: cfg.AfipIssuerCondicionIVA,
		Concepto:        cfg.AfipConcepto,
		DefaultAlicuota: cfg.AfipDefaultAlicuotaIVA,
		Certs:           provider,
		WSAAURL:         cfg.AfipWSAAURL,
		WSFEv1URL:       cfg.AfipWSFEv1URL,
		PadronURL:       cfg.AfipPadronURL,
	})
	if err != nil {
		logger.Log.Warn().Err(err).Msg("AFIP client init failed; integration disabled")
		return nil, false, false
	}
	logger.Log.Info().
		Str("env", string(client.Env)).
		Str("cuit", client.CUIT).
		Int("pto_vta", client.PuntoVenta).
		Bool("billing", billingOn).
		Bool("padron", padronOn).
		Msg("AFIP/ARCA integration enabled")
	return client, billingOn, padronOn
}

// newAfipTAManager wires the AfipTicketRepository into the afip.TAManager via a closure-based
// store, keeping the repository package free of afip imports.
func newAfipTAManager(client *afip.Client, repo *repository.AfipTicketRepository) *afip.TAManager {
	wsaaClient := wsaa.New(client.WSAAURL(), nil, client.Certs)
	store := &afip.FuncTAStore{
		GetFn: func(ctx context.Context, service string) (*afip.StoredTA, error) {
			row, err := repo.Get(ctx, service)
			if err != nil || row == nil {
				return nil, err
			}
			return &afip.StoredTA{
				Service:        row.Service,
				Token:          row.Token,
				Sign:           row.Sign,
				GenerationTime: row.GenerationTime,
				ExpirationTime: row.ExpirationTime,
			}, nil
		},
		UpsertFn: func(ctx context.Context, ta afip.StoredTA) error {
			return repo.Upsert(ctx, repository.AfipTicket{
				Service:        ta.Service,
				Token:          ta.Token,
				Sign:           ta.Sign,
				GenerationTime: ta.GenerationTime,
				ExpirationTime: ta.ExpirationTime,
			})
		},
	}
	return afip.NewTAManager(wsaaClient, store)
}

func seedLocalDevUsers(ctx context.Context) error {
	// Executed from the backend module directory (e.g. `cd backend && go run .`).
	const rel = "db/seed/local_dev_users.sql"
	path := filepath.Clean(rel)

	sqlBytes, err := os.ReadFile(path)
	if err != nil {
		return fmt.Errorf("read seed file %s: %w", path, err)
	}

	sqlText := strings.TrimSpace(string(sqlBytes))
	if sqlText == "" {
		return fmt.Errorf("seed file %s is empty", path)
	}

	tag, err := db.Pool.Exec(ctx, sqlText)
	if err != nil {
		return fmt.Errorf("execute seed file %s: %w", path, err)
	}

	logger.Log.Info().
		Str("path", path).
		Str("result", tag.String()).
		Msg("Seeded local dev users")
	return nil
}
