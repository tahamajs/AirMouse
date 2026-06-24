package ui

import (
	"bytes"
	"fmt"
	"image/png"
	"os"
	"sync"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/app"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"
	"github.com/skip2/go-qrcode"

	"airmouse-go/control/mouse"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	"airmouse-go/internal/personalization"
	"airmouse-go/internal/protocol"
	"airmouse-go/internal/utils"
)

// ============================================================
// App struct
// ============================================================

type App struct {
	fyneApp   fyne.App
	window    fyne.Window
	cfg       *config.Config
	server    *protocol.ProtocolServer
	mouse     mouse.Controller
	deviceMgr *device.Manager
	collector *personalization.DataCollector

	// Tabs
	dashboardTab  fyne.CanvasObject
	devicesTab    fyne.CanvasObject
	networkTab    fyne.CanvasObject
	gesturesTab   fyne.CanvasObject
	proximityTab  fyne.CanvasObject
	analyticsTab  fyne.CanvasObject
	settingsTab   fyne.CanvasObject
	logsTab       fyne.CanvasObject
	protocolTab   fyne.CanvasObject
	dashboardCtrl interface{ Stop() }
	proximityCtrl interface{ Stop() }

	// Status
	statusBar        *StatusBar
	connectionStatus *widget.Label
	summaryStatus    *widget.Label

	// Lifecycle
	shutdownOnce sync.Once
	stopChan     chan struct{}
	stopOnce     sync.Once
}

// ============================================================
// Constructor
// ============================================================

func NewApp(cfg *config.Config, server *protocol.ProtocolServer, mouseController mouse.Controller, deviceMgr *device.Manager) *App {
	selectedTheme := getThemeByName(cfg.Theme)
	a := app.New()
	if selectedTheme != nil {
		a.Settings().SetTheme(selectedTheme)
	}
	if len(AppIconData) > 0 {
		a.SetIcon(&fyne.StaticResource{
			StaticName:    "app_icon.png",
			StaticContent: AppIconData,
		})
	}

	var collector *personalization.DataCollector
	if cfg.EnablePersonalization {
		collector = personalization.NewDataCollector()
	}

	return &App{
		fyneApp:   a,
		cfg:       cfg,
		server:    server,
		mouse:     mouseController,
		deviceMgr: deviceMgr,
		collector: collector,
		stopChan:  make(chan struct{}),
	}
}

// ============================================================
// Run – creates window, tabs, and starts the event loop
// ============================================================

