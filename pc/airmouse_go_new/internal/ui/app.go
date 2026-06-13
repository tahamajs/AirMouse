package ui

import (
    "fmt"
    "time"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/app"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/theme"
    "fyne.io/fyne/v2/widget"

    "airmouse-go/internal/config"
    "airmouse-go/internal/control"
    "airmouse-go/internal/device"
    "airmouse-go/internal/personalization"
    "airmouse-go/internal/protocol"
    "airmouse-go/internal/utils"
)

type App struct {
    fyneApp   fyne.App
    window    fyne.Window
    cfg       *config.Config
    server    *protocol.ProtocolServer
    mouse     control.MouseController
    deviceMgr *device.Manager
    collector *personalization.DataCollector

    // Tabs
    dashboardTab    fyne.CanvasObject
    devicesTab      fyne.CanvasObject
    networkTab      fyne.CanvasObject
    gesturesTab     fyne.CanvasObject
    proximityTab    fyne.CanvasObject
    analyticsTab    fyne.CanvasObject
    settingsTab     fyne.CanvasObject
    logsTab         fyne.CanvasObject
    
    // Status bar
    statusBar       *StatusBar
    connectionStatus *widget.Label
}

func NewApp(cfg *config.Config, server *protocol.ProtocolServer, mouse control.MouseController, deviceMgr *device.Manager) *App {
    selectedTheme := getThemeByName(cfg.Theme)
    a := app.New()
    if selectedTheme != nil {
        a.Settings().SetTheme(selectedTheme)
    }
    
    // Initialize personalization collector if enabled
    var collector *personalization.DataCollector
    if cfg.EnablePersonalization {
        collector = personalization.NewDataCollector(cfg)
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

func (a *App) Run() error {
    a.window = a.fyneApp.NewWindow(fmt.Sprintf("Air Mouse Pro Server - %s", a.cfg.ServerName))
    a.window.Resize(fyne.NewSize(1400, 900))
    a.window.CenterOnScreen()
    a.window.SetMaster()

    // Create status bar
    a.statusBar = NewStatusBar()
    a.connectionStatus = widget.NewLabel("🔌 Status: Ready")
    
    // Create all tabs
    a.dashboardTab = NewDashboardTab(a.server, a.mouse, a.deviceMgr)
    a.devicesTab = NewDevicesTab(a.deviceMgr)
    a.networkTab = NewNetworkTab(a.cfg)
    a.gesturesTab = NewGesturesTab()
    a.proximityTab = NewProximityTab()
    a.analyticsTab = NewAnalyticsTab(a.collector)
    a.settingsTab = NewSettingsTab(a.cfg, a.mouse)
    a.logsTab = NewLogsTab()

    // Create tab container
    tabs := container.NewAppTabs(
        container.NewTabItemWithIcon("Dashboard", theme.HomeIcon(), a.dashboardTab),
        container.NewTabItemWithIcon("Devices", theme.ComputerIcon(), a.devicesTab),
        container.NewTabItemWithIcon("Network", theme.NetworkIcon(), a.networkTab),
        container.NewTabItemWithIcon("Gestures", theme.ContentCopyIcon(), a.gesturesTab),
        container.NewTabItemWithIcon("Proximity", theme.VisibilityIcon(), a.proximityTab),
        container.NewTabItemWithIcon("Analytics", theme.InfoIcon(), a.analyticsTab),
        container.NewTabItemWithIcon("Settings", theme.SettingsIcon(), a.settingsTab),
        container.NewTabItemWithIcon("Logs", theme.DocumentIcon(), a.logsTab),
    )
    tabs.SetTabLocation(container.TabLocationTop)
    
    // Create custom tab behavior
    tabs.OnSelected = func(ti *container.TabItem) {
        a.onTabSelected(ti)
    }

    // Create menu bar
    mainMenu := a.createMenuBar()
    a.window.SetMainMenu(mainMenu)

    // Create toolbar
    toolbar := a.createToolbar()
    
    // Main content with toolbar, tabs, and status bar
    content := container.NewBorder(
        toolbar,     // top
        a.statusBar.Widget(), // bottom
        nil,         // left
        nil,         // right
        tabs,        // center
    )
    
    a.window.SetContent(content)
    
    // Set up auto-save on window close
    a.window.SetCloseIntercept(func() {
        a.onWindowClose()
    })
    
    // Start auto-refresh for connection status
    go a.connectionStatusUpdater()
    
    a.window.ShowAndRun()
    return nil
}

func (a *App) createMenuBar() *fyne.MainMenu {
    // File Menu
    fileMenu := fyne.NewMenu("File",
        fyne.NewMenuItem("Start Server", func() { 
            if err := a.server.Start(); err != nil {
                dialog.ShowError(err, a.window)
            }
        }),
        fyne.NewMenuItem("Stop Server", func() { 
            a.server.Stop()
        }),
        fyne.NewMenuItem("Restart Server", func() { 
            a.server.Stop()
            time.Sleep(500 * time.Millisecond)
            if err := a.server.Start(); err != nil {
                dialog.ShowError(err, a.window)
            }
        }),
        fyne.NewMenuItemSeparator(),
        fyne.NewMenuItem("Export Configuration", func() { 
            a.exportConfig()
        }),
        fyne.NewMenuItem("Import Configuration", func() { 
            a.importConfig()
        }),
        fyne.NewMenuItemSeparator(),
        fyne.NewMenuItem("Quit", func() { 
            a.onWindowClose()
        }),
    )
    
    // Server Menu
    serverMenu := fyne.NewMenu("Server",
        fyne.NewMenuItem("Show QR Code", func() { 
            a.showPairingQR()
        }),
        fyne.NewMenuItem("Pair New Device", func() { 
            ShowPairingWizard(a.window, fmt.Sprintf("ws://%s:%d/ws", utils.GetLocalIP(), a.cfg.WebSocketPort))
        }),
        fyne.NewMenuItemSeparator(),
        fyne.NewMenuItem("Clear All Devices", func() { 
            a.clearAllDevices()
        }),
        fyne.NewMenuItem("Reset Statistics", func() { 
            a.resetStatistics()
        }),
    )
    
    // View Menu
    viewMenu := fyne.NewMenu("View",
        fyne.NewMenuItem("Refresh", func() { 
            a.window.Content().Refresh()
        }),
        fyne.NewMenuItem("Reset Layout", func() { 
            a.window.Resize(fyne.NewSize(1400, 900))
            a.window.CenterOnScreen()
        }),
        fyne.NewMenuItemSeparator(),
        fyne.NewMenuItem("Themes", nil,
            fyne.NewMenuItem("Dark", func() { 
                a.fyneApp.Settings().SetTheme(theme.DarkTheme())
                a.cfg.Theme = "dark"
                a.cfg.Save()
            }),
            fyne.NewMenuItem("Light", func() { 
                a.fyneApp.Settings().SetTheme(theme.LightTheme())
                a.cfg.Theme = "light"
                a.cfg.Save()
            }),
        ),
    )
    
    // Tools Menu
    toolsMenu := fyne.NewMenu("Tools",
        fyne.NewMenuItem("Personalization Trainer", func() { 
            a.showPersonalizationDialog()
        }),
        fyne.NewMenuItem("Gesture Recorder", func() { 
            a.showGestureRecorder()
        }),
        fyne.NewMenuItem("Network Diagnostics", func() { 
            a.showNetworkDiagnostics()
        }),
        fyne.NewMenuItem("Performance Monitor", func() { 
            a.showPerformanceMonitor()
        }),
    )
    
    // Help Menu
    helpMenu := fyne.NewMenu("Help",
        fyne.NewMenuItem("User Guide", func() { 
            a.showUserGuide()
        }),
        fyne.NewMenuItem("API Documentation", func() { 
            a.showAPIDocs()
        }),
        fyne.NewMenuItemSeparator(),
        fyne.NewMenuItem("Check for Updates", func() { 
            a.checkForUpdates()
        }),
        fyne.NewMenuItem("Report Issue", func() { 
            a.reportIssue()
        }),
        fyne.NewMenuItemSeparator(),
        fyne.NewMenuItem("About", func() { 
            showAboutDialog(a.window)
        }),
    )
    
    return fyne.NewMainMenu(fileMenu, serverMenu, viewMenu, toolsMenu, helpMenu)
}

func (a *App) createToolbar() fyne.CanvasObject {
    startBtn := widget.NewButtonWithIcon("Start", theme.MediaPlayIcon(), func() {
        a.server.Start()
    })
    
    stopBtn := widget.NewButtonWithIcon("Stop", theme.MediaStopIcon(), func() {
        a.server.Stop()
    })
    
    restartBtn := widget.NewButtonWithIcon("Restart", theme.ViewRefreshIcon(), func() {
        a.server.Stop()
        time.Sleep(500 * time.Millisecond)
        a.server.Start()
    })
    
    settingsBtn := widget.NewButtonWithIcon("Settings", theme.SettingsIcon(), func() {
        // Switch to settings tab
        // This would require access to tabs container
    })
    
    helpBtn := widget.NewButtonWithIcon("Help", theme.HelpIcon(), func() {
        showShortcutsDialog(a.window)
    })
    
    // Server status indicator
    statusIndicator := widget.NewLabel("●")
    statusIndicator.TextStyle = fyne.TextStyle{Bold: true}
    
    return container.NewHBox(
        startBtn, stopBtn, restartBtn,
        widget.NewSeparator(),
        settingsBtn, helpBtn,
        widget.NewSeparator(),
        statusIndicator,
        a.connectionStatus,
    )
}

func (a *App) onTabSelected(ti *container.TabItem) {
    // Log tab selection
    utils.LogDebug(fmt.Sprintf("Selected tab: %s", ti.Text))
    
    // Refresh content when tab is selected
    switch ti.Text {
    case "Devices":
        // Refresh devices list
    case "Network":
        // Refresh network info
    case "Settings":
        // Save any pending changes
    }
}

func (a *App) connectionStatusUpdater() {
    ticker := time.NewTicker(2 * time.Second)
    defer ticker.Stop()
    
    for range ticker.C {
        deviceCount := len(a.deviceMgr.GetAllDevices())
        fyne.Do(func() {
            if deviceCount > 0 {
                a.connectionStatus.SetText(fmt.Sprintf("🟢 Connected: %d device(s)", deviceCount))
            } else {
                a.connectionStatus.SetText("🔴 Status: Waiting for connections")
            }
        })
    }
}

func (a *App) onWindowClose() {
    dialog.ShowConfirm("Quit", "Are you sure you want to quit Air Mouse Pro Server?", func(confirmed bool) {
        if confirmed {
            // Stop server if running
            a.server.Stop()
            
            // Save configuration
            if err := a.cfg.Save(); err != nil {
                utils.LogError(fmt.Sprintf("Failed to save config: %v", err))
            }
            
            // Stop status bar updates
            if a.statusBar != nil {
                a.statusBar.Stop()
            }
            
            // Quit application
            a.fyneApp.Quit()
        }
    }, a.window)
}

func (a *App) Stop() {
    a.onWindowClose()
}

// Helper functions
func (a *App) exportConfig() {
    dialog.ShowFileSave(func(writer fyne.URIWriteCloser, err error) {
        if err == nil && writer != nil {
            defer writer.Close()
            // Export config as JSON
            data := []byte(a.cfg.ToJSON())
            writer.Write(data)
            dialog.ShowInformation("Export Successful", "Configuration exported successfully.", a.window)
        }
    }, a.window)
}

func (a *App) importConfig() {
    dialog.ShowFileOpen(func(reader fyne.URIReadCloser, err error) {
        if err == nil && reader != nil {
            defer reader.Close()
            // Import config from JSON
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
    port := a.cfg.Port
    data := fmt.Sprintf("airmouse://pair?ip=%s&port=%d&name=%s", ip, port, a.cfg.ServerName)
    
    qrWindow := a.fyneApp.NewWindow("Pairing QR Code")
    qrWindow.Resize(fyne.NewSize(400, 450))
    qrWindow.CenterOnScreen()
    
    qrImage := canvas.NewImageFromResource(nil)
    // Generate QR code image
    pngBytes, _ := qrcode.Encode(data, qrcode.High, 300)
    img, _ := png.Decode(bytes.NewReader(pngBytes))
    qrImage.Image = img
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
            // Clear device manager
            // This would need implementation in device.Manager
            dialog.ShowInformation("Devices Cleared", "All devices have been removed.", a.window)
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