package ui

import (
    "fmt"
    "time"
    
    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/theme"
    "fyne.io/fyne/v2/widget"
    
    "airmouse-go/internal/device"
)

type DevicesTab struct {
    list        *widget.List
    devices     []*device.DeviceInfo
    deviceMgr   *device.Manager
    details     *widget.Label
    refreshBtn  *widget.Button
    disconnectBtn *widget.Button
    blockBtn    *widget.Button
    renameBtn   *widget.Button
    statusLabel *widget.Label
    searchEntry *widget.Entry
    filterSelect *widget.Select
    selectedID  string
}

func NewDevicesTab(deviceMgr *device.Manager) fyne.CanvasObject {
    tab := &DevicesTab{
        deviceMgr: deviceMgr,
        devices:   deviceMgr.GetAllDevices(),
        selectedID: "",
    }
    
    // Header
    header := container.NewHBox(
        widget.NewLabelWithStyle("📱 Device Management", fyne.TextAlignLeading, fyne.TextStyle{Bold: true}),
    )
    
    // Search and filter
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
    
    // Device list
    tab.list = widget.NewList(
        func() int { return len(tab.getFilteredDevices()) },
        func() fyne.CanvasObject {
            return container.NewHBox(
                widget.NewLabel(""),
                widget.NewLabel(""),
                widget.NewLabel(""),
                widget.NewLabel(""),
            )
        },
        func(id int, obj fyne.CanvasObject) {
            devices := tab.getFilteredDevices()
            if id < len(devices) {
                d := devices[id]
                hbox := obj.(*fyne.Container)
                if len(hbox.Objects) >= 4 {
                    statusIcon := hbox.Objects[0].(*widget.Label)
                    nameLabel := hbox.Objects[1].(*widget.Label)
                    typeLabel := hbox.Objects[2].(*widget.Label)
                    timeLabel := hbox.Objects[3].(*widget.Label)
                    
                    // Status icon
                    if time.Since(d.LastActive) < 10*time.Second {
                        statusIcon.SetText("🟢")
                    } else {
                        statusIcon.SetText("🟡")
                    }
                    
                    nameLabel.SetText(d.Name)
                    typeLabel.SetText(fmt.Sprintf("[%s]", d.Type))
                    timeLabel.SetText(formatDurationShort(time.Since(d.LastActive)))
                    
                    // Highlight selected
                    if d.ID == tab.selectedID {
                        nameLabel.TextStyle = fyne.TextStyle{Bold: true}
                    } else {
                        nameLabel.TextStyle = fyne.TextStyle{}
                    }
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
    
    // Buttons
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
    
    // Details panel
    tab.details = widget.NewLabel("Select a device to view details")
    tab.details.Wrapping = fyne.TextWrapWord
    
    // Stats panel
    statsPanel := tab.createStatsPanel()
    
    // Layout
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
    
    // Auto-refresh
    go func() {
        for {
            time.Sleep(2 * time.Second)
            fyne.Do(func() {
                tab.refresh()
            })
        }
    }()
    
    return container.NewBorder(header, tab.statusLabel, nil, nil, split)
}

func (t *DevicesTab) getFilteredDevices() []*device.DeviceInfo {
    filtered := make([]*device.DeviceInfo, 0)
    searchText := t.searchEntry.Text
    filterType := t.filterSelect.Selected
    
    for _, d := range t.devices {
        // Type filter
        if filterType != "All" && string(d.Type) != filterType {
            continue
        }
        
        // Search filter
        if searchText != "" {
            if !contains(d.Name, searchText) && !contains(string(d.Type), searchText) {
                continue
            }
        }
        
        filtered = append(filtered, d)
    }
    
    return filtered
}

func (t *DevicesTab) refresh() {
    t.devices = t.deviceMgr.GetAllDevices()
    t.list.Refresh()
    
    // Check if selected device still exists
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
            t.details.SetText("Device disconnected")
        }
    }
}

func (t *DevicesTab) showDeviceDetails(d *device.DeviceInfo) {
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
        formatDuration(uptime),
        formatDuration(idleTime),
        formatBytes(d.BytesSent),
        formatBytes(d.BytesRecv),
        formatBytes(d.BytesSent+d.BytesRecv),
    )
    
    t.details.SetText(details)
}

func (t *DevicesTab) getStatusText(d *device.DeviceInfo) string {
    if time.Since(d.LastActive) < 10*time.Second {
        return "🟢 Active"
    } else if time.Since(d.LastActive) < 60*time.Second {
        return "🟡 Idle"
    }
    return "🔴 Inactive"
}

func (t *DevicesTab) updateButtons(enabled bool) {
    t.disconnectBtn.SetEnabled(enabled)
    t.blockBtn.SetEnabled(enabled)
    t.renameBtn.SetEnabled(enabled)
}

func (t *DevicesTab) disconnectDevice() {
    dialog.ShowConfirm("Disconnect Device", 
        "Are you sure you want to disconnect this device?",
        func(confirmed bool) {
            if confirmed {
                t.deviceMgr.UnregisterDevice(t.selectedID)
                t.selectedID = ""
                t.refresh()
                t.statusLabel.SetText("✓ Device disconnected")
                t.details.SetText("Device disconnected")
                t.updateButtons(false)
            }
        },
        fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *DevicesTab) blockDevice() {
    dialog.ShowConfirm("Block Device", 
        "Block this device from connecting in the future?",
        func(confirmed bool) {
            if confirmed {
                t.deviceMgr.BlockDevice(t.selectedID)
                t.disconnectDevice()
                t.statusLabel.SetText("✓ Device blocked")
            }
        },
        fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *DevicesTab) renameDevice() {
    // Find device
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
    
    dialog.ShowCustomConfirm("Rename Device", "Save", "Cancel", content, func(confirmed bool) {
        if confirmed && nameEntry.Text != "" {
            t.deviceMgr.UpdateDeviceName(t.selectedID, nameEntry.Text)
            t.refresh()
            t.statusLabel.SetText(fmt.Sprintf("✓ Device renamed to: %s", nameEntry.Text))
        }
    }, fyne.CurrentApp().Driver().AllWindows()[0])
}

func (t *DevicesTab) createStatsPanel() fyne.CanvasObject {
    totalLabel := widget.NewLabel("Total: 0")
    activeLabel := widget.NewLabel("Active: 0")
    idleLabel := widget.NewLabel("Idle: 0")
    trafficLabel := widget.NewLabel("Traffic: 0 B")
    
    // Update stats
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
            
            fyne.Do(func() {
                totalLabel.SetText(fmt.Sprintf("📊 Total: %d", total))
                activeLabel.SetText(fmt.Sprintf("🟢 Active: %d", active))
                idleLabel.SetText(fmt.Sprintf("🟡 Idle: %d", idle))
                trafficLabel.SetText(fmt.Sprintf("📈 Traffic: %s", formatBytes(totalTraffic)))
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

// Helper functions
func contains(s, substr string) bool {
    return len(substr) == 0 || len(s) >= len(substr) && 
           (s == substr || len(s) > len(substr) && 
            (s[:len(substr)] == substr || s[len(s)-len(substr):] == substr ||
             len(s) > len(substr) && contains(s[1:], substr)))
}

func formatDuration(d time.Duration) string {
    hours := int(d.Hours())
    minutes := int(d.Minutes()) % 60
    seconds := int(d.Seconds()) % 60
    
    if hours > 0 {
        return fmt.Sprintf("%dh %dm %ds", hours, minutes, seconds)
    }
    if minutes > 0 {
        return fmt.Sprintf("%dm %ds", minutes, seconds)
    }
    return fmt.Sprintf("%ds", seconds)
}

func formatBytes(bytes int64) string {
    const unit = 1024
    if bytes < unit {
        return fmt.Sprintf("%d B", bytes)
    }
    div, exp := int64(unit), 0
    for n := bytes / unit; n >= unit; n /= unit {
        div *= unit
        exp++
    }
    return fmt.Sprintf("%.1f %cB", float64(bytes)/float64(div), "KMGTPE"[exp])
}