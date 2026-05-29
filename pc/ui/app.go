package ui

import (
	"bytes"
	"fmt"
	"image"
	"image/color"
	"os"
	"strconv"
	"time"
	"net"

	"airmouse-go/config"
	"airmouse-go/control"
	"airmouse-go/server"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/layout"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"

	qrcode "github.com/skip2/go-qrcode"
	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/mem"
	"github.com/getlantern/systray"
)

var (
	appObj       fyne.App
	window       fyne.Window
	mouse        *control.MouseController
	tcpServer    *server.TCPServer
	configFile   *config.Config

	// UI elements
	statusPill   *widget.Label
	statsLabel   *widget.Label
	connLabel    *widget.Label
	ipLabel      *widget.Label
	qrImage      *canvas.Image
	logEntry     *widget.Entry
	connList     *widget.List
	connData     []string
)

// Log adds a message to the log widget (exported for server modules)
func Log(msg string) {
	logEntry.Append(msg + "\n")
}

func Setup(w fyne.Window, a fyne.App, cfg *config.Config, mouseCtrl *control.MouseController) {
	appObj = a
	window = w
	mouse = mouseCtrl
	configFile = cfg

	// ---------- Header ----------
	statusPill = widget.NewLabelWithStyle("Server stopped", fyne.TextAlignCenter, fyne.TextStyle{Bold: true})
	statusPill.BackgroundColor = color.NRGBA{R: 239, G: 91, B: 91, A: 255}

	title := widget.NewLabelWithStyle("Air Mouse Pro Server", fyne.TextAlignCenter, fyne.TextStyle{Bold: true})
	subtitle := widget.NewLabel("Desktop endpoint, discovery & live dashboard")

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
			Log("QR generation failed: " + err.Error())
			return
		}
		img, _, err := image.Decode(bytes.NewReader(pngBytes))
		if err != nil {
			Log("QR decode failed: " + err.Error())
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

	networkCard := widget.NewCard("Network Endpoint", "Choose IP address", container.NewVBox(
		ipEntry, portEntry,
		container.NewHBox(refreshBtn, copyBtn),
		ipLabel,
		container.NewHBox(genQrBtn, saveQrBtn),
		qrImage,
	))

	// ---------- Server Controls ----------
	startBtn := widget.NewButton("Start Server", func() {
		startServer(ipEntry.Text, portEntry.Text)
	})
	stopBtn := widget.NewButton("Stop Server", func() {
		stopServer()
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
	sensCard := widget.NewCard("Cursor Sensitivity", "", sensSlider)

	// ---------- Stats ----------
	statsLabel = widget.NewLabel("Clicks: 0  |  Dbl: 0  |  Right: 0  |  Scroll: 0")
	connLabel = widget.NewLabel("Connections: 0")
	statsCard := widget.NewCard("Runtime Summary", "", container.NewVBox(statsLabel, connLabel))

	// ---------- Connected Clients ----------
	connData = []string{}
	connList = widget.NewList(
		func() int { return len(connData) },
		func() fyne.CanvasObject { return widget.NewLabel("") },
		func(id widget.ListItemID, obj fyne.CanvasObject) {
			obj.(*widget.Label).SetText(connData[id])
		},
	)
	connCard := widget.NewCard("Connected Clients", "", connList)

	// ---------- Log ----------
	logEntry = widget.NewMultiLineEntry()
	logEntry.Disable()
	logEntry.SetPlaceHolder("Log output...")
	logCard := widget.NewCard("Live Log", "", container.NewScroll(logEntry))

	// ---------- Left / Right Split ----------
	left := container.NewVBox(
		networkCard,
		controlCard,
		sensCard,
		statsCard,
		connCard,
	)
	right := logCard
	split := container.NewHSplit(left, right)
	split.Offset = 0.45

	// ---------- Status Bar ----------
	statusBar := widget.NewLabel("Server stopped")
	mainContent := container.NewBorder(header, statusBar, nil, nil, split)

	w.SetContent(mainContent)
	w.Resize(fyne.NewSize(1100, 720))

	// System tray
	go setupTray()
}

func startServer(ip, portStr string) {
	port, _ := strconv.Atoi(portStr)
	tcpServer = server.NewTCPServer("0.0.0.0", port, mouse, Log, updateStats, updateConnList)
	if err := tcpServer.Start(); err != nil {
		Log("❌ TCP start error: " + err.Error())
		return
	}
	statusPill.BackgroundColor = color.NRGBA{G: 255, A: 255}
	statusPill.SetText("Server running")
}

func stopServer() {
	if tcpServer != nil {
		tcpServer.Stop()
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

func getLocalIP() string {
	// Return the first non-loopback IPv4 address (simplified)
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
				// Fyne doesn't directly support minimize/restore; we can bring window to front
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