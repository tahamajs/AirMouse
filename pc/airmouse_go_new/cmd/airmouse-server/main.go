package main

import (
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"net/http"
	"os"
	"os/exec"
	"os/signal"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"syscall"
	"time"

	"airmouse-go/control/mouse"
	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	"airmouse-go/internal/logger"
	"airmouse-go/internal/protocol"
	"airmouse-go/internal/proximity"
	"airmouse-go/internal/sysaction"
)

var (
	version   = "1.0.1"
	buildTime = "2026-10-25"
	gitCommit = "v1.0.1-release"
)

func main() {
	var (
		configPath  string
		logLevel    string
		showVersion bool
		headless    bool
	)
	flag.StringVar(&configPath, "config", "", "Path to config file")
	flag.StringVar(&logLevel, "log-level", "", "Log level (debug, info, warn, error)")
	flag.BoolVar(&showVersion, "version", false, "Show version and exit")
	flag.BoolVar(&headless, "headless", false, "Run server in headless mode without GUI")
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

	// Initialise logger
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
	checkGitHubUpdate()

	mouseCtrl := mouse.NewController(cfg.Sensitivity)
	sysaction.SetSystemMouseSpeed(cfg.Sensitivity)

	// On macOS, verify and prompt for Accessibility permissions if needed
	if runtime.GOOS == "darwin" {
		if !mouse.HasAccessibilityPermission() {
			logger.Warn("⚠️  macOS Accessibility permission is NOT granted. Mouse movement will be blocked by macOS.")
			logger.Info("Prompting for macOS Accessibility permissions... Please grant permission in System Settings.")
			mouse.RequestAccessibilityPermission()
		} else {
			logger.Info("✅ macOS Accessibility permission is verified and active.")
		}
	}

	deviceMgr := device.NewManager()
	authMgr := initAuth(cfg)

	// Proximity manager
	proxMgr := proximity.NewManager()
	proxMgr.SetGlobalThresholds(float32(cfg.ProximityNearThreshold), float32(cfg.ProximityFarThreshold))
	proxMgr.EnableAutoLock(cfg.ProximityEnabled)
	proxMgr.EnableAutoUnlock(cfg.ProximityEnabled)

	fusion := proximity.NewRSSIFusion(-59, 2.0)
	if cfg.ProximityEnabled {
		configDir := filepath.Dir(cfg.ConfigPath)
		statePath := filepath.Join(configDir, "proximity_state.json")
		_ = fusion.LoadState(statePath) // stub – implement if needed
		proxMgr.SetRSSIFusion(fusion)   // stub – implement if needed
		go func() {
			ticker := time.NewTicker(60 * time.Second)
			defer ticker.Stop()
			for range ticker.C {
				_ = fusion.SaveState(statePath)
			}
		}()
	}

	// Protocol server – expects mouse, device, auth, proximity
	protocolServer := protocol.NewProtocolServerWithProximity(mouseCtrl, deviceMgr, authMgr, proxMgr)

	wireLifecycleLogging(protocolServer, deviceMgr)

	logger.Info("Application started successfully")
	logger.Info("WebSocket server will listen on port %d", cfg.WebSocketPort)
	logger.Info("TCP server will listen on port %d", cfg.Port)
	logger.Info("UDP discovery will listen on port %d", cfg.UDPPort)
	logger.Info("Active protocols: %v", protocolServer.GetActiveProtocols())

	logger.Info("Starting server protocols...")
	if err := protocolServer.Start(); err != nil {
		logger.Fatal("Failed to start protocol server: %v", err)
	}
	logger.Info("Server started successfully")

	// Automatically open the Web Dashboard in the default browser (unless headless is forced)
	if !headless {
		dashboardURL := fmt.Sprintf("http://localhost:%d", cfg.WebSocketPort)
		logger.Info("Opening Web Dashboard at %s", dashboardURL)
		openBrowser(dashboardURL)
	}

	logger.Info("Air Mouse Pro Server is running. Press Ctrl+C to stop.")

	// Set up signal channel for SIGHUP (config reload) and SIGINT/SIGTERM (graceful shutdown)
	sigChan := make(chan os.Signal, 2)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM, syscall.SIGHUP)

	for sig := range sigChan {
		if sig == syscall.SIGHUP {
			logger.Info("Received SIGHUP: reloading configuration...")
			if err := config.Get().Reload(); err != nil {
				logger.Error("Failed to reload config: %v", err)
			} else {
				logger.Info("Configuration reloaded successfully")
			}
			continue
		}

		logger.Info("Received termination signal (%v). Initiating graceful shutdown...", sig)
		break
	}

	// Graceful shutdown with 5-second timeout
	shutdownDone := make(chan struct{})
	go func() {
		defer close(shutdownDone)
		if protocolServer != nil {
			logger.Info("Stopping protocol server...")
			protocolServer.Stop()
			logger.Info("Protocol server stopped")
		}
		if deviceMgr != nil {
			logger.Info("Stopping device manager...")
			deviceMgr.Stop()
			logger.Info("Device manager stopped")
		}
		if proxMgr != nil && fusion != nil {
			configDir := filepath.Dir(cfg.ConfigPath)
			statePath := filepath.Join(configDir, "proximity_state.json")
			_ = fusion.SaveState(statePath)
		}
	}()

	select {
	case <-shutdownDone:
		logger.Info("Graceful shutdown completed successfully")
	case <-time.After(5 * time.Second):
		logger.Warn("Graceful shutdown timed out (5s limit reached) – forcing exit")
	case sig := <-sigChan:
		logger.Warn("Received second signal (%v) – forcing immediate exit", sig)
	}
}

