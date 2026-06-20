//go:build windows

package ui

import (
	"bytes"
	"fmt"
	"time"

	"github.com/lxn/walk"
	"github.com/lxn/win"

	"airmouse-go/internal/utils"
)

// WindowsTrayApp implements the system tray for Windows using the walk library.
type WindowsTrayApp struct {
	tray      *walk.NotifyIcon
	app       *TrayApp
	stopChan  chan struct{}
	statusMap map[string]*walk.Action
	startBtn  *walk.Action
	stopBtn   *walk.Action
}

// NewWindowsTrayApp creates a new Windows tray application.
func NewWindowsTrayApp(app *TrayApp) *WindowsTrayApp {
	return &WindowsTrayApp{
		app:       app,
		stopChan:  make(chan struct{}),
		statusMap: make(map[string]*walk.Action),
	}
}

// Run starts the system tray and enters the message loop.
func (wt *WindowsTrayApp) Run() error {
	var err error
	wt.tray, err = walk.NewNotifyIcon()
	if err != nil {
		return fmt.Errorf("failed to create notify icon: %w", err)
	}

	// Load icon from embedded data
	icon, err := walk.NewIconFromReader(bytes.NewReader(AppIconData))
	if err != nil {
		// Fallback: try to load from resource or use default
		utils.LogError("Failed to load icon from embedded data: %v", err)
		icon, _ = walk.NewIconFromResourceID(1) // might fail, but we can ignore
	}
	if icon != nil {
		wt.tray.SetIcon(icon)
	}

	wt.tray.SetToolTip("Air Mouse Pro Server")

	// Build menu
	wt.buildMenu()

	// Show the tray icon
	wt.tray.SetVisible(true)

	// Show initial message
	wt.tray.ShowMessage("Air Mouse Pro", "Server is ready", walk.InfoIcon)

	// Start status updater
	go wt.updateLoop()

	// Wait for stop signal
	<-wt.stopChan

	// Cleanup
	if wt.tray != nil {
		wt.tray.Dispose()
	}
	return nil
}

// Stop shuts down the tray application.
func (wt *WindowsTrayApp) Stop() {
	close(wt.stopChan)
}

// buildMenu creates the context menu.
func (wt *WindowsTrayApp) buildMenu() {
	menu := walk.NewMenu()

	// ---- Status Section ----
	statusItem, _ := walk.NewAction("Server: Stopped")
	statusItem.SetEnabled(false)
	menu.Actions().Add(statusItem)
	wt.statusMap["status"] = statusItem

	clientsItem, _ := walk.NewAction("Clients: 0")
	clientsItem.SetEnabled(false)
	menu.Actions().Add(clientsItem)
	wt.statusMap["clients"] = clientsItem

	uptimeItem, _ := walk.NewAction("Uptime: --:--:--")
	uptimeItem.SetEnabled(false)
	menu.Actions().Add(uptimeItem)
	wt.statusMap["uptime"] = uptimeItem

	menu.Actions().Add(walk.NewSeparatorAction())

	// ---- Server Controls ----
	startBtn, _ := walk.NewAction("▶ Start Server")
	startBtn.Triggered().Attach(func() {
		if wt.app != nil {
			go wt.app.startServer()
		}
	})
	menu.Actions().Add(startBtn)
	wt.startBtn = startBtn

	stopBtn, _ := walk.NewAction("⏹ Stop Server")
	stopBtn.Triggered().Attach(func() {
		if wt.app != nil {
			go wt.app.stopServer()
		}
	})
	stopBtn.SetEnabled(false)
	menu.Actions().Add(stopBtn)
	wt.stopBtn = stopBtn

	restartBtn, _ := walk.NewAction("🔄 Restart Server")
	restartBtn.Triggered().Attach(func() {
		if wt.app != nil {
			go func() {
				wt.app.stopServer()
				time.Sleep(500 * time.Millisecond)
				wt.app.startServer()
			}()
		}
	})
	menu.Actions().Add(restartBtn)

	menu.Actions().Add(walk.NewSeparatorAction())

	// ---- QR and WiFi ----
	qrBtn, _ := walk.NewAction("📱 Show QR Code")
	qrBtn.Triggered().Attach(func() {
		if wt.app != nil {
			wt.app.showQRCodeWindow()
		}
	})
	menu.Actions().Add(qrBtn)

	wifiBtn, _ := walk.NewAction("📶 Share WiFi QR")
	wifiBtn.Triggered().Attach(func() {
		if wt.app != nil {
			wt.app.showWiFiQRCodeWindow()
		}
	})
	menu.Actions().Add(wifiBtn)

	copyIPBtn, _ := walk.NewAction("📋 Copy IP Address")
	copyIPBtn.Triggered().Attach(func() {
		if wt.app != nil {
			wt.app.copyIPToClipboard()
		}
	})
	menu.Actions().Add(copyIPBtn)

	menu.Actions().Add(walk.NewSeparatorAction())

	// ---- Web UI ----
	webBtn, _ := walk.NewAction("🌐 Open Web UI")
	webBtn.Triggered().Attach(func() {
		if wt.app != nil {
			wt.app.openBrowser(fmt.Sprintf("http://localhost:%d", wt.app.config.WebSocketPort))
		}
	})
	menu.Actions().Add(webBtn)

	apiBtn, _ := walk.NewAction("🔌 API Status")
	apiBtn.Triggered().Attach(func() {
		if wt.app != nil {
			wt.app.openBrowser(fmt.Sprintf("http://localhost:%d/api/status", wt.app.config.WebSocketPort))
		}
	})
	menu.Actions().Add(apiBtn)

	menu.Actions().Add(walk.NewSeparatorAction())

	// ---- Settings and Logs ----
	settingsBtn, _ := walk.NewAction("⚙️ Settings")
	settingsBtn.Triggered().Attach(func() {
		if wt.app != nil {
			wt.app.showSettingsWindow()
		}
	})
	menu.Actions().Add(settingsBtn)

	logsBtn, _ := walk.NewAction("📋 View Logs")
	logsBtn.Triggered().Attach(func() {
		if wt.app != nil {
			wt.app.showLogsWindow()
		}
	})
	menu.Actions().Add(logsBtn)

	aboutBtn, _ := walk.NewAction("ℹ️ About")
	aboutBtn.Triggered().Attach(func() {
		if wt.app != nil {
			wt.app.showAboutWindow()
		}
	})
	menu.Actions().Add(aboutBtn)

	menu.Actions().Add(walk.NewSeparatorAction())

	// ---- Quit ----
	quitBtn, _ := walk.NewAction("❌ Quit")
	quitBtn.Triggered().Attach(func() {
		if wt.app != nil && wt.app.isRunning {
			wt.app.stopServer()
		}
		wt.Stop()
	})
	menu.Actions().Add(quitBtn)

	wt.tray.ContextMenu().Actions().Add(menu.Actions()...)
}

