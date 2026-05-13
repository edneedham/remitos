package server

import (
	"context"
	"fmt"
	"net"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"server/internal/logger"
)

// ListenConfig configures the HTTP server lifecycle.
type ListenConfig struct {
	Addr       string
	Handler    http.Handler
	JobCancel  context.CancelFunc
	Version    string
	ShutdownMS int64
}

// ListenAndShutdown runs the server until SIGINT/SIGTERM, then cancels background jobs,
// gracefully shuts down HTTP, and returns (caller should close DB, etc.).
func ListenAndShutdown(cfg ListenConfig) error {
	if cfg.ShutdownMS <= 0 {
		cfg.ShutdownMS = 30_000
	}
	srv := &http.Server{
		Addr:              cfg.Addr,
		Handler:           cfg.Handler,
		ReadHeaderTimeout: 10 * time.Second,
		ReadTimeout:       6 * time.Minute,
		WriteTimeout:      6 * time.Minute,
		IdleTimeout:       120 * time.Second,
	}

	ln, err := net.Listen("tcp", cfg.Addr)
	if err != nil {
		return fmt.Errorf("listen %s: %w", cfg.Addr, err)
	}

	errCh := make(chan error, 1)
	go func() {
		errCh <- srv.Serve(ln)
	}()

	logger.Log.Info().
		Str("addr", ln.Addr().String()).
		Str("version", cfg.Version).
		Dur("read_header_timeout", srv.ReadHeaderTimeout).
		Dur("read_timeout", srv.ReadTimeout).
		Dur("write_timeout", srv.WriteTimeout).
		Dur("idle_timeout", srv.IdleTimeout).
		Msg("HTTP server listening")

	sigCtx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	select {
	case err := <-errCh:
		if cfg.JobCancel != nil {
			cfg.JobCancel()
		}
		if err != nil {
			return err
		}
	case <-sigCtx.Done():
	}

	if cfg.JobCancel != nil {
		cfg.JobCancel()
		logger.Log.Info().Msg("Background job context cancelled")
	}

	shutdownCtx, cancel := context.WithTimeout(context.Background(), time.Duration(cfg.ShutdownMS)*time.Millisecond)
	defer cancel()
	if err := srv.Shutdown(shutdownCtx); err != nil {
		logger.Log.Warn().Err(err).Msg("HTTP server shutdown completed with error")
	} else {
		logger.Log.Info().Msg("HTTP server shut down gracefully")
	}

	// Drain Serve return after Shutdown.
	<-errCh
	return nil
}

// Port returns PORT or default 8080.
func Port() string {
	port := os.Getenv("PORT")
	if port == "" {
		return "8080"
	}
	return port
}

// ListenAddr returns host:port for HTTP. Uses 0.0.0.0 so Cloud Run and similar
// platforms accept IPv4 health checks (binding only to [::] can fail readiness).
func ListenAddr() string {
	return "0.0.0.0:" + Port()
}
