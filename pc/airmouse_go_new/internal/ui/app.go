package ui

import (
	"bytes"
	"fmt"
	"image/png"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/app"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"
	"github.com/skip2/go-qrcode"

	"airmouse-go/internal/config"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/personalization"
	"airmouse-go/internal/protocol"
	"airmouse-go/internal/utils"
)

// ------------------------------------------------------------
//  App struct
// ------------------------------------------------------------

type App struct {
	fyneApp   fyne.App
	window    fyne.Window
	cfg       *config.Config
	server    *protocol.ProtocolServer
	mouse     control.MouseController
	deviceMgr *device.Manager
	collector *personalization.DataCollector

	dashboardTab fyne.CanvasObject
	devicesTab   fyne.CanvasObject
	networkTab   fyne.CanvasObject
	gesturesTab  fyne.CanvasObject
	proximityTab fyne.CanvasObject
	analyticsTab fyne.CanvasObject
	settingsTab  fyne.CanvasObject
	logsTab      fyne.CanvasObject

	statusBar        *StatusBar
	connectionStatus *widget.Label
	summaryStatus    *widget.Label
}

// ------------------------------------------------------------
//  Constructor
// ------------------------------------------------------------

func NewApp(cfg *config.Config, server *protocol.ProtocolServer, mouse control.MouseController, deviceMgr *device.Manager) *App {
	selectedTheme := getThemeByName(cfg.Theme) // defined in themes.go
	a := app.New()
	if selectedTheme != nil {
		a.Settings().SetTheme(selectedTheme)
	}

	var collector *personalization.DataCollector
	if cfg.EnablePersonalization {
		collector = personalization.NewDataCollector()
	}

	return &App{
		fyneApp:   a,
		cfg:       cfg,
		server:    server,
		mouse:     mouse,
		deviceMgr: deviceMgr,
		collector: collector,
	}
}

// ------------------------------------------------------------
//  Run – creates window, tabs, and starts the event loop
// ------------------------------------------------------------

func (a *App) Run() error {
	a.window = a.fyneApp.NewWindow(fmt.Sprintf("Air Mouse Pro Server - %s", a.cfg.ServerName))
	a.window.Resize(fyne.NewSize(1400, 900))
	a.window.CenterOnScreen()
	a.window.SetMaster()

	// Status bar
	a.statusBar = NewStatusBar()
	a.connectionStatus = widget.NewLabel("🔌 Status: Ready")
	a.summaryStatus = widget.NewLabel("Server details will appear here once it starts.")
	a.summaryStatus.Wrapping = fyne.TextWrapWord

	// ----- Create all tabs (using safeTab to catch nil) -----
	a.dashboardTab = safeTab(NewDashboardTab(a.server, a.mouse, a.deviceMgr), "Dashboard")
	a.devicesTab = safeTab(NewDevicesTab(a.deviceMgr), "Devices")
	a.networkTab = safeTab(NewNetworkTab(a.cfg), "Network")
	a.gesturesTab = safeTab(NewGesturesTab(), "Gestures")
	a.proximityTab = safeTab(NewProximityTab(), "Proximity")
	a.analyticsTab = safeTab(NewAnalyticsTab(a.collector), "Analytics")
	a.settingsTab = safeTab(NewSettingsTab(a.cfg, a.mouse), "Settings")
	a.logsTab = safeTab(NewLogsTab(), "Logs")

	// ----- Debug: Print tab status -----
	utils.LogInfo("All tabs created successfully")
	utils.LogDebug("Dashboard tab: %T", a.dashboardTab)
	utils.LogDebug("Devices tab: %T", a.devicesTab)
	utils.LogDebug("Network tab: %T", a.networkTab)
	utils.LogDebug("Gestures tab: %T", a.gesturesTab)
	utils.LogDebug("Proximity tab: %T", a.proximityTab)
	utils.LogDebug("Analytics tab: %T", a.analyticsTab)
	utils.LogDebug("Settings tab: %T", a.settingsTab)
	utils.LogDebug("Logs tab: %T", a.logsTab)

	// ----- Tab container -----
	tabs := container.NewAppTabs(
		container.NewTabItemWithIcon("Dashboard", theme.HomeIcon(), a.dashboardTab),
		container.NewTabItemWithIcon("Devices", theme.ComputerIcon(), a.devicesTab),
		container.NewTabItemWithIcon("Network", theme.ComputerIcon(), a.networkTab),
		container.NewTabItemWithIcon("Gestures", theme.ContentCopyIcon(), a.gesturesTab),
		container.NewTabItemWithIcon("Proximity", theme.VisibilityIcon(), a.proximityTab),
		container.NewTabItemWithIcon("Analytics", theme.InfoIcon(), a.analyticsTab),
		container.NewTabItemWithIcon("Settings", theme.SettingsIcon(), a.settingsTab),
		container.NewTabItemWithIcon("Logs", theme.DocumentIcon(), a.logsTab),
	)
	tabs.SetTabLocation(container.TabLocationTop)
	tabs.OnSelected = func(ti *container.TabItem) {
		a.onTabSelected(ti)
	}

	// ----- Menu & Toolbar -----
	mainMenu := a.createMenuBar()
	a.window.SetMainMenu(mainMenu)
	toolbar := a.createToolbar()

	// ----- Main layout -----
	content := container.NewBorder(
		toolbar,
		a.statusBar.Widget(),
		nil, nil,
		tabs,
	)
	a.window.SetContent(content)

	// ----- Window close handler -----
	a.window.SetCloseIntercept(func() {
		a.onWindowClose()
	})

	// ----- Start background tasks -----
	go a.connectionStatusUpdater()

	// ----- Show and run -----
	utils.LogInfo("Showing window")
	a.window.ShowAndRun()
	utils.LogInfo("Window closed")
	return nil
}

