// Package integration provides PostgreSQL helpers for opt-in integration tests.
// Set REMITOS_TEST_PG_DSN to a database URL (e.g. postgres://user:pass@127.0.0.1:5432/postgres?sslmode=disable).
// Tests call PostgresDSN which skips when unset so CI without Postgres still passes.
package integration

import (
	"context"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/golang-migrate/migrate/v4"
	_ "github.com/golang-migrate/migrate/v4/database/postgres"
	_ "github.com/golang-migrate/migrate/v4/source/file"
	"github.com/jackc/pgx/v5/pgxpool"
)

// PostgresDSN returns the integration database URL or skips the test when unset.
func PostgresDSN(t *testing.T) string {
	t.Helper()
	dsn := strings.TrimSpace(os.Getenv("REMITOS_TEST_PG_DSN"))
	if dsn == "" {
		t.Skip("integration: set REMITOS_TEST_PG_DSN (postgres URL with rights to create DB or use an empty test DB)")
	}
	return dsn
}

// ModuleRoot finds the directory containing go.mod (the backend module root).
func ModuleRoot(t *testing.T) string {
	t.Helper()
	wd, err := os.Getwd()
	if err != nil {
		t.Fatalf("getwd: %v", err)
	}
	for d := wd; ; {
		if _, err := os.Stat(filepath.Join(d, "go.mod")); err == nil {
			return d
		}
		parent := filepath.Dir(d)
		if parent == d {
			t.Fatal("go.mod not found from cwd")
		}
		d = parent
	}
}

// MustMigrateUp applies all migrations from db/migrations (run from backend module root).
func MustMigrateUp(t *testing.T, databaseURL string) {
	t.Helper()
	root := ModuleRoot(t)
	migURL := "file://" + filepath.Join(root, "db", "migrations")
	m, err := migrate.New(migURL, databaseURL)
	if err != nil {
		t.Fatalf("migrate.New: %v", err)
	}
	defer func() { _, _ = m.Close() }()
	if err := m.Up(); err != nil && err != migrate.ErrNoChange {
		t.Fatalf("migrate.Up: %v", err)
	}
}

// NewPool connects and runs migrations once per pool.
func NewPool(t *testing.T, databaseURL string) *pgxpool.Pool {
	t.Helper()
	MustMigrateUp(t, databaseURL)
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	pool, err := pgxpool.New(ctx, databaseURL)
	if err != nil {
		t.Fatalf("pgxpool.New: %v", err)
	}
	t.Cleanup(func() { pool.Close() })
	return pool
}

// TruncateTenantData removes tenant-scoped rows (companies CASCADE wipes dependents).
func TruncateTenantData(t *testing.T, pool *pgxpool.Pool) {
	t.Helper()
	ctx := context.Background()
	_, err := pool.Exec(ctx, `TRUNCATE companies CASCADE`)
	if err != nil {
		t.Fatalf("truncate: %v", err)
	}
}
