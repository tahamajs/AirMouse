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
//  DashboardTab – Optimized version without chart to prevent hangs
// ------------------------------------------------------------

type DashboardTab struct {
	statsLabel      *widget.Label
	connLabel       *widget.Label
	endpointLabel   *widget.Label
	uptimeLabel     *widget.Label
	aiStatusLabel   *widget.Label
	serverStatus    *widget.Label
	serverNameLabel *widget.Label
	deviceDetailBox *widget.Label
	recentLogsBox   *widget.Label

	controlBtn *widget.Button
	qrBtn      *widget.Button
	refreshBtn *widget.Button

	serverStart time.Time
	mu          sync.Mutex
	logMu       sync.Mutex
	recentLogs  []string
	mouse       control.MouseController
	server      *protocol.ProtocolServer
	deviceMgr   *device.Manager
	cfg         *config.Config
	stopChan    chan struct{}
}

// NewDashboardTab creates the dashboard tab content.
func NewDashboardTab(server *protocol.ProtocolServer, mouse control.MouseController, deviceMgr *device.Manager) fyne.CanvasObject {
	// Safety: if any dependency is nil, return a placeholder
	if server == nil || mouse == nil || deviceMgr == nil {
		return widget.NewLabelWithStyle("⚠️ Dashboard unavailable - dependencies missing",
			fyne.TextAlignCenter, fyne.TextStyle{Bold: true})
	}

	tab := &DashboardTab{
		mouse:     mouse,
		server:    server,
		deviceMgr: deviceMgr,
		cfg:       config.Get(),
		stopChan:  make(chan struct{}),
	}

	// Header with server name
	tab.serverNameLabel = widget.NewLabelWithStyle(
		fmt.Sprintf("🎯 %s", tab.cfg.ServerName),
		fyne.TextAlignLeading,
		fyne.TextStyle{Bold: true},
	)

	// Status displays
	tab.statsLabel = widget.NewLabel("📊 Clicks: 0  |  Double: 0  |  Right: 0  |  Scroll: 0")
	tab.connLabel = widget.NewLabel("📱 Connected devices: 0")

	tab.endpointLabel = widget.NewLabel("🔌 Endpoint: not started")
	tab.uptimeLabel = widget.NewLabel("⏱️ Uptime: --:--:--")
	tab.aiStatusLabel = widget.NewLabel("🧠 AI Smoothing: Disabled")

	tab.serverStatus = widget.NewLabelWithStyle(
		"⛔ Server Status: Stopped",
		fyne.TextAlignCenter,
		fyne.TextStyle{Bold: true},
	)
	tab.deviceDetailBox = widget.NewLabel("No connected devices yet.")
	tab.deviceDetailBox.Wrapping = fyne.TextWrapWord
	tab.recentLogsBox = widget.NewLabel("Waiting for logs...")
	tab.recentLogsBox.Wrapping = fyne.TextWrapWord
	tab.recentLogsBox.SetText("Waiting for logs...\n")

	utils.AddLogHook(func(level, msg string) {
		tab.addRecentLog(level, msg)
	})

	// Buttons
	tab.controlBtn = widget.NewButtonWithIcon("Start Server", theme.MediaPlayIcon(), func() {
		if server.IsRunning() {
			tab.controlBtn.Disable()
			tab.serverStatus.SetText("⏳ Server Status: Stopping...")
				go func() {
					server.Stop()
					RunOnMain(func() {
						tab.serverStatus.SetText("⛔ Server Status: Stopped")
						tab.controlBtn.SetText("Start Server")
						tab.controlBtn.SetIcon(theme.MediaPlayIcon())
						tab.controlBtn.Enable()
						tab.refreshBtn.Disable()
						tab.endpointLabel.SetText("🔌 Endpoint: not started")
						tab.uptimeLabel.SetText("⏱️ Uptime: --:--:--")
						tab.deviceDetailBox.SetText("No connected devices yet.")
					})
				}()
			return
		}

		tab.controlBtn.Disable()
		tab.serverStatus.SetText("⏳ Server Status: Starting...")
		go func() {
			err := server.Start()
			RunOnMain(func() {
				if server.IsRunning() {
					tab.mu.Lock()
					tab.serverStart = time.Now()
					tab.mu.Unlock()
					tab.serverStatus.SetText("✅ Server Status: Running")
					tab.controlBtn.SetText("Stop Server")
					tab.controlBtn.SetIcon(theme.MediaStopIcon())
					tab.controlBtn.Enable()
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
					tab.serverStatus.SetText("⛔ Server Status: Stopped")
					tab.controlBtn.SetText("Start Server")
					tab.controlBtn.SetIcon(theme.MediaPlayIcon())
					tab.controlBtn.Enable()
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
		}()
	})

	tab.refreshBtn = widget.NewButtonWithIcon("Refresh", theme.ViewRefreshIcon(), func() {
		tab.refreshStats()
	})
	tab.refreshBtn.Disable()

	tab.qrBtn = widget.NewButtonWithIcon("Show QR Code", theme.InfoIcon(), func() {
		tab.showPairingQRDialog()
	})

	// Profile card
	profileCard := NewGlassCard(container.NewPadded(tab.createProfileCard()))

	// Stats card
	statsCard := NewGlassCard(container.NewPadded(container.NewVBox(
		widget.NewLabelWithStyle("📊 Statistics", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		tab.statsLabel,
		tab.connLabel,
	)))

	// Actions card
	actionsCard := NewGlassCard(container.NewPadded(container.NewVBox(
		widget.NewLabelWithStyle("⚡ Quick Actions", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		container.NewHBox(
			widget.NewButton("⏸️ Pause", func() { control.SetMovementPaused(true) }),
			widget.NewButton("▶️ Resume", func() { control.SetMovementPaused(false) }),
			widget.NewButton("🔄 Reset Stats", func() { tab.mouse.ResetStats() }),
		),
	)))

	// Status card
	statusCard := NewGlassCard(container.NewPadded(container.NewVBox(
		widget.NewLabelWithStyle("📡 Server Status", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		tab.serverStatus,
		tab.endpointLabel,
		tab.uptimeLabel,
		tab.aiStatusLabel,
	)))

	deviceCard := NewGlassCard(container.NewPadded(container.NewVBox(
		widget.NewLabelWithStyle("📱 Connected Devices", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		tab.connLabel,
		widget.NewLabelWithStyle("Latest device details", fyne.TextAlignLeading, fyne.TextStyle{Bold: false}),
		tab.deviceDetailBox,
	)))

	logsCard := NewGlassCard(container.NewPadded(container.NewVBox(
		widget.NewLabelWithStyle("📝 Recent Activity", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		container.NewScroll(tab.recentLogsBox),
	)))

	featureCard := NewGlassCard(container.NewPadded(container.NewVBox(
		widget.NewLabelWithStyle("✨ Features", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("• One-tap server start/stop"),
		widget.NewLabel("• QR pairing for instant phone setup"),
		widget.NewLabel("• Live device stats and metadata"),
		widget.NewLabel("• Recent activity feed from the shared logger"),
		widget.NewLabel("• Pause, resume, and reset movement controls"),
	)))

	hero := NewGlassCard(container.NewVBox(
		tab.serverNameLabel,
		widget.NewLabel("Air Mouse Pro turns your phone into a smooth wireless pointer."),
		widget.NewLabel("Use the big control below to start or stop the server, then pair from the QR code or Network tab."),
	))

	// Main layout (two columns)
	content := container.NewVBox(
		hero,
		widget.NewSeparator(),
		container.NewGridWithColumns(2,
			statsCard,
			statusCard,
		),
		container.NewPadded(tab.controlBtn),
		container.NewHBox(tab.refreshBtn, tab.qrBtn),
		actionsCard,
		deviceCard,
		logsCard,
		featureCard,
		profileCard,
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
//  Stats updater (background goroutine) – with rate limiting
// ------------------------------------------------------------

func (t *DashboardTab) statsUpdater() {
	ticker := time.NewTicker(2 * time.Second) // Reduced from 1s to 2s to reduce load
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			t.refreshStats()
		case <-t.stopChan:
			return
		}
	}
}

// ------------------------------------------------------------
//  Refresh stats (called periodically)
// ------------------------------------------------------------

func (t *DashboardTab) refreshStats() {
	if t.mouse == nil {
		return
	}

	// Get stats once
	clicks, dbl, right, scroll := t.mouse.Stats()
	devices := t.deviceMgr.GetAllDevices()
	deviceCount := len(devices)

	var deviceDetails []string
	for _, d := range devices {
		deviceDetails = append(deviceDetails, fmt.Sprintf(
			"• %s [%s]\n  ID: %s\n  Status: %s\n  Connected: %s\n  Last active: %s\n  Sent: %s (%d msg)\n  Received: %s (%d msg)\n  RSSI: %d\n  IP: %s\n  MAC: %s\n  Version: %s",
			d.Name,
			d.Type,
			d.ID,
			d.Status,
			FormatDuration(time.Since(d.ConnectedAt)),
			FormatDuration(time.Since(d.LastActive)),
			FormatBytes(d.BytesSent), d.MessagesSent,
			FormatBytes(d.BytesRecv), d.MessagesRecv,
			d.RSSI,
			emptyOrDash(d.IPAddress),
			emptyOrDash(d.MACAddress),
			emptyOrDash(d.Version),
		))
	}
	// Update UI in one batch
	RunOnMain(func() {
		t.statsLabel.SetText(fmt.Sprintf(
			"📊 Clicks: %d  |  Double: %d  |  Right: %d  |  Scroll: %d",
			clicks, dbl, right, scroll,
		))
		t.connLabel.SetText(fmt.Sprintf("📱 Connected devices: %d", deviceCount))
		if deviceCount > 0 {
			t.deviceDetailBox.SetText(strings.Join(deviceDetails, "\n\n"))
		} else {
			t.deviceDetailBox.SetText("No connected devices yet.")
		}

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
	})
}

func (t *DashboardTab) addRecentLog(level, msg string) {
	t.logMu.Lock()
	defer t.logMu.Unlock()

	line := fmt.Sprintf("%s [%s] %s", time.Now().Format("15:04:05"), level, msg)
	t.recentLogs = append(t.recentLogs, line)
	if len(t.recentLogs) > 12 {
		t.recentLogs = t.recentLogs[len(t.recentLogs)-12:]
	}

	text := strings.Join(t.recentLogs, "\n")
	RunOnMain(func() {
		if t.recentLogsBox != nil {
			t.recentLogsBox.SetText(text)
		}
	})
}

func emptyOrDash(v string) string {
	if strings.TrimSpace(v) == "" {
		return "-"
	}
	return v
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
		"airmouse://pair?ip=%s&port=%d&ws=ws://%s:%d/ws&name=%s&version=%s&protocol=WEBSOCKET",
		ip, port, ip, t.cfg.WebSocketPort, t.cfg.ServerName, t.cfg.Version,
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

// Stop stops the dashboard background goroutines
func (t *DashboardTab) Stop() {
	close(t.stopChan)
}
