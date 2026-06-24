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

// ------------------------------------------------------------
// QuickPairDialog – simplified pairing
// ------------------------------------------------------------

// QuickPairDialog shows a simplified pairing dialog.

// ------------------------------------------------------------
// ShowDeviceCodePairing – device code pairing
// ------------------------------------------------------------

// ShowDeviceCodePairing displays a device code for pairing (alternative to QR).
