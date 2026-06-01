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
}

func NewDevicesTab(deviceMgr *device.Manager) fyne.CanvasObject {
    tab := &DevicesTab{
        deviceMgr: deviceMgr,
        devices:   deviceMgr.GetAllDevices(),
        details:   widget.NewLabel("Select a device to see details"),
    }
    tab.list = widget.NewList(
        func() int { return len(tab.devices) },
        func() fyne.CanvasObject { return widget.NewLabel("template") },
        func(id int, obj fyne.CanvasObject) {
            if id < len(tab.devices) {
                d := tab.devices[id]
                obj.(*widget.Label).SetText(fmt.Sprintf("%s | %s | Connected: %s", d.Name, d.Type, d.ConnectedAt.Format("15:04:05")))
            }
        },
    )
    tab.list.OnSelected = func(id int) {
        if id >= 0 && id < len(tab.devices) {
            d := tab.devices[id]
            tab.details.SetText(fmt.Sprintf(
                "ID: %s\nName: %s\nType: %s\nConnected: %s\nLast active: %s\nBytes sent: %d\nBytes recv: %d",
                d.ID, d.Name, d.Type, d.ConnectedAt.Format("2006-01-02 15:04:05"),
                d.LastActive.Format("2006-01-02 15:04:05"), d.BytesSent, d.BytesRecv,
            ))
        }
    }

    // Auto‑refresh list every 2 seconds
    go func() {
        for {
            time.Sleep(2 * time.Second)
            devs := deviceMgr.GetAllDevices()
            fyne.Do(func() {
                tab.devices = devs
                tab.list.Refresh()
            })
        }
    }()

    split := container.NewHSplit(
        container.NewBorder(nil, nil, nil, nil, tab.list),
        container.NewBorder(nil, nil, nil, nil, tab.details),
    )
    split.SetOffset(0.6)
    return container.NewBorder(
        widget.NewLabelWithStyle("Connected Devices", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        nil, nil, nil,
        split,
    )
}