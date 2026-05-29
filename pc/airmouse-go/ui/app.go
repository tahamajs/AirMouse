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
	"strings"   // <-- add this

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

	statusPill *widget.Label
	perfRunning bool       // <-- make sure this line exists

	// Dashboard
	statsLabel   *widget.Label
	connLabel    *widget.Label
	endpointLabel *widget.Label
	mdnsLabel    *widget.Label
	uptimeLabel  *widget.Label
	startBtn     *widget.Button
	stopBtn      *widget.Button
	serverStart  time.Time

	// Network
	ipEntry   *widget.Entry
	portEntry *widget.Entry
	qrImage   *canvas.Image

	// IP list
	ipList     *widget.List
	ipListData []string

	// Clients
	connList *widget.List
	connData []string

	// Logs
	logEntry *widget.Entry
	logLines []logLine
	filterVar string
	showInfo  = true
	showWarn  = true
	showError = true

	// Performance
	perfLabel *widget.Label

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
				"1. Make sure your PC and phone are on the same Wi‑Fi network.\n"+
					"2. Select the correct IP in the Network tab (or scan the QR code).\n"+
					"3. Click 'Start Server'.\n"+
					"4. Open the Air Mouse Android app and either:\n"+
					"   - Scan the QR code from the Network tab, or\n"+
					"   - Enter the IP and port manually (shown in Dashboard after start).\n\n"+
					"Tip: You can also use mDNS: airmouse.local:8080 if your network supports Bonjour.", w)
		}),
		fyne.NewMenuItem("What's my IP?", func() {
			dialog.ShowInformation("Your IP", "Your local IP is: "+getLocalIP()+"\nUse this in the Android app.", w)
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

	refreshIPList()

	return a
}

// ---------- Dashboard Tab ----------
func buildDashboardTab() fyne.CanvasObject {
	statsLabel = widget.NewLabel("Clicks: 0  |  Dbl: 0  |  Right: 0  |  Scroll: 0")
	connLabel = widget.NewLabel("Connections: 0")
	endpointLabel = widget.NewLabel("Endpoint: not set")
	mdnsLabel = widget.NewLabel("mDNS: not advertised")
	uptimeLabel = widget.NewLabel("Uptime: --:--:--")

	startBtn = widget.NewButtonWithIcon("Start Server", theme.MediaPlayIcon(), func() { startServer() })
	stopBtn = widget.NewButtonWithIcon("Stop Server", theme.MediaStopIcon(), func() { stopServer() })

	statusHint := widget.NewLabel("To begin, go to the Network tab, select your IP, then start the server.")
	statusHint.Alignment = fyne.TextAlignCenter

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
		statusHint,
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
	ipEntry.SetPlaceHolder("e.g., 192.168.1.10")
	ipEntry.Text = getLocalIP()
	portEntry = widget.NewEntry()
	portEntry.SetPlaceHolder("e.g., 8080")
	portEntry.Text = strconv.Itoa(cfg.Port)

	refreshBtn := widget.NewButton("Refresh", func() { refreshIPList() })
	copyBtn := widget.NewButton("Copy Endpoint", func() {
		endpoint := fmt.Sprintf("airmouse://%s:%s", ipEntry.Text, portEntry.Text)
		window.Clipboard().SetContent(endpoint)
		Log("📋 Copied: " + endpoint)
		dialog.ShowInformation("Copied", "Endpoint copied to clipboard!", window)
	})

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
			parts := strings.Split(ipListData[id], " ")
			if len(parts) > 0 {
				ipEntry.SetText(parts[0])
			}
		}
	}

	qrImage = canvas.NewImageFromImage(nil)
	qrImage.FillMode = canvas.ImageFillOriginal

	genQrBtn := widget.NewButton("Generate QR", func() { updateQR() })
	genQrBtn.Importance = widget.HighImportance
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
	manualIPEntry.SetPlaceHolder("Manual IP (e.g., for VPN)")
	manualIPCheck := widget.NewCheck("Use manual IP", func(b bool) {
		if b {
			ipEntry.SetText(manualIPEntry.Text)
		} else {
			ipEntry.SetText(getLocalIP())
		}
	})

	// Auto‑generate QR on IP/port change
	ipEntry.OnChanged = func(s string) { updateQR() }
	portEntry.OnChanged = func(s string) { updateQR() }

	netTab := container.NewVBox(
		widget.NewLabelWithStyle("Network Endpoint", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("Select your local IP from the list below, or enter it manually:"),
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
			Log("Disconnecting all clients")
			tcpServer.Stop()
			tcpServer = nil
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
func startServer() {
	if tcpServer != nil {
		dialog.ShowInformation("Already running", "Server is already active.", window)
		return
	}
	port, _ := strconv.Atoi(portEntry.Text)
	tcpServer = server.NewTCPServer("0.0.0.0", port, mouse, Log, updateStats, updateConnList)
	if err := tcpServer.Start(); err != nil {
		LogLevel("error", "TCP start error: "+err.Error())
		dialog.ShowError(err, window)
		return
	}
	serverStart = time.Now()
	statusPill.SetText("Server running")
	endpointLabel.SetText(fmt.Sprintf("Endpoint: %s:%d", getLocalIP(), port))
	mdnsLabel.SetText(fmt.Sprintf("mDNS: %s.local:%d", cfg.MDNSName, port))
	startBtn.Disable()
	stopBtn.Enable()
	dialog.ShowInformation("Server started", fmt.Sprintf("Server is running on %s:%d\nScan the QR code in the Network tab with your phone.", getLocalIP(), port), window)
}

func stopServer() {
	if tcpServer != nil {
		tcpServer.Stop()
		tcpServer = nil
	}
	serverStart = time.Time{}
	statusPill.SetText("Server stopped")
	endpointLabel.SetText("Endpoint: not set")
	mdnsLabel.SetText("mDNS: not advertised")
	startBtn.Enable()
	stopBtn.Disable()
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
		if filterVar != "" && !strings.Contains(l.Message, filterVar) {
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
			ips = append(ips, fmt.Sprintf("%s", ipnet.IP.String()))
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