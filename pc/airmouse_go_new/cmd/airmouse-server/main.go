package main

import (
    "context"
    "fmt"
    "os"
    "os/signal"
    "runtime"
    "syscall"
    "time"

    "airmouse-go/internal/auth"
    "airmouse-go/internal/config"
    "airmouse-go/internal/control"
    "airmouse-go/internal/device"
    "airmouse-go/internal/domain/repository"
    "airmouse-go/internal/domain/service"
    "airmouse-go/internal/infra/logger"
    "airmouse-go/internal/protocol"
    "airmouse-go/internal/ui"
    "airmouse-go/internal/utils"
)

var (
    version   = "3.0.0"
    buildTime = "2025-01-15"
    gitCommit = "unknown"
)

func main() {
    // Initialize logger
    cfg := config.Get()
    logger.Init(logger.Config{
        Level:     cfg.LogLevel,
        LogFile:   cfg.LogFile,
        UseColor:  cfg.LogColor,
        Timestamp: true,
    })
    
    logger.Info("╔═══════════════════════════════════════════════════════════════╗")
    logger.Info("║     Air Mouse Pro Server v%s - Starting...                     ║", version)
    logger.Info("╚═══════════════════════════════════════════════════════════════╝")
    
    // Print system info
    logger.Info("System Information:")
    logger.Info("  OS: %s", runtime.GOOS)
    logger.Info("  Arch: %s", runtime.GOARCH)
    logger.Info("  CPUs: %d", runtime.NumCPU())
    logger.Info("  Go Version: %s", runtime.Version())
    logger.Info("  Build Time: %s", buildTime)
    logger.Info("  Git Commit: %s", gitCommit)
    
    // Initialize repositories
    clientRepo := repository.NewClientRepository()
    gestureRepo := repository.NewGestureRepository()
    
    // Initialize services
    connectionService := service.NewConnectionService(clientRepo)
    gestureService := service.NewGestureService(gestureRepo, cfg.GestureConfidenceThreshold)
    
    // Initialize mouse controller
    mouseController, err := control.NewMouseController(cfg.Sensitivity)
    if err != nil {
        logger.Fatal("Failed to create mouse controller: %v", err)
    }
    
    // Initialize mouse repository
    mouseRepo := repository.NewMouseRepository(mouseController)
    
    // Initialize mouse service
    mouseService := service.NewMouseService(mouseRepo, nil)
    
    // Initialize device manager
    deviceManager := device.NewManager()
    
    // Initialize auth manager
    authManager := auth.NewManager(cfg.AuthSecret)
    if cfg.AuthEnabled {
        for _, token := range cfg.AuthTokens {
            authManager.GenerateAuthToken("", "")
        }
        logger.Info("Authentication enabled with %d tokens", len(cfg.AuthTokens))
    } else {
        logger.Info("Authentication disabled")
    }
    
    // Initialize protocol server
    protocolServer := protocol.NewProtocolServer(mouseController, deviceManager, authManager)
    
    // Create and run UI
    app := ui.NewApp(cfg, protocolServer, mouseController, deviceManager)
    
    // Setup graceful shutdown
    ctx, cancel := context.WithCancel(context.Background())
    defer cancel()
    
    go handleSignals(ctx, app, protocolServer)
    
    // Run application
    logger.Info("Application started successfully")
    logger.Info("WebSocket server will listen on port %d", cfg.WebSocketPort)
    logger.Info("TCP server will listen on port %d", cfg.Port)
    logger.Info("UDP discovery will listen on port %d", cfg.UDPPort)
    
    if err := app.Run(); err != nil {
        logger.Fatal("Application error: %v", err)
    }
    
    // Cleanup
    logger.Info("Shutting down...")
    protocolServer.Stop()
    logger.Close()
}

func handleSignals(ctx context.Context, app *ui.App, server *protocol.ProtocolServer) {
    sigChan := make(chan os.Signal, 1)
    signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM, syscall.SIGHUP)
    
    for {
        select {
        case sig := <-sigChan:
            logger.Info("Received signal: %v", sig)
            
            switch sig {
            case syscall.SIGHUP:
                // Reload configuration
                logger.Info("Reloading configuration...")
                if err := config.Get().Reload(); err != nil {
                    logger.Error("Failed to reload config: %v", err)
                } else {
                    logger.Info("Configuration reloaded successfully")
                    // Update logger level
                    logger.SetLevel(getLogLevel(config.Get().LogLevel))
                }
                
            case syscall.SIGINT, syscall.SIGTERM:
                // Graceful shutdown
                logger.Info("Initiating graceful shutdown...")
                
                // Stop the server
                if server != nil {
                    logger.Info("Stopping protocol server...")
                    server.Stop()
                }
                
                // Stop the app
                if app != nil {
                    logger.Info("Stopping UI...")
                    app.Stop()
                }
                
                logger.Info("Shutdown complete")
                logger.Close()
                os.Exit(0)
            }
            
        case <-ctx.Done():
            return
        }
    }
}

func getLogLevel(level string) logger.Level {
    switch level {
    case "debug":
        return logger.LevelDebug
    case "info":
        return logger.LevelInfo
    case "warn":
        return logger.LevelWarn
    case "error":
        return logger.LevelError
    default:
        return logger.LevelInfo
    }
}