// updateLoop periodically updates the tray icon and menu items.
func (wt *WindowsTrayApp) updateLoop() {
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			wt.updateStatus()
		case <-wt.stopChan:
			return
		}
	}
}

// updateStatus refreshes status labels and button states.
func (wt *WindowsTrayApp) updateStatus() {
	if wt.app == nil || wt.tray == nil {
		return
	}

	running := wt.app.isRunning

	// Update status text
	if running {
		wt.tray.SetToolTip("Air Mouse Pro - Running")
		wt.tray.ShowMessage("Air Mouse Pro", "Server is running", walk.InfoIcon)

		if status, ok := wt.statusMap["status"]; ok {
			status.SetText("Server: Running ✅")
		}
		if wt.startBtn != nil {
			wt.startBtn.SetEnabled(false)
		}
		if wt.stopBtn != nil {
			wt.stopBtn.SetEnabled(true)
		}

		// Update uptime
		if !wt.app.startTime.IsZero() {
			uptime := time.Since(wt.app.startTime)
			if uptimeItem, ok := wt.statusMap["uptime"]; ok {
				uptimeItem.SetText(fmt.Sprintf("Uptime: %02d:%02d:%02d",
					int(uptime.Hours()),
					int(uptime.Minutes())%60,
					int(uptime.Seconds())%60))
			}
		}
	} else {
		wt.tray.SetToolTip("Air Mouse Pro - Stopped")
		if status, ok := wt.statusMap["status"]; ok {
			status.SetText("Server: Stopped ⛔")
		}
		if wt.startBtn != nil {
			wt.startBtn.SetEnabled(true)
		}
		if wt.stopBtn != nil {
			wt.stopBtn.SetEnabled(false)
		}
		if uptimeItem, ok := wt.statusMap["uptime"]; ok {
			uptimeItem.SetText("Uptime: --:--:--")
		}
	}

	// Update clients count
	clientCount := len(wt.app.clients)
	if clientsItem, ok := wt.statusMap["clients"]; ok {
		clientsItem.SetText(fmt.Sprintf("Clients: %d", clientCount))
	}
}

// helper to show a message box
func (wt *WindowsTrayApp) showMessage(title, message string, icon walk.MsgBoxIcon) {
	walk.MsgBox(wt.tray, title, message, walk.MsgBoxOK|icon)
}

// Override ShowError for Windows
func (wt *WindowsTrayApp) ShowError(err error) {
	wt.showMessage("Error", err.Error(), walk.ErrorIcon)
}

// Override ShowInfo for Windows
func (wt *WindowsTrayApp) ShowInfo(title, message string) {
	wt.showMessage(title, message, walk.InfoIcon)
}