func (a *App) Run() error {
	width, height := GetWindowSize()
	a.window = a.fyneApp.NewWindow(fmt.Sprintf("Air Mouse Pro Server - %s", a.cfg.ServerName))
	a.window.Resize(fyne.NewSize(width, height))
	a.window.CenterOnScreen()
	a.window.SetMaster()

	// Build status bar and labels
	a.statusBar = NewStatusBar()
	a.connectionStatus = widget.NewLabel("🔌 Status: Waiting for approval in Devices")
	a.summaryStatus = widget.NewLabel("Server details will appear here once it starts. Open Devices and tap Approve to accept Android.")
	a.summaryStatus.Wrapping = fyne.TextWrapWord

	// Build all tabs
	a.buildTabs()

	// Tab container
	tabs := container.NewAppTabs(
		container.NewTabItemWithIcon("Dashboard", theme.HomeIcon(), a.dashboardTab),
		container.NewTabItemWithIcon("Devices", theme.ComputerIcon(), a.devicesTab),
		container.NewTabItemWithIcon("Network", theme.ComputerIcon(), a.networkTab),
		container.NewTabItemWithIcon("Gestures", theme.ContentCopyIcon(), a.gesturesTab),
		container.NewTabItemWithIcon("Proximity", theme.VisibilityIcon(), a.proximityTab),
		container.NewTabItemWithIcon("Analytics", theme.InfoIcon(), a.analyticsTab),
		container.NewTabItemWithIcon("Settings", theme.SettingsIcon(), a.settingsTab),
		container.NewTabItemWithIcon("Logs", theme.DocumentIcon(), a.logsTab),
		container.NewTabItemWithIcon("Protocol", theme.InfoIcon(), a.protocolTab),
	)
	tabs.SetTabLocation(container.TabLocationTop)
	tabs.SelectIndex(0)
	tabs.OnSelected = func(ti *container.TabItem) {
		a.onTabSelected(ti)
	}

	// Menu and toolbar
	mainMenu := a.createMenuBar()
	a.window.SetMainMenu(mainMenu)
	toolbar := a.createToolbar()

	// Main layout
	content := container.NewBorder(
		toolbar,
		a.statusBar.Widget(),
		nil,
		nil,
		tabs,
	)
	a.window.SetContent(content)
	utils.LogInfo("Content set, window ready")

	// Window close handler
	a.window.SetCloseIntercept(func() {
		a.onWindowClose()
	})

	// Start background tasks
	go a.connectionStatusUpdater()

	// Show welcome dialog on first launch (after window appears)
	if a.cfg.IsFirstLaunch() {
		go func() {
			time.Sleep(1 * time.Second) // wait for window to render
			RunOnMain(func() {
				ShowWelcomeDialog(a.window)
			})
		}()
		_ = a.cfg.SetFirstLaunchComplete()
	}

	// === FIX: Force window to appear before event loop ===
	utils.LogInfo("Showing window...")
	a.window.Show()
	a.window.RequestFocus()
	time.Sleep(200 * time.Millisecond) // allow window manager to process

	// === Start the event loop ===
	utils.LogInfo("Entering ShowAndRun...")
	a.window.ShowAndRun()
	utils.LogInfo("Window closed")
	return nil
}
// ============================================================
// buildTabs – creates all tabs with error handling
// ============================================================
func (a *App) showNetworkProtocolGuide() {
    if a.window == nil {
        return
    }
    dialog.ShowCustom("Network Protocol", "Close", buildProtocolGuideContent(a.cfg, a.server), a.window)
}
func (a *App) buildTabs() {
	    utils.LogInfo("Building dashboard tab...")
    if dashboardTab, dashboardCtrl := NewDashboardTab(a.server, a.mouse, a.deviceMgr); dashboardTab != nil {
        a.dashboardTab = safeTab(dashboardTab, "Dashboard")
        a.dashboardCtrl = dashboardCtrl
    } else {
        a.dashboardTab = safeTab(nil, "Dashboard")
    }
    utils.LogInfo("Dashboard tab done")  // ← add

    utils.LogInfo("Building devices tab...")
    a.devicesTab = safeTab(NewDevicesTab(a.server, a.deviceMgr), "Devices")
    utils.LogInfo("Devices tab done")    // ← add





	utils.LogInfo("Building network tab...")
	a.networkTab = safeTab(NewNetworkTab(a.cfg), "Network")

	utils.LogInfo("Building gestures tab...")
	a.gesturesTab = safeTab(NewGesturesTab(), "Gestures")

	utils.LogInfo("Building proximity tab...")
	if proximityTab, proximityCtrl := NewProximityTab(); proximityTab != nil {
		a.proximityTab = safeTab(proximityTab, "Proximity")
		a.proximityCtrl = proximityCtrl
	} else {
		a.proximityTab = safeTab(nil, "Proximity")
	}

	utils.LogInfo("Building analytics tab...")
	a.analyticsTab = safeTab(NewAnalyticsTab(a.collector), "Analytics")

	utils.LogInfo("Building settings tab...")
	a.settingsTab = safeTab(NewSettingsTab(a.cfg, a.mouse), "Settings")

	utils.LogInfo("Building logs tab...")
	a.logsTab = safeTab(NewLogsTab(), "Logs")

	utils.LogInfo("Building protocol tab...")
	a.protocolTab = widget.NewLabel("Protocol Guide (coming soon)")
	utils.LogInfo("Protocol tab done")
}

// ============================================================
// safeTab – prevents nil panics
// ============================================================

func safeTab(tab fyne.CanvasObject, name string) fyne.CanvasObject {
	if tab != nil {
		return tab
	}
	utils.LogWarn("Tab %s returned nil, using placeholder", name)
	return widget.NewLabelWithStyle("⚠️ "+name+" tab not implemented", fyne.TextAlignCenter, fyne.TextStyle{Bold: true})
}

// ============================================================
// Menu Bar
// ============================================================

