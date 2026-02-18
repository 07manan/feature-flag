package main

import (
	"context"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/lmittmann/tint"

	"github.com/manan/feature-flag/evaluation-api/internal/cache"
	"github.com/manan/feature-flag/evaluation-api/internal/config"
	"github.com/manan/feature-flag/evaluation-api/internal/handler"
	"github.com/manan/feature-flag/evaluation-api/internal/repository"
	"github.com/manan/feature-flag/evaluation-api/internal/service"
	"github.com/manan/feature-flag/evaluation-api/internal/subscriber"
)

func main() {
	logger := slog.New(tint.NewHandler(os.Stdout, &tint.Options{
		Level: slog.LevelInfo,
	}))
	slog.SetDefault(logger)

	cfg, err := config.Load()
	if err != nil {
		logger.Error("failed to load configuration", "error", err)
		os.Exit(1)
	}

	ctx := context.Background()
	poolConfig, err := pgxpool.ParseConfig(cfg.Database.ConnectionString())
	if err != nil {
		logger.Error("failed to parse database config", "error", err)
		os.Exit(1)
	}

	poolConfig.MaxConns = int32(cfg.Database.MaxConns)
	poolConfig.MinConns = int32(cfg.Database.MinConns)
	poolConfig.MaxConnLifetime = cfg.Database.MaxConnLifetime
	poolConfig.MaxConnIdleTime = cfg.Database.MaxConnIdleTime

	pool, err := pgxpool.NewWithConfig(ctx, poolConfig)
	if err != nil {
		logger.Error("failed to create database pool", "error", err)
		os.Exit(1)
	}
	defer pool.Close()

	if err := pool.Ping(ctx); err != nil {
		logger.Error("failed to connect to database", "error", err)
		os.Exit(1)
	}
	logger.Info("connected to database")

	memoryCache, err := cache.NewMemoryCache(cfg.MemoryCache, logger)
	if err != nil {
		logger.Error("failed to initialize memory cache", "error", err)
		os.Exit(1)
	}

	redisCache, err := cache.NewRedisCache(cfg.Redis, logger)
	if err != nil {
		logger.Error("failed to connect to redis", "error", err)
		os.Exit(1)
	}

	tieredCache := cache.NewTieredCache(memoryCache, redisCache, logger)
	defer tieredCache.Close()

	sub := subscriber.New(tieredCache.Client(), tieredCache, logger)
	sub.Start(ctx)
	defer sub.Stop()

	repo := repository.New(pool)
	svc := service.New(repo, tieredCache, logger)
	h := handler.New(svc, logger)
	router := handler.NewRouter(h, logger)

	server := &http.Server{
		Addr:         fmt.Sprintf(":%d", cfg.Server.Port),
		Handler:      router,
		ReadTimeout:  cfg.Server.ReadTimeout,
		WriteTimeout: cfg.Server.WriteTimeout,
	}

	go func() {
		logger.Info("starting evaluation API server", "port", cfg.Server.Port)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Error("server error", "error", err)
			os.Exit(1)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	logger.Info("shutting down server...")

	shutdownCtx, cancel := context.WithTimeout(context.Background(), cfg.Server.ShutdownTimeout)
	defer cancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		logger.Error("server forced to shutdown", "error", err)
		os.Exit(1)
	}

	logger.Info("server stopped gracefully")
}
