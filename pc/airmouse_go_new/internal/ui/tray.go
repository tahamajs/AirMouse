//go:build ignore

package ui

import (
    "airmouse-go/internal/config"
    "airmouse-go/internal/control"
    "airmouse-go/internal/device"
    "airmouse-go/internal/protocol"
    "airmouse-go/internal/utils"
    "bytes"
    "encoding/json"
    "fmt"
    "image"
    "image/png"
    "net"
    "net/http"
    "os/exec"
    "runtime"
    "strings"
    "time"

    "fyne.io/fyne/v2"
    "fyne.io/fyne/v2/app"
    "fyne.io/fyne/v2/canvas"
    "fyne.io/fyne/v2/container"
    "fyne.io/fyne/v2/dialog"
    "fyne.io/fyne/v2/theme"
    "fyne.io/fyne/v2/widget"
    "github.com/getlantern/systray"
    "github.com/gorilla/websocket"
    "github.com/skip2/go-qrcode"
)

type TrayApp struct {
    config       *config.Config
    isRunning    bool
    localIP      string
    clients      map[string]*protocol.Client
    upgrader     websocket.Upgrader
    httpServer   *http.Server
    startTime    time.Time
    stats        *ServerStats
    deviceMgr    *device.Manager
    mouse        control.MouseController
}

type ServerStats struct {
    TotalConnections  int64
    TotalMessages     int64
    TotalBytesSent    int64
    TotalBytesRecv    int64
    StartTime         time.Time
}

func NewTrayApp(cfg *config.Config, deviceMgr *device.Manager, mouse control.MouseController) *TrayApp {
    return &TrayApp{
        config:    cfg,
        isRunning: false,
        clients:   make(map[string]*protocol.Client),
        stats:     &ServerStats{StartTime: time.Now()},
        deviceMgr: deviceMgr,
        mouse:     mouse,
        upgrader: websocket.Upgrader{
            CheckOrigin: func(r *http.Request) bool { return true },
            ReadBufferSize:  1024,
            WriteBufferSize: 1024,
        },
    }
}

func (a *TrayApp) Run() error {
    systray.Run(a.onReady, a.onExit)
    return nil
}

func (a *TrayApp) Stop() {
    if a.isRunning {
        a.stopServer()
    }
    systray.Quit()
}

func (a *TrayApp) onReady() {
    // Set icons
    systray.SetTemplateIcon(IconData, IconData)
    systray.SetTitle("Air Mouse Pro")
    systray.SetTooltip("Air Mouse Pro Server - Ready")

    a.localIP = a.getLocalIP()

    // Server info section
    mServerName := systray.AddMenuItem(fmt.Sprintf("🖥️ %s", a.config.ServerName), "Server Name")
    mServerName.Disable()
    
    mIP := systray.AddMenuItem(fmt.Sprintf("🌐 IP: %s", a.localIP), "Local IP Address")
    mIP.Disable()
    
    mPort := systray.AddMenuItem(fmt.Sprintf("🔌 Port: %d", a.config.Port), "Server Port")
    mPort.Disable()

    systray.AddSeparator()

    // Status section
    mStatus := systray.AddMenuItem("⚙️ Status: Stopped", "Server Status")
    mStatus.Disable()
    
    mUptime := systray.AddMenuItem("⏱️ Uptime: --:--:--", "Server Uptime")
    mUptime.Disable()
    
    mClients := systray.AddMenuItem("📱 Clients: 0", "Connected Devices")
    mClients.Disable()
    
    mStats := systray.AddMenuItem("📊 Messages: 0", "Total Messages Processed")
    mStats.Disable()

    systray.AddSeparator()

    // Quick actions
    mShowQR := systray.AddMenuItem("📱 Show QR Code", "Display pairing QR code")
    mShowWifiQR := systray.AddMenuItem("📶 Share WiFi QR", "Share WiFi credentials")
    mCopyIP := systray.AddMenuItem("📋 Copy IP Address", "Copy IP to clipboard")

    systray.AddSeparator()

    // Web interfaces
    mWebUI := systray.AddMenuItem("🌐 Open Web UI", "Open web control panel")
    mDashboard := systray.AddMenuItem("📊 Dashboard", "Open statistics dashboard")
    mAPIStatus := systray.AddMenuItem("🔌 API Status", "Open API status endpoint")

    systray.AddSeparator()

    // Server controls
    mStart := systray.AddMenuItem("▶️ Start Server", "Start the Air Mouse server")
    mStart.SetIcon(IconData)
    
    mStop := systray.AddMenuItem("⏹️ Stop Server", "Stop the Air Mouse server")
    mStop.SetIcon(IconData)
    mStop.Disable()
    
    mRestart := systray.AddMenuItem("🔄 Restart Server", "Restart the Air Mouse server")
    mRestart.SetIcon(IconData)

    systray.AddSeparator()

    // Additional features
    mGestures := systray.AddMenuItem("✋ Gesture Settings", "Configure gesture recognition")
    mProximity := systray.AddMenuItem("📡 Proximity Settings", "Configure proximity lock")
    mPersonalization := systray.AddMenuItem("🧠 Personalization", "AI personalization settings")

    systray.AddSeparator()

    // Help & info
    mSettings := systray.AddMenuItem("⚙️ Settings", "Open settings window")
    mLogs := systray.AddMenuItem("📋 View Logs", "Open logs viewer")
    mAbout := systray.AddMenuItem("ℹ️ About", "About Air Mouse Pro")
    
    systray.AddSeparator()
    
    mQuit := systray.AddMenuItem("❌ Quit", "Exit Air Mouse Pro")

    // Start background updates
    go a.updateStatusLoop(mStatus, mUptime, mClients, mStats)
    go a.handleMenuClicks(mShowQR, mShowWifiQR, mCopyIP, mWebUI, mDashboard, mAPIStatus, 
        mStart, mStop, mRestart, mGestures, mProximity, mPersonalization, 
        mSettings, mLogs, mAbout, mQuit)
    
    // Auto-start if configured
    if a.config.AutoStartServer {
        go a.startServer()
    }
}

