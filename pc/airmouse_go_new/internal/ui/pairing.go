//go:build gui

package ui

import (
	"bytes"
	"image/png"
	"net/url"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/widget"
	qrcode "github.com/skip2/go-qrcode"

	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
)

// ShowPairingWizard displays a modal dialog with a QR code and instructions.
func ShowPairingWizard(parent fyne.Window, authMgr *auth.Manager, cfg *config.Config) {
	wsURL := getWebSocketURL(cfg)
	pairingData, err := authMgr.GetPairingQRData(wsURL)
	if err != nil {
		dialog.ShowError(err, parent)
		return
	}

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
		"1. Open the Air Mouse Android app\n" +
			"2. Tap the QR scanner icon\n" +
			"3. Scan this code\n" +
			"4. The app will automatically pair and connect",
	)
	instructions.Wrapping = fyne.TextWrapWord

	content := container.NewVBox(
		widget.NewLabelWithStyle("Pair a New Device", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		qrImage,
		instructions,
		widget.NewLabel("Server: "+wsURL),
	)

	dialog.ShowCustom("Pairing Wizard", "Close", content, parent)
}

func getWebSocketURL(cfg *config.Config) string {
	ip := getLocalIP()
	return url.URL{
		Scheme: "ws",
		Host:   ip + ":" + cfg.Port,
		Path:   "/ws",
	}.String()
}
