// cmd/airmouse-server/main.go
package main

import (
    "airmouse-go/internal/ui"
    "airmouse-go/internal/utils"
    "fmt"
    "os"
    "os/signal"
    "syscall"
)

func main() {
    utils.InitLogger()
    utils.LogInfo(fmt.Sprintf("Air Mouse Pro Server starting version=%s", "3.0.0"))

    app := ui.NewApp()

    go func() {
        sigChan := make(chan os.Signal, 1)
        signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)
        <-sigChan
        utils.LogInfo("Shutting down...")
        app.Stop()
        utils.LogInfo("Shutdown complete")
        os.Exit(0)
    }()

    if err := app.Run(); err != nil {
        utils.LogError(fmt.Sprintf("Application error: %v", err))
        os.Exit(1)
    }
}