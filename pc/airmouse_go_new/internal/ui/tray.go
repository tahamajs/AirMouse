// internal/ui/tray.go
package ui

import (
    "airmouse-go/internal/config"
    "airmouse-go/internal/device"
    "airmouse-go/internal/protocol"
    "airmouse-go/internal/utils"
    "bytes"
    "fmt"
    "image"
    "image/png"
    "net"
    "net/http"
    "os/exec"
    "runtime"
    "time"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/app"
    "fyne.io/fyne/v2/canvas"
    "fyne.io/fyne/v2/widget"
    "github.com/getlantern/systray"
    "github.com/gorilla/websocket"
    "github.com/skip2/go-qrcode"
)

type App struct {
    config     *config.Config
    isRunning  bool
    localIP    string
    clients    map[string]*protocol.Client
    upgrader   websocket.Upgrader
    httpServer *http.Server
}

func NewApp() *App {
    return &App{
        config:    config.Get(),
        isRunning: false,
        clients:   make(map[string]*protocol.Client),
        upgrader: websocket.Upgrader{
            CheckOrigin: func(r *http.Request) bool { return true },
        },
    }
}

func (a *App) Run() error {
    systray.Run(a.onReady, a.onExit)
    return nil
}

func (a *App) Stop() {
    if a.isRunning {
        a.stopServer()
    }
    systray.Quit()
}

func (a *App) onReady() {
    systray.SetTemplateIcon(IconData, IconData)
    systray.SetTitle("Air Mouse Pro")
    systray.SetTooltip("Air Mouse Pro Server")

    a.localIP = a.getLocalIP()

    mIP := systray.AddMenuItem(fmt.Sprintf("🌐 IP: %s", a.localIP), "Local IP")
    mIP.Disable()

    systray.AddSeparator()

    mStatus := systray.AddMenuItem("⚙️ Status: Stopped", "Server status")
    mStatus.Disable()

    mClients := systray.AddMenuItem("📱 Clients: 0", "Connected devices")
    mClients.Disable()

    systray.AddSeparator()

    mShowQR := systray.AddMenuItem("📱 Show QR Code", "Display QR code")
    mShowWifiQR := systray.AddMenuItem("📶 Share WiFi QR", "Share WiFi")

    systray.AddSeparator()

    mWebUI := systray.AddMenuItem("🌐 Open Web UI", "Open web interface")
    mDashboard := systray.AddMenuItem("📊 Dashboard", "Open dashboard")

    systray.AddSeparator()

    mStart := systray.AddMenuItem("▶️ Start", "Start server")
    mStop := systray.AddMenuItem("⏹️ Stop", "Stop server")
    mRestart := systray.AddMenuItem("🔄 Restart", "Restart server")

    systray.AddSeparator()

    mSettings := systray.AddMenuItem("⚙️ Settings", "Configure")
    mLogs := systray.AddMenuItem("📋 Logs", "View logs")
    mAbout := systray.AddMenuItem("ℹ️ About", "About")
    mQuit := systray.AddMenuItem("❌ Quit", "Exit")

    go a.startServer()
    go a.updateStatusLoop(mStatus, mClients)
    go a.handleMenuClicks(mShowQR, mShowWifiQR, mWebUI, mDashboard, mStart, mStop, mRestart, mSettings, mLogs, mAbout, mQuit)
}

func (a *App) updateStatusLoop(statusItem, clientsItem *systray.MenuItem) {
    ticker := time.NewTicker(2 * time.Second)
    for range ticker.C {
        if a.isRunning {
            statusItem.SetTitle("✅ Status: Running")
        } else {
            statusItem.SetTitle("⛔ Status: Stopped")
        }
        clientsItem.SetTitle(fmt.Sprintf("📱 Clients: %d", len(a.clients)))
    }
}

func (a *App) handleMenuClicks(showQR, showWifiQR, webUI, dashboard, start, stop, restart, settings, logs, about, quit *systray.MenuItem) {
    for {
        select {
        case <-showQR.ClickedCh:
            a.showQRCodeWindow()
        case <-showWifiQR.ClickedCh:
            a.showWiFiQRCodeWindow()
        case <-webUI.ClickedCh:
            a.openBrowser("http://localhost:8081")
        case <-dashboard.ClickedCh:
            a.openBrowser("http://localhost:8081/dashboard")
        case <-start.ClickedCh:
            if !a.isRunning {
                go a.startServer()
            }
        case <-stop.ClickedCh:
            if a.isRunning {
                a.stopServer()
            }
        case <-restart.ClickedCh:
            if a.isRunning {
                a.stopServer()
                time.Sleep(1 * time.Second)
                go a.startServer()
            }
        case <-settings.ClickedCh:
            a.showSettingsWindow()
        case <-logs.ClickedCh:
            a.showLogsWindow()
        case <-about.ClickedCh:
            a.showAboutWindow()
        case <-quit.ClickedCh:
            if a.isRunning {
                a.stopServer()
            }
            systray.Quit()
            return
        }
    }
}

