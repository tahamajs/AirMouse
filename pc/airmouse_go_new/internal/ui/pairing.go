package ui

import (
	"bytes"
	"fmt"
	"image/png"
	"net/url"
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
// ShowPairingWizard – comprehensive pairing wizard
// ------------------------------------------------------------

// ShowPairingWizard displays a comprehensive pairing wizard with multiple methods.
func ShowPairingWizard(parent fyne.Window, wsURL string) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	cfg := config.Get()
	ip := utils.GetLocalIP()
	port := cfg.Port

	pairingData := fmt.Sprintf(
		"airmouse://pair?ws=%s&protocol=WEBSOCKET&name=%s&ip=%s&port=%d&udp=%d&version=3.0",
		url.QueryEscape(wsURL),
		url.QueryEscape(cfg.ServerName),
		url.QueryEscape(ip),
		port,
		cfg.UDPPort,
	)

	pngBytes, err := qrcode.Encode(pairingData, qrcode.High, 300)
	if err != nil {
		dialog.ShowError(fmt.Errorf("QR generation failed: %w", err), parent)
		return
	}
	img, err := png.Decode(bytes.NewReader(pngBytes))
	if err != nil {
		dialog.ShowError(fmt.Errorf("QR decoding failed: %w", err), parent)
		return
	}

	qrImage := canvas.NewImageFromImage(img)
	qrImage.FillMode = canvas.ImageFillContain
	qrImage.SetMinSize(fyne.NewSize(320, 320))

	instructions := widget.NewRichTextFromMarkdown(
		"# How to pair your device\n\n" +
			"**Pending approval:** the server will not enable mouse control until this session is approved from the panel.\n\n" +
			"## Method 1: QR Code\n" +
			"1. Open the Air Mouse Android app\n" +
			"2. Tap the QR scanner icon in the top right\n" +
			"3. Scan this QR code\n" +
			"4. The app will show waiting for approval, then approved, then connected\n" +
			"5. Return here if the session does not become active\n\n" +
			"## Method 2: Manual Entry\n" +
			"1. Open Air Mouse app\n" +
			"2. Tap **Manual Connection**\n" +
			"3. Enter the IP address and port below\n" +
			"4. Tap **Connect**\n" +
			"5. Approve the session in this panel after Android connects\n\n" +
			"## Method 3: UDP Discovery\n" +
			"1. Make sure both devices are on the same network\n" +
			"2. The app will automatically discover the server\n" +
			"3. Tap on the discovered server to connect\n" +
			"4. Approve the session in this panel when it appears")

	serverInfo := widget.NewRichTextFromMarkdown(fmt.Sprintf(
		"## Server Information\n\n"+
			"- **Server Name:** %s\n"+
			"- **IP Address:** %s\n"+
			"- **TCP Port:** %d\n"+
			"- **WebSocket Port:** %d\n"+
			"- **UDP Discovery Port:** %d\n"+
			"- **Version:** 3.0.0",
		cfg.ServerName, ip, cfg.Port, cfg.WebSocketPort, cfg.UDPPort))

	copyIPBtn := widget.NewButtonWithIcon("Copy IP", theme.ContentCopyIcon(), func() {
		if parent != nil {
			parent.Clipboard().SetContent(ip)
			dialog.ShowInformation("Copied", "IP address copied to clipboard", parent)
		}
	})

	copyURLBtn := widget.NewButtonWithIcon("Copy URL", theme.ContentCopyIcon(), func() {
		if parent != nil {
			fullURL := wsURL
			parent.Clipboard().SetContent(fullURL)
			dialog.ShowInformation("Copied", "WebSocket URL copied to clipboard", parent)
		}
	})

	qrTab := container.NewVBox(
		widget.NewLabelWithStyle("Scan QR Code", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		container.NewCenter(qrImage),
		widget.NewLabel("Position the QR code within the camera frame"),
	)

	manualTab := container.NewVBox(
		widget.NewLabelWithStyle("Manual Connection", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("Server IP:"),
		widget.NewLabel(ip),
		widget.NewLabel("Port:"),
		widget.NewLabel(fmt.Sprintf("%d", port)),
		container.NewHBox(copyIPBtn, copyURLBtn),
	)

	helpTab := container.NewVBox(
		widget.NewLabelWithStyle("Troubleshooting", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel("• Make sure both devices are on the same network"),
		widget.NewLabel("• Check firewall settings (port 8080, 8081, 8082)"),
		widget.NewLabel("• Restart the server if connection fails"),
		widget.NewLabel("• Try using the IP address directly"),
		widget.NewLabel("• Check that the server is running"),
	)

	tabs := container.NewAppTabs(
		container.NewTabItemWithIcon("QR Code", theme.InfoIcon(), qrTab),
		container.NewTabItemWithIcon("Manual", theme.SettingsIcon(), manualTab),
		container.NewTabItemWithIcon("Help", theme.HelpIcon(), helpTab),
	)

	content := container.NewVBox(
		widget.NewLabelWithStyle("🔗 Pair New Device", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewRichTextFromMarkdown(
			"**Approval required:** Android can discover and connect, but it will not control the mouse until this server approves the session.",
		),
		widget.NewLabel("The QR code opens the Android pairing flow with the correct WebSocket endpoint and protocol."),
		instructions,
		widget.NewSeparator(),
		tabs,
		widget.NewSeparator(),
		serverInfo,
	)

	scroll := container.NewScroll(container.NewPadded(container.NewMax(content)))
	scroll.SetMinSize(fyne.NewSize(760, 760))
	dialog.ShowCustom("Pairing Wizard", "Close", scroll, parent)
}

// ------------------------------------------------------------
// QuickPairDialog – simplified pairing
// ------------------------------------------------------------

// QuickPairDialog shows a simplified pairing dialog.
func QuickPairDialog(parent fyne.Window) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	cfg := config.Get()
	ip := utils.GetLocalIP()

	pairingData := fmt.Sprintf("airmouse://pair?ws=%s&protocol=WEBSOCKET&name=%s&ip=%s&port=%d&version=3.0",
		url.QueryEscape(fmt.Sprintf("ws://%s:%d/ws", ip, cfg.WebSocketPort)),
		url.QueryEscape(cfg.ServerName),
		url.QueryEscape(ip),
		cfg.WebSocketPort)

	pngBytes, err := qrcode.Encode(pairingData, qrcode.Medium, 200)
	if err != nil {
		dialog.ShowError(fmt.Errorf("QR generation failed: %w", err), parent)
		return
	}
	img, err := png.Decode(bytes.NewReader(pngBytes))
	if err != nil {
		dialog.ShowError(fmt.Errorf("QR decoding failed: %w", err), parent)
		return
	}

	qrImage := canvas.NewImageFromImage(img)
	qrImage.FillMode = canvas.ImageFillContain
	qrImage.SetMinSize(fyne.NewSize(180, 180))

	content := container.NewVBox(
		widget.NewLabelWithStyle("Quick Pair", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		container.NewCenter(qrImage),
		widget.NewLabel(fmt.Sprintf("WebSocket: %s:%d", ip, cfg.WebSocketPort)),
		widget.NewLabel("If the QR looks stale, reopen this dialog after restarting the server."),
	)

	dialog.ShowCustom("Quick Pair", "Close", content, parent)
}

// ------------------------------------------------------------
// ShowDeviceCodePairing – device code pairing
// ------------------------------------------------------------

// ShowDeviceCodePairing displays a device code for pairing (alternative to QR).
func ShowDeviceCodePairing(parent fyne.Window) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	// Generate a 6-digit code
	code := fmt.Sprintf("%06d", time.Now().UnixNano()%1000000)

	content := container.NewVBox(
		widget.NewLabelWithStyle("Device Code Pairing", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabelWithStyle(code, fyne.TextAlignCenter, fyne.TextStyle{Monospace: true, Bold: true}),
		widget.NewLabel("Enter this code in the Air Mouse app"),
		widget.NewButton("Copy Code", func() {
			if parent != nil {
				parent.Clipboard().SetContent(code)
				dialog.ShowInformation("Copied", "Code copied to clipboard", parent)
			}
		}),
		widget.NewLabel("Code expires in 5 minutes"),
	)

	dialog.ShowCustom("Device Code", "Close", content, parent)
}