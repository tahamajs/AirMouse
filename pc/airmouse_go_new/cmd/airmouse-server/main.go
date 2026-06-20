package main

import (
    "context"
    "fmt"
    "os"
    "os/signal"
    "runtime"
    "syscall"
    "time"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/app"

    "airmouse-go/internal/auth"
    "airmouse-go/internal/config"
    "airmouse-go/internal/control"
    "airmouse-go/internal/device"
    "airmouse-go/internal/infra/logger"
    "airmouse-go/internal/protocol"
    "airmouse-go/internal/ui"
)

// Build‑time variables (set via -ldflags)
var (
    version   = "3.0.0"
    buildTime = "2025-01-15"
    gitCommit = "unknown"
)

func main() {
    // --- 1. Load configuration and initialize logger ---
    cfg := config.Get()
    logger.Init(cfg.LogLevel, cfg.LogFile)

    // Inject logger callbacks into the device package.
    device.SetLogger(
        func(msg string, args ...interface{}) {
            logger.Info(fmt.Sprintf(msg, args...))
        },
        func(msg string, args ...interface{}) {
            logger.Debug(fmt.Sprintf(msg, args...))
        },
    )

    printBanner()
    logSystemInfo()

    // --- 2. Initialize core components ---
    mouseController := control.NewMouseController(cfg.Sensitivity)
    deviceManager := device.NewManager()
    authManager := initAuth(cfg)
    protocolServer := protocol.NewProtocolServer(mouseController, deviceManager, authManager)
    wireLifecycleLogging(protocolServer, deviceManager)

    // --- 3. Create Fyne application ---
    a := app.New()
    if icon := loadAppIcon(); icon != nil {
        a.SetIcon(icon)
    }

    // --- 4. Build the UI (does NOT start the event loop yet) ---
    appUI := ui.NewApp(cfg, protocolServer, mouseController, deviceManager)

    // --- 5. Setup graceful shutdown ---
    ctx, cancel := context.WithCancel(context.Background())
    defer cancel()

    // Handle OS signals in a separate goroutine
    go handleSignals(ctx, appUI, protocolServer, deviceManager)

    // --- 6. Log server endpoints ---
    logger.Info("Application started successfully")
    logger.Info("WebSocket server will listen on port %d", cfg.WebSocketPort)
    logger.Info("TCP server will listen on port %d", cfg.Port)
    logger.Info("UDP discovery will listen on port %d", cfg.UDPPort)
    logger.Info("Active protocols: %v", protocolServer.GetActiveProtocols())

    // --- 7. Run the UI (blocks until the window is closed) ---
    if err := appUI.Run(); err != nil {
        logger.Error("Application error: %v", err)
        os.Exit(1)
    }

    // --- 8. Cleanup after UI exits ---
    logger.Info("Shutting down...")
    protocolServer.Stop()
    deviceManager.Stop()
    logger.Close()
}

// printBanner displays the startup banner.
func printBanner() {
    banner := `
╔═══════════════════════════════════════════════════════════════╗
║     Air Mouse Pro Server v%s                                   ║
║     Turn your phone into a wireless mouse                      ║
╚═══════════════════════════════════════════════════════════════╝
`
    logger.Info(fmt.Sprintf(banner, version))
}

// logSystemInfo prints system details.
func logSystemInfo() {
    logger.Info("System Information:")
    logger.Info("  OS: %s", runtime.GOOS)
    logger.Info("  Arch: %s", runtime.GOARCH)
    logger.Info("  CPUs: %d", runtime.NumCPU())
    logger.Info("  Go Version: %s", runtime.Version())
    logger.Info("  Build Time: %s", buildTime)
    logger.Info("  Git Commit: %s", gitCommit)
}

// initAuth sets up authentication (if enabled).
func initAuth(cfg *config.Config) *auth.Manager {
    authManager := auth.NewManager(cfg.AuthSecret)
    if cfg.AuthEnabled {
        for _, token := range cfg.AuthTokens {
            _, _ = authManager.GenerateAuthToken(token, "")
        }
        logger.Info("Authentication enabled with %d tokens", len(cfg.AuthTokens))
    } else {
        logger.Info("Authentication disabled")
    }
    return authManager
}

// loadAppIcon loads the embedded application icon (if available).
func loadAppIcon() fyne.Resource {
    // ui.AppIconData should be a []byte containing the PNG data.
    if len(ui.AppIconData) > 0 {
        return &fyne.StaticResource{
            StaticName:    "app_icon.png",
            StaticContent: ui.AppIconData,
        }
    }
    // No icon – Fyne will use a default.
    return nil
}

func wireLifecycleLogging(server *protocol.ProtocolServer, deviceMgr *device.DeviceManager) {
    if server != nil {
        server.AddEventListener(func(event protocol.ServerEvent) {
            switch event.Type {
            case "start":
                logger.Info("Protocol lifecycle event: start")
            case "stop":
                logger.Info("Protocol lifecycle event: stop")
            case "client_connected":
                logger.Info("Protocol lifecycle event: client connected id=%s", event.ClientID)
            case "client_disconnected":
                logger.Info("Protocol lifecycle event: client disconnected id=%s", event.ClientID)
            default:
                logger.Debug("Protocol lifecycle event: %s id=%s", event.Type, event.ClientID)
            }
        })
    }

    if deviceMgr != nil {
        deviceMgr.AddEventListener(func(event device.DeviceEvent) {
            logger.Info(
                "Device lifecycle event: type=%s id=%s name=%s",
                event.Type,
                event.DeviceID,
                event.DeviceName,
            )
        })
    }
}

// handleSignals listens for OS signals and performs graceful shutdown.
func handleSignals(ctx context.Context, appUI *ui.App, server *protocol.ProtocolServer, deviceMgr *device.DeviceManager) {
    sigChan := make(chan os.Signal, 1)
    signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM, syscall.SIGHUP)

    for {
        select {
        case sig := <-sigChan:
            logger.Info("Received signal: %v", sig)

            switch sig {
            case syscall.SIGHUP:
                // Reload configuration without restarting.
                logger.Info("Reloading configuration...")
                if err := config.Get().Reload(); err != nil {
                    logger.Error("Failed to reload config: %v", err)
                } else {
                    logger.Info("Configuration reloaded successfully")
                }

            case syscall.SIGINT, syscall.SIGTERM:
                // Graceful shutdown.
                logger.Info("Initiating graceful shutdown...")

                // Stop the protocol server (this will close all active connections).
                if server != nil {
                    logger.Info("Stopping protocol server...")
                    // Give it a short timeout.
                    done := make(chan struct{})
                    go func() {
                        server.Stop()
                        close(done)
                    }()
                    select {
                    case <-done:
                        logger.Info("Protocol server stopped")
                    case <-time.After(2 * time.Second):
                        logger.Warn("Protocol server stop timed out – forcing exit")
                    }
                }

                // Stop device manager
                if deviceMgr != nil {
                    logger.Info("Stopping device manager...")
                    deviceMgr.Stop()
                }

                // Stop the UI (this will cause appUI.Run() to return).
                if appUI != nil {
                    logger.Info("Closing UI...")
                    appUI.Stop()
                }

                logger.Info("Shutdown complete")
                logger.Close()
                os.Exit(0)

            default:
                logger.Debug("Unhandled signal: %v", sig)
            }

        case <-ctx.Done():
            logger.Debug("Context cancelled, stopping signal handler")
            return
        }
    }
}