func (a *TrayApp) updateStatusLoop(statusItem, uptimeItem, clientsItem, statsItem *systray.MenuItem) {
    ticker := time.NewTicker(1 * time.Second)
    for range ticker.C {
        // Update status
        if a.isRunning {
            statusItem.SetTitle("✅ Status: Running")
            statusItem.SetIcon(RunningIconData)
            
            // Update uptime
            if !a.startTime.IsZero() {
                uptime := time.Since(a.startTime)
                uptimeItem.SetTitle(fmt.Sprintf("⏱️ Uptime: %02d:%02d:%02d",
                    int(uptime.Hours()),
                    int(uptime.Minutes())%60,
                    int(uptime.Seconds())%60))
            }
        } else {
            statusItem.SetTitle("⛔ Status: Stopped")
            statusItem.SetIcon(StoppedIconData)
            uptimeItem.SetTitle("⏱️ Uptime: --:--:--")
        }
        
        // Update client count
        clientCount := len(a.clients)
        clientsItem.SetTitle(fmt.Sprintf("📱 Clients: %d", clientCount))
        
        // Update stats
        statsItem.SetTitle(fmt.Sprintf("📊 Messages: %d", a.stats.TotalMessages))
    }
}

func (a *TrayApp) handleMenuClicks(showQR, showWifiQR, copyIP, webUI, dashboard, apiStatus,
    start, stop, restart, gestures, proximity, personalization,
    settings, logs, about, quit *systray.MenuItem) {
    
    for {
        select {
        case <-showQR.ClickedCh:
            a.showQRCodeWindow()
        case <-showWifiQR.ClickedCh:
            a.showWiFiQRCodeWindow()
        case <-copyIP.ClickedCh:
            a.copyIPToClipboard()
        case <-webUI.ClickedCh:
            a.openBrowser(fmt.Sprintf("http://localhost:%d", a.config.WebSocketPort))
        case <-dashboard.ClickedCh:
            a.openBrowser(fmt.Sprintf("http://localhost:%d/dashboard", a.config.WebSocketPort))
        case <-apiStatus.ClickedCh:
            a.openBrowser(fmt.Sprintf("http://localhost:%d/api/status", a.config.WebSocketPort))
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
        case <-gestures.ClickedCh:
            a.showGesturesWindow()
        case <-proximity.ClickedCh:
            a.showProximityWindow()
        case <-personalization.ClickedCh:
            a.showPersonalizationWindow()
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

func (a *TrayApp) startServer() {
    if a.isRunning {
        return
    }
    
    utils.LogInfo("Starting Air Mouse Pro server...")
    a.startTime = time.Now()
    a.stats.StartTime = a.startTime
    
    go a.startWebSocketServer()
    go a.startUDPDiscovery()
    go a.startTCPFallback()
    
    a.isRunning = true
    utils.LogInfo(fmt.Sprintf("Server started on %s:%d", a.localIP, a.config.Port))
}

func (a *TrayApp) stopServer() {
    if !a.isRunning {
        return
    }
    
    utils.LogInfo("Stopping server...")
    
    if a.httpServer != nil {
        ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
        defer cancel()
        a.httpServer.Shutdown(ctx)
    }
    
    // Close all client connections
    for id, client := range a.clients {
        if client.Conn != nil {
            client.Conn.Close()
        }
        utils.LogInfo(fmt.Sprintf("Closed client connection: %s", id))
    }
    
    a.clients = make(map[string]*protocol.Client)
    a.isRunning = false
    utils.LogInfo("Server stopped")
}

func (a *TrayApp) startWebSocketServer() {
    addr := fmt.Sprintf(":%d", a.config.WebSocketPort)
    
    mux := http.NewServeMux()
    mux.HandleFunc("/ws", a.handleWebSocket)
    mux.HandleFunc("/health", a.handleHealth)
    mux.HandleFunc("/api/status", a.handleAPIStatus)
    mux.HandleFunc("/api/qrcode", a.handleAPIQRCode)
    mux.HandleFunc("/api/stats", a.handleAPIStats)
    mux.HandleFunc("/api/devices", a.handleAPIDevices)
    mux.HandleFunc("/", a.handleWebUI)

    a.httpServer = &http.Server{
        Addr:         addr,
        Handler:      mux,
        ReadTimeout:  10 * time.Second,
        WriteTimeout: 10 * time.Second,
    }
    
    utils.LogInfo(fmt.Sprintf("WebSocket server listening on %s", addr))

    if err := a.httpServer.ListenAndServe(); err != nil && err != http.ErrServerClosed && a.isRunning {
        utils.LogError(fmt.Sprintf("WebSocket server error: %v", err))
    }
}

func (a *TrayApp) startTCPFallback() {
    addr := fmt.Sprintf(":%d", a.config.Port)
    listener, err := net.Listen("tcp", addr)
    if err != nil {
        utils.LogError(fmt.Sprintf("TCP fallback failed: %v", err))
        return
    }
    defer listener.Close()
    
    utils.LogInfo(fmt.Sprintf("TCP fallback listening on %s", addr))
    
    for a.isRunning {
        conn, err := listener.Accept()
        if err != nil {
            continue
        }
        go a.handleTCPConnection(conn)
    }
}

func (a *TrayApp) handleTCPConnection(conn net.Conn) {
    defer conn.Close()
    clientID := conn.RemoteAddr().String()
    utils.LogInfo(fmt.Sprintf("TCP client connected: %s", clientID))
    
    buf := make([]byte, 4096)
    for a.isRunning {
        n, err := conn.Read(buf)
        if err != nil {
            break
        }
        a.stats.TotalMessages++
        a.stats.TotalBytesRecv += int64(n)
        // Process message...
    }
}

func (a *TrayApp) startUDPDiscovery() {
    addr, err := net.ResolveUDPAddr("udp", fmt.Sprintf(":%d", a.config.UDPPort))
    if err != nil {
        utils.LogError(fmt.Sprintf("UDP resolution failed: %v", err))
        return
    }
    
    conn, err := net.ListenUDP("udp", addr)
    if err != nil {
        utils.LogError(fmt.Sprintf("UDP listen failed: %v", err))
        return
    }
    defer conn.Close()
    
    utils.LogInfo(fmt.Sprintf("UDP discovery on port %d", a.config.UDPPort))

    buf := make([]byte, 1024)
    for a.isRunning {
        conn.SetReadDeadline(time.Now().Add(2 * time.Second))
        n, clientAddr, err := conn.ReadFromUDP(buf)
        if err != nil {
            continue
        }
        
        msg := strings.TrimSpace(string(buf[:n]))
        if msg == "AIRMOUSE_DISCOVERY" {
            response := map[string]interface{}{
                "type": "discovery_response",
                "ip":   a.localIP,
                "port": a.config.Port,
                "name": a.config.ServerName,
            }
            respData, _ := json.Marshal(response)
            conn.WriteToUDP(respData, clientAddr)
            utils.LogDebug(fmt.Sprintf("Discovery response sent to %s", clientAddr.IP))
        }
    }
}

func (a *TrayApp) handleWebSocket(w http.ResponseWriter, r *http.Request) {
    // Check for authentication token
    token := r.URL.Query().Get("token")
    if a.config.AuthEnabled && token != a.config.AuthToken {
        w.WriteHeader(http.StatusUnauthorized)
        w.Write([]byte(`{"error":"unauthorized"}`))
        return
    }
    
    conn, err := a.upgrader.Upgrade(w, r, nil)
    if err != nil {
        utils.LogError(fmt.Sprintf("WebSocket upgrade failed: %v", err))
        return
    }
    
    clientID := utils.GenerateID()
    clientName := r.URL.Query().Get("name")
    if clientName == "" {
        clientName = "Unknown Device"
    }
    
    client := &protocol.Client{
        ID:        clientID,
        Name:      clientName,
        Conn:      conn,
        LastSeen:  time.Now(),
        IsActive:  true,
        ConnectedAt: time.Now(),
    }
    
    a.clients[clientID] = client
    a.stats.TotalConnections++
    
    utils.LogInfo(fmt.Sprintf("WebSocket client connected: %s (%s)", clientName, clientID))
    
    // Send welcome message
    welcome := map[string]interface{}{
        "type": "welcome",
        "payload": map[string]interface{}{
            "server":  a.config.ServerName,
            "version": "3.0.0",
            "id":      clientID,
        },
    }
    conn.WriteJSON(welcome)

    defer func() {
        conn.Close()
        delete(a.clients, clientID)
        utils.LogInfo(fmt.Sprintf("WebSocket client disconnected: %s", clientID))
    }()

    for {
        var msg map[string]interface{}
        if err := conn.ReadJSON(&msg); err != nil {
            break
        }
        
        a.stats.TotalMessages++
        client.LastSeen = time.Now()
        a.handleClientMessage(client, msg)
    }
}

func (a *TrayApp) handleClientMessage(client *protocol.Client, msg map[string]interface{}) {
    msgType, _ := msg["type"].(string)
    
    switch msgType {
    case "move":
        if payload, ok := msg["payload"].(map[string]interface{}); ok {
            dx, _ := payload["dx"].(float64)
            dy, _ := payload["dy"].(float64)
            a.moveMouse(int(dx), int(dy))
            a.stats.TotalMessages++
        } else if dx, ok := msg["dx"].(float64); ok {
            dy, _ := msg["dy"].(float64)
            a.moveMouse(int(dx), int(dy))
        }
        
    case "click":
        var button string
        if payload, ok := msg["payload"].(map[string]interface{}); ok {
            button, _ = payload["button"].(string)
        } else {
            button, _ = msg["button"].(string)
        }
        if button == "" {
            button = "left"
        }
        a.clickMouse(button)
        utils.LogInfo(fmt.Sprintf("Click: %s from %s", button, client.Name))
        
    case "doubleclick":
        a.doubleClick()
        utils.LogInfo(fmt.Sprintf("Double click from %s", client.Name))
        
    case "rightclick":
        a.rightClick()
        utils.LogInfo(fmt.Sprintf("Right click from %s", client.Name))
        
    case "scroll":
        var delta float64
        if payload, ok := msg["payload"].(map[string]interface{}); ok {
            delta, _ = payload["delta"].(float64)
        } else {
            delta, _ = msg["delta"].(float64)
        }
        a.scrollMouse(int(delta))
        
    case "hello":
        if payload, ok := msg["payload"].(map[string]interface{}); ok {
            if name, ok := payload["name"].(string); ok && name != "" {
                client.Name = name
            }
        } else if name, ok := msg["name"].(string); ok && name != "" {
            client.Name = name
        }
        utils.LogInfo(fmt.Sprintf("Device identified: %s", client.Name))
        
    case "gesture":
        var gesture string
        var confidence float64
        if payload, ok := msg["payload"].(map[string]interface{}); ok {
            gesture, _ = payload["gesture"].(string)
            confidence, _ = payload["confidence"].(float64)
        }
        a.handleGesture(gesture, confidence)
        
    case "proximity":
        var isNear bool
        var distance float64
        if payload, ok := msg["payload"].(map[string]interface{}); ok {
            isNear, _ = payload["is_near"].(bool)
            distance, _ = payload["distance"].(float64)
        }
        a.handleProximity(isNear, distance)
        
    case "control":
        var command string
        if payload, ok := msg["payload"].(map[string]interface{}); ok {
            command, _ = payload["command"].(string)
        }
        a.handleControlCommand(command)
        
    case "ping":
        if client.Conn != nil {
            client.Conn.WriteJSON(map[string]string{"type": "pong"})
        }
        
    default:
        utils.LogDebug(fmt.Sprintf("Unknown message type: %s", msgType))
    }
}

func (a *TrayApp) moveMouse(dx, dy int) {
    // Platform-specific mouse movement using robotgo or native calls
    utils.LogDebug(fmt.Sprintf("Move mouse: dx=%d, dy=%d", dx, dy))
}

func (a *TrayApp) clickMouse(button string) {
    utils.LogDebug(fmt.Sprintf("Click mouse: %s", button))
}

func (a *TrayApp) doubleClick() {
    utils.LogDebug("Double click mouse")
}

func (a *TrayApp) rightClick() {
    utils.LogDebug("Right click mouse")
}

func (a *TrayApp) scrollMouse(delta int) {
    utils.LogDebug(fmt.Sprintf("Scroll mouse: delta=%d", delta))
}

func (a *TrayApp) handleGesture(gesture string, confidence float64) {
    utils.LogInfo(fmt.Sprintf("Gesture detected: %s (confidence: %.2f%%)", gesture, confidence*100))
    
    // Map gesture to action
    switch gesture {
    case "ThumbsUp":
        a.mediaPlayPause()
    case "ThumbsDown":
        a.mediaStop()
    case "LeftSwipe":
        a.mediaPrevious()
    case "RightSwipe":
        a.mediaNext()
    case "CircleCW":
        a.volumeUp()
    case "CircleCCW":
        a.volumeDown()
    }
}

func (a *TrayApp) handleProximity(isNear bool, distance float64) {
    utils.LogInfo(fmt.Sprintf("Proximity update: near=%v, distance=%.2fm", isNear, distance))
    
    if isNear && distance < 1.0 {
        a.unlockScreen()
    } else if !isNear && distance > 3.0 {
        a.lockScreen()
    }
}

func (a *TrayApp) handleControlCommand(command string) {
    switch command {
    case "pause_movement":
        utils.LogInfo("Movement paused")
    case "resume_movement":
        utils.LogInfo("Movement resumed")
    case "lock_screen":
        a.lockScreen()
    case "shutdown":
        a.shutdownComputer()
    }
}

func (a *TrayApp) showQRCodeWindow() {
    go func() {
        qrContent := fmt.Sprintf("airmouse://pair?ip=%s&port=%d&name=%s", a.localIP, a.config.Port, a.config.ServerName)
        qr, _ := qrcode.Encode(qrContent, qrcode.High, 300)
        img, _, _ := image.Decode(bytes.NewReader(qr))

        myApp := app.New()
        w := myApp.NewWindow("Air Mouse Pro - Pairing QR Code")
        w.SetIcon(theme.FyneLogo())

        imageWidget := canvas.NewImageFromImage(img)
        imageWidget.FillMode = canvas.ImageFillOriginal
        imageWidget.SetMinSize(fyne.NewSize(300, 300))

        infoText := widget.NewLabel(fmt.Sprintf(
            "Server: %s\nIP: %s\nPort: %d\n\nScan this QR code with\nthe Air Mouse Android app",
            a.config.ServerName, a.localIP, a.config.Port))
        infoText.Wrapping = fyne.TextWrapWord

        content := container.NewVBox(
            widget.NewLabelWithStyle("Pair New Device", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
            widget.NewSeparator(),
            imageWidget,
            infoText,
            widget.NewButton("Close", func() { w.Close() }),
        )

        w.SetContent(content)
        w.Resize(fyne.NewSize(400, 550))
        w.CenterOnScreen()
        w.Show()
    }()
}

func (a *TrayApp) showWiFiQRCodeWindow() {
    go func() {
        ssid := a.getWiFiSSID()
        password := a.getWiFiPassword()
        
        wifiString := fmt.Sprintf("WIFI:T:WPA;S:%s;P:%s;;", ssid, password)
        qr, _ := qrcode.Encode(wifiString, qrcode.Medium, 300)
        img, _, _ := image.Decode(bytes.NewReader(qr))

        myApp := app.New()
        w := myApp.NewWindow("Air Mouse Pro - WiFi QR Code")

        imageWidget := canvas.NewImageFromImage(img)
        imageWidget.FillMode = canvas.ImageFillOriginal
        imageWidget.SetMinSize(fyne.NewSize(300, 300))

        infoText := widget.NewLabel(fmt.Sprintf("Network: %s\nPassword: %s", ssid, password))
        infoText.Wrapping = fyne.TextWrapWord

        content := container.NewVBox(
            widget.NewLabelWithStyle("Share WiFi Credentials", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
            widget.NewSeparator(),
            imageWidget,
            infoText,
            widget.NewButton("Close", func() { w.Close() }),
        )

        w.SetContent(content)
        w.Resize(fyne.NewSize(400, 500))
        w.CenterOnScreen()
        w.Show()
    }()
}

func (a *TrayApp) copyIPToClipboard() {
    systray.SetClipboard(a.localIP)
    utils.LogInfo(fmt.Sprintf("IP copied to clipboard: %s", a.localIP))
}

func (a *TrayApp) getWiFiSSID() string {
    switch runtime.GOOS {
    case "darwin":
        cmd := exec.Command("/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport", "-I")
        output, _ := cmd.Output()
        lines := strings.Split(string(output), "\n")
        for _, line := range lines {
            if strings.Contains(line, "SSID:") {
                return strings.TrimSpace(strings.TrimPrefix(line, "SSID:"))
            }
        }
    case "windows":
        cmd := exec.Command("netsh", "wlan", "show", "interfaces")
        output, _ := cmd.Output()
        lines := strings.Split(string(output), "\n")
        for _, line := range lines {
            if strings.Contains(line, "SSID") {
                parts := strings.Split(line, ":")
                if len(parts) > 1 {
                    return strings.TrimSpace(parts[1])
                }
            }
        }
    case "linux":
        cmd := exec.Command("iwgetid", "-r")
        output, _ := cmd.Output()
        return strings.TrimSpace(string(output))
    }
    return "Unknown Network"
}

func (a *TrayApp) getWiFiPassword() string {
    // This is platform-specific and may require admin privileges
    return "password123" // Placeholder
}

func (a *TrayApp) lockScreen() {
    switch runtime.GOOS {
    case "windows":
        exec.Command("rundll32.exe", "user32.dll,LockWorkStation").Start()
    case "darwin":
        exec.Command("osascript", "-e", `tell application "System Events" to keystroke "q" using {command down, control down}`).Start()
    case "linux":
        exec.Command("gnome-screensaver-command", "-l").Start()
        exec.Command("xdg-screensaver", "lock").Start()
    }
    utils.LogInfo("Screen locked")
}

func (a *TrayApp) unlockScreen() {
    utils.LogInfo("Screen unlock request received")
}

func (a *TrayApp) shutdownComputer() {
    switch runtime.GOOS {
    case "windows":
        exec.Command("shutdown", "/s", "/t", "60").Start()
    case "darwin":
        exec.Command("osascript", "-e", `tell app "System Events" to shut down`).Start()
    case "linux":
        exec.Command("shutdown", "-h", "+1").Start()
    }
    utils.LogInfo("Computer shutdown initiated")
}

func (a *TrayApp) mediaPlayPause() {
    utils.LogInfo("Media: Play/Pause")
}

func (a *TrayApp) mediaStop() {
    utils.LogInfo("Media: Stop")
}

func (a *TrayApp) mediaPrevious() {
    utils.LogInfo("Media: Previous")
}

func (a *TrayApp) mediaNext() {
    utils.LogInfo("Media: Next")
}

func (a *TrayApp) volumeUp() {
    utils.LogInfo("Volume: Up")
}

func (a *TrayApp) volumeDown() {
    utils.LogInfo("Volume: Down")
}

func (a *TrayApp) openBrowser(url string) {
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

func (a *TrayApp) showSettingsWindow() {
    myApp := app.New()
    w := myApp.NewWindow("Air Mouse Pro - Settings")
    w.Resize(fyne.NewSize(600, 500))
    w.CenterOnScreen()
    
    // Server settings
    serverNameEntry := widget.NewEntry()
    serverNameEntry.SetText(a.config.ServerName)
    
    portEntry := widget.NewEntry()
    portEntry.SetText(fmt.Sprintf("%d", a.config.Port))
    
    wsPortEntry := widget.NewEntry()
    wsPortEntry.SetText(fmt.Sprintf("%d", a.config.WebSocketPort))
    
    autoStartCheck := widget.NewCheck("Auto-start server on launch", func(on bool) {
        a.config.AutoStartServer = on
        a.config.Save()
    })
    autoStartCheck.SetChecked(a.config.AutoStartServer)
    
    saveBtn := widget.NewButton("Save Settings", func() {
        a.config.ServerName = serverNameEntry.Text
        // Save other settings...
        a.config.Save()
        dialog.ShowInformation("Settings Saved", "Configuration has been saved.", w)
    })
    
    content := container.NewVBox(
        widget.NewLabelWithStyle("Server Configuration", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        widget.NewLabel("Server Name:"), serverNameEntry,
        widget.NewLabel("TCP Port:"), portEntry,
        widget.NewLabel("WebSocket Port:"), wsPortEntry,
        autoStartCheck,
        widget.NewSeparator(),
        saveBtn,
    )
    
    w.SetContent(container.NewScroll(content))
    w.Show()
}

func (a *TrayApp) showGesturesWindow() {
    myApp := app.New()
    w := myApp.NewWindow("Air Mouse Pro - Gesture Settings")
    w.Resize(fyne.NewSize(500, 400))
    w.CenterOnScreen()
    
    gestures := []string{"ThumbsUp: Play/Pause", "ThumbsDown: Stop", "LeftSwipe: Previous", "RightSwipe: Next", "CircleCW: Volume Up", "CircleCCW: Volume Down"}
    
    var gestureWidgets []fyne.CanvasObject
    for _, g := range gestures {
        gestureWidgets = append(gestureWidgets, widget.NewLabel(g))
    }
    
    content := container.NewVBox(
        widget.NewLabelWithStyle("Gesture Mapping", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        gestureWidgets...,
    )
    
    w.SetContent(content)
    w.Show()
}

func (a *TrayApp) showProximityWindow() {
    myApp := app.New()
    w := myApp.NewWindow("Air Mouse Pro - Proximity Settings")
    w.Resize(fyne.NewSize(500, 400))
    w.CenterOnScreen()
    
    enableCheck := widget.NewCheck("Enable Proximity Lock/Unlock", func(on bool) {
        a.config.ProximityEnabled = on
        a.config.Save()
    })
    enableCheck.SetChecked(a.config.ProximityEnabled)
    
    nearSlider := widget.NewSlider(0.5, 3.0)
    nearSlider.Step = 0.1
    nearSlider.Value = a.config.ProximityNearThreshold
    
    farSlider := widget.NewSlider(2.0, 10.0)
    farSlider.Step = 0.2
    farSlider.Value = a.config.ProximityFarThreshold
    
    content := container.NewVBox(
        widget.NewLabelWithStyle("Proximity Configuration", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        enableCheck,
        widget.NewLabel(fmt.Sprintf("Near Threshold: %.1f m", a.config.ProximityNearThreshold)),
        nearSlider,
        widget.NewLabel(fmt.Sprintf("Far Threshold: %.1f m", a.config.ProximityFarThreshold)),
        farSlider,
    )
    
    w.SetContent(content)
    w.Show()
}

func (a *TrayApp) showPersonalizationWindow() {
    myApp := app.New()
    w := myApp.NewWindow("Air Mouse Pro - Personalization")
    w.Resize(fyne.NewSize(500, 400))
    w.CenterOnScreen()
    
    enableCheck := widget.NewCheck("Enable AI Personalization", func(on bool) {
        a.config.EnablePersonalization = on
        a.config.Save()
    })
    enableCheck.SetChecked(a.config.EnablePersonalization)
    
    content := container.NewVBox(
        widget.NewLabelWithStyle("AI Personalization", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewSeparator(),
        enableCheck,
        widget.NewLabel("The AI learns your movement patterns"),
        widget.NewLabel("to provide smoother cursor control."),
        widget.NewProgressBar(),
        widget.NewButton("Train Now", func() {}),
    )
    
    w.SetContent(content)
    w.Show()
}

func (a *TrayApp) showLogsWindow() {
    myApp := app.New()
    w := myApp.NewWindow("Air Mouse Pro - Logs")
    w.Resize(fyne.NewSize(800, 600))
    w.CenterOnScreen()
    
    logText := widget.NewMultiLineEntry()
    logText.SetText("Loading logs...")
    logText.Disable()
    
    content := container.NewBorder(
        widget.NewLabelWithStyle("Server Logs", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewButton("Refresh", func() {
            // Refresh logs
        }),
        nil, nil,
        container.NewScroll(logText),
    )
    
    w.SetContent(content)
    w.Show()
}

func (a *TrayApp) showAboutWindow() {
    myApp := app.New()
    w := myApp.NewWindow("About Air Mouse Pro")
    w.Resize(fyne.NewSize(400, 350))
    w.CenterOnScreen()
    
    content := container.NewVBox(
        widget.NewLabelWithStyle("Air Mouse Pro", fyne.TextAlignCenter, fyne.TextStyle{Bold: true}),
        widget.NewLabel("Version 3.0.0"),
        widget.NewLabel(""),
        widget.NewLabel("Turn your phone into a wireless mouse"),
        widget.NewLabel(""),
        widget.NewLabel("© 2025 Air Mouse Team"),
        widget.NewLabel("University of Tehran"),
        widget.NewSeparator(),
        widget.NewButton("Close", func() { w.Close() }),
    )
    
    w.SetContent(content)
    w.Show()
}

func (a *TrayApp) handleHealth(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    w.Write([]byte(`{"status":"ok","timestamp":"` + time.Now().Format(time.RFC3339) + `"}`))
}

func (a *TrayApp) handleAPIStatus(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    response := map[string]interface{}{
        "running": a.isRunning,
        "ip":      a.localIP,
        "port":    a.config.Port,
        "clients": len(a.clients),
        "name":    a.config.ServerName,
        "version": "3.0.0",
    }
    json.NewEncoder(w).Encode(response)
}

func (a *TrayApp) handleAPIStats(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    response := map[string]interface{}{
        "total_messages":   a.stats.TotalMessages,
        "total_connections": a.stats.TotalConnections,
        "total_bytes_sent": a.stats.TotalBytesSent,
        "total_bytes_recv": a.stats.TotalBytesRecv,
        "uptime_seconds":   time.Since(a.stats.StartTime).Seconds(),
    }
    json.NewEncoder(w).Encode(response)
}

func (a *TrayApp) handleAPIDevices(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    devices := make([]map[string]interface{}, 0, len(a.clients))
    for id, client := range a.clients {
        devices = append(devices, map[string]interface{}{
            "id":         id,
            "name":       client.Name,
            "connected":  client.ConnectedAt.Format(time.RFC3339),
            "last_seen":  client.LastSeen.Format(time.RFC3339),
            "is_active":  client.IsActive,
        })
    }
    json.NewEncoder(w).Encode(devices)
}

func (a *TrayApp) handleAPIQRCode(w http.ResponseWriter, r *http.Request) {
    qrContent := fmt.Sprintf("airmouse://pair?ip=%s&port=%d&name=%s", a.localIP, a.config.Port, a.config.ServerName)
    qr, err := qrcode.Encode(qrContent, qrcode.Medium, 300)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    w.Header().Set("Content-Type", "image/png")
    w.Write(qr)
}

func (a *TrayApp) handleWebUI(w http.ResponseWriter, r *http.Request) {
    html := `<!DOCTYPE html>
<html>
<head>
    <title>Air Mouse Pro</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: #fff;
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            padding: 20px;
        }
        .container {
            background: rgba(255,255,255,0.1);
            backdrop-filter: blur(10px);
            border-radius: 20px;
            padding: 30px;
            text-align: center;
            max-width: 500px;
            width: 100%;
        }
        h1 { font-size: 2em; margin-bottom: 20px; }
        .qr-code { margin: 20px 0; }
        .qr-code img { max-width: 250px; border-radius: 10px; }
        .info { background: rgba(0,0,0,0.3); border-radius: 10px; padding: 15px; margin: 15px 0; }
        .info p { margin: 5px 0; }
        button {
            background: #e94560;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 6px;
            cursor: pointer;
            font-size: 16px;
            margin: 5px;
        }
        button:hover { background: #c7354e; }
        .status {
            display: inline-block;
            width: 10px;
            height: 10px;
            border-radius: 50%;
            margin-right: 5px;
        }
        .status.online { background: #4CAF50; box-shadow: 0 0 5px #4CAF50; }
        .status.offline { background: #f44336; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🎯 Air Mouse Pro</h1>
        <div class="qr-code" id="qrcode"></div>
        <div class="info">
            <p><strong id="serverName">Loading...</strong></p>
            <p>IP: <span id="ip">-</span></p>
            <p>Port: <span id="port">8080</span></p>
            <p>Status: <span class="status" id="statusIndicator"></span> <span id="status">Checking...</span></p>
            <p>Clients: <span id="clients">0</span></p>
        </div>
        <div>
            <button onclick="location.reload()">Refresh</button>
            <button onclick="window.open('/api/status')">API Status</button>
        </div>
    </div>
    <script>
        async function loadStatus() {
            try {
                const response = await fetch('/api/status');
                const data = await response.json();
                document.getElementById('serverName').innerText = data.name || 'Air Mouse Pro';
                document.getElementById('ip').innerHTML = data.ip;
                document.getElementById('port').innerHTML = data.port;
                document.getElementById('clients').innerHTML = data.clients;
                const statusSpan = document.getElementById('status');
                const indicator = document.getElementById('statusIndicator');
                if (data.running) {
                    statusSpan.innerHTML = 'Running';
                    indicator.className = 'status online';
                } else {
                    statusSpan.innerHTML = 'Stopped';
                    indicator.className = 'status offline';
                }
            } catch(e) {
                document.getElementById('status').innerHTML = 'Offline';
            }
        }
        
        async function loadQRCode() {
            try {
                const response = await fetch('/api/qrcode');
                const blob = await response.blob();
                const url = URL.createObjectURL(blob);
                document.getElementById('qrcode').innerHTML = '<img src="' + url + '" alt="QR Code">';
            } catch(e) {
                document.getElementById('qrcode').innerHTML = '<p>QR code unavailable</p>';
            }
        }
        
        loadStatus();
        loadQRCode();
        setInterval(loadStatus, 3000);
    </script>
</body>
</html>`
    w.Header().Set("Content-Type", "text/html")
    w.Write([]byte(html))
}

func (a *TrayApp) getLocalIP() string {
    addrs, err := net.InterfaceAddrs()
    if err != nil {
        return "127.0.0.1"
    }
    for _, addr := range addrs {
        if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() && ipnet.IP.To4() != nil {
            return ipnet.IP.String()
        }
    }
    return "127.0.0.1"
}

func (a *TrayApp) onExit() {
    if a.isRunning {
        a.stopServer()
    }
}
