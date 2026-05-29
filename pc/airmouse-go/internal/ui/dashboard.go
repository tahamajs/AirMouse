package ui

import (
	"fmt"
	"sync"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/protocol"
	"airmouse-go/internal/utils"
)

type DashboardTab struct {
	statsLabel     *widget.Label
	connLabel      *widget.Label
	endpointLabel  *widget.Label
	uptimeLabel    *widget.Label
	aiStatusLabel  *widget.Label
	serverStatus   *widget.Label
	startBtn       *widget.Button
	stopBtn        *widget.Button
	serverStart    time.Time
	mu             sync.Mutex
	mouse          control.MouseController
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
			ip := getLocalIP()
			tab.endpointLabel.SetText(fmt.Sprintf("Endpoint: %s:%d (TCP) | ws://%s:%d", ip, cfg.Port, ip, cfg.WebSocketPort))
			// Update AI status from config
			if cfg.AIEnabled {
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

	// Stats updater
	go func() {
		for {
			time.Sleep(1 * time.Second)
			clicks, dbl, right, scroll := mouse.Stats()
			tab.statsLabel.SetText(fmt.Sprintf("Clicks: %d  |  Double: %d  |  Right: %d  |  Scroll: %d", clicks, dbl, right, scroll))
			devices := deviceMgr.GetAllDevices()
			tab.connLabel.SetText(fmt.Sprintf("Connected devices: %d", len(devices)))
			tab.mu.Lock()
			if !tab.serverStart.IsZero() {
				uptime := time.Since(tab.serverStart)
				tab.uptimeLabel.SetText(fmt.Sprintf("Uptime: %02d:%02d:%02d", int(uptime.Hours()), int(uptime.Minutes())%60, int(uptime.Seconds())%60))
			}
			tab.mu.Unlock()
		}
	}()

	return container.NewVBox(
		widget.NewLabelWithStyle("Server Dashboard", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		tab.serverStatus,
		container.NewHBox(tab.startBtn, tab.stopBtn),
		tab.statsLabel,
		tab.connLabel,
		tab.endpointLabel,
		tab.uptimeLabel,
		widget.NewSeparator(),
		widget.NewLabelWithStyle("Advanced Features", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		tab.aiStatusLabel,
	)
}