package main

import (
	"context"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"airmouse-go/internal/domain/service"
	"airmouse-go/internal/handler/http"
	"airmouse-go/internal/handler/websocket"
	"airmouse-go/internal/infra/logger"
	"airmouse-go/internal/infra/mouse"
	"airmouse-go/internal/pkg/config"
	"airmouse-go/internal/repository"
)

func main() {
	cfg := config.Load()
	logger.Init(cfg.LogLevel)

	mouseCtrl, err := mouse.NewMouseController(cfg.Sensitivity)
	if err != nil {
		logger.Fatal("Failed to create mouse controller", "error", err)
	}

	mouseRepo := repository.NewMouseRepository(mouseCtrl)
	gestureRepo := repository.NewGestureRepository()
	clientRepo := repository.NewClientRepository()

	mouseService := service.NewMouseService(mouseRepo, cfg.Sensitivity, cfg.PredictiveBlendFactor)
	gestureService := service.NewGestureService(gestureRepo, cfg.GestureConfidenceThreshold)
	connectionService := service.NewConnectionService(clientRepo)

	hub := websocket.NewHub(mouseService, gestureService, connectionService)
	go hub.Run()

	router := http.NewRouter(hub, cfg)

	srv := &http.Server{
		Addr:         ":" + cfg.Port,
		Handler:      router,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
	}

	go func() {
		logger.Info("Starting server", "port", cfg.Port)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Fatal("Server failed", "error", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	logger.Info("Shutting down server...")

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := srv.Shutdown(ctx); err != nil {
		logger.Error("Server shutdown error", "error", err)
	}
	logger.Info("Server stopped")
}