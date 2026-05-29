package ui

import (
	"bytes"
	"fmt"
	"image"
	"image/color"
	"net"
	"os"
	"strconv"
	"sync"
	"time"

	"airmouse-go/config"
	"airmouse-go/control"
	"airmouse-go/server"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"

	qrcode "github.com/skip2/go-qrcode"
	"github.com/getlantern/systray"
	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/mem"
)

var (
	appObj    fyne.App
	window    fyne.Window
	mouse     *control.MouseController
	tcpServer *server.TCPServer
	cfg       *config.Config
	mouse     MouseController   // was *control.MouseController

	// UI widgets (shared across tabs)
	statusPill *widget.Label
	statsLabel *widget.Label
	connLabel  *widget.Label
	qrImage    *canvas.Image
	logEntry   *widget.Entry
	connList   *widget.List
	connData   []string

	// Log filter state
	logLines  []logLine
	filterVar string
	showInfo  = true
	showWarn  = true
	showError = true

	// Performance
	perfRunning bool
	perfLabel   *widget.Label

	// Network fields (so we can read them from any tab)
	ipEntry   *widget.Entry
	portEntry *widget.Entry

	mu sync.Mutex
)

type logLine struct {
	Level   string
	Message string
}

// Log adds a message to the log (exported for server modules)
func Log(msg string) { LogLevel("info", msg) }

func LogLevel(level, msg string) {
	prefix := map[string]string{"info": "ℹ️", "warning": "⚠️", "error": "❌"}[level]
	mu.Lock()
	logLines = append(logLines, logLine{level, prefix + " " + msg})
	mu.Unlock()
	refreshFilteredLog()
}

// NewApp creates the Fyne application with a beautiful multi‑tab UI.
func NewApp(cfgFile *config.Config, mouseCtrl *control.MouseController) fyne.App {
	cfg = cfgFile
	mouse = mouseCtrl

	a := fyne.NewApp()
	appObj = a
	w := a.NewWindow("Air Mouse Pro Server")
	window = w

	// ---------- Header (shared) ----------
	statusPill = widget.NewLabelWithStyle("Server stopped", fyne.TextAlignCenter, fyne.TextStyle{Bold: true})
	statusPill.BackgroundColor = color.NRGBA{R: 239, G: 91, B: 91, A: 255}
	title := widget.NewLabelWithStyle("Air Mouse Pro Server", fyne.TextAlignCenter, fyne.TextStyle{Bold: true})
	header := container.NewVBox(
		container.NewCenter(title),
		container.NewCenter(statusPill),
	)

	// ---------- Tabs ----------
	dashboard := buildDashboardTab()
	networkTab := buildNetworkTab()
	clientsTab := buildClientsTab()
	settingsTab := buildSettingsTab()
	logsTab := buildLogsTab()

	tabs := container.NewAppTabs(
		container.NewTabItemWithIcon("Dashboard", theme.HomeIcon(), dashboard),
		container.NewTabItemWithIcon("Network", theme.ComputerIcon(), networkTab),
		container.NewTabItemWithIcon("Clients", theme.ListIcon(), clientsTab),
		container.NewTabItemWithIcon("Settings", theme.SettingsIcon(), settingsTab),
		container.NewTabItemWithIcon("Logs", theme.DocumentIcon(), logsTab),
	)
	tabs.SetTabLocation(container.TabLocationLeading)

	// ---------- Menu Bar ----------
	fileMenu := fyne.NewMenu("File",
		fyne.NewMenuItem("Start Server", func() { startServer() }),
		fyne.NewMenuItem("Stop Server", func() { stopServer() }),
		fyne.NewMenuItemSeparator(),
		fyne.NewMenuItem("Exit", func() { a.Quit() }),
	)
	viewMenu := fyne.NewMenu("View",
		fyne.NewMenuItem("Refresh IP List", func() { ipEntry.Text = getLocalIP() }),
		fyne.NewMenuItem("Clear Logs", func() {
			mu.Lock()
			logLines = nil
			mu.Unlock()
			logEntry.SetText("")
		}),
	)
	helpMenu := fyne.NewMenu("Help",
		fyne.NewMenuItem("Connection Wizard", func() {
			dialog.ShowInformation("Connection Wizard",
				"1. Connect PC and phone to same Wi‑Fi\n"+
					"2. Start server\n"+
					"3. On phone, scan QR or enter IP:port\n"+
					"4. Move phone to control mouse", w)
		}),
		fyne.NewMenuItem("About", func() {
			dialog.ShowInformation("About Air Mouse",
				"Air Mouse Pro Server\nUniversity of Tehran – Embedded Systems", w)
		}),
	)
	w.SetMainMenu(fyne.NewMainMenu(fileMenu, viewMenu, helpMenu))

	// ---------- Bottom Status Bar ----------
	perfLabel = widget.NewLabel("CPU: ---%  MEM: ---%")
	go updatePerformance()

	content := container.NewBorder(
		header,
		perfLabel,
		nil, nil,
		tabs,
	)

	w.SetContent(content)
	w.Resize(fyne.NewSize(1100, 720))

	// System tray
	go setupTray()

	return a
}

