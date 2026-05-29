package ui

import (
	"bytes"
	"fmt"
	"image"
	_ "image/png"
	"net"
	"strconv"
	"sync"
	"time"

	"airmouse-go/config"
	"airmouse-go/control"
	"airmouse-go/server"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/app"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"

	qrcode "github.com/skip2/go-qrcode"
	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/mem"
)

var (
	appObj    fyne.App
	window    fyne.Window
	mouse     control.MouseController
	tcpServer *server.TCPServer
	cfg       *config.Config

	// Header
	statusPill *widget.Label

	// Dashboard widgets
	statsLabel   *widget.Label
	connLabel    *widget.Label
	endpointLabel *widget.Label
	mdnsLabel    *widget.Label
	uptimeLabel  *widget.Label

	// Network widgets
	ipEntry    *widget.Entry
	portEntry  *widget.Entry
	qrImage    *canvas.Image
	ipList     *widget.List
	ipListData []string

	// Clients widgets
	connList  *widget.List
	connData  []string
	clientMu  sync.Mutex

	// Logs
	logEntry *widget.Entry
	logLines []logLine
	filterVar string
	showInfo  = true
	showWarn  = true
	showError = true

	// Performance
	perfRunning bool
	perfLabel   *widget.Label

	mu sync.Mutex
)

type logLine struct {
	Level   string
	Message string
}

func Log(msg string) { LogLevel("info", msg) }

func LogLevel(level, msg string) {
	prefix := map[string]string{"info": "ℹ️", "warning": "⚠️", "error": "❌"}[level]
	mu.Lock()
	logLines = append(logLines, logLine{level, prefix + " " + msg})
	mu.Unlock()
	refreshFilteredLog()
}

func NewApp(cfgFile *config.Config, mouseCtrl control.MouseController) fyne.App {
	cfg = cfgFile
	mouse = mouseCtrl

	a := app.New()
	appObj = a
	w := a.NewWindow("Air Mouse Pro Server")
	window = w

	// ---------- Header ----------
	statusPill = widget.NewLabelWithStyle("Server stopped", fyne.TextAlignCenter, fyne.TextStyle{Bold: true})

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
		fyne.NewMenuItem("Refresh IP List", func() { refreshIPList() }),
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
	w.Show()

	// Auto‑start IP list refresh
	refreshIPList()

	return a
}

