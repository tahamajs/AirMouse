package ui

import (
    "bytes"
    "fmt"
    "image/png"
    "sync"
    "time"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/canvas"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/widget"
    qrcode "github.com/skip2/go-qrcode"

    "airmouse-go/internal/config"
    "airmouse-go/internal/control"
    "airmouse-go/internal/device"
    "airmouse-go/internal/protocol"
    "airmouse-go/internal/utils"
)

type DashboardTab struct {
    statsLabel    *widget.Label
    connLabel     *widget.Label
    endpointLabel *widget.Label
    uptimeLabel   *widget.Label
    aiStatusLabel *widget.Label
    serverStatus  *widget.Label
    startBtn      *widget.Button
    stopBtn       *widget.Button
    qrBtn         *widget.Button
    serverStart   time.Time
    mu            sync.Mutex
    mouse         control.MouseController
}

func NewDashboardTab(server *protocol.ProtocolServer, mouse control.MouseController, deviceMgr *device.Manager) fyne.CanvasObject {
    tab := &DashboardTab{mouse: mouse}

    tab.statsLabel = widget.NewLabel("Clicks: 0  |  Double: 0  |  Right: 0  |  Scroll: 0")
    tab.connLabel = widget.NewLabel("Connected devices: 0")
    tab.endpointLabel = widget.NewLabel("Endpoint: not started")
    tab.uptimeLabel = widget.NewLabel("Uptime: --:--:--")
    tab.aiStatusLabel = widget.NewLabel("AI Smoothing: Disabled")
    tab.serverStatus = widget.NewLabelWithStyle("Server Status: Stopped", fyne.TextAlignCenter, fyne.TextStyle{Bold: true})

    tab.startBtn = widget.NewButtonWithIcon("Start Server", nil, func() {
        if err := server.Start(); err == nil {
            tab.mu.Lock()
            tab.serverStart = time.Now()
            tab.mu.Unlock()
            tab.serverStatus.SetText("Server Status: Running")
            tab.startBtn.Disable()
            tab.stopBtn.Enable()
            cfg := config.Get()
            ip := utils.GetLocalIP()
            tab.endpointLabel.SetText(fmt.Sprintf("Endpoint: %s:%d (TCP) | ws://%s:%d", ip, cfg.Port, ip, cfg.WebSocketPort))
            if cfg.EnableAISmoothing {
                tab.aiStatusLabel.SetText("AI Smoothing: Enabled")
            } else {
                tab.aiStatusLabel.SetText("AI Smoothing: Disabled")
            }
        }
    })
    tab.stopBtn = widget.NewButtonWithIcon("Stop Server", nil, func() {
        server.Stop()
        tab.serverStatus.SetText("Server Status: Stopped")
        tab.startBtn.Enable()
        tab.stopBtn.Disable()
        tab.endpointLabel.SetText("Endpoint: not started")
        tab.uptimeLabel.SetText("Uptime: --:--:--")
    })
    tab.stopBtn.Disable()

    tab.qrBtn = widget.NewButtonWithIcon("Show QR Code", nil, func() {
        showQuickQRDialog(fyne.CurrentApp().Driver().AllWindows()[0])
    })

    // Stats updater
    go func() {
        for {
            time.Sleep(1 * time.Second)
            clicks, dbl, right, scroll := mouse.Stats()
            devices := deviceMgr.GetAllDevices()
            fyne.Do(func() {
                tab.statsLabel.SetText(fmt.Sprintf("Clicks: %d  |  Double: %d  |  Right: %d  |  Scroll: %d", clicks, dbl, right, scroll))
                tab.connLabel.SetText(fmt.Sprintf("Connected devices: %d", len(devices)))
                tab.mu.Lock()
                if !tab.serverStart.IsZero() {
                    uptime := time.Since(tab.serverStart)
                    tab.uptimeLabel.SetText(fmt.Sprintf("Uptime: %02d:%02d:%02d", int(uptime.Hours()), int(uptime.Minutes())%60, int(uptime.Seconds())%60))
                }
                tab.mu.Unlock()
            })
        }
    }()

    return container.NewVBox(
        widget.NewLabelWithStyle("Server Dashboard", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        tab.serverStatus,
        container.NewHBox(tab.startBtn, tab.stopBtn, tab.qrBtn),
        tab.statsLabel,
        tab.connLabel,
        tab.endpointLabel,
        tab.uptimeLabel,
        widget.NewSeparator(),
        widget.NewLabelWithStyle("Advanced Features", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        tab.aiStatusLabel,
    )
}

func showQuickQRDialog(parent fyne.Window) {
    ip := utils.GetLocalIP()
    port := config.Get().Port
    data := fmt.Sprintf("airmouse://%s:%d", ip, port)
    pngBytes, err := qrcode.Encode(data, qrcode.High, 250)
    if err != nil {
        dialog.ShowError(err, parent)
        return
    }
    img, err := png.Decode(bytes.NewReader(pngBytes))
    if err != nil {
        dialog.ShowError(err, parent)
        return
    }
    qrImage := canvas.NewImageFromImage(img)
    qrImage.FillMode = canvas.ImageFillOriginal
    var popUp *widget.PopUp
    content := container.NewVBox(
        widget.NewLabelWithStyle("Scan with Air Mouse Android App", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        qrImage,
        widget.NewLabel(data),
        widget.NewButton("Close", func() { popUp.Hide() }),
    )
    popUp = widget.NewModalPopUp(content, parent.Canvas())
    popUp.Resize(fyne.NewSize(300, 350))
    popUp.Show()
}