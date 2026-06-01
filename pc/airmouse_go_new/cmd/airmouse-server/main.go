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

    // Start the UI on the main goroutine (this blocks until the window is closed)
    app := ui.NewApp(cfg, server, mouseCtrl, deviceMgr)

    // Handle shutdown in a separate goroutine
    go func() {
        sigChan := make(chan os.Signal, 1)
        signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)
        <-sigChan
        utils.LogInfo("Shutting down...")
        server.Stop()
        app.Stop()
        utils.LogInfo("Shutdown complete")
        os.Exit(0)
    }()

    // Run the GUI (this blocks)
    if err := app.Run(); err != nil {
        utils.LogError("UI error", "error", err)
    }
}