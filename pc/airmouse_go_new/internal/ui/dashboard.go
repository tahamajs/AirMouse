package ui

import (
	"bytes"
	"fmt"
	"image/png"
	"strings"
	"sync"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"
	qrcode "github.com/skip2/go-qrcode"

	"airmouse-go/internal/config"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/protocol"
	"airmouse-go/internal/utils"
)

// ------------------------------------------------------------
//  DashboardTab
// ------------------------------------------------------------

type DashboardTab struct {
	statsLabel      *widget.Label
	connLabel       *widget.Label
	endpointLabel   *widget.Label
	uptimeLabel     *widget.Label
	aiStatusLabel   *widget.Label
	serverStatus    *widget.Label
	serverNameLabel *widget.Label
	deviceListLabel *widget.Label

	startBtn   *widget.Button
	stopBtn    *widget.Button
	qrBtn      *widget.Button
	refreshBtn *widget.Button

	speedChart    fyne.CanvasObject
	qualityWidget *ConnectionQualityWidget // defined in connection.go

	serverStart time.Time
	mu          sync.Mutex
	mouse       control.MouseController
	server      *protocol.ProtocolServer
	deviceMgr   *device.Manager
	cfg         *config.Config
}

// NewDashboardTab creates the dashboard tab content.
func NewDashboardTab(server *protocol.ProtocolServer, mouse control.MouseController, deviceMgr *device.Manager) fyne.CanvasObject {
	tab := &DashboardTab{
		mouse:     mouse,
		server:    server,
		deviceMgr: deviceMgr,
		cfg:       config.Get(),
	}

	// Header with server name
	tab.serverNameLabel = widget.NewLabelWithStyle(
		fmt.Sprintf("🎯 %s", tab.cfg.ServerName),
		fyne.TextAlignCenter,
		fyne.TextStyle{Bold: true},
	)

	// Status displays
	tab.statsLabel = widget.NewLabel("📊 Clicks: 0  |  Double: 0  |  Right: 0  |  Scroll: 0")
	tab.connLabel = widget.NewLabel("📱 Connected devices: 0")
	tab.deviceListLabel = widget.NewLabel("")
	tab.deviceListLabel.Wrapping = fyne.TextWrapWord

	tab.endpointLabel = widget.NewLabel("🔌 Endpoint: not started")
	tab.uptimeLabel = widget.NewLabel("⏱️ Uptime: --:--:--")
	tab.aiStatusLabel = widget.NewLabel("🧠 AI Smoothing: Disabled")

	tab.serverStatus = widget.NewLabelWithStyle(
		"⛔ Server Status: Stopped",
		fyne.TextAlignCenter,
		fyne.TextStyle{Bold: true},
	)

	// Speed chart & quality widget – use the existing constructors from other files
	tab.speedChart = NewSpeedChart()          // defined in speedchart_stub.go (or elsewhere)
	tab.qualityWidget = NewConnectionQualityWidget() // defined in connection.go

	// Buttons
	tab.startBtn = widget.NewButtonWithIcon("Start Server", theme.MediaPlayIcon(), func() {
		if err := server.Start(); err == nil {
			tab.mu.Lock()
			tab.serverStart = time.Now()
			tab.mu.Unlock()
			tab.serverStatus.SetText("✅ Server Status: Running")
			tab.startBtn.Disable()
			tab.stopBtn.Enable()
			tab.refreshBtn.Enable()

			ip := utils.GetLocalIP()
			tab.endpointLabel.SetText(fmt.Sprintf(
				"🔌 Endpoint: %s:%d (TCP) | ws://%s:%d\n📡 UDP Discovery: port %d",
				ip, tab.cfg.Port, ip, tab.cfg.WebSocketPort, tab.cfg.UDPPort,
			))

			if tab.cfg.EnableAISmoothing {
				tab.aiStatusLabel.SetText("🧠 AI Smoothing: Enabled ✅")
			} else {
				tab.aiStatusLabel.SetText("🧠 AI Smoothing: Disabled ⭕")
			}
		} else {
			win := getCurrentWindow()
			if win != nil {
				dialog.ShowError(fmt.Errorf("Failed to start server: %v", err), win)
			}
		}
	})

	tab.stopBtn = widget.NewButtonWithIcon("Stop Server", theme.MediaStopIcon(), func() {
		server.Stop()
		tab.serverStatus.SetText("⛔ Server Status: Stopped")
		tab.startBtn.Enable()
		tab.stopBtn.Disable()
		tab.refreshBtn.Disable()
		tab.endpointLabel.SetText("🔌 Endpoint: not started")
		tab.uptimeLabel.SetText("⏱️ Uptime: --:--:--")
	})
	tab.stopBtn.Disable()

	tab.refreshBtn = widget.NewButtonWithIcon("Refresh", theme.ViewRefreshIcon(), func() {
		tab.refreshStats()
	})
	tab.refreshBtn.Disable()

	tab.qrBtn = widget.NewButtonWithIcon("Show QR Code", theme.InfoIcon(), func() {
		tab.showPairingQRDialog()
	})

	// Profile card
	profileCard := tab.createProfileCard()

	// Stats card
	statsCard := container.NewVBox(
		widget.NewLabelWithStyle("📊 Statistics", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		tab.statsLabel,
		tab.connLabel,
		tab.deviceListLabel,
	)

	// Actions card
	actionsCard := container.NewVBox(
		widget.NewLabelWithStyle("⚡ Quick Actions", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		container.NewHBox(
			widget.NewButton("⏸️ Pause", func() { control.SetMovementPaused(true) }),
			widget.NewButton("▶️ Resume", func() { control.SetMovementPaused(false) }),
			widget.NewButton("🔄 Reset Stats", func() { tab.mouse.ResetStats() }),
		),
	)

	// Main layout (two columns)
	content := container.NewVBox(
		tab.serverNameLabel,
		widget.NewSeparator(),
		tab.serverStatus,
		container.NewHBox(tab.startBtn, tab.stopBtn, tab.refreshBtn, tab.qrBtn),
		widget.NewSeparator(),
		container.NewGridWithColumns(2,
			container.NewVBox(
				statsCard,
				tab.qualityWidget.Widget(),
				actionsCard,
			),
			container.NewVBox(
				widget.NewLabelWithStyle("📈 Cursor Speed", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
				tab.speedChart,
			),
		),
		widget.NewSeparator(),
		profileCard,
		widget.NewSeparator(),
		container.NewVBox(
			tab.endpointLabel,
			tab.uptimeLabel,
			tab.aiStatusLabel,
		),
	)

	// Start background stats updater
	go tab.statsUpdater()

	return container.NewScroll(content)
}

// ------------------------------------------------------------
//  Profile card
// ------------------------------------------------------------

func (t *DashboardTab) createProfileCard() fyne.CanvasObject {
	nameLabel := widget.NewLabelWithStyle("👤 User Profile", fyne.TextAlignLeading, fyne.TextStyle{Bold: true})

	userName := widget.NewLabel(fmt.Sprintf("Name: %s", t.cfg.UserName))
	serverName := widget.NewLabel(fmt.Sprintf("Server: %s", t.cfg.ServerName))
	version := widget.NewLabel(fmt.Sprintf("Version: %s", t.cfg.Version))

	editBtn := widget.NewButton("Edit Profile", func() {
		t.showProfileEditor()
	})

	return container.NewVBox(
		nameLabel,
		widget.NewSeparator(),
		userName,
		serverName,
		version,
		editBtn,
	)
}

// ------------------------------------------------------------
//  Stats updater (background goroutine)
// ------------------------------------------------------------

func (t *DashboardTab) statsUpdater() {
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()
	for range ticker.C {
		t.refreshStats()
	}
}

// ------------------------------------------------------------
//  Refresh stats (called periodically)
// ------------------------------------------------------------

func (t *DashboardTab) refreshStats() {
	if t.mouse == nil {
		return
	}
	clicks, dbl, right, scroll := t.mouse.Stats()
	devices := t.deviceMgr.GetAllDevices()
	deviceCount := len(devices)

	// Build device list string
	var deviceNames []string
	for _, d := range devices {
		deviceNames = append(deviceNames, fmt.Sprintf("%s (%s)", d.Name, d.Type))
	}
	deviceListStr := "None"
	if len(deviceNames) > 0 {
		deviceListStr = strings.Join(deviceNames, ", ")
	}

	RunOnMain(func() {
		t.statsLabel.SetText(fmt.Sprintf(
			"📊 Clicks: %d  |  Double: %d  |  Right: %d  |  Scroll: %d",
			clicks, dbl, right, scroll,
		))
		t.connLabel.SetText(fmt.Sprintf("📱 Connected devices: %d", deviceCount))
		t.deviceListLabel.SetText(fmt.Sprintf("📱 Devices: %s", deviceListStr))

		t.mu.Lock()
		if !t.serverStart.IsZero() {
			uptime := time.Since(t.serverStart)
			t.uptimeLabel.SetText(fmt.Sprintf(
				"⏱️ Uptime: %02d:%02d:%02d",
				int(uptime.Hours()),
				int(uptime.Minutes())%60,
				int(uptime.Seconds())%60,
			))
		}
		t.mu.Unlock()

		// Update connection quality
		if deviceCount > 0 {
			t.qualityWidget.SetQuality(-45, 15)
		} else {
			t.qualityWidget.SetQuality(-99, 0)
		}
	})
}

// ------------------------------------------------------------
//  Profile editor dialog
// ------------------------------------------------------------

func (t *DashboardTab) showProfileEditor() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	nameEntry := widget.NewEntry()
	nameEntry.SetText(t.cfg.UserName)
	nameEntry.SetPlaceHolder("Enter your name")

	serverNameEntry := widget.NewEntry()
	serverNameEntry.SetText(t.cfg.ServerName)
	serverNameEntry.SetPlaceHolder("Enter server name")

	content := container.NewVBox(
		widget.NewLabelWithStyle("Edit Profile", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("Your Name:"),
		nameEntry,
		widget.NewLabel("Server Name:"),
		serverNameEntry,
	)

	dialog.ShowCustomConfirm("Edit Profile", "Save", "Cancel", content, func(save bool) {
		if save {
			t.cfg.UserName = nameEntry.Text
			t.cfg.ServerName = serverNameEntry.Text
			if err := t.cfg.Save(); err == nil {
				t.serverNameLabel.SetText(fmt.Sprintf("🎯 %s", t.cfg.ServerName))
				dialog.ShowInformation("Profile Updated", "Your profile has been saved.", win)
				t.refreshStats()
			} else {
				dialog.ShowError(fmt.Errorf("Failed to save: %v", err), win)
			}
		}
	}, win)
}

// ------------------------------------------------------------
//  QR code pairing dialog
// ------------------------------------------------------------

func (t *DashboardTab) showPairingQRDialog() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	ip := utils.GetLocalIP()
	port := t.cfg.Port

	pairingData := fmt.Sprintf(
		"airmouse://pair?ip=%s&port=%d&name=%s&version=%s",
		ip, port, t.cfg.ServerName, t.cfg.Version,
	)

	pngBytes, err := qrcode.Encode(pairingData, qrcode.High, 300)
	if err != nil {
		dialog.ShowError(err, win)
		return
	}
	img, err := png.Decode(bytes.NewReader(pngBytes))
	if err != nil {
		dialog.ShowError(err, win)
		return
	}
	qrImage := canvas.NewImageFromImage(img)
	qrImage.FillMode = canvas.ImageFillOriginal

	instructions := widget.NewLabel(
		"📱 How to pair:\n\n" +
			"1. Open Air Mouse app on your phone\n" +
			"2. Tap the QR scanner icon\n" +
			"3. Scan this QR code\n" +
			"4. Your device will appear in the Devices tab\n\n" +
			fmt.Sprintf("Server: %s\nIP: %s\nPort: %d\nVersion: %s",
				t.cfg.ServerName, ip, port, t.cfg.Version),
	)
	instructions.Wrapping = fyne.TextWrapWord

	content := container.NewVBox(
		widget.NewLabelWithStyle("🔗 Pair New Device", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		qrImage,
		instructions,
	)

	dialog.ShowCustom("Pairing Information", "Close", content, win)
}