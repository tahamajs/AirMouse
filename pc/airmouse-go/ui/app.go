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

	// UI widgets
	statusPill *widget.Label
	statsLabel *widget.Label
	connLabel  *widget.Label
	ipLabel    *widget.Label
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

	mu sync.Mutex
)

type logLine struct {
	Level   string
	Message string
}

func Log(msg string) {
	LogLevel("info", msg)
}

func LogLevel(level, msg string) {
	prefix := map[string]string{"info": "ℹ️", "warning": "⚠️", "error": "❌"}[level]
	mu.Lock()
	logLines = append(logLines, logLine{level, prefix + " " + msg})
	mu.Unlock()
	refreshFilteredLog()
}

// NewApp creates the Fyne application and sets up the UI.
func NewApp(config *config.Config, mouseCtrl *control.MouseController) fyne.App {
	cfg = config
	mouse = mouseCtrl

	a := fyne.NewApp()
	appObj = a
	w := a.NewWindow("Air Mouse Pro Server")
	window = w

	// ---------- Header ----------
	statusPill = widget.NewLabelWithStyle("Server stopped", fyne.TextAlignCenter, fyne.TextStyle{Bold: true})
	statusPill.BackgroundColor = color.NRGBA{R: 239, G: 91, B: 91, A: 255}

	title := widget.NewLabelWithStyle("Air Mouse Pro Server", fyne.TextAlignCenter, fyne.TextStyle{Bold: true})
	subtitle := widget.NewLabel("Desktop endpoint, discovery & live dashboard")
	subtitle.Alignment = fyne.TextAlignCenter

	header := container.NewVBox(
		container.NewCenter(title),
		container.NewCenter(subtitle),
		container.NewCenter(statusPill),
	)

	// ---------- Network Endpoint ----------
	ipEntry := widget.NewEntry()
	ipEntry.SetPlaceHolder("192.168.1.x")
	ipEntry.Text = getLocalIP()
	portEntry := widget.NewEntry()
	portEntry.SetPlaceHolder("8080")
	portEntry.Text = "8080"

	ipLabel = widget.NewLabel("Current endpoint: (none)")

	refreshBtn := widget.NewButton("Refresh", func() {
		ipEntry.Text = getLocalIP()
	})
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

	ipCard := widget.NewCard("Network Endpoint", "Choose IP address to advertise", container.NewVBox(
		ipEntry, portEntry,
		container.NewHBox(refreshBtn, copyBtn),
		manualIPCheck, manualIPEntry,
		container.NewHBox(genQrBtn, saveQrBtn),
		qrImage,
		ipLabel,
	))

	// ---------- Server Controls ----------
	startBtn := widget.NewButton("Start Server", func() {
		port, _ := strconv.Atoi(portEntry.Text)
		tcpServer = server.NewTCPServer("0.0.0.0", port, mouse, Log, updateStats, updateConnList)
		if err := tcpServer.Start(); err != nil {
			LogLevel("error", "TCP start error: "+err.Error())
			return
		}
		statusPill.BackgroundColor = color.NRGBA{G: 255, A: 255}
		statusPill.SetText("Server running")
	})
	stopBtn := widget.NewButton("Stop Server", func() {
		if tcpServer != nil {
			tcpServer.Stop()
		}
		statusPill.BackgroundColor = color.NRGBA{R: 255, A: 255}
		statusPill.SetText("Server stopped")
	})
	controlCard := widget.NewCard("Server Controls", "", container.NewHBox(startBtn, stopBtn))

	// ---------- Sensitivity ----------
	sensSlider := widget.NewSlider(0.2, 2.0)
	sensSlider.Value = cfg.Sensitivity
	sensSlider.OnChanged = func(v float64) {
		mouse.sensitivity = v
		cfg.Sensitivity = v
		config.Save("config.json", cfg)
	}
	sensCard := widget.NewCard("Cursor Sensitivity", "Tune pointer speed", sensSlider)

	// ---------- Runtime Summary ----------
	statsLabel = widget.NewLabel("Clicks: 0  |  Dbl: 0  |  Right: 0  |  Scroll: 0")
	connLabel = widget.NewLabel("Connections: 0")
	statsCard := widget.NewCard("Runtime Summary", "Live server counters", container.NewVBox(statsLabel, connLabel))

	// ---------- Connected Clients ----------
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
	connCard := widget.NewCard("Connected Clients", "", container.NewBorder(nil, disconnectBtn, nil, nil, connList))

	// ---------- Live Log ----------
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
	logCard := widget.NewCard("Live Log", "Connections, gestures, errors", container.NewBorder(filterToolbar, nil, nil, nil, container.NewScroll(logEntry)))

	// ---------- Diagnostics ----------
	diagText := widget.NewLabel("Compatibility: Works on same Wi‑Fi network. Bluetooth/USB coming soon.")
	diagCard := widget.NewCard("Server Diagnostics", "Quick status", diagText)

	// ---------- Performance Monitor ----------
	perfLabel = widget.NewLabel("CPU: ---%  MEM: ---%")
	go updatePerformance()

	// ---------- Status Bar ----------
	statusBar := widget.NewLabel("Server stopped")
	mainContent := container.NewBorder(
		header,
		container.NewVBox(statusBar, perfLabel),
		nil, nil,
		container.NewHSplit(
			container.NewVBox(ipCard, controlCard, sensCard, statsCard, connCard),
			container.NewVSplit(logCard, diagCard),
		),
	)

	// ---------- Menu Bar ----------
	fileMenu := fyne.NewMenu("File",
		fyne.NewMenuItem("Start Server", func() { startBtn.OnTapped() }),
		fyne.NewMenuItem("Stop Server", func() { stopBtn.OnTapped() }),
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
					"4. Move phone to control mouse", window)
		}),
		fyne.NewMenuItem("About", func() {
			dialog.ShowInformation("About Air Mouse",
				"Air Mouse Pro Server\n"+
					"University of Tehran – Embedded Systems", window)
		}),
	)
	w.SetMainMenu(fyne.NewMainMenu(fileMenu, viewMenu, helpMenu))

	w.SetContent(mainContent)
	w.Resize(fyne.NewSize(1100, 720))

	// System tray
	go setupTray()

	return a
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