package ui

import (
	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/app"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"
	"github.com/getlantern/systray"

	"airmouse-go/internal/config"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/domain/service"
	"airmouse-go/internal/infra/mouse"
	"airmouse-go/internal/repository"
)

type App struct {
	fyneApp    fyne.App
	window     fyne.Window
	cfg        *config.Config
	mouseCtrl  mouse.MouseController
	deviceMgr  *device.Manager
	mouseSvc   *service.MouseService
	connSvc    *service.ConnectionService

	dashboardTab fyne.CanvasObject
	devicesTab   fyne.CanvasObject
	networkTab   fyne.CanvasObject
	settingsTab  fyne.CanvasObject
	analyticsTab fyne.CanvasObject
	logsTab      fyne.CanvasObject
}

func NewApp(cfg *config.Config) *App {
	selectedTheme := getThemeByName(cfg.Theme)
	fyneApp := app.NewWithID("airmouse.pro")
	if selectedTheme != nil {
		fyneApp.Settings().SetTheme(selectedTheme)
	}
	// Create services
	mouseRepo := repository.NewMouseRepository()
	mouseSvc := service.NewMouseService(mouseRepo, cfg.Sensitivity)
	gestureRepo := repository.NewGestureRepository()
	gestureSvc, _ := service.NewGestureService(gestureRepo)
	clientRepo := repository.NewClientRepository()
	connSvc := service.NewConnectionService(clientRepo, cfg.MaxClients)
	mouseCtrl := mouse.New()

	return &App{
		fyneApp:   fyneApp,
		cfg:       cfg,
		mouseCtrl: mouseCtrl,
		mouseSvc:  mouseSvc,
		connSvc:   connSvc,
		deviceMgr: device.NewManager(), // adjust as needed
	}
}

func (a *App) Run() error {
	a.window = a.fyneApp.NewWindow("Air Mouse Pro Server")
	a.window.Resize(fyne.NewSize(1200, 800))

	a.dashboardTab = NewDashboardTab(a.cfg, a.mouseSvc, a.deviceMgr)
	a.devicesTab = NewDevicesTab(a.deviceMgr)
	a.networkTab = NewNetworkTab(a.cfg)
	a.settingsTab = NewSettingsTab(a.cfg, a.mouseSvc)
	a.analyticsTab = NewAnalyticsTab(nil) // collector can be added later
	a.logsTab = NewLogsTab()

	tabs := container.NewAppTabs(
		container.NewTabItemWithIcon("Dashboard", theme.HomeIcon(), a.dashboardTab),
		container.NewTabItemWithIcon("Devices", theme.ComputerIcon(), a.devicesTab),
		container.NewTabItemWithIcon("Network", theme.NetworkIcon(), a.networkTab),
		container.NewTabItemWithIcon("Settings", theme.SettingsIcon(), a.settingsTab),
		container.NewTabItemWithIcon("Analytics", theme.ContentAddIcon(), a.analyticsTab),
		container.NewTabItemWithIcon("Logs", theme.DocumentIcon(), a.logsTab),
	)
	tabs.SetTabLocation(container.TabLocationLeading)

	// Menu
	fileMenu := fyne.NewMenu("File",
		fyne.NewMenuItem("Quit", func() { a.fyneApp.Quit() }),
	)
	helpMenu := fyne.NewMenu("Help",
		fyne.NewMenuItem("About", func() { showAboutDialog(a.window) }),
	)
	a.window.SetMainMenu(fyne.NewMainMenu(fileMenu, helpMenu))

	if a.cfg.AlwaysOnTop {
		a.window.SetAlwaysOnTop(true)
	}

	a.window.SetContent(tabs)
	a.window.ShowAndRun()
	return nil
}

func (a *App) Stop() { a.fyneApp.Quit() }