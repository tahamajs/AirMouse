package main

import (
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	"airmouse-go/internal/domain/service"
	"airmouse-go/internal/handler/http"
	"airmouse-go/internal/handler/websocket"
	"airmouse-go/internal/infra/config"
	"airmouse-go/internal/infra/logger"
	mouseInfra "airmouse-go/internal/infra/mouse"
	"airmouse-go/internal/repository"
)

func main() {
	cfg := config.Get()
	logger.Init(cfg.LogLevel, cfg.LogFile)
	defer logger.Close()

	// Repositories
	mouseRepo := repository.NewMouseRepository()
	gestureRepo := repository.NewGestureRepository()
	clientRepo := repository.NewClientRepository()

	// Infrastructure mouse controller
	mouseCtrl := mouseInfra.New()

	// Domain services (with infrastructure dependency)
	mouseSvc := service.NewMouseService(mouseRepo, mouseCtrl, cfg.Sensitivity)
	gestureSvc, _ := service.NewGestureService(gestureRepo)
	connSvc := service.NewConnectionService(clientRepo, cfg.MaxClients)

	// WebSocket hub
	hub := websocket.NewHub()
	go hub.Run()

	// WebSocket handler
	wsHandler := websocket.NewHandler(hub, mouseSvc, gestureSvc, connSvc)

	// HTTP router and middleware
	router := http.NewRouter(wsHandler)
	handler := http.RecoverMiddleware(http.LoggingMiddleware(router.Handler()))

	// Start server
	addr := cfg.Host + ":" + string(rune(cfg.Port))
	srv := &http.Server{
		Addr:    addr,
		Handler: handler,
	}

	go func() {
		logger.Info("Air Mouse server listening on %s", addr)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Error("Server failed: %v", err)
		}
	}()

	// Wait for interrupt signal
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	logger.Info("Shutting down...")
	srv.Close()
	logger.Info("Shutdown complete")
}