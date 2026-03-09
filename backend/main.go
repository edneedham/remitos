package main

import (
	"fmt"
	"net/http"
	"os"

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

	logger.Log.Info().Msg("Database setup complete!")

	userRepo := repository.NewUserRepository(db.Pool)
	companyRepo := repository.NewCompanyRepository(db.Pool)
	warehouseRepo := repository.NewWarehouseRepository(db.Pool)
	deviceRepo := repository.NewDeviceRepository(db.Pool)
	refreshTokenRepo := repository.NewRefreshTokenRepository(db.Pool)
	jwtSvc := jwt.NewService(cfg.JWTSecret)
	authHandler := handlers.NewAuthHandler(userRepo, companyRepo, warehouseRepo, deviceRepo, refreshTokenRepo, db.Pool, jwtSvc)
	warehouseHandler := handlers.NewWarehouseHandler(warehouseRepo)
	adminHandler := handlers.NewAdminHandler(userRepo, deviceRepo, jwtSvc)
	scanHandler, err := handlers.NewScanHandler()
	if err != nil {
		logger.Log.Warn().Err(err).Msg("Failed to initialize scan handler, /scan endpoint will not be available")
	}

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
	r := middleware.Router(h)

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
