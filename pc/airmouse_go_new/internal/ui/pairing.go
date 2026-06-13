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

// ShowPairingWizard displays a comprehensive pairing wizard with multiple methods
func ShowPairingWizard(parent fyne.Window, wsURL string) {
    cfg := config.Get()
    ip := utils.GetLocalIP()
    port := cfg.Port
    
    // Create pairing data with full device info
    pairingData := fmt.Sprintf("airmouse://pair?ip=%s&port=%d&name=%s&version=3.0&type=mobile",
        ip, port, url.QueryEscape(cfg.ServerName))
    
    // Generate QR code
    pngBytes, err := qrcode.Encode(pairingData, qrcode.High, 300)
    if err != nil {
        dialog.ShowError(err, parent)
        return
    }
    img, err := png.Decode(bytes.NewReader(pngBytes))
    if err != nil {
        dialog.ShowError(err, parent)
        return
    }
    
    qrImage := canvas.NewImageFromImage(img)
    qrImage.FillMode = canvas.ImageFillOriginal
    
    // Instructions
    instructions := widget.NewLabel(
        "📱 **How to pair your device**\n\n" +
        "**Method 1: QR Code**\n" +
        "1. Open the Air Mouse Android app\n" +
        "2. Tap the QR scanner icon in the top right\n" +
        "3. Scan this QR code\n" +
        "4. The app will automatically connect\n\n" +
        "**Method 2: Manual Entry**\n" +
        "1. Open Air Mouse app\n" +
        "2. Tap 'Manual Connection'\n" +
        "3. Enter the IP address and port below\n" +
        "4. Tap 'Connect'\n\n" +
        "**Method 3: UDP Discovery**\n" +
        "1. Make sure both devices are on the same network\n" +
        "2. The app will automatically discover the server\n" +
        "3. Tap on the discovered server to connect")
    instructions.Wrapping = fyne.TextWrapWord
    
    // Server info
    serverInfo := widget.NewLabel(fmt.Sprintf(
        "**Server Information**\n\n"+
        "Server Name: %s\n"+
        "IP Address: %s\n"+
        "TCP Port: %d\n"+
        "WebSocket Port: %d\n"+
        "UDP Discovery Port: %d\n"+
        "Version: 3.0.0",
        cfg.ServerName, ip, cfg.Port, cfg.WebSocketPort, cfg.UDPPort))
    serverInfo.Wrapping = fyne.TextWrapWord
    
    // Copy buttons
    copyIPBtn := widget.NewButtonWithIcon("Copy IP", theme.ContentCopyIcon(), func() {
        parent.Clipboard().SetContent(ip)
        dialog.ShowInformation("Copied", "IP address copied to clipboard", parent)
    })
    
    copyURLBtn := widget.NewButtonWithIcon("Copy URL", theme.ContentCopyIcon(), func() {
        fullURL := fmt.Sprintf("airmouse://%s:%d", ip, port)
        parent.Clipboard().SetContent(fullURL)
        dialog.ShowInformation("Copied", "Connection URL copied to clipboard", parent)
    })
    
    // Tabs for different pairing methods
    qrTab := container.NewVBox(
        widget.NewLabelWithStyle("Scan QR Code", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        qrImage,
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
    
    // Main content
    content := container.NewVBox(
        widget.NewLabelWithStyle("🔗 Pair New Device", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        tabs,
        widget.NewSeparator(),
        serverInfo,
    )
    
    dialog.ShowCustom("Pairing Wizard", "Close", container.NewScroll(content), parent)
}

// QuickPairDialog shows a simplified pairing dialog
func QuickPairDialog(parent fyne.Window) {
    cfg := config.Get()
    ip := utils.GetLocalIP()
    
    pairingData := fmt.Sprintf("airmouse://pair?ip=%s&port=%d&name=%s", ip, cfg.Port, cfg.ServerName)
    pngBytes, _ := qrcode.Encode(pairingData, qrcode.Medium, 200)
    img, _ := png.Decode(bytes.NewReader(pngBytes))
    
    qrImage := canvas.NewImageFromImage(img)
    qrImage.FillMode = canvas.ImageFillOriginal
    qrImage.SetMinSize(fyne.NewSize(200, 200))
    
    content := container.NewVBox(
        widget.NewLabelWithStyle("Quick Pair", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        qrImage,
        widget.NewLabel(fmt.Sprintf("IP: %s:%d", ip, cfg.Port)),
        widget.NewButton("Close", func() { parent.Hide() }),
    )
    
    dialog.ShowCustom("Quick Pair", "Close", content, parent)
}

// ShowDeviceCodePairing displays a device code for pairing (alternative to QR)
func ShowDeviceCodePairing(parent fyne.Window) {
    // Generate a 6-digit code
    code := fmt.Sprintf("%06d", time.Now().UnixNano()%1000000)
    
    content := container.NewVBox(
        widget.NewLabelWithStyle("Device Code Pairing", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        widget.NewLabelWithStyle(code, fyne.TextAlignCenter, fyne.TextStyle{Monospace: true, Bold: true}),
        widget.NewLabel("Enter this code in the Air Mouse app"),
        widget.NewButton("Copy Code", func() {
            parent.Clipboard().SetContent(code)
            dialog.ShowInformation("Copied", "Code copied to clipboard", parent)
        }),
        widget.NewLabel("Code expires in 5 minutes"),
    )
    
    dialog.ShowCustom("Device Code", "Close", content, parent)
}