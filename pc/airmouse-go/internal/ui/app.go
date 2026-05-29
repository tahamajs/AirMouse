package ui

import (
	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/app"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/config"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/protocol"
)

type App struct {
	fyneApp    fyne.App
	window     fyne.Window
	cfg        *config.Config
	server     *protocol.ProtocolServer
	mouse      control.MouseController
	deviceMgr  *device.Manager

	// Tabs
	dashboardTab fyne.CanvasObject
	devicesTab   fyne.CanvasObject
	networkTab   fyne.CanvasObject
	settingsTab  fyne.CanvasObject
	logsTab      fyne.CanvasObject

	// System tray
	trayIcon *widget.Icon
}

func NewApp(cfg *config.Config, server *protocol.ProtocolServer, mouse control.MouseController, deviceMgr *device.Manager) *App {
	// Apply theme based on config
	selectedTheme := getThemeByName(cfg.Theme)
	if selectedTheme != nil {
		app := app.NewWithID("airmouse.pro")
		app.Settings().SetTheme(selectedTheme)
		return &App{
			fyneApp:   app,
			cfg:       cfg,
			server:    server,
			mouse:     mouse,
			deviceMgr: deviceMgr,
		}
	}
	// Fallback
	return &App{
		fyneApp:   app.New(),
		cfg:       cfg,
		server:    server,
		mouse:     mouse,
		deviceMgr: deviceMgr,
	}
}

func (a *App) Run() error {
	a.window = a.fyneApp.NewWindow("Air Mouse Pro Server")
	a.window.SetMaster()
	a.window.Resize(fyne.NewSize(1100, 720))

	// Create tabs
	a.dashboardTab = NewDashboardTab(a.server, a.mouse, a.deviceMgr)
	a.devicesTab = NewDevicesTab(a.deviceMgr)
	a.networkTab = NewNetworkTab(a.cfg)
	a.settingsTab = NewSettingsTab(a.cfg, a.mouse)
	a.logsTab = NewLogsTab()

	tabs := container.NewAppTabs(
		container.NewTabItemWithIcon("Dashboard", theme.HomeIcon(), a.dashboardTab),
		container.NewTabItemWithIcon("Devices", theme.ComputerIcon(), a.devicesTab),
		container.NewTabItemWithIcon("Network", theme.NetworkIcon(), a.networkTab),
		container.NewTabItemWithIcon("Settings", theme.SettingsIcon(), a.settingsTab),
		container.NewTabItemWithIcon("Logs", theme.DocumentIcon(), a.logsTab),
	)
	tabs.SetTabLocation(container.TabLocationLeading)

	// Menu bar
	fileMenu := fyne.NewMenu("File",
		fyne.NewMenuItem("Start Server", func() { a.server.Start() }),
		fyne.NewMenuItem("Stop Server", func() { a.server.Stop() }),
		fyne.NewMenuItemSeparator(),
		fyne.NewMenuItem("Quit", func() { a.fyneApp.Quit() }),
	)
	viewMenu := fyne.NewMenu("View",
		fyne.NewMenuItem("Refresh", func() {
			a.devicesTab.Refresh()
			a.dashboardTab.Refresh()
		}),
		fyne.NewMenuItem("Clear Logs", func() {
			a.logsTab.Clear()
		}),
	)
	helpMenu := fyne.NewMenu("Help",
		fyne.NewMenuItem("About", func() { showAboutDialog(a.window) }),
	)

	mainMenu := fyne.NewMainMenu(fileMenu, viewMenu, helpMenu)
	a.window.SetMainMenu(mainMenu)

	// Set always on top if configured
	if a.cfg.AlwaysOnTop {
		a.window.SetAlwaysOnTop(true)
	}

	a.window.SetContent(tabs)
	a.window.ShowAndRun()
	return nil
}

func (a *App) Stop() {
	a.fyneApp.Quit()
}

func showAboutDialog(w fyne.Window) {
	dialog := widget.NewModalPopUp(
		container.NewVBox(
			widget.NewLabelWithStyle("Air Mouse Pro Server", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
			widget.NewLabel("Version 2.0.0"),
			widget.NewLabel("University of Tehran – Embedded Systems Lab"),
			widget.NewSeparator(),
			widget.NewLabel("Multi-protocol mouse server\nTCP | WebSocket | UDP | Bluetooth"),
			widget.NewButton("OK", func() { dialog.Hide() }),
		),
		w.Canvas(),
	)
	dialog.Resize(fyne.NewSize(400, 300))
	dialog.Show()
}