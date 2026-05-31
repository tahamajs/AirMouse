package ui

import (
	"fmt"
	"sync"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	"airmouse-go/internal/domain/service"
)

type DashboardTab struct {
	statsLabel    *widget.Label
	connLabel     *widget.Label
	endpointLabel *widget.Label
	uptimeLabel   *widget.Label
	serverStatus  *widget.Label
	startBtn      *widget.Button
	stopBtn       *widget.Button
	serverStart   time.Time
	mu            sync.Mutex
}

func NewDashboardTab(cfg *config.Config, mouseSvc *service.MouseService, deviceMgr *device.Manager) fyne.CanvasObject {
	tab := &DashboardTab{}

	tab.statsLabel = widget.NewLabel("Clicks: 0 | Double: 0 | Right: 0 | Scroll: 0")
	tab.connLabel = widget.NewLabel("Connected devices: 0")
	tab.endpointLabel = widget.NewLabel(fmt.Sprintf("Endpoint: %s:%d", getLocalIP(), cfg.Port))
	tab.uptimeLabel = widget.NewLabel("Uptime: --:--:--")
	tab.serverStatus = widget.NewLabelWithStyle("Server Status: Running", fyne.TextAlignCenter, fyne.TextStyle{Bold: true})

	tab.startBtn = widget.NewButtonWithIcon("Start Server", nil, func() {})
	tab.startBtn.Disable()
	tab.stopBtn = widget.NewButtonWithIcon("Stop Server", nil, func() {})
	tab.stopBtn.Disable()

	go func() {
		for {
			time.Sleep(1 * time.Second)
			clicks, dbl, right, scroll, _ := mouseSvc.GetStatistics()
			tab.statsLabel.SetText(fmt.Sprintf("Clicks: %d | Double: %d | Right: %d | Scroll: %d", clicks, dbl, right, scroll))
			devices := deviceMgr.GetAllDevices()
			tab.connLabel.SetText(fmt.Sprintf("Connected devices: %d", len(devices)))
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
	)
}