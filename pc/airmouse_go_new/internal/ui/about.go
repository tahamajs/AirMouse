package ui

import (
	"fmt"
	"runtime"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/canvas"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"
)

// ShowAboutDialog displays the about dialog with application information.
func ShowAboutDialog(parent fyne.Window) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}

	// Build information
	buildTime := "2025-01-15"
	goVersion := runtime.Version()
	osInfo := fmt.Sprintf("%s (%s)", runtime.GOOS, runtime.GOARCH)

	// Credits
	credits := []string{
		"Project Lead: Taha Majd",
		"Android Development: Taha Majd",
		"Go Server: Taha Majd",
		"AI/ML Integration: Taha Majd",
		"UI/UX Design: Taha Majd",
		"Testing & QA: Taha Majd",
	}

	// Features list
	features := []string{
		"✓ Real-time cursor control",
		"✓ Multi-protocol support (TCP/WebSocket/UDP)",
		"✓ AI-powered gesture recognition",
		"✓ Proximity-based auto-lock/unlock",
		"✓ Personalization & learning",
		"✓ Cross-platform (Windows/Linux/macOS)",
		"✓ Modern Fyne GUI",
		"✓ QR code pairing",
		"✓ Bluetooth HID support",
		"✓ USB gadget support",
	}

	// Logo placeholder
	logo := canvas.NewRectangle(theme.PrimaryColor())
	logo.SetMinSize(fyne.NewSize(100, 100))
	logo.CornerRadius = 50

	// Version info
	versionLabel := widget.NewLabelWithStyle("Version 3.0.0", fyne.TextAlignCenter, fyne.TextStyle{Bold: true})

	// Build info
	buildInfo := container.NewVBox(
		widget.NewLabelWithStyle("Build Information", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel(fmt.Sprintf("Build Date: %s", buildTime)),
		widget.NewLabel(fmt.Sprintf("Go Version: %s", goVersion)),
		widget.NewLabel(fmt.Sprintf("Platform: %s", osInfo)),
	)

	// Features
	featuresText := "Key Features:\n\n"
	for _, f := range features {
		featuresText += f + "\n"
	}
	featuresList := widget.NewLabel(featuresText)

	// Credits
	creditsText := "Credits:\n\n"
	for _, c := range credits {
		creditsText += "• " + c + "\n"
	}
	creditsList := widget.NewLabel(creditsText)

	// License info
	licenseInfo := widget.NewLabel(
		"© 2025 University of Tehran - Embedded Systems Laboratory\n\n" +
			"This software is licensed under the MIT License.\n" +
			"See LICENSE file for full details.",
	)
	licenseInfo.Wrapping = fyne.TextWrapWord

	// Links
	githubBtn := widget.NewButtonWithIcon("GitHub Repository", theme.InfoIcon(), func() {
		dialog.ShowInformation("GitHub", "https://github.com/yourusername/airmouse-go", parent)
	})
	docsBtn := widget.NewButtonWithIcon("Documentation", theme.DocumentIcon(), func() {
		dialog.ShowInformation("Documentation", "Full documentation available at docs.airmouse.io", parent)
	})

	// Main content layout
	content := container.NewVBox(
		container.NewCenter(logo),
		widget.NewLabelWithStyle("Air Mouse Pro Server", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		versionLabel,
		widget.NewLabelWithStyle("Turn your phone into a wireless mouse", fyne.TextAlignCenter, fyne.TextStyle{Italic: true}),
		widget.NewSeparator(),

		// Two-column layout for features and credits
		container.NewGridWithColumns(2,
			featuresList,
			creditsList,
		),

		widget.NewSeparator(),
		buildInfo,
		widget.NewSeparator(),
		licenseInfo,
		widget.NewSeparator(),
		container.NewHBox(githubBtn, docsBtn),

		widget.NewLabel("\nSpecial thanks to all contributors and open-source projects that made this possible."),

		// Footer with dynamic copyright year
		widget.NewLabel(fmt.Sprintf("© %d Air Mouse Project", time.Now().Year())),
	)

	dialog.ShowCustom("About Air Mouse Pro", "Close", container.NewScroll(content), parent)
}

// ShowSystemInfoDialog displays system information.
func ShowSystemInfoDialog(parent fyne.Window) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	var memStats runtime.MemStats
	runtime.ReadMemStats(&memStats)

	content := container.NewVBox(
		widget.NewLabelWithStyle("System Information", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel(fmt.Sprintf("OS: %s", runtime.GOOS)),
		widget.NewLabel(fmt.Sprintf("Architecture: %s", runtime.GOARCH)),
		widget.NewLabel(fmt.Sprintf("CPUs: %d", runtime.NumCPU())),
		widget.NewLabel(fmt.Sprintf("Go Version: %s", runtime.Version())),
		widget.NewLabel(fmt.Sprintf("Memory Usage: %.2f MB", float64(memStats.Alloc)/1024/1024)),
		widget.NewLabel(fmt.Sprintf("Total Allocated: %.2f MB", float64(memStats.TotalAlloc)/1024/1024)),
		widget.NewLabel(fmt.Sprintf("Goroutines: %d", runtime.NumGoroutine())),
	)

	dialog.ShowCustom("System Information", "Close", content, parent)
}

// ShowContributorsDialog displays contributor information.
func ShowContributorsDialog(parent fyne.Window) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	contributors := []struct {
		Name    string
		Role    string
		Contact string
	}{
		{"Taha Majd", "Lead Developer", "taha@airmouse.io"},
		{"University of Tehran", "Research Partner", "embedded@ut.ac.ir"},
		{"Open Source Community", "Contributors", "github.com/airmouse"},
	}

	var contributorWidgets []fyne.CanvasObject
	for _, c := range contributors {
		contributorWidgets = append(contributorWidgets,
			widget.NewLabelWithStyle(c.Name, fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
			widget.NewLabel(c.Role),
			widget.NewLabel(c.Contact),
			widget.NewSeparator(),
		)
	}

	content := container.NewVBox(contributorWidgets...)
	dialog.ShowCustom("Contributors", "Close", content, parent)
}

// ShowLicensesDialog displays open source licenses.
func ShowLicensesDialog(parent fyne.Window) {
	if parent == nil {
		parent = getCurrentWindow()
		if parent == nil {
			return
		}
	}
	licenses := map[string]string{
		"fyne.io/fyne/v2":    "BSD 3-Clause License",
		"gorilla/websocket":  "BSD 2-Clause License",
		"go-vgo/robotgo":     "MIT License",
		"skip2/go-qrcode":    "MIT License",
		"golang-jwt/jwt":     "MIT License",
		"gonum/org/v1/gonum": "BSD 3-Clause License",
	}

	var licenseWidgets []fyne.CanvasObject
	licenseWidgets = append(licenseWidgets, widget.NewLabelWithStyle("Third-Party Licenses", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}))
	licenseWidgets = append(licenseWidgets, widget.NewSeparator())

	for lib, license := range licenses {
		licenseWidgets = append(licenseWidgets,
			widget.NewLabel(fmt.Sprintf("• %s", lib)),
			widget.NewLabel(fmt.Sprintf("  %s", license)),
			widget.NewSeparator(),
		)
	}

	content := container.NewVBox(licenseWidgets...)
	dialog.ShowCustom("Open Source Licenses", "Close", container.NewScroll(content), parent)
}