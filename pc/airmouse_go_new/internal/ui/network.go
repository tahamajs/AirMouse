package ui

import (
	"bytes"
	"fmt"
	"image"
	"image/color"
	"image/png"
	"net"
	"net/url"
	"strconv"
	"strings"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"
	qrcode "github.com/skip2/go-qrcode"

	"airmouse-go/internal/config"
	"airmouse-go/internal/utils"
)

// ------------------------------------------------------------
// NetworkTab
// ------------------------------------------------------------

type NetworkTab struct {
	ipEntry       *widget.Entry
	portEntry     *widget.Entry
	wsPortEntry   *widget.Entry
	udpPortEntry  *widget.Entry
	qrImage       *canvas.Image
	ipList        *widget.List
	ipData        []string
	statusLabel   *widget.Label
	overviewLabel *widget.Label
	refreshBtn    *widget.Button
	copyBtn       *widget.Button
	copyAllBtn    *widget.Button
	genQrBtn      *widget.Button
	saveQrBtn     *widget.Button
	testConnBtn   *widget.Button
	cfg           *config.Config
}

// NewNetworkTab creates the network configuration tab.
func NewNetworkTab(cfg *config.Config) fyne.CanvasObject {
	tab := &NetworkTab{
		cfg:    cfg,
		ipData: getIPList(),
	}

	header := container.NewVBox(
		widget.NewLabelWithStyle("🌐 Network Configuration", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		widget.NewLabel("Use this page to discover the host IP, verify the ports, and generate a QR code for Android approval."),
	)

	// IP Address selection
	tab.ipEntry = widget.NewEntry()
	tab.ipEntry.SetPlaceHolder("Select or enter IP address")
	tab.ipEntry.Text = utils.GetLocalIP()
	tab.ipEntry.Validator = func(s string) error {
		if net.ParseIP(s) == nil {
			return fmt.Errorf("invalid IP address")
		}
		return nil
	}

	tab.ipList = widget.NewList(
		func() int { return len(tab.ipData) },
		func() fyne.CanvasObject {
			return container.NewHBox(
				widget.NewLabel(""),
				widget.NewLabel(""),
			)
		},
		func(id int, obj fyne.CanvasObject) {
			if id >= 0 && id < len(tab.ipData) {
				hbox := obj.(*fyne.Container)
				if len(hbox.Objects) >= 2 {
					icon := hbox.Objects[0].(*widget.Label)
					label := hbox.Objects[1].(*widget.Label)

					icon.SetText("🌐")
					label.SetText(tab.ipData[id])

					// Highlight active IP
					if tab.ipData[id] == tab.ipEntry.Text {
						label.TextStyle = fyne.TextStyle{Bold: true}
					} else {
						label.TextStyle = fyne.TextStyle{}
					}
				}
			}
		},
	)
	tab.ipList.OnSelected = func(id int) {
		if id >= 0 && id < len(tab.ipData) {
			tab.ipEntry.SetText(tab.ipData[id])
			tab.updateQR()
		}
	}

	// Port configuration
	tab.portEntry = widget.NewEntry()
	tab.portEntry.SetPlaceHolder("TCP Port")
	tab.portEntry.Text = strconv.Itoa(cfg.Port)
	tab.portEntry.Validator = func(s string) error {
		p, err := strconv.Atoi(s)
		if err != nil || p < 1 || p > 65535 {
			return fmt.Errorf("port must be between 1 and 65535")
		}
		return nil
	}

	tab.wsPortEntry = widget.NewEntry()
	tab.wsPortEntry.SetPlaceHolder("WebSocket Port")
	tab.wsPortEntry.Text = strconv.Itoa(cfg.WebSocketPort)
	tab.wsPortEntry.Validator = func(s string) error {
		p, err := strconv.Atoi(s)
		if err != nil || p < 1 || p > 65535 {
			return fmt.Errorf("port must be between 1 and 65535")
		}
		return nil
	}

	tab.udpPortEntry = widget.NewEntry()
	tab.udpPortEntry.SetPlaceHolder("UDP Discovery Port")
	tab.udpPortEntry.Text = strconv.Itoa(cfg.UDPPort)
	tab.udpPortEntry.Validator = func(s string) error {
		p, err := strconv.Atoi(s)
		if err != nil || p < 1 || p > 65535 {
			return fmt.Errorf("port must be between 1 and 65535")
		}
		return nil
	}

	// Status label
	tab.statusLabel = widget.NewLabel("⏳ Waiting for approval")
	tab.statusLabel.Importance = widget.SuccessImportance
	tab.overviewLabel = widget.NewLabel("")
	tab.overviewLabel.Wrapping = fyne.TextWrapWord
	tab.overviewLabel.SetText(tab.pairingSummary())

	// Buttons
	tab.refreshBtn = widget.NewButtonWithIcon("Refresh IPs", theme.ViewRefreshIcon(), func() {
		tab.ipData = getIPList()
		tab.ipList.Refresh()
		tab.statusLabel.SetText("🔄 IP list refreshed")
		tab.statusLabel.Importance = widget.MediumImportance

		time.AfterFunc(2*time.Second, func() {
			RunOnMain(func() {
				tab.statusLabel.SetText("⏳ Waiting for approval")
				tab.statusLabel.Importance = widget.SuccessImportance
			})
		})
	})

	tab.testConnBtn = widget.NewButtonWithIcon("Test Connection", theme.ConfirmIcon(), func() {
		tab.testConnection()
	})

	tab.copyBtn = widget.NewButtonWithIcon("Copy Endpoint", theme.ContentCopyIcon(), func() {
		win := getCurrentWindow()
		if win == nil {
			return
		}
		endpoint := fmt.Sprintf("ws://%s:%s/ws", tab.ipEntry.Text, tab.wsPortEntry.Text)
		win.Clipboard().SetContent(endpoint)
		dialog.ShowInformation("Copied", "WebSocket approval endpoint copied to clipboard", win)
	})

	tab.copyAllBtn = widget.NewButtonWithIcon("Copy All", theme.ContentCopyIcon(), func() {
		win := getCurrentWindow()
		if win == nil {
			return
		}
		info := fmt.Sprintf(
			"Server: %s\nIP: %s\nTCP Port: %s\nWebSocket Port: %s\nUDP Port: %s\nEndpoint: ws://%s:%s/ws",
			tab.cfg.ServerName,
			tab.ipEntry.Text,
			tab.portEntry.Text,
			tab.wsPortEntry.Text,
			tab.udpPortEntry.Text,
			tab.ipEntry.Text,
			tab.wsPortEntry.Text,
		)
		win.Clipboard().SetContent(info)
		dialog.ShowInformation("Copied", "All network details copied to clipboard", win)
	})

	tab.genQrBtn = widget.NewButtonWithIcon("Generate QR", theme.InfoIcon(), tab.updateQR)

	tab.saveQrBtn = widget.NewButtonWithIcon("Save QR", theme.DownloadIcon(), func() {
		win := getCurrentWindow()
		if win == nil {
			return
		}
		dialog.ShowFileSave(func(writer fyne.URIWriteCloser, err error) {
			if err == nil && writer != nil {
				defer writer.Close()
				data := tab.pairingURI()
				pngBytes, err := qrcode.Encode(data, qrcode.High, 300)
				if err != nil {
					dialog.ShowError(fmt.Errorf("QR generation failed: %w", err), win)
					return
				}
				_, err = writer.Write(pngBytes)
				if err != nil {
					dialog.ShowError(fmt.Errorf("failed to save QR: %w", err), win)
					return
				}
				dialog.ShowInformation("Saved", "QR code saved successfully", win)
			}
		}, win)
	})

	// QR code display – start with a placeholder
	tab.qrImage = canvas.NewImageFromImage(generatePlaceholderQR())
	tab.qrImage.FillMode = canvas.ImageFillContain
	tab.qrImage.SetMinSize(fyne.NewSize(210, 210))
	tab.updateQR()

	// Auto-save when fields change
	tab.ipEntry.OnChanged = func(s string) {
		if tab.ipEntry.Validate() == nil {
			cfg.Host = s
			_ = cfg.Save()
			tab.overviewLabel.SetText(tab.pairingSummary())
			tab.updateQR()
			tab.ipList.Refresh()
		}
	}

	tab.portEntry.OnChanged = func(s string) {
		if p, err := strconv.Atoi(s); err == nil && p > 0 && p < 65536 {
			cfg.Port = p
			_ = cfg.Save()
			tab.overviewLabel.SetText(tab.pairingSummary())
			tab.updateQR()
		}
	}

	tab.wsPortEntry.OnChanged = func(s string) {
		if p, err := strconv.Atoi(s); err == nil && p > 0 && p < 65536 {
			cfg.WebSocketPort = p
			_ = cfg.Save()
			tab.overviewLabel.SetText(tab.pairingSummary())
			tab.updateQR()
		}
	}

	tab.udpPortEntry.OnChanged = func(s string) {
		if p, err := strconv.Atoi(s); err == nil && p > 0 && p < 65536 {
			cfg.UDPPort = p
			_ = cfg.Save()
			tab.overviewLabel.SetText(tab.pairingSummary())
		}
	}

	overviewCard := NewGlassCard(container.NewPadded(container.NewVBox(
		widget.NewLabelWithStyle("Connection Overview", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel(fmt.Sprintf("Server name: %s", tab.cfg.ServerName)),
		widget.NewLabel(fmt.Sprintf("Current host IP: %s", tab.ipEntry.Text)),
		widget.NewLabel(fmt.Sprintf("TCP / WS / UDP: %s / %s / %s", tab.portEntry.Text, tab.wsPortEntry.Text, tab.udpPortEntry.Text)),
		tab.overviewLabel,
		widget.NewLabel("The Android app can scan the QR code below and then request approval on the selected protocol."),
	)))

	ipCard := NewGlassCard(container.NewPadded(container.NewVBox(
		widget.NewLabelWithStyle("Host Selection", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("Available IP addresses:"),
		container.NewScroll(tab.ipList),
		widget.NewLabel("Selected IP:"),
		tab.ipEntry,
		widget.NewLabel("Ports:"),
		container.NewGridWithColumns(3,
			tab.portEntry,
			tab.wsPortEntry,
			tab.udpPortEntry,
		),
		container.NewHBox(tab.refreshBtn, tab.testConnBtn, tab.copyBtn, tab.copyAllBtn),
	)))

	qrCard := NewGlassCard(container.NewPadded(container.NewVBox(
		widget.NewLabelWithStyle("Pairing QR Code", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		container.NewCenter(tab.qrImage),
		container.NewHBox(tab.genQrBtn, tab.saveQrBtn),
	)))

	content := container.NewVBox(
		header,
		widget.NewSeparator(),
		overviewCard,
		ipCard,
		qrCard,
		widget.NewSeparator(),
		tab.statusLabel,
	)

	return container.NewScroll(container.NewPadded(content))
}

// updateQR refreshes the QR code image based on the current IP and port.
// If generation fails, a placeholder image is displayed and an error dialog is shown.
func (t *NetworkTab) updateQR() {
	data := buildPairingURI(t.safeIP(), t.portEntry.Text, t.wsPortEntry.Text, t.udpPortEntry.Text, t.cfg.ServerName)

	pngBytes, err := qrcode.Encode(data, qrcode.High, 250)
	if err != nil {
		utils.LogError("QR encode failed: %v", err)
		t.qrImage.Image = generatePlaceholderQR()
		t.qrImage.Refresh()
		win := getCurrentWindow()
		if win != nil {
			dialog.ShowError(fmt.Errorf("QR generation failed: %v", err), win)
		}
		return
	}
	img, err := png.Decode(bytes.NewReader(pngBytes))
	if err != nil {
		utils.LogError("QR decode failed: %v", err)
		t.qrImage.Image = generatePlaceholderQR()
		t.qrImage.Refresh()
		win := getCurrentWindow()
		if win != nil {
			dialog.ShowError(fmt.Errorf("QR decode failed: %v", err), win)
		}
		return
	}
	t.qrImage.Image = img
	t.qrImage.Refresh()
}

// generatePlaceholderQR returns a simple placeholder image (gray rectangle with text).
func generatePlaceholderQR() image.Image {
	img := image.NewRGBA(image.Rect(0, 0, 250, 250))
	// Fill with gray
	for x := 0; x < 250; x++ {
		for y := 0; y < 250; y++ {
			img.Set(x, y, color.RGBA{200, 200, 200, 255})
		}
	}
	return img
}

func (t *NetworkTab) pairingSummary() string {
	return fmt.Sprintf("Pairing URI: %s", t.pairingURI())
}

func (t *NetworkTab) pairingURI() string {
	return buildPairingURI(t.safeIP(), t.portEntry.Text, t.wsPortEntry.Text, t.udpPortEntry.Text, t.cfg.ServerName)
}

func (t *NetworkTab) safeIP() string {
	ip := strings.TrimSpace(t.ipEntry.Text)
	if ip == "" {
		return utils.GetLocalIP()
	}
	return ip
}

func buildPairingURI(ip, port, wsPort, udpPort, serverName string) string {
	return fmt.Sprintf(
		"airmouse://pair?ip=%s&tcp=%s&ws=ws://%s:%s/ws&udp=%s&name=%s&version=3.0&type=mobile&protocol=WEBSOCKET",
		url.QueryEscape(ip),
		url.QueryEscape(port),
		url.QueryEscape(ip),
		url.QueryEscape(wsPort),
		url.QueryEscape(udpPort),
		url.QueryEscape(serverName),
	)
}

// testConnection attempts a TCP connection to the configured IP and port.
func (t *NetworkTab) testConnection() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	ip := t.ipEntry.Text
	port := t.portEntry.Text

	t.statusLabel.SetText(fmt.Sprintf("🔄 Testing connection to %s:%s...", ip, port))
	t.statusLabel.Importance = widget.WarningImportance

	go func() {
		conn, err := net.DialTimeout("tcp", fmt.Sprintf("%s:%s", ip, port), 3*time.Second)

		RunOnMain(func() {
			if err != nil {
				t.statusLabel.SetText(fmt.Sprintf("❌ Connection failed: %v", err))
				t.statusLabel.Importance = widget.DangerImportance
				dialog.ShowError(fmt.Errorf("Cannot connect to %s:%s\n\nMake sure the server is running and firewall allows the port", ip, port), win)
			} else {
				_ = conn.Close()
				t.statusLabel.SetText(fmt.Sprintf("✅ Connection successful to %s:%s", ip, port))
				t.statusLabel.Importance = widget.SuccessImportance
				dialog.ShowInformation("Connection Test", fmt.Sprintf("Connection succeeded to %s:%s", ip, port), win)
			}
		})
	}()
}

// getIPList returns a list of all non‑loopback IPv4 addresses on the machine,
// with the most likely LAN IP (private, non‑loopback) placed first.
func getIPList() []string {
	var ips []string
	ifaces, err := net.Interfaces()
	if err != nil {
		return []string{"127.0.0.1"}
	}
	for _, iface := range ifaces {
		if iface.Flags&net.FlagLoopback != 0 || iface.Flags&net.FlagUp == 0 {
			continue
		}
		addrs, err := iface.Addrs()
		if err != nil {
			continue
		}
		for _, addr := range addrs {
			if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() && ipnet.IP.To4() != nil {
				ips = append(ips, ipnet.IP.String())
			}
		}
	}
	// If no IPs found, fallback to loopback
	if len(ips) == 0 {
		ips = append(ips, "127.0.0.1")
	}
	// Sort: put private IPs first (e.g., 192.168.x.x, 10.x.x.x, 172.16-31.x.x)
	// Simple: prefer IPs that are not in the 169.254.x.x (link-local) range.
	var preferred, others []string
	for _, ip := range ips {
		if strings.HasPrefix(ip, "169.254.") {
			others = append(others, ip)
		} else {
			preferred = append(preferred, ip)
		}
	}
	// Combine preferred first, then others
	result := append(preferred, others...)
	return result
}