package main

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"github.com/joho/godotenv"
	"server/config"
	"server/db"
	"server/internal/logger"
	"server/internal/server"
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

	logger.Log.Info().Msg("Connected to database successfully")

	if err := runMigrations(cfg); err != nil {
		db.Close()
		logger.Log.Fatal().Err(err).Msg("Failed to run migrations")
	}

	if cfg.SeedLocalDevUsers {
		if err := seedLocalDevUsers(context.Background()); err != nil {
			db.Close()
			logger.Log.Fatal().Err(err).Msg("Failed to seed local dev users")
		}
	}

	logger.Log.Info().Msg("Database setup complete!")

	handler, jobCancel, err := server.Build(cfg)
	if err != nil {
		db.Close()
		logger.Log.Fatal().Err(err).Msg("Failed to build HTTP handler")
	}

	addr := server.ListenAddr()
	if err := server.ListenAndShutdown(server.ListenConfig{
		Addr:      addr,
		Handler:   handler,
		JobCancel: jobCancel,
		Version:   Version,
	}); err != nil {
		logger.Log.Error().Err(err).Msg("Server stopped with error")
	}
	db.Close()
	logger.Log.Info().Msg("Database pool closed")
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
