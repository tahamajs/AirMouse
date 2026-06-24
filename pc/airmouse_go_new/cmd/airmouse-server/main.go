package main

import (
	"context"
	"flag"
	"fmt"
	"os"
	"os/signal"
	"path/filepath"
	"runtime"
	"syscall"
	"time"

	"airmouse-go/control/mouse"
	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	"airmouse-go/internal/logger"
	"airmouse-go/internal/protocol"
	"airmouse-go/internal/proximity"
	"airmouse-go/internal/ui"
)

var (
	version   = "3.0.0"
	buildTime = "2025-01-15"
	gitCommit = "unknown"
)

func main() {
	var (
		configPath  string
		logLevel    string
		showVersion bool
	)
	flag.StringVar(&configPath, "config", "", "Path to config file")
	flag.StringVar(&logLevel, "log-level", "", "Log level (debug, info, warn, error)")
	flag.BoolVar(&showVersion, "version", false, "Show version and exit")
	flag.Parse()

	if showVersion {
		fmt.Printf("Air Mouse Pro Server v%s\n", version)
		fmt.Printf("Build Time: %s\n", buildTime)
		fmt.Printf("Git Commit: %s\n", gitCommit)
		os.Exit(0)
	}

	cfg := config.Get()
	if configPath != "" {
		cfg.ConfigPath = configPath
		if err := cfg.Reload(); err != nil {
			fmt.Fprintf(os.Stderr, "Failed to load config: %v\n", err)
			os.Exit(1)
		}
	}
	if logLevel != "" {
		cfg.LogLevel = logLevel
	}
	if cfg.DebugMode && cfg.LogLevel == "info" {
		cfg.LogLevel = "debug"
	}

	// --- 3. Initialise logger (fixed signature) ---
	logger.Init(logger.Config{
		Level: cfg.LogLevel,
	})

	defer logger.Close()

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

	mouseCtrl := mouse.NewController(cfg.Sensitivity)
	deviceMgr := device.NewManager()
	authMgr := initAuth(cfg)

	// --- Proximity manager (with stubbed persistence) ---
	proxMgr := proximity.NewManager()
	nearThreshold := float32(cfg.ProximityNearThreshold)
	farThreshold := float32(cfg.ProximityFarThreshold)
	proxMgr.SetGlobalThresholds(nearThreshold, farThreshold)
	proxMgr.EnableAutoLock(cfg.ProximityEnabled)
	proxMgr.EnableAutoUnlock(cfg.ProximityEnabled)

	// Create RSSI fusion with persistence stubs (we'll add methods later)
	fusion := proximity.NewRSSIFusion(-59, 2.0)
	if cfg.ProximityEnabled {
		// Compute config directory – use filepath.Dir(cfg.ConfigPath)
		configDir := filepath.Dir(cfg.ConfigPath)
		statePath := filepath.Join(configDir, "proximity_state.json")
		_ = fusion.LoadState(statePath) // stub: does nothing
		proxMgr.SetRSSIFusion(fusion)    // stub: does nothing
		go func() {
			ticker := time.NewTicker(60 * time.Second)
			defer ticker.Stop()
			for range ticker.C {
				_ = fusion.SaveState(statePath) // stub: does nothing
			}
		}()
	}

	// Create protocol server (accepts 4 arguments; proximity is passed inside)
	protocolServer := protocol.NewProtocolServerWithProximity(mouseCtrl, deviceMgr, authMgr, proxMgr)

	wireLifecycleLogging(protocolServer, deviceMgr)

	// --- UI: now takes 4 arguments (proxMgr removed) ---
	appUI := ui.NewApp(cfg, protocolServer, mouseCtrl, deviceMgr)

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	go handleSignals(ctx, appUI, protocolServer, deviceMgr, proxMgr, fusion, cfg)

	logger.Info("Application started successfully")
	logger.Info("WebSocket server will listen on port %d", cfg.WebSocketPort)
	logger.Info("TCP server will listen on port %d", cfg.Port)
	logger.Info("UDP discovery will listen on port %d", cfg.UDPPort)
	logger.Info("Active protocols: %v", protocolServer.GetActiveProtocols())
	logger.Info("Launching desktop UI window...")

	if err := appUI.Run(); err != nil {
		logger.Error("Application error: %v", err)
		os.Exit(1)
	}

	logger.Info("Shutting down...")
	protocolServer.Stop()
	deviceMgr.Stop()
	if proxMgr != nil && fusion != nil {
		configDir := filepath.Dir(cfg.ConfigPath)
		statePath := filepath.Join(configDir, "proximity_state.json")
		_ = fusion.SaveState(statePath)
	}
	logger.Info("Shutdown complete")
}

func printBanner() {
	banner := `
╔═══════════════════════════════════════════════════════════════╗
║     Air Mouse Pro Server v%s                                   ║
║     Turn your phone into a wireless mouse                      ║
╚═══════════════════════════════════════════════════════════════╝
`
	logger.Info(fmt.Sprintf(banner, version))
}

func logSystemInfo() {
	logger.Info("System Information:")
	logger.Info("  OS: %s", runtime.GOOS)
	logger.Info("  Arch: %s", runtime.GOARCH)
	logger.Info("  CPUs: %d", runtime.NumCPU())
	logger.Info("  Go Version: %s", runtime.Version())
	logger.Info("  Build Time: %s", buildTime)
	logger.Info("  Git Commit: %s", gitCommit)
}

func initAuth(cfg *config.Config) *auth.Manager {
	authMgr := auth.NewManager(cfg.AuthSecret)
	if cfg.AuthEnabled {
		for _, token := range cfg.AuthTokens {
			if token != "" {
				_, _ = authMgr.GenerateAuthToken(token, "")
			}
		}
		logger.Info("Authentication enabled with %d tokens", len(cfg.AuthTokens))
	} else {
		logger.Info("Authentication disabled")
	}
	return authMgr
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

func handleSignals(ctx context.Context, appUI *ui.App, server *protocol.ProtocolServer,
	deviceMgr *device.DeviceManager, proxMgr *proximity.Manager,
	fusion *proximity.RSSIFusion, cfg *config.Config) {

	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM, syscall.SIGHUP)

	for {
		select {
		case sig := <-sigChan:
			logger.Info("Received signal: %v", sig)
			switch sig {
			case syscall.SIGHUP:
				logger.Info("Reloading configuration...")
				if err := config.Get().Reload(); err != nil {
					logger.Error("Failed to reload config: %v", err)
				} else {
					logger.Info("Configuration reloaded successfully")
				}
			case syscall.SIGINT, syscall.SIGTERM:
				logger.Info("Initiating graceful shutdown...")
				if server != nil {
					logger.Info("Stopping protocol server...")
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
				if deviceMgr != nil {
					logger.Info("Stopping device manager...")
					deviceMgr.Stop()
				}
				if proxMgr != nil && fusion != nil {
					configDir := filepath.Dir(cfg.ConfigPath)
					statePath := filepath.Join(configDir, "proximity_state.json")
					_ = fusion.SaveState(statePath)
				}
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