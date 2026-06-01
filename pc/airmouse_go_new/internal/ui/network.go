package ui

import (
    "bytes"
    "fmt"
    "image/png"
    "net"
    "strconv"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/canvas"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/widget"
    qrcode "github.com/skip2/go-qrcode"

    "airmouse-go/internal/config"
    "airmouse-go/internal/utils"
)

type NetworkTab struct {
    ipEntry   *widget.Entry
    portEntry *widget.Entry
    qrImage   *canvas.Image
    ipList    *widget.List
    ipData    []string
}

func NewNetworkTab(cfg *config.Config) fyne.CanvasObject {
    tab := &NetworkTab{
        ipData: getAllLocalIPs(),
    }
    tab.ipEntry = widget.NewEntry()
    tab.ipEntry.SetPlaceHolder("IP Address")
    tab.ipEntry.Text = utils.GetLocalIP()
    tab.portEntry = widget.NewEntry()
    tab.portEntry.SetPlaceHolder("Port")
    tab.portEntry.Text = strconv.Itoa(cfg.Port)

    tab.ipList = widget.NewList(
        func() int { return len(tab.ipData) },
        func() fyne.CanvasObject { return widget.NewLabel("") },
        func(id int, obj fyne.CanvasObject) {
            obj.(*widget.Label).SetText(tab.ipData[id])
        },
    )
    tab.ipList.OnSelected = func(id int) {
        if id >= 0 && id < len(tab.ipData) {
            tab.ipEntry.SetText(tab.ipData[id])
            tab.updateQR()
        }
    }

    refreshBtn := widget.NewButton("Refresh IPs", func() {
        tab.ipData = getAllLocalIPs()
        tab.ipList.Refresh()
    })
    copyBtn := widget.NewButton("Copy Endpoint", func() {
        endpoint := fmt.Sprintf("airmouse://%s:%s", tab.ipEntry.Text, tab.portEntry.Text)
        fyne.CurrentApp().Driver().AllWindows()[0].Clipboard().SetContent(endpoint)
        dialog.ShowInformation("Copied", "Endpoint copied to clipboard", fyne.CurrentApp().Driver().AllWindows()[0])
    })

    tab.qrImage = canvas.NewImageFromResource(nil)
    tab.qrImage.FillMode = canvas.ImageFillOriginal
    genQrBtn := widget.NewButton("Generate QR", tab.updateQR)
    saveQrBtn := widget.NewButton("Save QR", func() {
        dialog.ShowFileSave(func(writer fyne.URIWriteCloser, err error) {
            if err == nil && writer != nil {
                defer writer.Close()
                data := fmt.Sprintf("airmouse://%s:%s", tab.ipEntry.Text, tab.portEntry.Text)
                pngBytes, _ := qrcode.Encode(data, qrcode.High, 220)
                writer.Write(pngBytes)
            }
        }, fyne.CurrentApp().Driver().AllWindows()[0])
    })

    tab.ipEntry.OnChanged = func(string) { tab.updateQR() }
    tab.portEntry.OnChanged = func(string) { tab.updateQR() }

    return container.NewVBox(
        widget.NewLabelWithStyle("Network Configuration", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        widget.NewLabel("Available IP addresses:"),
        container.NewScroll(tab.ipList),
        tab.ipEntry,
        tab.portEntry,
        container.NewHBox(refreshBtn, copyBtn),
        container.NewHBox(genQrBtn, saveQrBtn),
        tab.qrImage,
    )
}

func (t *NetworkTab) updateQR() {
    data := fmt.Sprintf("airmouse://%s:%s", t.ipEntry.Text, t.portEntry.Text)
    pngBytes, err := qrcode.Encode(data, qrcode.High, 200)
    if err != nil {
        return
    }
    img, err := png.Decode(bytes.NewReader(pngBytes))
    if err != nil {
        return
    }
    t.qrImage.Image = img
    t.qrImage.Refresh()
}

func getAllLocalIPs() []string {
    var ips []string
    addrs, err := net.InterfaceAddrs()
    if err != nil {
        return []string{"127.0.0.1"}
    }
    for _, addr := range addrs {
        if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() && ipnet.IP.To4() != nil {
            ips = append(ips, ipnet.IP.String())
        }
    }
    if len(ips) == 0 {
        ips = append(ips, "127.0.0.1")
    }
    return ips
}