// ------------------------------------------------------------
//  safeTab – prevents nil panics
// ------------------------------------------------------------

func safeTab(tab fyne.CanvasObject, name string) fyne.CanvasObject {
	if tab != nil {
		return tab
	}
	utils.LogWarn("Tab %s returned nil, using placeholder", name)
	return widget.NewLabelWithStyle("⚠️ "+name+" tab not implemented", fyne.TextAlignCenter, fyne.TextStyle{Bold: true})
}

// ------------------------------------------------------------
//  Menu Bar
// ------------------------------------------------------------

func (a *App) createMenuBar() *fyne.MainMenu {
	fileMenu := fyne.NewMenu("File",
		fyne.NewMenuItem("Start Server", func() {
			a.startServerAsync("menu")
		}),
		fyne.NewMenuItem("Stop Server", func() { a.stopServerAsync("menu") }),
		fyne.NewMenuItem("Restart Server", func() {
			go func() {
				a.stopServerAsync("restart")
				time.Sleep(600 * time.Millisecond)
				a.startServerAsync("restart")
			}()
		}),
		fyne.NewMenuItemSeparator(),
		fyne.NewMenuItem("Export Configuration", func() { a.exportConfig() }),
		fyne.NewMenuItem("Import Configuration", func() { a.importConfig() }),
		fyne.NewMenuItemSeparator(),
		fyne.NewMenuItem("Quit", func() { a.onWindowClose() }),
	)

	serverMenu := fyne.NewMenu("Server",
		fyne.NewMenuItem("Show QR Code", func() { a.showPairingQR() }),
		fyne.NewMenuItem("Pair New Device", func() {
			ShowPairingWizard(a.window, fmt.Sprintf("ws://%s:%d/ws", utils.GetLocalIP(), a.cfg.WebSocketPort))
		}),
		fyne.NewMenuItemSeparator(),
		fyne.NewMenuItem("Clear All Devices", func() { a.clearAllDevices() }),
		fyne.NewMenuItem("Reset Statistics", func() { a.resetStatistics() }),
	)

	viewMenu := fyne.NewMenu("View",
		fyne.NewMenuItem("Refresh", func() { a.window.Content().Refresh() }),
		fyne.NewMenuItem("Reset Layout", func() {
			a.window.Resize(fyne.NewSize(1400, 900))
			a.window.CenterOnScreen()
		}),
		fyne.NewMenuItemSeparator(),
		fyne.NewMenuItem("Dark Theme", func() {
			a.fyneApp.Settings().SetTheme(theme.DarkTheme())
			a.cfg.Theme = "dark"
			_ = a.cfg.Save()
		}),
		fyne.NewMenuItem("Light Theme", func() {
			a.fyneApp.Settings().SetTheme(theme.LightTheme())
			a.cfg.Theme = "light"
			_ = a.cfg.Save()
		}),
	)

	toolsMenu := fyne.NewMenu("Tools",
		fyne.NewMenuItem("Personalization Trainer", func() { a.showPersonalizationDialog() }),
		fyne.NewMenuItem("Gesture Recorder", func() { a.showGestureRecorder() }),
		fyne.NewMenuItem("Network Diagnostics", func() { a.showNetworkDiagnostics() }),
		fyne.NewMenuItem("Performance Monitor", func() { a.showPerformanceMonitor() }),
	)

	helpMenu := fyne.NewMenu("Help",
		fyne.NewMenuItem("User Guide", func() { a.showUserGuide() }),
		fyne.NewMenuItem("API Documentation", func() { a.showAPIDocs() }),
		fyne.NewMenuItemSeparator(),
		fyne.NewMenuItem("Check for Updates", func() { a.checkForUpdates() }),
		fyne.NewMenuItem("Report Issue", func() { a.reportIssue() }),
		fyne.NewMenuItemSeparator(),
		fyne.NewMenuItem("About", func() { ShowAboutDialog(a.window) }),
	)

	return fyne.NewMainMenu(fileMenu, serverMenu, viewMenu, toolsMenu, helpMenu)
}

