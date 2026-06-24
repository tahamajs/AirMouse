package ui

import (
	"fmt"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/config"
	"airmouse-go/internal/protocol"
	"airmouse-go/internal/utils"
)

func NewProtocolGuideTab(cfg *config.Config, server *protocol.ProtocolServer) fyne.CanvasObject {
	if cfg == nil {
		return widget.NewLabel("Network protocol guide unavailable")
	}
	// Use a simple function to build content; avoid any blocking calls.
	return container.NewScroll(buildProtocolGuideContent(cfg, server))
}

func buildProtocolGuideContent(cfg *config.Config, server *protocol.ProtocolServer) fyne.CanvasObject {
	// Safely check if server is running without potential deadlock
	running := false
	if server != nil {
		// Non‑blocking check – assume false if any issue
		defer func() {
			if r := recover(); r != nil {
				// If panic, treat as not running
			}
		}()
		running = server.IsRunning()
	}

	statusText := "Stopped"
	if running {
		statusText = "Running"
	}

	// Build content without using markdown (which might cause issues)
	return container.NewVBox(
		widget.NewLabelWithStyle("Network Protocol", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		widget.NewLabel("This page explains how the Android app and the Go server wait for approval, approve the session, and then connect."),
		widget.NewSeparator(),
		widget.NewLabel(fmt.Sprintf("Server status: %s", statusText)),
		widget.NewLabel(fmt.Sprintf("TCP port: %d", cfg.Port)),
		widget.NewLabel(fmt.Sprintf("WebSocket port: %d", cfg.WebSocketPort)),
		widget.NewLabel(fmt.Sprintf("UDP discovery port: %d", cfg.UDPPort)),
		widget.NewSeparator(),
		widget.NewLabelWithStyle("Handshake", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		widget.NewLabel("1. Android connects over WebSocket or TCP and waits for approval."),
		widget.NewLabel("2. Android sends hello with device name, app version, and pairing token."),
		widget.NewLabel("3. The server panel shows the device as pending until you tap Approve, then Go replies with welcome."),
		widget.NewLabel("4. Cursor, click, and scroll messages begin after approval and the session becomes connected."),
		widget.NewSeparator(),
		widget.NewLabelWithStyle("Message Rules", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		widget.NewLabel("• move is best effort and not ACKed."),
		widget.NewLabel("• click, doubleclick, rightclick, and scroll are reliable and ACKed."),
		widget.NewLabel("• Android retries reliable packets when ACKs do not arrive in time."),
		widget.NewLabel("• Go accepts both flat JSON and payload-wrapped JSON for compatibility."),
		widget.NewLabel("• The server also accepts DeltaX / DeltaY and Scroll aliases."),
		widget.NewSeparator(),
		widget.NewLabelWithStyle("Assignment Summary", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
		widget.NewLabel("The goal is a smooth, low-latency cursor flow with approval, discovery, and ACK-based reliability."),
		widget.NewLabel(fmt.Sprintf("Current local IP: %s", utils.GetLocalIP())),
	)
}