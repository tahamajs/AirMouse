package ui

import (
    "fmt"
    "time"
    
    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/widget"
    "airmouse-go/internal/device"
)

type DevicesTab struct {
    list      *widget.List
    devices   []*device.DeviceInfo
    deviceMgr *device.Manager
    details   *widget.Label
    refreshBtn *widget.Button
}

func NewDevicesTab(deviceMgr *device.Manager) fyne.CanvasObject {
    tab := &DevicesTab{
        deviceMgr: deviceMgr,
        devices:   deviceMgr.GetAllDevices(),
        details:   widget.NewLabel("Select a device to see details"),
    }
    
    // Create refresh button
    tab.refreshBtn = widget.NewButton("Refresh", func() {
        tab.refresh()
    })
    
    // Create list
    tab.list = widget.NewList(
        func() int { return len(tab.devices) },
        func() fyne.CanvasObject { 
            return container.NewHBox(
                widget.NewLabel(""),
                widget.NewLabel(""),
            )
        },
        func(id int, obj fyne.CanvasObject) {
            if id < len(tab.devices) {
                d := tab.devices[id]
                hbox := obj.(*fyne.Container)
                if len(hbox.Objects) >= 2 {
                    nameLabel := hbox.Objects[0].(*widget.Label)
                    typeLabel := hbox.Objects[1].(*widget.Label)
                    
                    nameLabel.SetText(fmt.Sprintf("%s", d.Name))
                    typeLabel.SetText(fmt.Sprintf(" [%s]", d.Type))
                }
            }
        },
    )
    
    tab.list.OnSelected = func(id int) {
        if id >= 0 && id < len(tab.devices) {
            d := tab.devices[id]
            tab.details.SetText(fmt.Sprintf(
                "━━━━━━━━━━━━━━━━━━━━━━━━\n"+
                "📱 DEVICE INFORMATION\n"+
                "━━━━━━━━━━━━━━━━━━━━━━━━\n\n"+
                "ID: %s\n"+
                "Name: %s\n"+
                "Type: %s\n"+
                "Connected: %s\n"+
                "Last Active: %s\n"+
                "━━━━━━━━━━━━━━━━━━━━━━━━\n"+
                "📊 STATISTICS\n"+
                "━━━━━━━━━━━━━━━━━━━━━━━━\n"+
                "Bytes Sent: %d\n"+
                "Bytes Received: %d\n"+
                "Uptime: %s\n",
                d.ID, d.Name, d.Type,
                d.ConnectedAt.Format("2006-01-02 15:04:05"),
                d.LastActive.Format("2006-01-02 15:04:05"),
                d.BytesSent, d.BytesRecv,
                formatDuration(time.Since(d.ConnectedAt)),
            ))
        }
    }
    
    // Auto-refresh every 2 seconds
    go func() {
        for {
            time.Sleep(2 * time.Second)
            fyne.Do(tab.refresh)
        }
    }()
    
    // Layout
    leftPanel := container.NewBorder(
        widget.NewLabelWithStyle("Connected Devices", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        tab.refreshBtn, nil, nil,
        container.NewScroll(tab.list),
    )
    
    rightPanel := container.NewBorder(
        widget.NewLabelWithStyle("Device Details", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        nil, nil, nil,
        container.NewScroll(tab.details),
    )
    
    split := container.NewHSplit(leftPanel, rightPanel)
    split.SetOffset(0.4)
    return split
}

func (t *DevicesTab) refresh() {
    t.devices = t.deviceMgr.GetAllDevices()
    t.list.Refresh()
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