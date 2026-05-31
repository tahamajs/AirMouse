package main

import (
	"os"
	"os/signal"
	"syscall"

	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/protocol"
	"airmouse-go/internal/ui"
	"airmouse-go/internal/utils"
)

func main() {
	utils.InitLogger()
	cfg := config.Get()
	utils.LogInfo("Air Mouse Pro Server starting", "version", "3.0.0")

	// Device manager
	deviceMgr := device.NewManager()

	// Auth manager
	authMgr := auth.NewManager(cfg.AuthSecret)

	// Mouse controller
	mouseCtrl := control.NewMouseController(cfg.Sensitivity)
	mouseCtrl.SetSmoothing(true)
	mouseCtrl.SetAcceleration(true, 1.5)

	// Enable ML prediction if configured
	if cfg.EnableMLPrediction {
		mouseCtrl.EnableMLPrediction(true)
		mouseCtrl.SetMLBlendFactor(cfg.MLBlendFactor)
		utils.LogInfo("ML‑powered trajectory prediction enabled")
	}

	// Protocol server
	server := protocol.NewProtocolServer(mouseCtrl, deviceMgr, authMgr)
	if err := server.Start(); err != nil {
		utils.LogFatal("Failed to start server", "error", err)
	}

	// UI (optional, can be disabled for headless)
	app := ui.NewApp(cfg, server, mouseCtrl, deviceMgr)
	go func() {
		if err := app.Run(); err != nil {
			utils.LogError("UI error", "error", err)
		}
	}()

	// Wait for shutdown
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)
	<-sigChan

	utils.LogInfo("Shutting down...")
	server.Stop()
	app.Stop()
	utils.LogInfo("Shutdown complete")
}


tabs := container.NewAppTabs(
	container.NewTabItemWithIcon("Dashboard", theme.HomeIcon(), a.dashboardTab),
	container.NewTabItemWithIcon("Devices", theme.ComputerIcon(), a.devicesTab),
	container.NewTabItemWithIcon("Network", theme.NetworkIcon(), a.networkTab),
	container.NewTabItemWithIcon("Gestures", theme.ContentAddIcon(), NewGesturesTab()),
	container.NewTabItemWithIcon("Proximity", theme.VisibilityIcon(), NewProximityTab()),
	container.NewTabItemWithIcon("Settings", theme.SettingsIcon(), a.settingsTab),
	container.NewTabItemWithIcon("Logs", theme.DocumentIcon(), a.logsTab),
)
tabs.SetTabLocation(container.TabLocationLeading)