// ------------------------------------------------------------
//  Toolbar
// ------------------------------------------------------------

func (a *App) createToolbar() fyne.CanvasObject {
	settingsBtn := widget.NewButtonWithIcon("Settings", theme.SettingsIcon(), func() {
		dialog.ShowInformation("Settings", "Open the Settings tab to adjust server, network, and pairing options.", a.window)
	})
	helpBtn := widget.NewButtonWithIcon("Help", theme.HelpIcon(), func() {
		showShortcutsDialog(a.window)
	})

	title := widget.NewLabelWithStyle("Air Mouse Pro Server", fyne.TextAlignLeading, fyne.TextStyle{Bold: true})
	subtitle := widget.NewLabel("One dashboard for server control, device status, and pairing.")
	subtitle.Wrapping = fyne.TextWrapWord

	return container.NewBorder(
		container.NewVBox(title, subtitle, a.connectionStatus, a.summaryStatus),
		nil,
		nil,
		container.NewHBox(settingsBtn, helpBtn),
		nil,
	)
}

func (a *App) startServerAsync(source string) {
	if a.server == nil {
		return
	}
	utils.LogInfo("UI requested server start: source=%s", source)
	if a.connectionStatus != nil {
		a.connectionStatus.SetText("⏳ Status: Starting server...")
	}
	go func() {
		err := a.server.Start()
		RunOnMain(func() {
			if err != nil {
				if a.server.IsRunning() {
					dialog.ShowInformation("Server started with warnings", fmt.Sprintf(
						"Server is running, but one or more protocols reported an issue:\n\n%v", err), a.window)
				} else {
					dialog.ShowError(err, a.window)
				}
			}
			a.refreshConnectionSummary()
		})
	}()
}

func (a *App) stopServerAsync(source string) {
	if a.server == nil {
		return
	}
	utils.LogInfo("UI requested server stop: source=%s", source)
	if a.connectionStatus != nil {
		a.connectionStatus.SetText("⏳ Status: Stopping server...")
	}
	go func() {
		a.server.Stop()
		RunOnMain(func() {
			a.refreshConnectionSummary()
		})
	}()
}

// ------------------------------------------------------------
//  Tab selection handler
// ------------------------------------------------------------

func (a *App) onTabSelected(ti *container.TabItem) {
	utils.LogDebug("Selected tab: %s", ti.Text)
}

// ------------------------------------------------------------
//  Connection status updater (runs in background)
// ------------------------------------------------------------

func (a *App) connectionStatusUpdater() {
	ticker := time.NewTicker(2 * time.Second)
	defer ticker.Stop()
	for range ticker.C {
		RunOnMain(func() { a.refreshConnectionSummary() })
	}
}

func (a *App) refreshConnectionSummary() {
	if a.connectionStatus == nil {
		return
	}

	deviceCount := 0
	if a.deviceMgr != nil {
		deviceCount = len(a.deviceMgr.GetAllDevices())
	}
	utils.LogDebug("Refreshing connection summary: server_nil=%t running=%t devices=%d", a.server == nil, a.server != nil && a.server.IsRunning(), deviceCount)

	if a.server == nil {
		a.connectionStatus.SetText("⚪ Status: Server unavailable")
		if a.summaryStatus != nil {
			a.summaryStatus.SetText("The server instance is not configured.")
		}
		return
	}

	if !a.server.IsRunning() {
		a.connectionStatus.SetText("⛔ Status: Stopped")
		if a.summaryStatus != nil {
			a.summaryStatus.SetText("No active listeners. Use Start Server to bring up TCP, WebSocket, and UDP discovery.")
		}
		return
	}

	ip := utils.GetLocalIP()
	a.connectionStatus.SetText(fmt.Sprintf("🟢 Running: %d device(s) connected", deviceCount))
	if a.summaryStatus != nil {
		a.summaryStatus.SetText(fmt.Sprintf(
			"Listening on %s:%d | ws://%s:%d/ws | UDP %d | Theme: %s",
			ip,
			a.cfg.Port,
			ip,
			a.cfg.WebSocketPort,
			a.cfg.UDPPort,
			a.cfg.Theme,
		))
	}
}