// ---------- Dashboard Tab ----------
func buildDashboardTab() fyne.CanvasObject {
	statsLabel = widget.NewLabel("Clicks: 0  |  Dbl: 0  |  Right: 0  |  Scroll: 0")
	connLabel = widget.NewLabel("Connections: 0")

	startBtn := widget.NewButtonWithIcon("Start Server", theme.MediaPlayIcon(), func() { startServer() })
	stopBtn := widget.NewButtonWithIcon("Stop Server", theme.MediaStopIcon(), func() { stopServer() })

	// Recent activity preview (mini log)
	miniLog := widget.NewLabel("No recent activity")
	go func() {
		for {
			time.Sleep(2 * time.Second)
			mu.Lock()
			if len(logLines) > 0 {
				last := logLines[len(logLines)-1].Message
				miniLog.SetText(last)
			}
			mu.Unlock()
		}
	}()

	dash := container.NewVBox(
		widget.NewLabelWithStyle("Server Dashboard", fyne.TextAlignCenter, fyne.TextStyle{Bold: true, Italic: true}),
		widget.NewSeparator(),
		container.NewHBox(startBtn, stopBtn),
		widget.NewSeparator(),
		statsLabel,
		connLabel,
		widget.NewSeparator(),
		widget.NewLabelWithStyle("Recent Activity", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		miniLog,
	)
	return container.NewScroll(dash)
}

// ---------- Network Tab ----------
func buildNetworkTab() fyne.CanvasObject {
	ipEntry = widget.NewEntry()
	ipEntry.SetPlaceHolder("192.168.1.x")
	ipEntry.Text = getLocalIP()
	portEntry = widget.NewEntry()
	portEntry.SetPlaceHolder("8080")
	portEntry.Text = strconv.Itoa(cfg.Port)

	ipLabel := widget.NewLabel("Current endpoint: (none)")

	refreshBtn := widget.NewButton("Refresh", func() { ipEntry.Text = getLocalIP() })
	copyBtn := widget.NewButton("Copy Endpoint", func() {
		endpoint := fmt.Sprintf("airmouse://%s:%s", ipEntry.Text, portEntry.Text)
		window.Clipboard().SetContent(endpoint)
		Log("📋 Copied: " + endpoint)
	})

	qrImage = canvas.NewImageFromImage(nil)
	qrImage.FillMode = canvas.ImageFillOriginal

	genQrBtn := widget.NewButton("Generate QR", func() {
		data := fmt.Sprintf("airmouse://%s:%s", ipEntry.Text, portEntry.Text)
		pngBytes, err := qrcode.Encode(data, qrcode.High, 220)
		if err != nil {
			LogLevel("error", "QR generation failed: "+err.Error())
			return
		}
		img, _, err := image.Decode(bytes.NewReader(pngBytes))
		if err != nil {
			LogLevel("error", "QR decode failed: "+err.Error())
			return
		}
		qrImage.Image = img
		qrImage.Refresh()
	})
	saveQrBtn := widget.NewButton("Save QR", func() {
		dialog.ShowFileSave(func(file fyne.URIWriteCloser, err error) {
			if err != nil || file == nil {
				return
			}
			defer file.Close()
			data := fmt.Sprintf("airmouse://%s:%s", ipEntry.Text, portEntry.Text)
			pngBytes, _ := qrcode.Encode(data, qrcode.High, 220)
			file.Write(pngBytes)
		}, window)
	})

	manualIPEntry := widget.NewEntry()
	manualIPEntry.SetPlaceHolder("Manual IP address")
	manualIPCheck := widget.NewCheck("Use manual IP", nil)

	netTab := container.NewVBox(
		widget.NewLabelWithStyle("Network Endpoint", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		ipEntry, portEntry,
		container.NewHBox(refreshBtn, copyBtn),
		manualIPCheck, manualIPEntry,
		container.NewHBox(genQrBtn, saveQrBtn),
		qrImage,
		ipLabel,
	)
	return container.NewScroll(netTab)
}

// ---------- Clients Tab ----------
func buildClientsTab() fyne.CanvasObject {
	connData = []string{}
	connList = widget.NewList(
		func() int { return len(connData) },
		func() fyne.CanvasObject { return widget.NewLabel("") },
		func(id widget.ListItemID, obj fyne.CanvasObject) {
			obj.(*widget.Label).SetText(connData[id])
		},
	)
	disconnectBtn := widget.NewButton("Disconnect Selected", func() {
		idx := connList.Selected()
		if idx < 0 || idx >= len(connData) {
			return
		}
		addr := connData[idx]
		if tcpServer != nil {
			tcpServer.DisconnectByAddr(addr)
		}
	})
	return container.NewBorder(
		widget.NewLabelWithStyle("Connected Clients", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		disconnectBtn,
		nil, nil,
		connList,
	)
}

// ---------- Settings Tab ----------
func buildSettingsTab() fyne.CanvasObject {
	sensSlider := widget.NewSlider(0.2, 2.0)
	sensSlider.Value = cfg.Sensitivity
	sensLabel := widget.NewLabel(fmt.Sprintf("Sensitivity: %.2f", cfg.Sensitivity))
	sensSlider.OnChanged = func(v float64) {
		mouse.sensitivity = v
		cfg.Sensitivity = v
		sensLabel.SetText(fmt.Sprintf("Sensitivity: %.2f", v))
		config.Save("config.json", cfg)
	}

	alwaysOnTopCheck := widget.NewCheck("Always on Top", func(b bool) {
		cfg.AlwaysOnTop = b
		config.Save("config.json", cfg)
	})
	alwaysOnTopCheck.SetChecked(cfg.AlwaysOnTop)

	themeSelect := widget.NewSelect([]string{"Dark", "Light", "Pure Black", "High Contrast"}, func(s string) {
		Log("Theme changed to " + s)
		// In a real app, you would reconfigure the theme.
	})

	return container.NewVBox(
		widget.NewLabelWithStyle("Settings", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("Cursor Sensitivity"),
		sensSlider,
		sensLabel,
		widget.NewSeparator(),
		widget.NewLabel("Window Behaviour"),
		alwaysOnTopCheck,
		widget.NewSeparator(),
		widget.NewLabel("Appearance"),
		themeSelect,
	)
}

// ---------- Logs Tab ----------
func buildLogsTab() fyne.CanvasObject {
	logEntry = widget.NewMultiLineEntry()
	logEntry.Disable()
	logEntry.SetPlaceHolder("Log output...")

	searchEntry := widget.NewEntry()
	searchEntry.SetPlaceHolder("Filter...")
	searchEntry.OnChanged = func(s string) {
		filterVar = s
		refreshFilteredLog()
	}

	infoCheck := widget.NewCheck("Info", func(b bool) { showInfo = b; refreshFilteredLog() })
	infoCheck.SetChecked(true)
	warnCheck := widget.NewCheck("Warn", func(b bool) { showWarn = b; refreshFilteredLog() })
	warnCheck.SetChecked(true)
	errCheck := widget.NewCheck("Error", func(b bool) { showError = b; refreshFilteredLog() })
	errCheck.SetChecked(true)

	exportBtn := widget.NewButton("Export Log", func() {
		dialog.ShowFileSave(func(file fyne.URIWriteCloser, err error) {
			if err != nil || file == nil {
				return
			}
			defer file.Close()
			for _, l := range logLines {
				file.Write([]byte(l.Message + "\n"))
			}
		}, window)
	})

	filterToolbar := container.NewHBox(
		widget.NewLabel("Filter:"),
		searchEntry,
		infoCheck,
		warnCheck,
		errCheck,
		exportBtn,
	)

	return container.NewBorder(
		widget.NewLabelWithStyle("Live Log", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		filterToolbar,
		nil, nil,
		container.NewScroll(logEntry),
	)
}

// ---------- Server actions ----------
func startServer() {
	if tcpServer != nil {
		return
	}
	port, _ := strconv.Atoi(portEntry.Text)
	tcpServer = server.NewTCPServer("0.0.0.0", port, mouse, Log, updateStats, updateConnList)
	if err := tcpServer.Start(); err != nil {
		LogLevel("error", "TCP start error: "+err.Error())
		return
	}
	statusPill.BackgroundColor = color.NRGBA{G: 255, A: 255}
	statusPill.SetText("Server running")
}

func stopServer() {
	if tcpServer != nil {
		tcpServer.Stop()
		tcpServer = nil
	}
	statusPill.BackgroundColor = color.NRGBA{R: 255, A: 255}
	statusPill.SetText("Server stopped")
}

func updateStats(clicks, dbl, right, scroll int) {
	statsLabel.SetText(fmt.Sprintf("Clicks: %d  |  Dbl: %d  |  Right: %d  |  Scroll: %d", clicks, dbl, right, scroll))
}

func updateConnList(list []string) {
	connData = list
	connLabel.SetText(fmt.Sprintf("Connections: %d", len(list)))
	connList.Refresh()
}

func refreshFilteredLog() {
	mu.Lock()
	defer mu.Unlock()
	var buf bytes.Buffer
	for _, l := range logLines {
		if filterVar != "" && !contains(l.Message, filterVar) {
			continue
		}
		if l.Level == "info" && !showInfo {
			continue
		}
		if l.Level == "warning" && !showWarn {
			continue
		}
		if l.Level == "error" && !showError {
			continue
		}
		buf.WriteString(l.Message + "\n")
	}
	logEntry.SetText(buf.String())
}

func contains(s, substr string) bool {
	for i := 0; i <= len(s)-len(substr); i++ {
		if s[i:i+len(substr)] == substr {
			return true
		}
	}
	return false
}

func getLocalIP() string {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return "127.0.0.1"
	}
	for _, addr := range addrs {
		if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() && ipnet.IP.To4() != nil {
			return ipnet.IP.String()
		}
	}
	return "127.0.0.1"
}

func updatePerformance() {
	perfRunning = true
	for perfRunning {
		c, _ := cpu.Percent(1*time.Second, false)
		m, _ := mem.VirtualMemory()
		cpuPercent := 0.0
		if len(c) > 0 {
			cpuPercent = c[0]
		}
		perfLabel.SetText(fmt.Sprintf("CPU: %.0f%%  MEM: %.0f%%", cpuPercent, m.UsedPercent))
		time.Sleep(2 * time.Second)
	}
}

// ---------- System Tray ----------
func setupTray() {
	systray.Run(onReady, onExit)
}

func onReady() {
	systray.SetTitle("Air Mouse")
	systray.SetTooltip("Air Mouse Server")
	mShow := systray.AddMenuItem("Show Window", "Restore")
	mQuit := systray.AddMenuItem("Quit", "Exit")
	go func() {
		for {
			select {
			case <-mShow.ClickedCh:
				window.RequestFocus()
			case <-mQuit.ClickedCh:
				systray.Quit()
			}
		}
	}()
}

func onExit() {
	os.Exit(0)
}