func (a *App) startServer() {
    if a.isRunning {
        return
    }
    utils.LogInfo("Starting server...")
    go a.startWebSocketServer()
    go a.startUDPDiscovery()
    a.isRunning = true
    utils.LogInfo("Server started")
}

func (a *App) stopServer() {
    if !a.isRunning {
        return
    }
    utils.LogInfo("Stopping server...")
    if a.httpServer != nil {
        a.httpServer.Close()
    }
    a.clients = make(map[string]*protocol.Client)
    a.isRunning = false
    utils.LogInfo("Server stopped")
}

func (a *App) startWebSocketServer() {
    addr := ":8081"
    http.HandleFunc("/ws", a.handleWebSocket)
    http.HandleFunc("/health", a.handleHealth)
    http.HandleFunc("/api/status", a.handleAPIStatus)
    http.HandleFunc("/api/qrcode", a.handleAPIQRCode)
    http.HandleFunc("/", a.handleWebUI)

    a.httpServer = &http.Server{Addr: addr}
    utils.LogInfo(fmt.Sprintf("WebSocket server on %s", addr))

    if err := a.httpServer.ListenAndServe(); err != nil && a.isRunning {
        utils.LogError(fmt.Sprintf("Server error: %v", err))
    }
}

func (a *App) startUDPDiscovery() {
    addr, _ := net.ResolveUDPAddr("udp", ":8082")
    conn, _ := net.ListenUDP("udp", addr)
    defer conn.Close()
    utils.LogInfo("UDP discovery on port 8082")

    buf := make([]byte, 1024)
    for a.isRunning {
        n, clientAddr, _ := conn.ReadFromUDP(buf)
        msg := string(buf[:n])
        if msg == "AIRMOUSE_DISCOVERY" {
            resp := fmt.Sprintf("AIRMOUSE_SERVER:8080:%s", a.localIP)
            conn.WriteToUDP([]byte(resp), clientAddr)
        }
    }
}

func (a *App) handleWebSocket(w http.ResponseWriter, r *http.Request) {
    conn, err := a.upgrader.Upgrade(w, r, nil)
    if err != nil {
        return
    }
    clientID := fmt.Sprintf("%d", time.Now().UnixNano())
    client := &protocol.Client{ID: clientID, Conn: conn, LastSeen: time.Now(), IsActive: true}
    a.clients[clientID] = client
    utils.LogInfo(fmt.Sprintf("Client connected: %s", clientID))

    defer func() {
        conn.Close()
        delete(a.clients, clientID)
    }()

    for {
        var msg map[string]interface{}
        if err := conn.ReadJSON(&msg); err != nil {
            break
        }
        a.handleClientMessage(client, msg)
    }
}

func (a *App) handleClientMessage(client *protocol.Client, msg map[string]interface{}) {
    msgType, _ := msg["type"].(string)
    switch msgType {
    case "move":
        if payload, ok := msg["payload"].(map[string]interface{}); ok {
            dx, _ := payload["dx"].(float64)
            dy, _ := payload["dy"].(float64)
            a.moveMouse(int(dx), int(dy))
        }
    case "click":
        if payload, ok := msg["payload"].(map[string]interface{}); ok {
            button, _ := payload["button"].(string)
            utils.LogInfo(fmt.Sprintf("Click: %s", button))
        }
    case "doubleclick":
        utils.LogInfo("Double click")
    case "rightclick":
        utils.LogInfo("Right click")
    case "scroll":
        if payload, ok := msg["payload"].(map[string]interface{}); ok {
            delta, _ := payload["delta"].(float64)
            utils.LogInfo(fmt.Sprintf("Scroll: %d", int(delta)))
        }
    }
}

func (a *App) moveMouse(dx, dy int) {
    // Platform-specific mouse movement
    utils.LogDebug(fmt.Sprintf("Move: dx=%d, dy=%d", dx, dy))
}

func (a *App) showQRCodeWindow() {
    go func() {
        qrContent := fmt.Sprintf("airmouse://connect?ip=%s&port=8080", a.localIP)
        qr, _ := qrcode.Encode(qrContent, qrcode.Medium, 300)
        img, _, _ := image.Decode(bytes.NewReader(qr))

        myApp := app.New()
        w := myApp.NewWindow("Air Mouse Connection")

        imageWidget := canvas.NewImageFromImage(img)
        imageWidget.FillMode = canvas.ImageFillOriginal
        imageWidget.SetMinSize(fyne.NewSize(300, 300))

        infoText := widget.NewLabel(fmt.Sprintf("IP: %s\nPort: 8080", a.localIP))
        infoText.Wrapping = fyne.TextWrapWord

        content := widget.NewVBox(
            widget.NewLabel("Scan with Air Mouse App:"),
            imageWidget,
            widget.NewSeparator(),
            infoText,
        )

        w.SetContent(content)
        w.Resize(fyne.NewSize(350, 500))
        w.Show()
    }()
}

