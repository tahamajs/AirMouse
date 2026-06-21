package ui

import (
	"fmt"
	"time"

	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/container"
	"fyne.io/fyne/v2/dialog"
	"fyne.io/fyne/v2/theme"
	"fyne.io/fyne/v2/widget"

	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	"airmouse-go/internal/utils"
)

// ------------------------------------------------------------
// DevicesTab – Device management tab
// ------------------------------------------------------------

type DevicesTab struct {
	list          *widget.List
	devices       []*device.DeviceInfo
	deviceMgr     *device.Manager
	details       *widget.Label
	refreshBtn    *widget.Button
	disconnectBtn *widget.Button
	blockBtn      *widget.Button
	renameBtn     *widget.Button
	statusLabel   *widget.Label
	searchEntry   *widget.Entry
	filterSelect  *widget.Select
	selectedID    string
}

// NewDevicesTab creates a new devices management tab.
func NewDevicesTab(deviceMgr *device.Manager) fyne.CanvasObject {
	if deviceMgr == nil {
		return widget.NewLabelWithStyle("⚠️ Device Manager unavailable",
			fyne.TextAlignCenter, fyne.TextStyle{Bold: true})
	}

	tab := &DevicesTab{
		deviceMgr:  deviceMgr,
		devices:    deviceMgr.GetAllDevices(),
		selectedID: "",
	}

	// ----- Header -----
	header := container.NewHBox(
		widget.NewLabelWithStyle("📱 Device Management", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
	)

	// ----- Search & Filter -----
	tab.searchEntry = widget.NewEntry()
	tab.searchEntry.SetPlaceHolder("Search devices...")
	tab.searchEntry.OnChanged = func(s string) {
		tab.refresh()
	}

	filterOptions := []string{"All", "TCP", "WebSocket", "UDP", "Bluetooth", "USB"}
	tab.filterSelect = widget.NewSelect(filterOptions, func(s string) {
		tab.refresh()
	})
	tab.filterSelect.SetSelected("All")

	// ----- Device List -----
	tab.list = widget.NewList(
		func() int { return len(tab.getFilteredDevices()) },
		func() fyne.CanvasObject {
			return container.NewHBox(
				widget.NewLabel(""), // status icon
				widget.NewLabel(""), // name
				widget.NewLabel(""), // type
				widget.NewLabel(""), // time
				widget.NewButton("Pair", func() {}),
			)
		},
		func(id int, obj fyne.CanvasObject) {
			devices := tab.getFilteredDevices()
			if id < 0 || id >= len(devices) {
				return
			}
			d := devices[id]
			hbox := obj.(*fyne.Container)
			if len(hbox.Objects) >= 5 {
				statusIcon := hbox.Objects[0].(*widget.Label)
				nameLabel := hbox.Objects[1].(*widget.Label)
				typeLabel := hbox.Objects[2].(*widget.Label)
				timeLabel := hbox.Objects[3].(*widget.Label)
				pairBtn := hbox.Objects[4].(*widget.Button)

				if time.Since(d.LastActive) < 10*time.Second {
					statusIcon.SetText("🟢")
				} else {
					statusIcon.SetText("🟡")
				}

				nameLabel.SetText(d.Name)
				typeLabel.SetText(fmt.Sprintf("[%s]", d.Type))
				timeLabel.SetText(FormatDurationShort(time.Since(d.LastActive)))

				if d.ID == tab.selectedID {
					nameLabel.TextStyle = fyne.TextStyle{Bold: true}
				} else {
					nameLabel.TextStyle = fyne.TextStyle{}
				}

				pairBtn.SetText("Pair")
				pairBtn.OnTapped = func() {
					tab.selectedID = d.ID
					tab.showPairingForDevice(d)
				}
			}
		},
	)

	tab.list.OnSelected = func(id int) {
		devices := tab.getFilteredDevices()
		if id >= 0 && id < len(devices) {
			d := devices[id]
			tab.selectedID = d.ID
			tab.showDeviceDetails(d)
			tab.updateButtons(true)
		}
	}

	// ----- Buttons -----
	tab.refreshBtn = widget.NewButtonWithIcon("Refresh", theme.ViewRefreshIcon(), func() {
		tab.refresh()
	})

	tab.disconnectBtn = widget.NewButtonWithIcon("Disconnect", theme.CancelIcon(), func() {
		if tab.selectedID != "" {
			tab.disconnectDevice()
		}
	})
	tab.disconnectBtn.Disable()

	tab.blockBtn = widget.NewButtonWithIcon("Block", theme.ErrorIcon(), func() {
		if tab.selectedID != "" {
			tab.blockDevice()
		}
	})
	tab.blockBtn.Disable()

	tab.renameBtn = widget.NewButtonWithIcon("Rename", theme.DocumentCreateIcon(), func() {
		if tab.selectedID != "" {
			tab.renameDevice()
		}
	})
	tab.renameBtn.Disable()

	tab.statusLabel = widget.NewLabel("")

	// ----- Details Panel -----
	tab.details = widget.NewLabel("Select a device to view details")
	tab.details.Wrapping = fyne.TextWrapWord

	// ----- Statistics Panel -----
	statsPanel := tab.createStatsPanel()

	// ----- Layout -----
	toolbar := container.NewHBox(
		widget.NewLabel("🔍"), tab.searchEntry,
		widget.NewLabel("Type:"), tab.filterSelect,
		tab.refreshBtn,
	)

	buttonBar := container.NewHBox(
		tab.disconnectBtn,
		tab.blockBtn,
		tab.renameBtn,
	)

	leftPanel := container.NewBorder(
		toolbar,
		buttonBar,
		nil, nil,
		container.NewScroll(tab.list),
	)

	rightPanel := container.NewBorder(
		widget.NewLabelWithStyle("Device Details", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		statsPanel,
		nil, nil,
		container.NewScroll(tab.details),
	)

	split := container.NewHSplit(leftPanel, rightPanel)
	split.SetOffset(0.5)

	// ----- Auto‑refresh (every 2 seconds) -----
	go func() {
		for {
			time.Sleep(2 * time.Second)
			RunOnMain(func() {
				tab.refresh()
			})
		}
	}()

	return container.NewBorder(header, tab.statusLabel, nil, nil, split)
}

// getFilteredDevices returns devices filtered by search text and type.
func (t *DevicesTab) getFilteredDevices() []*device.DeviceInfo {
	filtered := make([]*device.DeviceInfo, 0)
	searchText := t.searchEntry.Text
	filterType := t.filterSelect.Selected

	for _, d := range t.devices {
		typeStr := string(d.Type)
		if filterType != "All" && typeStr != filterType {
			continue
		}
		if searchText != "" {
			if !containsIgnoreCase(d.Name, searchText) && !containsIgnoreCase(typeStr, searchText) {
				continue
			}
		}
		filtered = append(filtered, d)
	}
	return filtered
}

// refresh updates the device list and UI state.
func (t *DevicesTab) refresh() {
	if t.list == nil {
		return
	}
	t.devices = t.deviceMgr.GetAllDevices()
	t.list.Refresh()

	if t.selectedID != "" {
		exists := false
		for _, d := range t.devices {
			if d.ID == t.selectedID {
				exists = true
				break
			}
		}
		if !exists {
			t.selectedID = ""
			t.updateButtons(false)
			t.details.SetText("Waiting for approval")
		}
	}
}

// showDeviceDetails populates the details panel.
func (t *DevicesTab) showDeviceDetails(d *device.DeviceInfo) {
	if d == nil || t.details == nil {
		return
	}

	uptime := time.Since(d.ConnectedAt)
	idleTime := time.Since(d.LastActive)

	details := fmt.Sprintf(
		"━━━━━━━━━━━━━━━━━━━━━━━━\n"+
			"📱 DEVICE INFORMATION\n"+
			"━━━━━━━━━━━━━━━━━━━━━━━━\n\n"+
			"ID: %s\n"+
			"Name: %s\n"+
			"Type: %s\n"+
			"Status: %s\n\n"+
			"━━━━━━━━━━━━━━━━━━━━━━━━\n"+
			"⏱️ TIMING\n"+
			"━━━━━━━━━━━━━━━━━━━━━━━━\n"+
			"Connected: %s\n"+
			"Last Active: %s\n"+
			"Uptime: %s\n"+
			"Idle: %s\n\n"+
			"━━━━━━━━━━━━━━━━━━━━━━━━\n"+
			"📊 STATISTICS\n"+
			"━━━━━━━━━━━━━━━━━━━━━━━━\n"+
			"Bytes Sent: %s\n"+
			"Bytes Received: %s\n"+
			"Total Traffic: %s\n",
		d.ID,
		d.Name,
		d.Type,
		t.getStatusText(d),
		d.ConnectedAt.Format("2006-01-02 15:04:05"),
		d.LastActive.Format("2006-01-02 15:04:05"),
		FormatDuration(uptime),
		FormatDuration(idleTime),
		FormatBytes(d.BytesSent),
		FormatBytes(d.BytesRecv),
		FormatBytes(d.BytesSent+d.BytesRecv),
	)
	t.details.SetText(details)
}

func (t *DevicesTab) showPairingForDevice(d *device.DeviceInfo) {
	win := getCurrentWindow()
	if win == nil || d == nil {
		return
	}

	content := container.NewVBox(
		widget.NewLabelWithStyle("Pair Device", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		widget.NewLabel(fmt.Sprintf("Name: %s", d.Name)),
		widget.NewLabel(fmt.Sprintf("Type: %s", d.Type)),
		widget.NewLabel(fmt.Sprintf("ID: %s", d.ID)),
		widget.NewLabel("This opens the pairing wizard using the current server QR / WebSocket endpoint."),
		widget.NewButtonWithIcon("Open Pairing Wizard", theme.ConfirmIcon(), func() {
			ShowPairingWizard(win, fmt.Sprintf("ws://%s:%d/ws", utils.GetLocalIP(), config.Get().WebSocketPort))
		}),
	)

	dialog.ShowCustom("Pair Device", "Close", content, win)
}

// getStatusText returns a status string with emoji.
func (t *DevicesTab) getStatusText(d *device.DeviceInfo) string {
	if d == nil {
		return "Unknown"
	}
	if time.Since(d.LastActive) < 10*time.Second {
		return "🟢 Active"
	} else if time.Since(d.LastActive) < 60*time.Second {
		return "🟡 Idle"
	}
	return "🔴 Inactive"
}

// updateButtons enables/disables action buttons.
func (t *DevicesTab) updateButtons(enabled bool) {
	if t.disconnectBtn == nil || t.blockBtn == nil || t.renameBtn == nil {
		return
	}
	if enabled {
		t.disconnectBtn.Enable()
		t.blockBtn.Enable()
		t.renameBtn.Enable()
	} else {
		t.disconnectBtn.Disable()
		t.blockBtn.Disable()
		t.renameBtn.Disable()
	}
}

// disconnectDevice shows confirmation and disconnects.
func (t *DevicesTab) disconnectDevice() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	if t.selectedID == "" {
		return
	}
	dialog.ShowConfirm("Disconnect Device",
		"Are you sure you want to disconnect this device?",
		func(confirmed bool) {
			if confirmed {
				_ = t.deviceMgr.UnregisterDevice(t.selectedID)
				t.selectedID = ""
				t.refresh()
				t.statusLabel.SetText("Waiting for approval")
				t.details.SetText("Waiting for approval")
				t.updateButtons(false)
			}
		},
		win)
}

// blockDevice blocks the selected device.
func (t *DevicesTab) blockDevice() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	if t.selectedID == "" {
		return
	}
	dialog.ShowConfirm("Block Device",
		"Block this device from connecting in the future?",
		func(confirmed bool) {
			if confirmed {
				_ = t.deviceMgr.BlockDevice(t.selectedID)
				t.disconnectDevice()
				t.statusLabel.SetText("✓ Device blocked")
			}
		},
		win)
}

// renameDevice shows a rename dialog.
func (t *DevicesTab) renameDevice() {
	win := getCurrentWindow()
	if win == nil {
		return
	}
	if t.selectedID == "" {
		return
	}
	var targetDevice *device.DeviceInfo
	for _, d := range t.devices {
		if d.ID == t.selectedID {
			targetDevice = d
			break
		}
	}
	if targetDevice == nil {
		return
	}

	nameEntry := widget.NewEntry()
	nameEntry.SetText(targetDevice.Name)
	nameEntry.SetPlaceHolder("Enter new device name")

	content := container.NewVBox(
		widget.NewLabel("New Device Name:"),
		nameEntry,
	)

	dialog.ShowCustomConfirm("Rename Device", "Save", "Cancel", content,
		func(confirmed bool) {
			if confirmed && nameEntry.Text != "" {
				_ = t.deviceMgr.UpdateDeviceName(t.selectedID, nameEntry.Text)
				t.refresh()
				t.statusLabel.SetText(fmt.Sprintf("✓ Device renamed to: %s", nameEntry.Text))
			}
		},
		win)
}

// createStatsPanel returns a live statistics panel.
func (t *DevicesTab) createStatsPanel() fyne.CanvasObject {
	totalLabel := widget.NewLabel("Total: 0")
	activeLabel := widget.NewLabel("Active: 0")
	idleLabel := widget.NewLabel("Idle: 0")
	trafficLabel := widget.NewLabel("Traffic: 0 B")

	go func() {
		for {
			time.Sleep(1 * time.Second)
			devices := t.deviceMgr.GetAllDevices()
			total := len(devices)
			active := 0
			idle := 0
			var totalTraffic int64

			for _, d := range devices {
				if time.Since(d.LastActive) < 10*time.Second {
					active++
				} else if time.Since(d.LastActive) < 60*time.Second {
					idle++
				}
				totalTraffic += d.BytesSent + d.BytesRecv
			}

			RunOnMain(func() {
				totalLabel.SetText(fmt.Sprintf("📊 Total: %d", total))
				activeLabel.SetText(fmt.Sprintf("🟢 Active: %d", active))
				idleLabel.SetText(fmt.Sprintf("🟡 Idle: %d", idle))
				trafficLabel.SetText(fmt.Sprintf("📈 Traffic: %s", FormatBytes(totalTraffic)))
			})
		}
	}()

	return container.NewVBox(
		widget.NewLabelWithStyle("Statistics", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		widget.NewSeparator(),
		totalLabel,
		activeLabel,
		idleLabel,
		trafficLabel,
	)
}
