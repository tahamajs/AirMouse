package ui

import (
	"fmt"
	"image/color"
	"sync"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/config"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/protocol"
)

// ------------------------------------------------------------
//  Premium Dashboard
// ------------------------------------------------------------

type PremiumDashboard struct {
	// Status
	statusCircle *canvas.Circle
	statusLabel  *widget.Label

	// Stats
	statsGrid *fyne.Container

	// Buttons
	startBtn *widget.Button
	stopBtn  *widget.Button

	// Info
	serverInfo *widget.Label
	deviceInfo *widget.Label

	// Internal
	stopChan    chan struct{}
	mu          sync.Mutex
	mouse       control.MouseController
	server      *protocol.ProtocolServer
	deviceMgr   *device.Manager
	cfg         *config.Config
	serverStart time.Time
}

// NewPremiumDashboard creates a premium dashboard
func NewPremiumDashboard(server *protocol.ProtocolServer, mouse control.MouseController, deviceMgr *device.Manager) fyne.CanvasObject {
	if server == nil || mouse == nil || deviceMgr == nil {
		return widget.NewLabelWithStyle("⚠️ Dashboard unavailable",
			fyne.TextAlignCenter, fyne.TextStyle{Bold: true})
	}

	d := &PremiumDashboard{
		mouse:     mouse,
		server:    server,
		deviceMgr: deviceMgr,
		cfg:       config.Get(),
		stopChan:  make(chan struct{}),
	}

	// ----- Status Indicator -----
	d.statusCircle = canvas.NewCircle(color.RGBA{239, 68, 68, 255})
	d.statusCircle.Resize(fyne.NewSize(18, 18))

	d.statusLabel = widget.NewLabelWithStyle("Server Stopped", fyne.TextAlignCenter, fyne.TextStyle{Bold: true})

	// ----- Stats Grid -----
	d.statsGrid = d.createStatsGrid()

	// ----- Server Controls -----
	d.startBtn = widget.NewButtonWithIcon("▶ Start Server", theme.MediaPlayIcon(), func() {
		err := server.Start()
		if server.IsRunning() {
			d.mu.Lock()
			d.serverStart = time.Now()
			d.mu.Unlock()
			d.updateUI(true)
		}
		if err != nil {
			win := getCurrentWindow()
			if win != nil {
				if server.IsRunning() {
					dialog.ShowInformation("Server started with warnings", fmt.Sprintf(
						"Server is running, but one or more protocols reported an issue:\n\n%v", err), win)
				} else {
					dialog.ShowError(fmt.Errorf("Failed to start server: %v", err), win)
				}
			}
		}
	})
	d.startBtn.Importance = widget.HighImportance

	d.stopBtn = widget.NewButtonWithIcon("⏹ Stop Server", theme.MediaStopIcon(), func() {
		d.stopBtn.Disable()
		d.statusLabel.SetText("⏳ Stopping...")
		go func() {
			server.Stop()
			RunOnMain(func() {
				d.updateUI(false)
				d.stopBtn.Enable()
			})
		}()
	})
	d.stopBtn.Disable()

	// ----- Info Labels -----
	d.serverInfo = widget.NewLabel("🔌 Server: Not Started")
	d.deviceInfo = widget.NewLabel("📱 Devices: 0")

	// ----- Layout -----
	header := container.NewHBox(
		widget.NewLabelWithStyle("🎯 Air Mouse Pro", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		container.NewHBox(
			d.statusCircle,
			d.statusLabel,
		),
	)

	statusCard := NewGlassCard(
		container.NewVBox(
			widget.NewLabelWithStyle("Server Status", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
			d.serverInfo,
			d.deviceInfo,
			container.NewHBox(d.startBtn, d.stopBtn),
		),
	)

	statsCard := NewGlassCard(
		container.NewVBox(
			widget.NewLabelWithStyle("📊 Live Statistics", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
			d.statsGrid,
		),
	)

	content := container.NewVBox(
		header,
		widget.NewSeparator(),
		container.NewGridWithColumns(2, statusCard, statsCard),
	)

	// Start background updater
	go d.statsUpdater()

	return container.NewPadded(content)
}

// createStatsGrid creates the statistics grid
func (d *PremiumDashboard) createStatsGrid() *fyne.Container {
	// Create stat items
	statItems := []struct {
		label string
		icon  string
		value string
	}{
		{"Clicks", "🖱️", "0"},
		{"Double", "🔄", "0"},
		{"Right", "📌", "0"},
		{"Scroll", "📜", "0"},
	}

	var widgets []fyne.CanvasObject
	for _, item := range statItems {
		stat := widget.NewLabel(item.value)
		stat.TextStyle = fyne.TextStyle{Bold: true}
		stat.Alignment = fyne.TextAlignCenter

		card := container.NewVBox(
			widget.NewLabel(item.icon),
			widget.NewLabelWithStyle(item.label, fyne.TextAlignCenter, fyne.TextStyle{}),
			stat,
		)
		widgets = append(widgets, card)
	}

	return container.NewGridWithColumns(4, widgets...)
}

// statsUpdater updates stats periodically
func (d *PremiumDashboard) statsUpdater() {
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			d.updateStats()
		case <-d.stopChan:
			return
		}
	}
}

// updateStats updates the statistics
func (d *PremiumDashboard) updateStats() {
	if d.mouse == nil {
		return
	}
	clicks, dbl, right, scroll := d.mouse.Stats()
	devices := d.deviceMgr.GetActiveDevices()
	deviceCount := len(devices)

	// Update device info
	d.deviceInfo.SetText(fmt.Sprintf("📱 Devices: %d", deviceCount))

	RunOnMain(func() {
		// Update stats grid
		if d.statsGrid != nil && len(d.statsGrid.Objects) >= 4 {
			values := []string{
				fmt.Sprintf("%d", clicks),
				fmt.Sprintf("%d", dbl),
				fmt.Sprintf("%d", right),
				fmt.Sprintf("%d", scroll),
			}
			for i, obj := range d.statsGrid.Objects {
				if vbox, ok := obj.(*fyne.Container); ok && len(vbox.Objects) >= 3 {
					if statLabel, ok := vbox.Objects[2].(*widget.Label); ok {
						if i < len(values) {
							statLabel.SetText(values[i])
						}
					}
				}
			}
		}

		d.mu.Lock()
		if !d.serverStart.IsZero() {
			uptime := time.Since(d.serverStart)
			d.serverInfo.SetText(fmt.Sprintf("🔌 Server: Running (uptime: %02d:%02d:%02d)",
				int(uptime.Hours()),
				int(uptime.Minutes())%60,
				int(uptime.Seconds())%60))
		}
		d.mu.Unlock()
	})
}

// updateUI updates the UI state
func (d *PremiumDashboard) updateUI(running bool) {
	RunOnMain(func() {
		if running {
			d.statusCircle.FillColor = color.RGBA{16, 185, 129, 255}
			d.statusLabel.SetText("Server Running")
			d.startBtn.Disable()
			d.stopBtn.Enable()
		} else {
			d.statusCircle.FillColor = color.RGBA{239, 68, 68, 255}
			d.statusLabel.SetText("Server Stopped")
			d.startBtn.Enable()
			d.stopBtn.Disable()
			d.serverInfo.SetText("🔌 Server: Not Started")
		}
		d.statusCircle.Refresh()
	})
}

// Stop stops the dashboard background goroutines
func (d *PremiumDashboard) Stop() {
	close(d.stopChan)
}
