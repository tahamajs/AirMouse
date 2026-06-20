package ui

import (
	"bytes"
	"fmt"
	"image/png"
	"net"
	"net/url"
	"strconv"
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
	ipEntry      *widget.Entry
	portEntry    *widget.Entry
	wsPortEntry  *widget.Entry
	udpPortEntry *widget.Entry
	qrImage      *canvas.Image
	ipList       *widget.List
	ipData       []string
	statusLabel  *widget.Label
	refreshBtn   *widget.Button
	copyBtn      *widget.Button
	genQrBtn     *widget.Button
	saveQrBtn    *widget.Button
	testConnBtn  *widget.Button
	cfg          *config.Config
}

// NewNetworkTab creates the network configuration tab.
func NewNetworkTab(cfg *config.Config) fyne.CanvasObject {
	tab := &NetworkTab{
		cfg:    cfg,
		ipData: getIPList(),
	}

	// Header
	header := container.NewHBox(
		widget.NewLabelWithStyle("🌐 Network Configuration", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
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
	tab.statusLabel = widget.NewLabel("✅ Ready")
	tab.statusLabel.Importance = widget.SuccessImportance

	// Buttons
	tab.refreshBtn = widget.NewButtonWithIcon("Refresh IPs", theme.ViewRefreshIcon(), func() {
		tab.ipData = getIPList()
		tab.ipList.Refresh()
		tab.statusLabel.SetText("🔄 IP list refreshed")
		tab.statusLabel.Importance = widget.MediumImportance

		time.AfterFunc(2*time.Second, func() {
			RunOnMain(func() {
				tab.statusLabel.SetText("✅ Ready")
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
		endpoint := fmt.Sprintf("airmouse://%s:%s", tab.ipEntry.Text, tab.portEntry.Text)
		win.Clipboard().SetContent(endpoint)
		dialog.ShowInformation("Copied", "Endpoint copied to clipboard", win)
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
				data := fmt.Sprintf("airmouse://%s:%s?name=%s", tab.ipEntry.Text, tab.portEntry.Text, url.QueryEscape(tab.cfg.ServerName))
				pngBytes, _ := qrcode.Encode(data, qrcode.High, 300)
				_, _ = writer.Write(pngBytes)
				dialog.ShowInformation("Saved", "QR code saved successfully", win)
			}
		}, win)
	})

	// QR code display
	tab.qrImage = canvas.NewImageFromResource(nil)
	tab.qrImage.FillMode = canvas.ImageFillOriginal
	tab.qrImage.SetMinSize(fyne.NewSize(250, 250))
	tab.updateQR()

	// Auto-save when fields change
	tab.ipEntry.OnChanged = func(s string) {
		if tab.ipEntry.Validate() == nil {
			cfg.Host = s
			_ = cfg.Save()
			tab.updateQR()
			tab.ipList.Refresh()
		}
	}

	tab.portEntry.OnChanged = func(s string) {
		if p, err := strconv.Atoi(s); err == nil && p > 0 && p < 65536 {
			cfg.Port = p
			_ = cfg.Save()
			tab.updateQR()
		}
	}

	tab.wsPortEntry.OnChanged = func(s string) {
		if p, err := strconv.Atoi(s); err == nil && p > 0 && p < 65536 {
			cfg.WebSocketPort = p
			_ = cfg.Save()
		}
	}

	tab.udpPortEntry.OnChanged = func(s string) {
		if p, err := strconv.Atoi(s); err == nil && p > 0 && p < 65536 {
			cfg.UDPPort = p
			_ = cfg.Save()
		}
	}

	// Layout
	content := container.NewVBox(
		header,
		widget.NewSeparator(),
		widget.NewLabel("Available IP addresses:"),
		container.NewScroll(tab.ipList),
		widget.NewLabel("Selected IP:"), tab.ipEntry,
		widget.NewLabel("Ports:"),
		container.NewGridWithColumns(3,
			tab.portEntry,
			tab.wsPortEntry,
			tab.udpPortEntry,
		),
		container.NewHBox(tab.refreshBtn, tab.testConnBtn, tab.copyBtn),
		container.NewHBox(tab.genQrBtn, tab.saveQrBtn),
		widget.NewSeparator(),
		widget.NewLabelWithStyle("Pairing QR Code", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		container.NewCenter(tab.qrImage),
		widget.NewSeparator(),
		tab.statusLabel,
	)

	return container.NewScroll(content)
}

// updateQR refreshes the QR code image based on the current IP and port.
func (t *NetworkTab) updateQR() {
	data := fmt.Sprintf("airmouse://%s:%s?name=%s",
		t.ipEntry.Text,
		t.portEntry.Text,
		url.QueryEscape(t.cfg.ServerName))

	pngBytes, err := qrcode.Encode(data, qrcode.High, 250)
	if err != nil {
		return
	}
	img, err := png.Decode(bytes.NewReader(pngBytes))
	if err != nil {
		return
	}
	t.qrImage.Image = img
	t.qrImage.Refresh()
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
				dialog.ShowInformation("Connection Test", fmt.Sprintf("Successfully connected to %s:%s", ip, port), win)
			}
		})
	}()
}

// getIPList returns a list of all non‑loopback IPv4 addresses on the machine.
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
	if len(ips) == 0 {
		ips = append(ips, "127.0.0.1")
	}
	return ips
}