package main

import (
	"context"
	"errors"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/pietro-a/isp-network-tester/server/internal/config"
	"github.com/pietro-a/isp-network-tester/server/internal/middleware"
	"github.com/pietro-a/isp-network-tester/server/internal/server"
	"github.com/pietro-a/isp-network-tester/server/internal/version"
)

func main() {
	var logLevel = new(slog.LevelVar)
	logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level:     logLevel,
		AddSource: true,
	}))

	cfg, err := config.ParseConfig()
	if err != nil {
		logger.Error("configuration error", "err", err)
		os.Exit(1)
	}

	logLevel.Set(cfg.LogLevel)

	srv, err := server.New(cfg.BufferSize, cfg.MaxDownloadSize, cfg.MaxConcurrentDownloads, logger)
	if err != nil {
		logger.Error("data buffer error", "err", err)
		os.Exit(1)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", srv.Health)
	mux.HandleFunc("GET /download", srv.Download)

	handler := middleware.RequestLogger(logger)(
		middleware.Recover(logger)(mux),
	)

	httpServer := &http.Server{
		Addr:              cfg.ListenAddr,
		Handler:           handler,
		ReadHeaderTimeout: time.Duration(cfg.ReadHeaderTimeout) * time.Second,
		IdleTimeout:       time.Duration(cfg.IdleTimeout) * time.Second,
		MaxHeaderBytes:    1 << 20, // 1 MiB
	}

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	serverErr := make(chan error, 1)

	go func() {
		logger.Info("starting ispnettest server", "version", version.GetVersion())
		serverErr <- httpServer.ListenAndServe()
	}()

	select {
	case err := <-serverErr:
		if !errors.Is(err, http.ErrServerClosed) {
			logger.Error("ispnettest server failed", "err", err)
			os.Exit(1)
		}

	case <-ctx.Done():
		shutdownCtx, cancel := context.WithTimeout(
			context.Background(),
			time.Duration(cfg.ShutdownTimeout)*time.Second,
		)
		defer cancel()

		if err := httpServer.Shutdown(shutdownCtx); err != nil {
			logger.Error("ispnettest server shutdown failed", "err", err)
			_ = httpServer.Close()
			os.Exit(1)
		}
		logger.Info("ispnettest server stopped")
	}
}