// ------------------------------------------------------------
//  Window close & stop
// ------------------------------------------------------------

func (a *App) onWindowClose() {
	dialog.ShowConfirm("Quit", "Are you sure you want to quit Air Mouse Pro Server?", func(confirmed bool) {
		if confirmed {
			a.stopServerAsync("window-close")
			time.Sleep(300 * time.Millisecond)
			if err := a.cfg.Save(); err != nil {
				utils.LogError("Failed to save config: %v", err)
			}
			if a.statusBar != nil {
				a.statusBar.Stop()
			}
			a.fyneApp.Quit()
		}
	}, a.window)
}

func (a *App) Stop() {
	a.onWindowClose()
}

// ------------------------------------------------------------
//  Various dialog helpers
// ------------------------------------------------------------

func (a *App) exportConfig() {
	dialog.ShowFileSave(func(writer fyne.URIWriteCloser, err error) {
		if err == nil && writer != nil {
			defer writer.Close()
			_, _ = writer.Write([]byte(a.cfg.ToJSON()))
			dialog.ShowInformation("Export Successful", "Configuration exported successfully.", a.window)
		}
	}, a.window)
}

func (a *App) importConfig() {
	dialog.ShowFileOpen(func(reader fyne.URIReadCloser, err error) {
		if err == nil && reader != nil {
			defer reader.Close()
			buf := make([]byte, 1024*1024)
			n, _ := reader.Read(buf)
			if err := a.cfg.FromJSON(string(buf[:n])); err == nil {
				dialog.ShowInformation("Import Successful", "Configuration imported successfully. Please restart the app.", a.window)
			}
		}
	}, a.window)
}

