package ui

import (
    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/app"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/theme"

    "airmouse-go/internal/config"
    "airmouse-go/internal/control"
    "airmouse-go/internal/device"
    "airmouse-go/internal/protocol"
)

type App struct {
    fyneApp   fyne.App
    window    fyne.Window
    cfg       *config.Config
    server    *protocol.ProtocolServer
    mouse     control.MouseController
    deviceMgr *device.Manager

    dashboardTab fyne.CanvasObject
    devicesTab   fyne.CanvasObject
    networkTab   fyne.CanvasObject
    settingsTab  fyne.CanvasObject
    logsTab      fyne.CanvasObject
}

func NewApp(cfg *config.Config, server *protocol.ProtocolServer, mouse control.MouseController, deviceMgr *device.Manager) *App {
    selectedTheme := getThemeByName(cfg.Theme)
    a := app.New()
    if selectedTheme != nil {
        a.Settings().SetTheme(selectedTheme)
    }
    return &App{
        fyneApp:   a,
        cfg:       cfg,
        server:    server,
        mouse:     mouse,
        deviceMgr: deviceMgr,
    }
}

func (a *App) Run() error {
    a.window = a.fyneApp.NewWindow("Air Mouse Pro Server")
    a.window.Resize(fyne.NewSize(1200, 800))
    a.window.CenterOnScreen()

    // Create tabs
    a.dashboardTab = NewDashboardTab(a.server, a.mouse, a.deviceMgr)
    a.devicesTab = NewDevicesTab(a.deviceMgr)
    a.networkTab = NewNetworkTab(a.cfg)
    a.settingsTab = NewSettingsTab(a.cfg, a.mouse)
    a.logsTab = NewLogsTab()

    tabs := container.NewAppTabs(
        container.NewTabItemWithIcon("Dashboard", theme.HomeIcon(), a.dashboardTab),
        container.NewTabItemWithIcon("Devices", theme.ComputerIcon(), a.devicesTab),
        container.NewTabItemWithIcon("Network", theme.ComputerIcon(), a.networkTab),
        container.NewTabItemWithIcon("Settings", theme.SettingsIcon(), a.settingsTab),
        container.NewTabItemWithIcon("Logs", theme.DocumentIcon(), a.logsTab),
    )
    tabs.SetTabLocation(container.TabLocationTop)

    // Menu bar
    fileMenu := fyne.NewMenu("File",
        fyne.NewMenuItem("Start Server", func() { a.server.Start() }),
        fyne.NewMenuItem("Stop Server", func() { a.server.Stop() }),
        fyne.NewMenuItemSeparator(),
        fyne.NewMenuItem("Quit", func() { a.fyneApp.Quit() }),
    )
    viewMenu := fyne.NewMenu("View",
        fyne.NewMenuItem("Refresh", func() { a.window.Content().Refresh() }),
    )
    helpMenu := fyne.NewMenu("Help",
        fyne.NewMenuItem("About", func() { showAboutDialog(a.window) }),
        fyne.NewMenuItem("Shortcuts", func() { showShortcutsDialog(a.window) }),
    )
    mainMenu := fyne.NewMainMenu(fileMenu, viewMenu, helpMenu)
    a.window.SetMainMenu(mainMenu)

    a.window.SetContent(tabs)
    a.window.ShowAndRun()
    return nil
}

func (a *App) Stop() { a.fyneApp.Quit() }

func showShortcutsDialog(w fyne.Window) {
    dialog.ShowInformation(
        "Shortcuts",
        "Ctrl+S – Start Server\nCtrl+Shift+S – Stop Server\nCtrl+Q – Quit\nCtrl+R – Refresh\nCtrl+Shift+L – Clear Logs\nCtrl+Shift+P – Open Pairing Wizard",
        w,
    )
}