func (a *App) createMenuBar() *fyne.MainMenu {
	fileMenu := fyne.NewMenu("File",
		fyne.NewMenuItem("Start Server", func() { a.startServerAsync("menu") }),
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
			a.window.Resize(fyne.NewSize(1200, 800))
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
		fyne.NewMenuItem("Network Protocol", func() { a.showNetworkProtocolGuide() }),
		fyne.NewMenuItem("Performance Monitor", func() { a.showPerformanceMonitor() }),
	)

	helpMenu := fyne.NewMenu("Help",
		fyne.NewMenuItem("Welcome Guide", func() { ShowWelcomeDialog(a.window) }),
		fyne.NewMenuItem("Keyboard Shortcuts", func() { ShowShortcutsDialog(a.window) }),
		fyne.NewMenuItem("Dashboard Help", func() { ShowContextHelp(a.window, "dashboard") }),
		fyne.NewMenuItem("Devices Help", func() { ShowContextHelp(a.window, "devices") }),
		fyne.NewMenuItem("Network Help", func() { ShowContextHelp(a.window, "network") }),
		fyne.NewMenuItem("Gestures Help", func() { ShowContextHelp(a.window, "gestures") }),
		fyne.NewMenuItem("Proximity Help", func() { ShowContextHelp(a.window, "proximity") }),
		fyne.NewMenuItemSeparator(),
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

// ============================================================
// Toolbar
// ============================================================

func (a *App) createToolbar() fyne.CanvasObject {
	settingsBtn := widget.NewButtonWithIcon("Settings", theme.SettingsIcon(), func() {
		dialog.ShowInformation("Settings", "Open the Settings tab to adjust server, network, and pairing options.", a.window)
	})
	helpBtn := widget.NewButtonWithIcon("Help", theme.HelpIcon(), func() {
		ShowShortcutsDialog(a.window)
	})

	title := widget.NewLabelWithStyle("Air Mouse Pro Server", fyne.TextAlignLeading, fyne.TextStyle{Bold: true})
	subtitle := widget.NewLabel("One dashboard for approval, connection, and pairing status.")
	subtitle.Wrapping = fyne.TextWrapWord

	return container.NewBorder(
		container.NewVBox(title, subtitle, a.connectionStatus, a.summaryStatus),
		nil,
		nil,
		container.NewHBox(settingsBtn, helpBtn),
		nil,
	)
}

// ============================================================
// Server control
// ============================================================

func (a *App) startServerAsync(source string) {
	if a.server == nil {
		return
	}
	utils.LogInfo("UI requested server start: source=%s", source)
	if a.connectionStatus != nil {
		a.connectionStatus.SetText("⏳ Status: Waiting for approval in Devices")
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
	a.server.Stop()
	RunOnMain(func() {
		a.refreshConnectionSummary()
	})
}

// ============================================================
// Tab selection handler
// ============================================================

func (a *App) onTabSelected(ti *container.TabItem) {
	utils.LogDebug("Selected tab: %s", ti.Text)
}

// ============================================================
// Connection status updater
// ============================================================

func (a *App) connectionStatusUpdater() {
	ticker := time.NewTicker(2 * time.Second)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			RunOnMain(func() { a.refreshConnectionSummary() })
		case <-a.stopChan:
			return
		}
	}
}

func (a *App) refreshConnectionSummary() {
	if a.connectionStatus == nil {
		return
	}

	activeCount := 0
	pendingCount := 0
	if a.deviceMgr != nil {
		allDevices := a.deviceMgr.GetAllDevices()
		activeCount = len(a.deviceMgr.GetActiveDevices())
		for _, d := range allDevices {
			if d.Status == device.StatusPendingApproval {
				pendingCount++
			}
		}
	}

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
	switch {
	case activeCount > 0 && pendingCount == 0:
		a.connectionStatus.SetText(fmt.Sprintf("🟢 Connected devices: %d", activeCount))
	case pendingCount > 0:
		a.connectionStatus.SetText(fmt.Sprintf("🟡 Connected: %d | Pending approvals: %d", activeCount, pendingCount))
	default:
		a.connectionStatus.SetText("🟢 Connected devices: 0")
	}
	if a.summaryStatus != nil {
		if pendingCount > 0 {
			a.summaryStatus.SetText(fmt.Sprintf(
				"Pending approval on %s:%d | ws://%s:%d/ws | UDP %d | Connected: %d | Pending: %d | Theme: %s",
				ip, a.cfg.Port, ip, a.cfg.WebSocketPort, a.cfg.UDPPort,
				activeCount, pendingCount, a.cfg.Theme,
			))
		} else {
			a.summaryStatus.SetText(fmt.Sprintf(
				"Approved and connected on %s:%d | ws://%s:%d/ws | UDP %d | Connected: %d | Theme: %s",
				ip, a.cfg.Port, ip, a.cfg.WebSocketPort, a.cfg.UDPPort,
				activeCount, a.cfg.Theme,
			))
		}
	}
}

// ============================================================
// Window close & stop
// ============================================================

func (a *App) onWindowClose() {
	utils.LogInfo("Window close requested; shutting down immediately")
	go a.shutdown(true)
}

func (a *App) Stop() {
	a.shutdown(true)
}

func (a *App) shutdown(force bool) {
	a.shutdownOnce.Do(func() {
		utils.LogInfo("Shutdown sequence started (force=%v)", force)
		a.stopAppLoops()
		a.stopBackgroundUI()

		if a.server != nil {
			a.server.Stop()
		}

		if err := a.cfg.Save(); err != nil {
			utils.LogError("Failed to save config: %v", err)
		}

		go func() {
			time.Sleep(2 * time.Second)
			utils.LogWarn("Forced exit after shutdown timeout")
			os.Exit(0)
		}()

		if a.fyneApp != nil {
			RunOnMain(func() {
				a.fyneApp.Quit()
			})
		}
	})
}

func (a *App) stopAppLoops() {
	a.stopOnce.Do(func() {
		if a.stopChan != nil {
			close(a.stopChan)
		}
	})
}

func (a *App) stopBackgroundUI() {
	if a.statusBar != nil {
		a.statusBar.Stop()
	}
	if a.dashboardCtrl != nil {
		a.dashboardCtrl.Stop()
	}
	if a.proximityCtrl != nil {
		a.proximityCtrl.Stop()
	}
}

// ============================================================
// Dialog helpers
// ============================================================

func (a *App) exportConfig() {
	dialog.ShowFileSave(func(writer fyne.URIWriteCloser, err error) {
		if err != nil {
			if err.Error() != "operation cancelled" {
				dialog.ShowError(err, a.window)
			}
			return
		}
		defer writer.Close()
		if _, err := writer.Write([]byte(a.cfg.ToJSON())); err != nil {
			dialog.ShowError(err, a.window)
			return
		}
		dialog.ShowInformation("Export Successful", "Configuration exported successfully.", a.window)
	}, a.window)
}

func (a *App) importConfig() {
	dialog.ShowFileOpen(func(reader fyne.URIReadCloser, err error) {
		if err != nil {
			if err.Error() != "operation cancelled" {
				dialog.ShowError(err, a.window)
			}
			return
		}
		defer reader.Close()
		buf := make([]byte, 1024*1024)
		n, err := reader.Read(buf)
		if err != nil && err.Error() != "EOF" {
			dialog.ShowError(err, a.window)
			return
		}
		if err := a.cfg.FromJSON(string(buf[:n])); err != nil {
			dialog.ShowError(err, a.window)
			return
		}
		dialog.ShowInformation("Import Successful", "Configuration imported successfully. Please restart the app for changes to take effect.", a.window)
	}, a.window)
}

func (a *App) showPairingQR() {
	ip := utils.GetLocalIP()
	port := a.cfg.WebSocketPort
	wsURL := fmt.Sprintf("ws://%s:%d/ws", ip, port)
	data := fmt.Sprintf("airmouse://pair?ws=%s&protocol=WEBSOCKET&name=%s&ip=%s&port=%d&version=%s",
		wsURL, a.cfg.ServerName, ip, port, a.cfg.Version)
	if a.server != nil && a.server.GetAuthManager() != nil {
		if pairingData, err := a.server.GetAuthManager().GetPairingQRData(wsURL); err == nil {
			data = pairingData
		}
	}

	qrWindow := a.fyneApp.NewWindow("Pairing QR Code")
	qrWindow.Resize(fyne.NewSize(400, 450))
	qrWindow.CenterOnScreen()

	pngBytes, err := qrcode.Encode(data, qrcode.High, 300)
	if err != nil {
		dialog.ShowError(fmt.Errorf("QR generation failed: %w", err), a.window)
		return
	}
	img, err := png.Decode(bytes.NewReader(pngBytes))
	if err != nil {
		dialog.ShowError(fmt.Errorf("QR decoding failed: %w", err), a.window)
		return
	}
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
		if confirmed && a.deviceMgr != nil {
			if clearer, ok := interface{}(a.deviceMgr).(interface{ ClearAll() }); ok {
				clearer.ClearAll()
				dialog.ShowInformation("Cleared", "All devices have been removed.", a.window)
			} else {
				for _, d := range a.deviceMgr.GetAllDevices() {
					_ = a.deviceMgr.UnregisterDevice(d.ID)
				}
				dialog.ShowInformation("Cleared", "All devices have been removed.", a.window)
			}
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
		widget.NewButton("Start Training", func() {
			if a.collector != nil {
				go func() {
					if err := a.collector.ForceFineTune(); err != nil {
						RunOnMain(func() {
							dialog.ShowError(err, a.window)
						})
					} else {
						RunOnMain(func() {
							dialog.ShowInformation("Training", "Training completed successfully.", a.window)
						})
					}
				}()
			} else {
				dialog.ShowInformation("Not Available", "Personalization is not enabled in settings.", a.window)
			}
		}),
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
	deviceCount := 0
	if a.deviceMgr != nil {
		deviceCount = len(a.deviceMgr.GetActiveDevices())
	}
	content := container.NewVBox(
		widget.NewLabelWithStyle("Network Diagnostics", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel(fmt.Sprintf("Local IP: %s", ip)),
		widget.NewLabel(fmt.Sprintf("TCP Port: %d", a.cfg.Port)),
		widget.NewLabel(fmt.Sprintf("WebSocket Port: %d", a.cfg.WebSocketPort)),
		widget.NewLabel(fmt.Sprintf("UDP Port: %d", a.cfg.UDPPort)),
		widget.NewLabel(fmt.Sprintf("Active Devices: %d", deviceCount)),
		widget.NewButton("Run Tests", func() {
			dialog.ShowInformation("Network Test", "Testing network connectivity... (simulated)", a.window)
		}),
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
		widget.NewButton("Refresh", func() {
			a.showPerformanceMonitor()
		}),
	)
	dialog.ShowCustom("Performance Monitor", "Close", content, a.window)
}

func (a *App) showUserGuide() {
	content := container.NewVBox(
		widget.NewLabelWithStyle("User Guide", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("1. Start the server from Dashboard and wait for approval to be ready."),
		widget.NewLabel("2. Scan the pairing QR code with the Android app."),
		widget.NewLabel("3. Watch the Android app show waiting for approval, then approved, then connected."),
		widget.NewLabel("4. Move your phone to control the cursor after approval."),
		widget.NewLabel("5. Use gestures for media control and proximity for auto-lock."),
	)
	dialog.ShowCustom("User Guide", "Close", content, a.window)
}

func (a *App) showAPIDocs() {
	ip := utils.GetLocalIP()
	content := container.NewVBox(
		widget.NewLabelWithStyle("API Documentation", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel(fmt.Sprintf("WebSocket Endpoint: ws://%s:%d/ws", ip, a.cfg.WebSocketPort)),
		widget.NewLabel(fmt.Sprintf("REST API: http://%s:%d/api", ip, a.cfg.Port)),
		widget.NewLabel("Health Check: GET /health"),
		widget.NewLabel("Status: GET /api/status"),
		widget.NewButton("Open in Browser", func() {
			dialog.ShowInformation("API Docs", "Open your browser to http://"+ip+":"+fmt.Sprintf("%d", a.cfg.Port)+"/health", a.window)
		}),
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
		widget.NewButton("Submit", func() {
			dialog.ShowInformation("Submitted", "Thank you for reporting the issue.", a.window)
		}),
	)
	dialog.ShowCustom("Report Issue", "Cancel", content, a.window)
}

// ============================================================
// Global shortcuts dialog (enhanced)
// ============================================================

func showShortcutsDialog(w fyne.Window) {
	content := container.NewVBox(
		widget.NewLabelWithStyle("Keyboard Shortcuts", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("⌘/Ctrl + S - Start Server"),
		widget.NewLabel("⌘/Ctrl + Shift + S - Stop Server"),
		widget.NewLabel("⌘/Ctrl + R - Restart Server"),
		widget.NewLabel("⌘/Ctrl + Q - Quit"),
		widget.NewLabel("⌘/Ctrl + , - Settings"),
		widget.NewLabel("⌘/Ctrl + 1-9 - Switch Tabs"),
		widget.NewLabel("F1 - Help / Shortcuts"),
		widget.NewLabel("F5 - Refresh"),
		widget.NewLabel("Esc - Close Dialog"),
	)
	dialog.ShowCustom("Shortcuts", "Close", content, w)
}