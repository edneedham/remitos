// Package db holds the global PostgreSQL pool used by the binary.
//
// Prefer passing *pgxpool.Pool into constructors from new code (see internal/server.Build)
// so tests and alternate entrypoints can inject a pool without mutating this package.
package db

import (
	"context"
	"fmt"
	"runtime"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"server/config"
)

var Pool *pgxpool.Pool

func Connect(cfg *config.Config) error {
	dsn := fmt.Sprintf(
		"postgres://%s:%s@%s:%d/%s?sslmode=%s",
		cfg.DBUser,
		cfg.DBPassword,
		cfg.DBHost,
		cfg.DBPort,
		cfg.DBName,
		cfg.DBSSLMode,
	)

	poolConfig, err := pgxpool.ParseConfig(dsn)
	if err != nil {
		return fmt.Errorf("unable to parse database config: %w", err)
	}

	maxConns := cfg.DBPoolMaxConns
	if maxConns <= 0 {
		n := runtime.NumCPU() * 4
		if n < 4 {
			n = 4
		}
		if n > 32 {
			n = 32
		}
		maxConns = n
	}
	poolConfig.MaxConns = int32(maxConns)
	poolConfig.MaxConnLifetime = time.Hour
	poolConfig.MaxConnIdleTime = 30 * time.Minute

	// Avoid hanging forever on Cloud Run if DB host/port/firewall is wrong (startup probe timeout).
	if poolConfig.ConnConfig != nil && poolConfig.ConnConfig.ConnectTimeout == 0 {
		poolConfig.ConnConfig.ConnectTimeout = 45 * time.Second
	}

	poolCtx, poolCancel := context.WithTimeout(context.Background(), 90*time.Second)
	defer poolCancel()
	Pool, err = pgxpool.NewWithConfig(poolCtx, poolConfig)
	if err != nil {
		return fmt.Errorf("unable to create connection pool: %w", err)
	}

	pingCtx, pingCancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer pingCancel()
	if err := Pool.Ping(pingCtx); err != nil {
		return fmt.Errorf("unable to ping database: %w", err)
	}

	return nil
}

func Close() {
	if Pool != nil {
		Pool.Close()
	}
}