func (a *App) showWiFiQRCodeWindow() {
    go func() {
        ssid := a.getWiFiSSID()
        wifiString := fmt.Sprintf("WIFI:T:WPA;S:%s;P:;;", ssid)
        qr, _ := qrcode.Encode(wifiString, qrcode.Medium, 300)
        img, _, _ := image.Decode(bytes.NewReader(qr))

        myApp := app.New()
        w := myApp.NewWindow("WiFi Connection")

        imageWidget := canvas.NewImageFromImage(img)
        imageWidget.FillMode = canvas.ImageFillOriginal
        imageWidget.SetMinSize(fyne.NewSize(300, 300))

        infoText := widget.NewLabel(fmt.Sprintf("Network: %s", ssid))
        infoText.Wrapping = fyne.TextWrapWord

        content := widget.NewVBox(
            widget.NewLabel("Scan to connect to WiFi:"),
            imageWidget,
            widget.NewSeparator(),
            infoText,
        )

        w.SetContent(content)
        w.Resize(fyne.NewSize(350, 500))
        w.Show()
    }()
}

func (a *App) getWiFiSSID() string {
    switch runtime.GOOS {
    case "darwin":
        cmd := exec.Command("/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport", "-I")
        output, _ := cmd.Output()
        return string(output)
    default:
        return "WiFi"
    }
}

func (a *App) openBrowser(url string) {
    var cmd *exec.Cmd
    switch runtime.GOOS {
    case "windows":
        cmd = exec.Command("rundll32", "url.dll,FileProtocolHandler", url)
    case "darwin":
        cmd = exec.Command("open", url)
    default:
        cmd = exec.Command("xdg-open", url)
    }
    cmd.Start()
}

func (a *App) showSettingsWindow() {
    myApp := app.New()
    w := myApp.NewWindow("Settings")
    content := widget.NewVBox(
        widget.NewLabel("Server Settings"),
        widget.NewButton("Save", func() { w.Close() }),
    )
    w.SetContent(content)
    w.Resize(fyne.NewSize(400, 300))
    w.Show()
}

func (a *App) showLogsWindow() {
    myApp := app.New()
    w := myApp.NewWindow("Logs")
    logText := widget.NewMultiLineEntry()
    logText.SetText("Server logs...")
    w.SetContent(logText)
    w.Resize(fyne.NewSize(600, 400))
    w.Show()
}

func (a *App) showAboutWindow() {
    myApp := app.New()
    w := myApp.NewWindow("About")
    content := widget.NewVBox(
        widget.NewLabelWithStyle("Air Mouse Pro", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewLabel("Version 3.0.0"),
        widget.NewLabel(""),
        widget.NewLabel("Turn your phone into a wireless mouse"),
        widget.NewLabel("© 2025 Air Mouse Team"),
    )
    w.SetContent(content)
    w.Resize(fyne.NewSize(350, 300))
    w.Show()
}

func (a *App) handleHealth(w http.ResponseWriter, r *http.Request) {
    w.Write([]byte(`{"status":"ok"}`))
}

func (a *App) handleAPIStatus(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    fmt.Fprintf(w, `{"running":%v,"ip":"%s","clients":%d}`, a.isRunning, a.localIP, len(a.clients))
}

func (a *App) handleAPIQRCode(w http.ResponseWriter, r *http.Request) {
    qrContent := fmt.Sprintf("airmouse://connect?ip=%s&port=8080", a.localIP)
    qr, _ := qrcode.Encode(qrContent, qrcode.Medium, 300)
    w.Header().Set("Content-Type", "image/png")
    w.Write(qr)
}

func (a *App) handleWebUI(w http.ResponseWriter, r *http.Request) {
    html := `<!DOCTYPE html>
<html>
<head><title>Air Mouse Pro</title>
<style>
body{font-family:sans-serif;background:#1a1a2e;color:#eee;text-align:center;padding:20px}
img{max-width:250px;margin:20px}
button{background:#e94560;color:white;border:none;padding:10px 20px;border-radius:6px;cursor:pointer}
</style>
</head>
<body>
<h1>🎯 Air Mouse Pro</h1>
<div id="qrcode"></div>
<p>IP: <span id="ip">-</span></p>
<button onclick="location.reload()">Refresh</button>
<script>
fetch('/api/status').then(r=>r.json()).then(d=>{
    document.getElementById('ip').innerHTML=d.ip;
});
fetch('/api/qrcode').then(r=>r.blob()).then(b=>{
    const url=URL.createObjectURL(b);
    document.getElementById('qrcode').innerHTML='<img src="'+url+'">';
});
</script>
</body>
</html>`
    w.Header().Set("Content-Type", "text/html")
    w.Write([]byte(html))
}

func (a *App) getLocalIP() string {
    addrs, _ := net.InterfaceAddrs()
    for _, addr := range addrs {
        if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() && ipnet.IP.To4() != nil {
            return ipnet.IP.String()
        }
    }
    return "127.0.0.1"
}

func (a *App) onExit() {
    if a.isRunning {
        a.stopServer()
    }
}