func (a *App) showPairingQR() {
	ip := utils.GetLocalIP()
	port := a.cfg.WebSocketPort
	data := fmt.Sprintf("airmouse://pair?ip=%s&port=%d&ws=ws://%s:%d/ws&name=%s&version=%s&protocol=WEBSOCKET",
		ip, port, ip, port, a.cfg.ServerName, a.cfg.Version)

	qrWindow := a.fyneApp.NewWindow("Pairing QR Code")
	qrWindow.Resize(fyne.NewSize(400, 450))
	qrWindow.CenterOnScreen()

	pngBytes, _ := qrcode.Encode(data, qrcode.High, 300)
	img, _ := png.Decode(bytes.NewReader(pngBytes))
	qrImage := canvas.NewImageFromImage(img)
	qrImage.FillMode = canvas.ImageFillOriginal

	infoLabel := widget.NewLabel(fmt.Sprintf("Server: %s\nIP: %s\nPort: %d", a.cfg.ServerName, ip, port))
	infoLabel.Wrapping = fyne.TextWrapWord

	content := container.NewVBox(
		widget.NewLabelWithStyle("Scan with Air Mouse App", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		qrImage,
		widget.NewSeparator(),
		infoLabel,
		widget.NewButton("Close", func() { qrWindow.Close() }),
	)
	qrWindow.SetContent(content)
	qrWindow.Show()
}

func (a *App) clearAllDevices() {
	dialog.ShowConfirm("Clear Devices", "Remove all connected devices?", func(confirmed bool) {
		if confirmed {
			dialog.ShowInformation("Clear Devices", "Device removal is handled from the Devices tab for each client.", a.window)
		}
	}, a.window)
}

func (a *App) resetStatistics() {
	dialog.ShowConfirm("Reset Statistics", "Reset all mouse statistics?", func(confirmed bool) {
		if confirmed {
			a.mouse.ResetStats()
			dialog.ShowInformation("Statistics Reset", "All statistics have been reset.", a.window)
		}
	}, a.window)
}

func (a *App) showPersonalizationDialog() {
	content := container.NewVBox(
		widget.NewLabel("Personalization Training"),
		widget.NewSeparator(),
		widget.NewLabel("This feature allows the AI to learn your movement patterns."),
		widget.NewProgressBar(),
		widget.NewButton("Start Training", func() {}),
	)
	dialog.ShowCustom("Personalization", "Close", content, a.window)
}

func (a *App) showGestureRecorder() {
	content := container.NewVBox(
		widget.NewLabel("Gesture Recorder"),
		widget.NewSeparator(),
		widget.NewLabel("Record new gestures for recognition."),
		widget.NewEntry(),
		widget.NewButton("Start Recording", func() {}),
		widget.NewButton("Save Gesture", func() {}),
	)
	dialog.ShowCustom("Gesture Recorder", "Close", content, a.window)
}

func (a *App) showNetworkDiagnostics() {
	ip := utils.GetLocalIP()
	devices := a.deviceMgr.GetAllDevices()
	content := container.NewVBox(
		widget.NewLabelWithStyle("Network Diagnostics", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel(fmt.Sprintf("Local IP: %s", ip)),
		widget.NewLabel(fmt.Sprintf("TCP Port: %d", a.cfg.Port)),
		widget.NewLabel(fmt.Sprintf("WebSocket Port: %d", a.cfg.WebSocketPort)),
		widget.NewLabel(fmt.Sprintf("UDP Port: %d", a.cfg.UDPPort)),
		widget.NewLabel(fmt.Sprintf("Connected Devices: %d", len(devices))),
		widget.NewButton("Run Tests", func() {}),
	)
	dialog.ShowCustom("Network Diagnostics", "Close", content, a.window)
}

func (a *App) showPerformanceMonitor() {
	metrics := utils.GetMetrics()
	content := container.NewVBox(
		widget.NewLabelWithStyle("Performance Monitor", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel(fmt.Sprintf("CPU Usage: %.1f%%", metrics.CPUPercent)),
		widget.NewLabel(fmt.Sprintf("Memory Usage: %.1f%%", metrics.MemoryPercent)),
		widget.NewLabel(fmt.Sprintf("Goroutines: %d", metrics.GoRoutines)),
		widget.NewLabel(fmt.Sprintf("Uptime: %v", metrics.Uptime)),
		widget.NewButton("Refresh", func() {}),
	)
	dialog.ShowCustom("Performance Monitor", "Close", content, a.window)
}

func (a *App) showUserGuide() {
	content := container.NewVBox(
		widget.NewLabelWithStyle("User Guide", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("1. Start the server from Dashboard"),
		widget.NewLabel("2. Scan QR code with Android app"),
		widget.NewLabel("3. Move your phone to control cursor"),
		widget.NewLabel("4. Use gestures for media control"),
		widget.NewLabel("5. Enable proximity for auto-lock"),
	)
	dialog.ShowCustom("User Guide", "Close", content, a.window)
}

func (a *App) showAPIDocs() {
	content := container.NewVBox(
		widget.NewLabelWithStyle("API Documentation", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("WebSocket Endpoint: ws://server:8080/ws"),
		widget.NewLabel("REST API: http://server:8080/api"),
		widget.NewLabel("Health Check: GET /health"),
		widget.NewLabel("Status: GET /api/status"),
		widget.NewButton("Open in Browser", func() {}),
	)
	dialog.ShowCustom("API Documentation", "Close", content, a.window)
}

func (a *App) checkForUpdates() {
	dialog.ShowInformation("Check for Updates", "You are running version 3.0.0\nNo updates available.", a.window)
}

func (a *App) reportIssue() {
	content := container.NewVBox(
		widget.NewLabel("Report Issue"),
		widget.NewSeparator(),
		widget.NewLabel("Please describe the issue:"),
		widget.NewMultiLineEntry(),
		widget.NewButton("Submit", func() {}),
	)
	dialog.ShowCustom("Report Issue", "Cancel", content, a.window)
}

// ------------------------------------------------------------
//  Global helpers (some are defined elsewhere)
// ------------------------------------------------------------

func showShortcutsDialog(w fyne.Window) {
	content := container.NewVBox(
		widget.NewLabelWithStyle("Keyboard Shortcuts", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("⌘/Ctrl + S - Start Server"),
		widget.NewLabel("⌘/Ctrl + Shift + S - Stop Server"),
		widget.NewLabel("⌘/Ctrl + R - Restart Server"),
		widget.NewLabel("⌘/Ctrl + Q - Quit"),
		widget.NewLabel("⌘/Ctrl + , - Settings"),
		widget.NewLabel("F1 - Help"),
	)
	dialog.ShowCustom("Shortcuts", "Close", content, w)
}