// ---------- Dashboard Tab ----------
func buildDashboardTab() fyne.CanvasObject {
	statsLabel = widget.NewLabel("Clicks: 0  |  Dbl: 0  |  Right: 0  |  Scroll: 0")
	connLabel = widget.NewLabel("Connections: 0")
	endpointLabel = widget.NewLabel("Endpoint: not set")
	mdnsLabel = widget.NewLabel("mDNS: not advertised")
	uptimeLabel = widget.NewLabel("Uptime: 00:00")

	startBtn := widget.NewButtonWithIcon("Start Server", theme.MediaPlayIcon(), func() { startServer() })
	stopBtn := widget.NewButtonWithIcon("Stop Server", theme.MediaStopIcon(), func() { stopServer() })

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

	// Uptime ticker
	serverStart := time.Time{}
	go func() {
		for {
			time.Sleep(1 * time.Second)
			if !serverStart.IsZero() {
				d := time.Since(serverStart).Truncate(time.Second)
				uptimeLabel.SetText(fmt.Sprintf("Uptime: %02d:%02d:%02d", int(d.Hours()), int(d.Minutes())%60, int(d.Seconds())%60))
			}
		}
	}()

	dash := container.NewVBox(
		widget.NewLabelWithStyle("Server Dashboard", fyne.TextAlignCenter, fyne.TextStyle{Bold: true, Italic: true}),
		widget.NewSeparator(),
		container.NewHBox(startBtn, stopBtn),
		widget.NewSeparator(),
		statsLabel,
		connLabel,
		endpointLabel,
		mdnsLabel,
		uptimeLabel,
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

	refreshBtn := widget.NewButton("Refresh", func() { refreshIPList() })
	copyBtn := widget.NewButton("Copy Endpoint", func() {
		endpoint := fmt.Sprintf("airmouse://%s:%s", ipEntry.Text, portEntry.Text)
		window.Clipboard().SetContent(endpoint)
		Log("📋 Copied: " + endpoint)
	})

	// IP list
	ipListData = []string{}
	ipList = widget.NewList(
		func() int { return len(ipListData) },
		func() fyne.CanvasObject { return widget.NewLabel("") },
		func(id widget.ListItemID, obj fyne.CanvasObject) {
			obj.(*widget.Label).SetText(ipListData[id])
		},
	)
	ipList.OnSelected = func(id widget.ListItemID) {
		if id >= 0 && id < len(ipListData) {
			// Extract IP from "192.168.1.5 (en0)" format
			parts := split(ipListData[id], " ")
			if len(parts) > 0 {
				ipEntry.SetText(parts[0])
			}
		}
	}

	qrImage = canvas.NewImageFromImage(nil)
	qrImage.FillMode = canvas.ImageFillOriginal

	genQrBtn := widget.NewButton("Generate QR", func() {
		updateQR()
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
	manualIPCheck := widget.NewCheck("Use manual IP", func(b bool) {
		if b {
			ipEntry.SetText(manualIPEntry.Text)
		} else {
			ipEntry.SetText(getLocalIP())
		}
	})

	// Auto‑generate QR when IP/port changes
	ipEntry.OnChanged = func(s string) { updateQR() }
	portEntry.OnChanged = func(s string) { updateQR() }

	netTab := container.NewVBox(
		widget.NewLabelWithStyle("Network Endpoint", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("Available IPs (click to select):"),
		container.NewScroll(ipList),
		ipEntry,
		portEntry,
		container.NewHBox(refreshBtn, copyBtn),
		manualIPCheck,
		manualIPEntry,
		container.NewHBox(genQrBtn, saveQrBtn),
		qrImage,
	)
	return container.NewScroll(netTab)
}

// ---------- Clients Tab ----------
func buildClientsTab() fyne.CanvasObject {
	connData = []string{}
	var selectedAddr string
	connList = widget.NewList(
		func() int { return len(connData) },
		func() fyne.CanvasObject { return widget.NewLabel("") },
		func(id widget.ListItemID, obj fyne.CanvasObject) {
			obj.(*widget.Label).SetText(connData[id])
		},
	)
	connList.OnSelected = func(id widget.ListItemID) {
		if id >= 0 && id < len(connData) {
			selectedAddr = connData[id]
		}
	}
	disconnectBtn := widget.NewButton("Disconnect Selected", func() {
		if selectedAddr != "" && tcpServer != nil {
			tcpServer.DisconnectByAddr(selectedAddr)
		}
	})
	disconnectAllBtn := widget.NewButton("Disconnect All", func() {
		if tcpServer != nil {
			// Implement DisconnectAll on server side
			Log("Disconnecting all clients")
		}
	})

	return container.NewBorder(
		widget.NewLabelWithStyle("Connected Clients", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		container.NewHBox(disconnectBtn, disconnectAllBtn),
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
		mouse.SetSensitivity(v)
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

	clearBtn := widget.NewButton("Clear", func() {
		mu.Lock()
		logLines = nil
		mu.Unlock()
		logEntry.SetText("")
	})

	filterToolbar := container.NewHBox(
		widget.NewLabel("Filter:"),
		searchEntry,
		infoCheck,
		warnCheck,
		errCheck,
		exportBtn,
		clearBtn,
	)

	return container.NewBorder(
		widget.NewLabelWithStyle("Live Log", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		filterToolbar,
		nil, nil,
		container.NewScroll(logEntry),
	)
}

// ---------- Server actions ----------
var serverStartTime time.Time

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
	serverStartTime = time.Now()
	statusPill.SetText("Server running")
	endpointLabel.SetText(fmt.Sprintf("Endpoint: %s:%d", getLocalIP(), port))
	mdnsLabel.SetText(fmt.Sprintf("mDNS: %s.local:%d", cfg.MDNSName, port))
}

func stopServer() {
	if tcpServer != nil {
		tcpServer.Stop()
		tcpServer = nil
	}
	serverStartTime = time.Time{}
	statusPill.SetText("Server stopped")
	endpointLabel.SetText("Endpoint: not set")
	mdnsLabel.SetText("mDNS: not advertised")
}

func updateStats(clicks, dbl, right, scroll int) {
	statsLabel.SetText(fmt.Sprintf("Clicks: %d  |  Dbl: %d  |  Right: %d  |  Scroll: %d", clicks, dbl, right, scroll))
}

func updateConnList(list []string) {
	clientMu.Lock()
	connData = list
	clientMu.Unlock()
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

// Helper to refresh the IP list
func refreshIPList() {
	ipListData = getIPList()
	ipList.Refresh()
}

func getIPList() []string {
	var ips []string
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return []string{"127.0.0.1 (lo)"}
	}
	for _, addr := range addrs {
		if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() && ipnet.IP.To4() != nil {
			ips = append(ips, fmt.Sprintf("%s (%s)", ipnet.IP.String(), "en0")) // simplified; real interface name retrieval is complex
		}
	}
	if len(ips) == 0 {
		return []string{"127.0.0.1 (lo)"}
	}
	return ips
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

func updateQR() {
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
}

func split(s string, sep string) []string {
	var result []string
	for _, part := range bytes.Split([]byte(s), []byte(sep)) {
		result = append(result, string(part))
	}
	return result
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