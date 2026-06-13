package main

import (
    "fmt"
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
    utils.LogInfo(fmt.Sprintf("Air Mouse Pro Server starting version=%s", "3.0.0"))

    deviceMgr := device.NewManager()
    authMgr := auth.NewManager(cfg.AuthSecret)
    mouseCtrl := control.NewMouseController(cfg.Sensitivity)
    mouseCtrl.SetSmoothing(true)
    mouseCtrl.SetAcceleration(true, 1.5)

    if cfg.EnableMLPrediction {
        mouseCtrl.EnableMLPrediction(true)
        mouseCtrl.SetMLBlendFactor(cfg.MLBlendFactor)
        utils.LogInfo("ML-powered trajectory prediction enabled")
    }

    server := protocol.NewProtocolServer(mouseCtrl, deviceMgr, authMgr)
    if err := server.Start(); err != nil {
        utils.LogFatal("Failed to start server", "error", err)
    }

    // UI must run on the main goroutine
    app := ui.NewApp(cfg, server, mouseCtrl, deviceMgr)

    // Handle shutdown in background
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

    // This blocks until the window is closed
    if err := app.Run(); err != nil {
        utils.LogError(fmt.Sprintf("UI error error=%v", err))
        os.Exit(1)
    }
}