func openBrowser(url string) {
	var err error
	switch runtime.GOOS {
	case "linux":
		err = exec.Command("xdg-open", url).Start()
	case "windows":
		err = exec.Command("rundll32", "url.dll,FileProtocolHandler", url).Start()
	case "darwin":
		err = exec.Command("open", url).Start()
	default:
		err = fmt.Errorf("unsupported platform")
	}
	if err != nil {
		logger.Warn("Failed to open browser automatically: %v", err)
	}
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

func handleSignals(ctx context.Context, server *protocol.ProtocolServer,
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

// checkGitHubUpdate queries GitHub Releases in background on startup to notify user of available updates
func checkGitHubUpdate() {
	go func() {
		client := &http.Client{Timeout: 4 * time.Second}
		req, err := http.NewRequest("GET", "https://api.github.com/repos/tahamajs/AirMouse/releases/latest", nil)
		if err != nil {
			return
		}
		req.Header.Set("User-Agent", "AirMouse-Go-Server/1.0.1")
		req.Header.Set("Accept", "application/vnd.github.v3+json")

		resp, err := client.Do(req)
		if err != nil {
			return // offline or unreachable
		}
		defer resp.Body.Close()

		if resp.StatusCode != http.StatusOK {
			return
		}

		var rel struct {
			TagName string `json:"tag_name"`
			HTMLURL string `json:"html_url"`
		}
		if err := json.NewDecoder(resp.Body).Decode(&rel); err != nil {
			return
		}

		remoteVer := strings.TrimPrefix(rel.TagName, "v")
		if isRemoteNewer(remoteVer, version) {
			logger.Info("✨ New AirMouse version available: v%s (Current: v%s) -> %s", remoteVer, version, rel.HTMLURL)
		} else {
			logger.Info("GitHub check: AirMouse Pro is up to date (v%s)", version)
		}
	}()
}

func isRemoteNewer(remote, local string) bool {
	rParts := parseSemVer(remote)
	lParts := parseSemVer(local)
	for i := 0; i < len(rParts) && i < len(lParts); i++ {
		if rParts[i] > lParts[i] {
			return true
		}
		if rParts[i] < lParts[i] {
			return false
		}
	}
	return len(rParts) > len(lParts)
}

func parseSemVer(v string) []int {
	v = strings.Split(v, "-")[0]
	parts := strings.Split(v, ".")
	var res []int
	for _, p := range parts {
		if n, err := strconv.Atoi(p); err == nil {
			res = append(res, n)
		}
	}
	for len(res) < 3 {
		res = append(res, 0)
	}
	return res
}
