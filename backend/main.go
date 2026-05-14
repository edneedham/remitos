package main

import (
	"context"
	"errors"
	"fmt"
	"net"
	"net/url"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/pgx/v5"
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
	migrateURL := migrateDatabaseURL(cfg)
	m, err := migrate.New(
		"file://db/migrations",
		migrateURL,
	)
	if err != nil {
		return fmt.Errorf("failed to create migrate instance: %w", err)
	}
	defer func() {
		_, _ = m.Close()
	}()

	if ver, dirty, verr := m.Version(); verr == nil {
		logger.Log.Info().
			Uint("current_migration_version", ver).
			Bool("dirty", dirty).
			Msg("Migration state before Up")
	} else if errors.Is(verr, migrate.ErrNilVersion) {
		logger.Log.Info().Msg("Migration state before Up: no version yet (fresh database)")
	} else {
		logger.Log.Warn().Err(verr).Msg("Could not read migration version before Up")
	}

	if err := m.Up(); err != nil && err != migrate.ErrNoChange {
		var dirty migrate.ErrDirty
		if errors.As(err, &dirty) {
			return fmt.Errorf("migrations are dirty at version %d (fix DB then migrate force): %w", dirty.Version, err)
		}
		return fmt.Errorf("failed to run migrations: %w", err)
	}

	logger.Log.Info().Msg("Migrations completed successfully")
	return nil
}

// migrateDatabaseURL uses the pgx5 migrate driver (same stack as pgxpool) and
// URL-encodes credentials so passwords with @, :, etc. match lib/pq behavior.
func migrateDatabaseURL(cfg *config.Config) string {
	u := &url.URL{
		Scheme: "pgx5",
		User:   url.UserPassword(cfg.DBUser, cfg.DBPassword),
		Host:   net.JoinHostPort(cfg.DBHost, strconv.Itoa(cfg.DBPort)),
		Path:   "/" + cfg.DBName,
	}
	q := url.Values{}
	q.Set("sslmode", cfg.DBSSLMode)
	u.RawQuery = q.Encode()
	return u.String()
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
