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
}

func NewDevicesTab(deviceMgr *device.Manager) fyne.CanvasObject {
	tab := &DevicesTab{
		deviceMgr: deviceMgr,
		devices:   deviceMgr.GetAllDevices(),
	}
	tab.list = widget.NewList(
		func() int { return len(tab.devices) },
		func() fyne.CanvasObject { return widget.NewLabel("template") },
		func(id int, obj fyne.CanvasObject) {
			if id < len(tab.devices) {
				d := tab.devices[id]
				obj.(*widget.Label).SetText(fmt.Sprintf("%s | %s | Connected: %s",
					d.Name, d.Type, d.ConnectedAt.Format("15:04:05")))
			}
		},
	)

	go func() {
		for {
			time.Sleep(2 * time.Second)
			tab.devices = deviceMgr.GetAllDevices()
			tab.list.Refresh()
		}
	}()

	return container.NewBorder(
		widget.NewLabelWithStyle("Connected Devices", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
		nil, nil, nil,
		tab.list,
	)
}