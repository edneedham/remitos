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
	"server/internal/handlers"
	"server/internal/jwt"
	"server/internal/logger"
	"server/internal/middleware"
	notifymail "server/internal/notifications/email"
	"server/internal/payments/mercadopago"
	"server/internal/repository"
)

func main() {
	logger.Init()
	logger.Log.Info().Str("version", Version).Msg("Starting server")

	if err := godotenv.Load(); err != nil {
		logger.Log.Info().Msg("No .env file found, using environment variables")
	}

	cfg := config.Load()

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
	deviceRepo := repository.NewDeviceRepository(db.Pool)
	refreshTokenRepo := repository.NewRefreshTokenRepository(db.Pool)
	transferRepo := repository.NewWebSessionTransferRepository(db.Pool)
	subscriptionRepo := repository.NewSubscriptionRepository(db.Pool)
	imageRepo := repository.NewImageRepository(db.Pool)
	jwtSvc := jwt.NewService(cfg.JWTSecret)
	mpClient := mercadopago.New(cfg.MercadoPagoAccessToken)

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
	authHandler := handlers.NewAuthHandler(userRepo, companyRepo, warehouseRepo, syncRepo, invoiceRepo, deviceRepo, refreshTokenRepo, transferRepo, subscriptionRepo, db.Pool, jwtSvc, mpClient, cfg.SignupAllowMockPayment, authReleases, mailSender, cfg.PublicSiteURL)
	warehouseHandler := handlers.NewWarehouseHandler(warehouseRepo)
	adminHandler := handlers.NewAdminHandler(userRepo, deviceRepo, jwtSvc)
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

	h.Mount("/auth", authHandler.Routes())
	h.Mount("/warehouses", warehouseHandler.Routes())
	if scanHandler != nil {
		h.Group(func(r chi.Router) {
			r.Use(middleware.Auth(middleware.AuthDeps{JwtSvc: jwtSvc, DeviceRepo: deviceRepo}))
			r.Mount("/scan", scanHandler.Routes())
		})
	}
	h.Group(func(r chi.Router) {
		r.Use(middleware.Auth(middleware.AuthDeps{JwtSvc: jwtSvc, DeviceRepo: deviceRepo}))
		r.Use(middleware.RequireRole("admin"))
		r.Mount("/admin", adminHandler.Routes())
	})
	if imageHandler != nil {
		h.Group(func(r chi.Router) {
			r.Use(middleware.Auth(middleware.AuthDeps{JwtSvc: jwtSvc, DeviceRepo: deviceRepo}))
			r.Mount("/images", imageHandler.Routes())
		})
		logger.Log.Info().Msg("Image upload endpoint registered at /images")
	}
	h.Group(func(r chi.Router) {
		r.Use(middleware.Auth(middleware.AuthDeps{JwtSvc: jwtSvc, DeviceRepo: deviceRepo}))
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
