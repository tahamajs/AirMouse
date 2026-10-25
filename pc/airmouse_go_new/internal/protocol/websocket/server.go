package websocket

import (
	"bytes"
	"crypto/md5"
	"crypto/tls"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"hash"
	"image"
	"image/jpeg"
	"io"
	"math"
	"net"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	gorilla "github.com/gorilla/websocket"
	"github.com/kbinani/screenshot"
	"github.com/nfnt/resize"

	"airmouse-go/control/common"
	"airmouse-go/control/mouse"
	"airmouse-go/control/syscmd"
	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	"airmouse-go/internal/proximity"
	"airmouse-go/internal/sysaction"
	"airmouse-go/internal/tlsutil"
	"airmouse-go/internal/utils"
	"airmouse-go/web"
)

const (
	maxCoordLimit  = 500.0
	maxScrollLimit = 10.0
	replayWindowMs = 5000
)

type replayDetector struct {
	mu          sync.Mutex
	lastSeq     uint64
	initialized bool
}

func newReplayDetector() *replayDetector {
	return &replayDetector{}
}

func (rd *replayDetector) checkAndAdvanceSeq(seq uint64) error {
	rd.mu.Lock()
	defer rd.mu.Unlock()
	if !rd.initialized {
		rd.lastSeq = seq
		rd.initialized = true
		return nil
	}
	if seq <= rd.lastSeq {
		return fmt.Errorf("replay_detected: seq %d <= last_seq %d", seq, rd.lastSeq)
	}
	rd.lastSeq = seq
	return nil
}

func validateTimestamp(ts, nowMs int64) error {
	diff := nowMs - ts
	if diff < 0 {
		diff = -diff
	}
	if diff > replayWindowMs {
		return fmt.Errorf("replay_rejected: delta %d ms > %d ms", diff, replayWindowMs)
	}
	return nil
}

var upgrader = gorilla.Upgrader{
	CheckOrigin:       func(r *http.Request) bool { return true },
	ReadBufferSize:    4096,
	WriteBufferSize:   4096,
	EnableCompression: true,
}

// ------------------------------------------------------------
// WSClient - WebSocket client
// ------------------------------------------------------------

type WSClient struct {
	ID             string
	DeviceID       string
	Name           string
	Conn           *gorilla.Conn
	Send           chan []byte
	BinarySend     chan []byte
	ConnectedAt    time.Time
	LastActive     time.Time
	BytesSent      int64
	BytesRecv      int64
	UserAgent      string
	IP             string
	Approved       atomic.Bool
	Authenticated  atomic.Bool
	ReplayDetector *replayDetector
	Mode           string
	Theme          string
	closeOnce      sync.Once
}

func (c *WSClient) Close() {
	c.closeOnce.Do(func() {
		_ = c.Conn.Close()
		close(c.Send)
		close(c.BinarySend)
	})
}

// ------------------------------------------------------------
// Server - WebSocket server
// ------------------------------------------------------------

type Server struct {
	port         int
	listener     net.Listener
	tlsConfig    *tls.Config
	clients      map[string]*WSClient
	mouse        mouse.Controller
	deviceMgr    *device.Manager
	authMgr      *auth.Manager
	proxMgr      *proximity.Manager
	mu           sync.RWMutex
	server       *http.Server
	running      bool
	totalClients int64
	startTime    time.Time
	fileSessions map[string]*fileSession
	latestAndroidScreenFrame     []byte
	latestAndroidScreenFrameTime time.Time
	
	GlobalApproveDevice func(deviceID string) error
	GlobalRejectDevice  func(deviceID string) error
}

// SetTLSConfig sets or overrides the TLS configuration for the WebSocket server.
func (s *Server) SetTLSConfig(tlsConfig *tls.Config) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.tlsConfig = tlsConfig
}

type fileSession struct {
	id        string
	name      string
	path      string
	tempPath  string
	size      int64
	received  int64
	md5Hex    string
	direction string
	hasher    hash.Hash
	file      *os.File
}

// NewServer creates a new WebSocket server.
func NewServer(port int, mouseCtrl mouse.Controller, deviceMgr *device.Manager, authMgr *auth.Manager, proxMgr *proximity.Manager) *Server {
	return &Server{
		port:         port,
		clients:      make(map[string]*WSClient),
		mouse:        mouseCtrl,
		deviceMgr:    deviceMgr,
		authMgr:      authMgr,
		proxMgr:      proxMgr,
		fileSessions: make(map[string]*fileSession),
		startTime:    time.Now(),
	}
}

// Start starts the WebSocket server.
func (s *Server) Start() error {
	cfg := config.Get()
	addr := fmt.Sprintf("0.0.0.0:%d", s.port)
	rawListener, err := net.Listen("tcp", addr)
	if err != nil {
		return fmt.Errorf("failed to bind WebSocket port %d: %w", s.port, err)
	}

	useTLS := (s.tlsConfig != nil) || (cfg != nil && cfg.TLSEnabled)
	if useTLS {
		tlsCfg := s.tlsConfig
		if tlsCfg == nil {
			var tlsErr error
			tlsCfg, tlsErr = tlsutil.GetServerTLSConfig(cfg.TLSCertPath, cfg.TLSKeyPath, []string{"localhost", "127.0.0.1"})
			if tlsErr != nil {
				_ = rawListener.Close()
				return fmt.Errorf("failed to load TLS config for WebSocket: %w", tlsErr)
			}
			s.tlsConfig = tlsCfg
		}
		s.listener = tls.NewListener(rawListener, tlsCfg)
		utils.LogInfo("WebSocket server started with TLS (WSS) on port %d", s.port)
	} else {
		s.listener = rawListener
		utils.LogInfo("WebSocket server started (cleartext WS) on port %d", s.port)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/ws", s.handleWebSocket)
	mux.HandleFunc("/health", s.handleHealth)
	mux.HandleFunc("/stats", s.handleStats) // legacy
	mux.HandleFunc("/api/stats", s.handleAPIStats)
	mux.HandleFunc("/api/status", s.handleAPIStatus)
	mux.HandleFunc("/status", s.handleAPIStatus)
	mux.HandleFunc("/api/devices", s.handleAPIDevices)
	mux.HandleFunc("/api/devices/approve", s.handleDeviceApprove)
	mux.HandleFunc("/api/devices/reject", s.handleDeviceReject)
	mux.HandleFunc("/api/config", s.handleAPIConfig)
	mux.HandleFunc("/api/status/toggle", s.handleAPIStatusToggle)
	mux.HandleFunc("/api/command", s.handleAPICommand)
	mux.HandleFunc("/api/files", s.handleAPIFiles)
	mux.HandleFunc("/api/files/delete", s.handleAPIFilesDelete)
	mux.HandleFunc("/api/displays", s.handleAPIDisplays)
	mux.HandleFunc("/upload", s.handleHTTPUpload)
	mux.HandleFunc("/download/", s.handleHTTPDownload)
	mux.HandleFunc("/mirror", s.handleScreenMirror)
	mux.HandleFunc("/screen", s.handleScreenMirror)
	mux.Handle("/static/", http.FileServer(http.FS(web.WebFS)))
	mux.Handle("/client/", http.StripPrefix("/client/", http.FileServer(http.FS(web.WebFS))))
	mux.HandleFunc("/mobile", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/client/index.html", http.StatusMovedPermanently)
	})
	mux.HandleFunc("/client", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/client/index.html", http.StatusMovedPermanently)
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/" {
			path := strings.TrimPrefix(r.URL.Path, "/")
			if _, err := web.WebFS.Open(path); err == nil {
				http.FileServer(http.FS(web.WebFS)).ServeHTTP(w, r)
				return
			}
			http.NotFound(w, r)
			return
		}
		// Serve index.html directly to avoid http.FileServer's redirect-to-dir loop
		file, err := web.WebFS.Open("index.html")
		if err != nil {
			http.Error(w, "Failed to load index.html", http.StatusInternalServerError)
			return
		}
		defer file.Close()
		content, err := io.ReadAll(file)
		if err != nil {
			http.Error(w, "Failed to read index.html", http.StatusInternalServerError)
			return
		}
		w.Header().Set("Content-Type", "text/html; charset=utf-8")
		_, _ = w.Write(content)
	})

	s.server = &http.Server{
		Handler:      mux,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	s.mu.Lock()
	s.running = true
	s.startTime = time.Now()
	s.mu.Unlock()

	go func() {
		utils.LogInfo("WebSocket server starting on port %d", s.port)
		if err := s.server.Serve(s.listener); err != nil && err != http.ErrServerClosed {
			utils.LogError("WebSocket server error: %v", err)
		}
	}()
	return nil
}

// Stop stops the WebSocket server.
func (s *Server) Stop() error {
	s.mu.Lock()
	if !s.running {
		s.mu.Unlock()
		return nil
	}
	s.running = false
	listener := s.listener
	s.listener = nil
	server := s.server
	s.server = nil

	clients := make([]*WSClient, 0, len(s.clients))
	for _, c := range s.clients {
		clients = append(clients, c)
	}
	s.clients = make(map[string]*WSClient)
	s.mu.Unlock()

	// 1. Broadcast server_closing packet to all connected WebSocket clients
	closingMsg := ServerClosingMessage()
	for _, c := range clients {
		_ = c.Conn.SetWriteDeadline(time.Now().Add(500 * time.Millisecond))
		_ = c.Conn.WriteMessage(gorilla.TextMessage, closingMsg)
	}
	time.Sleep(50 * time.Millisecond) // Short buffer flush window

	// 2. Close listener and HTTP server
	if listener != nil {
		_ = listener.Close()
	}
	if server != nil {
		_ = server.Close()
	}

	// 3. Close all client connections
	for _, c := range clients {
		c.Close()
	}

	// 4. Release mouse buttons and keys so nothing remains stuck
	if s.mouse != nil {
		s.mouse.MouseUp("left")
		s.mouse.MouseUp("right")
		s.mouse.MouseUp("middle")
	}
	sysaction.ReleaseAllKeys()

	utils.LogInfo("WebSocket server stopped cleanly with graceful teardown on port %d", s.port)
	return nil
}

// ClientSessionSnapshot represents connected WebSocket client details for live metrics.
type ClientSessionSnapshot struct {
	ID          string    `json:"id"`
	DeviceID    string    `json:"device_id"`
	Name        string    `json:"name"`
	IP          string    `json:"ip"`
	Protocol    string    `json:"protocol"`
	ConnectedAt time.Time `json:"connected_at"`
	LastActive  time.Time `json:"last_active"`
	BytesSent   int64     `json:"bytes_sent"`
	BytesRecv   int64     `json:"bytes_recv"`
	Approved    bool      `json:"approved"`
}

// GetClientSessions returns snapshots of all currently connected WebSocket clients.
func (s *Server) GetClientSessions() []ClientSessionSnapshot {
	s.mu.RLock()
	defer s.mu.RUnlock()
	sessions := make([]ClientSessionSnapshot, 0, len(s.clients))
	for _, c := range s.clients {
		sessions = append(sessions, ClientSessionSnapshot{
			ID:          c.ID,
			DeviceID:    c.DeviceID,
			Name:        c.Name,
			IP:          c.IP,
			Protocol:    "WebSocket",
			ConnectedAt: c.ConnectedAt,
			LastActive:  c.LastActive,
			BytesSent:   c.BytesSent,
			BytesRecv:   c.BytesRecv,
			Approved:    c.Approved.Load(),
		})
	}
	return sessions
}

// handleWebSocket handles WebSocket connections.
func (s *Server) handleWebSocket(w http.ResponseWriter, r *http.Request) {
	cfg := config.Get()

	// Authenticate if token provided via query parameter
	authenticatedFromQuery := false
	if cfg.AuthEnabled {
		token := r.URL.Query().Get("token")
		if token != "" {
			if s.authMgr != nil && s.authMgr.ValidateToken(token) {
				authenticatedFromQuery = true
			} else {
				w.WriteHeader(http.StatusUnauthorized)
				_, _ = w.Write([]byte(`{"error":"unauthorized"}`))
				return
			}
		}
	}

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		utils.LogError("WebSocket upgrade failed: %v", err)
		return
	}
	if tcpConn, ok := conn.UnderlyingConn().(*net.TCPConn); ok {
		_ = tcpConn.SetNoDelay(true)
	}

	id := utils.GenerateID()
	client := &WSClient{
		ID:             id,
		Name:           "Unknown",
		Conn:           conn,
		Send:           make(chan []byte, 256),
		BinarySend:     make(chan []byte, 128),
		ConnectedAt:    time.Now(),
		LastActive:     time.Now(),
		UserAgent:      r.UserAgent(),
		IP:             r.RemoteAddr,
		ReplayDetector: newReplayDetector(),
	}
	client.Approved.Store(false)
	client.Authenticated.Store(authenticatedFromQuery)

	s.mu.Lock()
	s.clients[id] = client
	s.totalClients++
	s.mu.Unlock()

	if s.deviceMgr != nil {
		_ = s.deviceMgr.RegisterDevice(id, device.TypeWebSocket, "Android")
		_ = s.deviceMgr.UpdateDeviceStatus(id, device.StatusPendingApproval)
	}

	utils.LogInfo("WebSocket client connected: id=%s, ip=%s", id, client.IP)
	utils.LogInfo("WebSocket approval pending: id=%s", id)
	utils.LogDebug("WebSocket initial client state: id=%s approved=%v device=%s", id, client.Approved.Load(), client.DeviceID)

	go s.readLoop(client)
	go s.writeLoop(client)
}

// handleHealth handles health checks.
func (s *Server) handleHealth(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	_, _ = w.Write([]byte(`{"status":"ok","timestamp":"` + time.Now().Format(time.RFC3339) + `"}`))
}

// handleStats handles stats requests.
func (s *Server) handleStats(w http.ResponseWriter, r *http.Request) {
	s.mu.RLock()
	clientCount := len(s.clients)
	s.mu.RUnlock()

	w.Header().Set("Content-Type", "application/json")
	_, _ = fmt.Fprintf(w, `{"clients":%d,"running":%v,"uptime":%.0f}`, clientCount, s.running, time.Since(s.startTime).Seconds())
}

// readLoop reads messages from the client.
func (s *Server) readLoop(client *WSClient) {
	defer func() {
		client.Close()

		if s.mouse != nil {
			s.mouse.MouseUp("left")
			s.mouse.MouseUp("right")
			s.mouse.MouseUp("middle")
		}
		sysaction.ReleaseAllKeys()

		s.mu.Lock()
		delete(s.clients, client.ID)
		if session, ok := s.fileSessions[client.ID]; ok {
			if session.file != nil {
				_ = session.file.Close()
			}
			_ = os.Remove(session.tempPath)
			delete(s.fileSessions, client.ID)
		}
		s.mu.Unlock()

		if s.deviceMgr != nil {
			deviceID := client.DeviceID
			if deviceID == "" {
				deviceID = client.ID
			}
			_ = s.deviceMgr.UpdateDeviceStatus(deviceID, device.StatusDisconnected)
		}
		utils.LogInfo("WebSocket client disconnected: id=%s", client.ID)
	}()

	for {
		messageType, data, err := client.Conn.ReadMessage()
		if err != nil {
			if !gorilla.IsUnexpectedCloseError(err, gorilla.CloseGoingAway, gorilla.CloseAbnormalClosure) {
				utils.LogDebug("WebSocket read error: %v", err)
			}
			return
		}

		s.mu.Lock()
		client.LastActive = time.Now()
		client.BytesRecv += int64(len(data))
		devID := client.DeviceID
		if devID == "" {
			devID = client.ID
		}
		s.mu.Unlock()
		utils.RecordPacketIn(int64(len(data)))

		if s.deviceMgr != nil {
			_ = s.deviceMgr.UpdateDeviceActivity(devID, 0, int64(len(data)))
		}

		if messageType == gorilla.BinaryMessage {
			if s.handleBinaryFileChunk(client, data) {
				continue
			}
			s.mu.Lock()
			s.latestAndroidScreenFrame = data
			s.latestAndroidScreenFrameTime = time.Now()
			s.mu.Unlock()
			continue
		}

		msgType, payload, id, err := DecodeWireMessage(data)
		if err != nil {
			utils.LogDebug("Invalid WebSocket message: %v", err)
			continue
		}

		s.processMessage(client, msgType, payload, id)
	}
}

// writeLoop writes messages to the client.
func (s *Server) writeLoop(client *WSClient) {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()

	for {
		select {
		case msg, ok := <-client.Send:
			if !ok {
				return
			}
			_ = client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := client.Conn.WriteMessage(gorilla.TextMessage, msg); err != nil {
				utils.LogDebug("WebSocket write error: %v", err)
				return
			}
			utils.RecordPacketOut(int64(len(msg)))
			s.mu.Lock()
			client.BytesSent += int64(len(msg))
			devID := client.DeviceID
			if devID == "" {
				devID = client.ID
			}
			s.mu.Unlock()

			if s.deviceMgr != nil {
				_ = s.deviceMgr.UpdateDeviceActivity(devID, int64(len(msg)), 0)
			}

		case data, ok := <-client.BinarySend:
			if !ok {
				return
			}
			_ = client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := client.Conn.WriteMessage(gorilla.BinaryMessage, data); err != nil {
				utils.LogDebug("WebSocket binary write error: %v", err)
				return
			}
			s.mu.Lock()
			client.BytesSent += int64(len(data))
			devID := client.DeviceID
			if devID == "" {
				devID = client.ID
			}
			s.mu.Unlock()

			if s.deviceMgr != nil {
				_ = s.deviceMgr.UpdateDeviceActivity(devID, int64(len(data)), 0)
			}

		case <-ticker.C:
			_ = client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := client.Conn.WriteMessage(gorilla.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

// processMessage processes incoming messages.
func (s *Server) processMessage(client *WSClient, msgType string, payload map[string]any, id *string) {
	if id != nil && (msgType == "click" || msgType == "doubleclick" || msgType == "rightclick" || msgType == "scroll" ||
		msgType == "left_click" || msgType == "right_click" || msgType == "double_click" || msgType == "middle_click" || msgType == "middleclick" ||
		msgType == "hscroll" || msgType == "key" || msgType == "keypress" || msgType == "key_press" || msgType == "keydown" ||
		msgType == "key_down" || msgType == "keyup" || msgType == "key_up" || msgType == "hotkey" || msgType == "type" || msgType == "text" ||
		msgType == "slide_next" || msgType == "slide_prev" || msgType == "volume_up" || msgType == "volume_down" || msgType == "mute" ||
		msgType == "play_pause" || msgType == "lock_screen" || msgType == "power_sleep" || msgType == "brightness" || msgType == "voice_command") {
		if ack := AckMessage(id); len(ack) > 0 {
			select {
			case client.Send <- ack:
				utils.RecordPacketOut(int64(len(ack)))
			default:
				utils.LogDebug("WebSocket ack dropped (client send buffer full): %s", client.ID)
			}
		}
	}

	// If auth is enabled, unauthenticated clients must authenticate via hello before sending actions
	if cfg := config.Get(); cfg.AuthEnabled {
		if !client.Authenticated.Load() && msgType != "hello" && msgType != "device_info" && msgType != "ping" && msgType != "heartbeat" {
			utils.LogDebug("Ignoring WebSocket %s while unauthenticated: device=%s", msgType, client.ID)
			return
		}
	}

	approved := client.Approved.Load()
	if !approved && msgType != "hello" && msgType != "device_info" && msgType != "ping" && msgType != "heartbeat" {
		utils.LogDebug("Ignoring WebSocket %s while waiting for approval: device=%s", msgType, client.ID)
		return
	}

	// Replay and timestamp checks if seq/ts are present
	if client.ReplayDetector != nil {
		nowMs := time.Now().UnixMilli()
		if tsVal, ok := payload["ts"]; ok {
			ts := int64(toFloat64(tsVal))
			if ts > 0 {
				if err := validateTimestamp(ts, nowMs); err != nil {
					utils.RecordDroppedPacket()
					utils.LogInfo("WebSocket replay rejection (timestamp): client=%s err=%v", client.ID, err)
					return
				}
			}
		}
		if seqVal, ok := payload["seq"]; ok {
			seq := uint64(toFloat64(seqVal))
			if seq > 0 {
				if err := client.ReplayDetector.checkAndAdvanceSeq(seq); err != nil {
					utils.RecordDroppedPacket()
					utils.LogInfo("WebSocket replay rejection (sequence): client=%s err=%v", client.ID, err)
					return
				}
			}
		}
	}

	switch msgType {
	case "file":
		s.processFileMessage(client, payload)

	case "move", "move_rel":
		dx := firstFloat64(payload, "dx", "DeltaX", "deltaX")
		dy := firstFloat64(payload, "dy", "DeltaY", "deltaY")
		if math.IsNaN(dx) || math.IsInf(dx, 0) || math.IsNaN(dy) || math.IsInf(dy, 0) ||
			math.Abs(dx) > maxCoordLimit || math.Abs(dy) > maxCoordLimit {
			utils.RecordDroppedPacket()
			utils.LogInfo("WebSocket move dropped (out of bounds or NaN/Inf): dx=%.2f dy=%.2f", dx, dy)
			return
		}
		if s.mouse != nil {
			s.mouse.Move(dx, dy)
		}

	case "move_abs":
		targetX := firstFloat64(payload, "x", "target_x", "X")
		targetY := firstFloat64(payload, "y", "target_y", "Y")
		if math.IsNaN(targetX) || math.IsInf(targetX, 0) || math.IsNaN(targetY) || math.IsInf(targetY, 0) {
			utils.RecordDroppedPacket()
			utils.LogInfo("WebSocket move_abs dropped (NaN/Inf): x=%.2f y=%.2f", targetX, targetY)
			return
		}
		if s.mouse != nil {
			curX, curY := s.mouse.GetPosition()
			dx := targetX - curX
			dy := targetY - curY
			if math.Abs(dx) > maxCoordLimit {
				if dx > 0 {
					dx = maxCoordLimit
				} else {
					dx = -maxCoordLimit
				}
			}
			if math.Abs(dy) > maxCoordLimit {
				if dy > 0 {
					dy = maxCoordLimit
				} else {
					dy = -maxCoordLimit
				}
			}
			s.mouse.Move(dx, dy)
		}

	case "click", "left_click":
		button, _ := payload["button"].(string)
		if button == "" {
			button = "left"
		}
		if s.mouse != nil {
			s.mouse.Click(button)
		}
		utils.LogDebug("Click received: device=%s, button=%s", client.ID, button)

	case "mouse_down":
		button, _ := payload["button"].(string)
		if button == "" {
			button = "left"
		}
		if s.mouse != nil {
			s.mouse.MouseDown(button)
		}
		utils.LogDebug("Mouse down received: device=%s, button=%s", client.ID, button)

	case "mouse_up":
		button, _ := payload["button"].(string)
		if button == "" {
			button = "left"
		}
		if s.mouse != nil {
			s.mouse.MouseUp(button)
		}
		utils.LogDebug("Mouse up received: device=%s, button=%s", client.ID, button)

	case "doubleclick", "double_click":
		if s.mouse != nil {
			s.mouse.DoubleClick()
		}
		utils.LogDebug("Double click received: device=%s", client.ID)

	case "rightclick", "right_click":
		if s.mouse != nil {
			s.mouse.Click("right")
		}
		utils.LogDebug("Right click received: device=%s", client.ID)

	case "middle_click", "middleclick":
		if s.mouse != nil {
			s.mouse.Click("middle")
		}
		utils.LogDebug("Middle click received: device=%s", client.ID)

	case "scroll":
		delta := firstInt(payload, "delta", "Scroll", "scroll", "amount", "dy")
		if math.Abs(float64(delta)) > maxScrollLimit {
			utils.RecordDroppedPacket()
			utils.LogInfo("WebSocket scroll dropped (|delta| <= %.1f): delta=%d", maxScrollLimit, delta)
			return
		}
		if s.mouse != nil {
			s.mouse.Scroll(delta)
		}
		utils.LogDebug("Scroll received: device=%s, delta=%d", client.ID, delta)

	case "hscroll":
		delta := firstInt(payload, "delta", "hscroll", "amount", "dx")
		if math.Abs(float64(delta)) > maxScrollLimit {
			utils.RecordDroppedPacket()
			utils.LogInfo("WebSocket hscroll dropped (|delta| <= %.1f): delta=%d", maxScrollLimit, delta)
			return
		}
		sysaction.MouseHScroll(delta)
		utils.LogDebug("WebSocket hscroll received: device=%s, delta=%d", client.ID, delta)

	case "slide_next":
		utils.LogInfo("WebSocket slide_next received: device=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("slide_next")

	case "slide_prev":
		utils.LogInfo("WebSocket slide_prev received: device=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("slide_prev")

	case "volume_up":
		utils.LogInfo("WebSocket volume_up received: device=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("volume_up")

	case "volume_down":
		utils.LogInfo("WebSocket volume_down received: device=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("volume_down")

	case "mute":
		utils.LogInfo("WebSocket mute received: device=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("mute")

	case "play_pause":
		utils.LogInfo("WebSocket play_pause received: device=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("play_pause")

	case "lock_screen":
		utils.LogInfo("WebSocket lock_screen received: device=%s", client.ID)
		_ = syscmd.ExecuteSystemCommand("lock_screen")

	case "power_sleep":
		utils.LogInfo("WebSocket power_sleep received: device=%s", client.ID)
		sysaction.Execute(sysaction.ActionSleep)

	case "brightness":
		dir, _ := payload["direction"].(string)
		if dir == "" {
			dir, _ = payload["action"].(string)
		}
		delta := firstFloat64(payload, "delta", "amount", "value")
		if delta < 0 {
			dir = "down"
		} else if dir == "" {
			dir = "up"
		}
		if delta == 0 {
			delta = 10.0
		}
		utils.LogInfo("WebSocket brightness received: dir=%s delta=%.1f device=%s", dir, delta, client.ID)
		sysaction.AdjustBrightness(dir, math.Abs(delta))

	case "hello", "device_info":
		token, _ := payload["token"].(string)
		if config.Get().AuthEnabled {
			authenticated := client.Authenticated.Load()
			if !authenticated {
				if token != "" && s.authMgr != nil && s.authMgr.ValidateToken(token) {
					client.Authenticated.Store(true)
					authenticated = true
				}
			}
			if !authenticated {
				errMsg := `{"type":"error","payload":{"message":"connection rejected: invalid pairing token"}}` + "\n"
				_ = client.Conn.WriteMessage(gorilla.TextMessage, []byte(errMsg))
				_ = client.Conn.Close()
				utils.LogInfo("WebSocket client rejected: id=%s reason=invalid token", client.ID)
				return
			}
		}

		name, _ := payload["name"].(string)
		version, _ := payload["version"].(string)
		deviceName, _ := payload["device_name"].(string)
		model, _ := payload["model"].(string)
		manufacturer, _ := payload["manufacturer"].(string)
		brand, _ := payload["brand"].(string)
		androidVersion, _ := payload["android_version"].(string)
		sdkInt, _ := payload["sdk_int"].(string)
		deviceIDValue, _ := payload["device_id"].(string)
		protocolName, _ := payload["protocol"].(string)
		transport, _ := payload["transport"].(string)
		fingerprint := deviceIDValue
		if fingerprint == "" {
			fingerprint = device.StableDeviceID(name, deviceName, model, manufacturer, brand)
		}
		if name == "" {
			name = "Unknown"
		}
		utils.LogInfo("Handshake/Device info received from Android (WebSocket): id=%s name=%s", client.ID, name)
		utils.LogDebug("WebSocket handshake payload: id=%s version=%s device=%s model=%s android=%s protocol=%s transport=%s", client.ID, version, deviceName, model, androidVersion, protocolName, transport)

		isDeviceInfo := (msgType == "device_info")

		s.mu.Lock()
		client.Name = name
		if s.deviceMgr != nil {
			_ = s.deviceMgr.RenameDeviceID(client.ID, fingerprint)
			client.DeviceID = fingerprint
			_ = s.deviceMgr.UpsertDevice(fingerprint, device.TypeWebSocket, name, map[string]string{
				"fingerprint":     fingerprint,
				"ip_address":      client.IP,
				"version":         version,
				"device_name":     deviceName,
				"device_model":    model,
				"manufacturer":    manufacturer,
				"brand":           brand,
				"android_version": androidVersion,
				"device_id":       deviceIDValue,
				"sdk_int":         sdkInt,
				"protocol":        protocolName,
				"transport":       transport,
				"user_agent":      client.UserAgent,
			})
			_ = s.deviceMgr.UpdateDeviceStatus(fingerprint, device.StatusPendingApproval)
		}
		s.mu.Unlock()

		if isDeviceInfo || s.autoApproveWSClient(client, fingerprint) {
			_ = s.ApproveDevice(fingerprint)
			break
		}

		utils.LogInfo("WebSocket client awaiting approval: id=%s, name=%s", client.ID, name)
		utils.LogDebug("WebSocket approval state pending: id=%s approved=%v device_id=%s", client.ID, client.Approved.Load(), client.DeviceID)

	case "gesture":
		gesture, _ := payload["gesture"].(string)
		if gesture == "" {
			gesture, _ = payload["name"].(string)
		}
		confidence := toFloat64(payload["confidence"])
		if confidence == 0 {
			confidence = 1.0
		}
		utils.LogInfo("Gesture received: device=%s, gesture=%s, confidence=%.2f", client.ID, gesture, confidence)
		go sysaction.ExecuteGesture(gesture, confidence)

	case "proximity":
		if s.proxMgr == nil {
			utils.LogDebug("Proximity manager not available, ignoring proximity update")
			break
		}
		deviceID, _ := payload["device_id"].(string)
		isNear, _ := payload["is_near"].(bool)
		distance := firstFloat64(payload, "distance")
		rssi := int32(firstFloat64(payload, "rssi"))

		update := proximity.ProximityUpdate{
			DeviceID:  deviceID,
			IsNear:    isNear,
			Distance:  float32(distance),
			RSSI:      rssi,
			Timestamp: time.Now().Unix(),
		}
		s.proxMgr.ProcessUpdate(update)
		utils.LogDebug("Proximity update: device=%s near=%v dist=%.2f", deviceID, isNear, distance)

	case "control":
		command, _ := payload["command"].(string)
		switch command {
		case "mode_change":
			mode, _ := payload["mode"].(string)
			client.Mode = mode
			common.SetActiveMode(mode)
			utils.LogInfo("WebSocket mode changed: device=%s mode=%s", client.ID, mode)
		case "theme_change":
			theme, _ := payload["theme"].(string)
			client.Theme = theme
			common.SetActiveTheme(theme)
			utils.LogInfo("WebSocket theme changed: device=%s theme=%s", client.ID, theme)
		case "start", "touchpad_start", "resume", "resume_movement":
			common.SetMovementPaused(false)
			utils.LogInfo("Movement resumed by device: %s command=%s", client.ID, command)
		case "touchpad_stop", "pause", "pause_movement":
			common.SetMovementPaused(true)
			utils.LogInfo("Movement paused by device: %s command=%s", client.ID, command)
		case "show_desktop", "task_view", "switch_window", "lock_screen", "window_close", "zoom_in", "zoom_out", "zoom_reset",
			"volume_up", "volume_down", "mute", "play_pause", "next_track", "prev_track", "stop",
			"window_maximize", "window_minimize", "window_fullscreen",
			"browser_back", "browser_forward", "browser_refresh", "browser_home":
			go func(cmd string) {
				if err := syscmd.ExecuteSystemCommand(cmd); err != nil {
					utils.LogError("WebSocket system command failed: device=%s command=%s err=%v", client.ID, cmd, err)
				} else {
					utils.LogInfo("WebSocket system command executed: device=%s command=%s", client.ID, cmd)
				}
			}(command)
		case "calibrate":
			utils.LogInfo("WebSocket calibration control command received: device=%s", client.ID)
		default:
			utils.LogDebug("Unknown control command: %s", command)
		}

	case "presentation":
		action, _ := payload["action"].(string)
		utils.LogInfo("Presentation action received: device=%s action=%s", client.ID, action)
		switch action {
		case "prev":
			_ = syscmd.ExecuteSystemCommand("prev_track")
		case "next":
			_ = syscmd.ExecuteSystemCommand("next_track")
		case "fullscreen":
			_ = syscmd.ExecuteSystemCommand("window_fullscreen")
		}

	case "media":
		action, _ := payload["action"].(string)
		utils.LogInfo("Media action received: device=%s action=%s", client.ID, action)
		switch action {
		case "playpause":
			_ = syscmd.ExecuteSystemCommand("play_pause")
		case "prev":
			_ = syscmd.ExecuteSystemCommand("prev_track")
		case "next":
			_ = syscmd.ExecuteSystemCommand("next_track")
		case "volumeup":
			_ = syscmd.ExecuteSystemCommand("volume_up")
		case "volumedown":
			_ = syscmd.ExecuteSystemCommand("volume_down")
		}

	case "laser", "draw", "annotation":
		utils.LogInfo("Received %s data from client %s", msgType, client.ID)

	case "steering":
		angle := toFloat64(payload["angle"])
		utils.LogInfo("Gaming Mode Steering received: device=%s, angle=%.2f", client.ID, angle)
		// Emulate keypress based on steering angle
		if angle > 15 {
			go sysaction.KeyTap("d") // Steer Right
		} else if angle < -15 {
			go sysaction.KeyTap("a") // Steer Left
		}

	case "shooter":
		action, _ := payload["action"].(string)
		utils.LogInfo("Gaming Mode Shooter received: device=%s, action=%s", client.ID, action)
		if action == "shoot" {
			go sysaction.KeyTap("space")
		} else if action == "reload" {
			go sysaction.KeyTap("r")
		}

	case "keypress", "key_press", "key":
		keyVal, _ := payload["key"].(string)
		if keyVal == "" {
			keyVal, _ = payload["code"].(string)
		}
		if keyVal == "" {
			keyVal, _ = payload["value"].(string)
		}
		if keyVal != "" {
			utils.LogInfo("WebSocket key event %s: key=%s client=%s", msgType, keyVal, client.ID)
			go sysaction.KeyTap(keyVal)
		}

	case "keydown", "key_down":
		keyVal, _ := payload["key"].(string)
		if keyVal == "" {
			keyVal, _ = payload["code"].(string)
		}
		if keyVal == "" {
			keyVal, _ = payload["value"].(string)
		}
		if keyVal != "" {
			utils.LogInfo("WebSocket keydown: key=%s client=%s", keyVal, client.ID)
			go sysaction.KeyDown(keyVal)
		}

	case "keyup", "key_up":
		keyVal, _ := payload["key"].(string)
		if keyVal == "" {
			keyVal, _ = payload["code"].(string)
		}
		if keyVal == "" {
			keyVal, _ = payload["value"].(string)
		}
		if keyVal != "" {
			utils.LogInfo("WebSocket keyup: key=%s client=%s", keyVal, client.ID)
			go sysaction.KeyUp(keyVal)
		}

	case "hotkey":
		keyVal, _ := payload["key"].(string)
		var modifiers []string
		if modsRaw, ok := payload["modifiers"].([]interface{}); ok {
			for _, m := range modsRaw {
				if ms, ok := m.(string); ok {
					modifiers = append(modifiers, ms)
				}
			}
		} else if modsStr, ok := payload["modifiers"].(string); ok {
			for _, part := range strings.Split(modsStr, "+") {
				part = strings.TrimSpace(part)
				if part != "" {
					modifiers = append(modifiers, part)
				}
			}
		}
		if keyVal != "" || len(modifiers) > 0 {
			utils.LogInfo("WebSocket hotkey: key=%s mods=%v client=%s", keyVal, modifiers, client.ID)
			go sysaction.Hotkey(modifiers, keyVal)
		}

	case "type", "text":
		textVal, _ := payload["text"].(string)
		if textVal == "" {
			textVal, _ = payload["value"].(string)
		}
		if textVal != "" {
			utils.LogInfo("WebSocket type event %s: client=%s", msgType, client.ID)
			go sysaction.TypeText(textVal)
		}

	case "smarthome":
		utils.LogInfo("Received smarthome command from client %s", client.ID)

	case "calibration_data":
		if !client.Approved.Load() {
			utils.LogDebug("Ignoring WebSocket calibration_data while waiting for approval: %s", client.ID)
			return
		}
		cp, err := DecodeCalibrationPayload(payload)
		if err != nil {
			utils.LogError("Failed to decode WebSocket calibration_data: %v", err)
			return
		}
		utils.LogInfo("WebSocket calibration_data received: device=%s, quality=%s, gyro_bias=(%.4f,%.4f,%.4f), accel_bias=(%.4f,%.4f,%.4f)",
			client.ID, cp.Quality, cp.Gyro.X, cp.Gyro.Y, cp.Gyro.Z, cp.Accel.X, cp.Accel.Y, cp.Accel.Z)

	case "ping", "heartbeat":
		select {
		case client.Send <- PongMessage():
		default:
		}

	case "pong":
		utils.LogDebug("Pong received from: %s", client.ID)
		if ts, ok := payload["timestamp"].(float64); ok && ts > 0 {
			nowMs := float64(time.Now().UnixMilli())
			rttMs := nowMs - ts
			if rttMs >= 0 && rttMs < 10000 {
				utils.RecordPingLatency(rttMs)
			}
		}

	case "stats_req":
		snapshot := utils.GetTelemetrySnapshot()
		resp := StatsRespMessage(map[string]any{
			"packets_received":  snapshot.TotalPacketsIn,
			"packets_sent":      snapshot.TotalPacketsOut,
			"bytes_received":    snapshot.TotalBytesIn,
			"bytes_sent":        snapshot.TotalBytesOut,
			"throughput_kbps":   snapshot.BytesInKBps,
			"dropped_packets":   snapshot.DroppedPackets,
			"avg_ping_ms":       snapshot.AveragePingMs,
			"uptime_seconds":    snapshot.UptimeSeconds,
			"clients_connected": s.GetClientCount(),
		}, nil)
		select {
		case client.Send <- resp:
		default:
		}

	case "stats_resp", "stats":
		utils.LogDebug("WebSocket stats received from client: %s", client.ID)

	case "battery", "battery_report":
		var level int = -1
		var charging bool
		if lvl, ok := payload["level"].(float64); ok {
			level = int(lvl)
		} else if pct, ok := payload["percentage"].(float64); ok {
			level = int(pct)
		}
		if chg, ok := payload["charging"].(bool); ok {
			charging = chg
		} else if isChg, ok := payload["is_charging"].(bool); ok {
			charging = isChg
		}
		if level >= 0 && s.deviceMgr != nil {
			devID := client.DeviceID
			if devID == "" {
				devID = client.ID
			}
			s.deviceMgr.UpdateDeviceBattery(devID, level, charging)
			utils.LogInfo("Updated device battery: id=%s level=%d%% charging=%v", devID, level, charging)
		}

	case "voice_command":
		command, _ := payload["command"].(string)
		utils.LogInfo("WebSocket voice command received: command=%s client=%s", command, client.ID)
		cmdLower := strings.ToLower(strings.TrimSpace(command))
		var act sysaction.Action
		switch {
		case strings.Contains(cmdLower, "play") || strings.Contains(cmdLower, "pause") || strings.Contains(cmdLower, "resume"):
			act = sysaction.ActionPlayPause
		case strings.Contains(cmdLower, "next"):
			act = sysaction.ActionNextTrack
		case strings.Contains(cmdLower, "previous") || strings.Contains(cmdLower, "prev"):
			act = sysaction.ActionPrevTrack
		case strings.Contains(cmdLower, "stop"):
			act = sysaction.ActionStop
		case strings.Contains(cmdLower, "volume up") || strings.Contains(cmdLower, "louder"):
			act = sysaction.ActionVolumeUp
		case strings.Contains(cmdLower, "volume down") || strings.Contains(cmdLower, "quieter"):
			act = sysaction.ActionVolumeDown
		case strings.Contains(cmdLower, "mute"):
			act = sysaction.ActionMute
		case strings.Contains(cmdLower, "lock"):
			act = sysaction.ActionLockScreen
		case strings.Contains(cmdLower, "desktop") || strings.Contains(cmdLower, "home"):
			act = sysaction.ActionShowDesktop
		case strings.Contains(cmdLower, "close"):
			act = sysaction.ActionCloseWindow
		case strings.Contains(cmdLower, "copy"):
			act = sysaction.ActionCopy
		case strings.Contains(cmdLower, "paste"):
			act = sysaction.ActionPaste
		case strings.Contains(cmdLower, "undo"):
			act = sysaction.ActionUndo
		case strings.Contains(cmdLower, "redo"):
			act = sysaction.ActionRedo
		}
		if act != "" {
			go sysaction.Execute(act)
		}

	default:
		utils.LogDebug("Unknown message type: %s from device: %s", msgType, client.ID)
	}
}

// autoApproveWSClient checks if the device is trusted and auto-approves.
func (s *Server) autoApproveWSClient(client *WSClient, fingerprint string) bool {
	if s.deviceMgr == nil || fingerprint == "" {
		return false
	}
	// Use config trusted list
	if !config.Get().IsTrustedDevice(fingerprint) {
		return false
	}
	// Update device manager to connected state
	_ = s.deviceMgr.UpsertDevice(fingerprint, device.TypeWebSocket, client.Name, map[string]string{
		"fingerprint": fingerprint,
		"ip_address":  client.IP,
		"user_agent":  client.UserAgent,
	})
	_ = s.deviceMgr.UpdateDeviceStatus(fingerprint, device.StatusConnected)

	cfg := config.Get()
	welcomeMsg := WelcomeMessage(cfg.ServerName, cfg.Version)
	select {
	case client.Send <- welcomeMsg:
		client.Approved.Store(true)
		client.DeviceID = fingerprint
		common.SetMovementPaused(false)
		common.ClearPause()
		utils.LogInfo("WebSocket auto-approved trusted device: %s (fingerprint: %s)", client.ID, fingerprint)
		return true
	default:
		utils.LogWarn("WebSocket auto-approve welcome send blocked: %s", client.ID)
		return false
	}
}

// UpdateScreenFrame updates the latest screen frame from external sources (e.g. UDP client mirroring).
func (s *Server) UpdateScreenFrame(data []byte) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.latestAndroidScreenFrame = data
	s.latestAndroidScreenFrameTime = time.Now()
}

// ApproveDevice approves a pending WebSocket client and sends the welcome message.
func (s *Server) ApproveDevice(deviceID string) error {
	s.mu.Lock()
	defer s.mu.Unlock()

	var client *WSClient
	for _, c := range s.clients {
		if c.DeviceID == deviceID || c.ID == deviceID {
			client = c
			break
		}
	}
	if client == nil {
		return fmt.Errorf("websocket client not found: %s", deviceID)
	}

	if client.Approved.Load() {
		return nil
	}

	client.Approved.Store(true)
	common.SetMovementPaused(false)
	common.ClearPause()

	if s.deviceMgr != nil {
		_ = s.deviceMgr.UpdateDeviceStatus(deviceID, device.StatusConnected)
	}

	cfg := config.Get()
	cfg.AddTrustedDevice(deviceID)

	welcomeMsg := WelcomeMessage(cfg.ServerName, cfg.Version)
	select {
	case client.Send <- welcomeMsg:
		utils.LogInfo("WebSocket approval accepted and welcome sent: device=%s", deviceID)
	default:
		utils.LogWarn("WebSocket approval accepted but welcome send blocked (channel full): device=%s", deviceID)
	}
	return nil
}

// ---------------------------------------------------------------------
// File transfer helpers
// ---------------------------------------------------------------------

func (s *Server) safeSendBinary(client *WSClient, data []byte) bool {
	defer func() {
		_ = recover()
	}()
	select {
	case client.BinarySend <- data:
		return true
	default:
		return false
	}
}

func (s *Server) safeSendText(client *WSClient, data []byte) bool {
	defer func() {
		_ = recover()
	}()
	select {
	case client.Send <- data:
		return true
	default:
		return false
	}
}

func fileMD5(path string) (string, error) {
	f, err := os.Open(path)
	if err != nil {
		return "", err
	}
	defer f.Close()

	h := md5.New()
	if _, err := io.Copy(h, f); err != nil {
		return "", err
	}
	return hex.EncodeToString(h.Sum(nil)), nil
}

func moveFile(src, dst string) error {
	if err := os.Rename(src, dst); err == nil {
		return nil
	}
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer in.Close()

	out, err := os.OpenFile(dst, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0644)
	if err != nil {
		return err
	}
	defer out.Close()

	if _, err = io.Copy(out, in); err != nil {
		return err
	}
	in.Close()
	return os.Remove(src)
}

func (s *Server) handleWSDownload(client *WSClient, id string, name string) {
	dir := s.fileTransferDir()
	path := filepath.Join(dir, sanitizeFileName(name))

	md5Hex, err := fileMD5(path)
	if err != nil {
		errEscaped := strings.ReplaceAll(err.Error(), `"`, `\"`)
		s.safeSendText(client, []byte(fmt.Sprintf(`{"type":"file","action":"error","id":"%s","message":"%s"}`+"\n", id, errEscaped)))
		return
	}

	file, err := os.Open(path)
	if err != nil {
		errEscaped := strings.ReplaceAll(err.Error(), `"`, `\"`)
		s.safeSendText(client, []byte(fmt.Sprintf(`{"type":"file","action":"error","id":"%s","message":"%s"}`+"\n", id, errEscaped)))
		return
	}
	defer file.Close()

	fi, err := file.Stat()
	if err != nil {
		errEscaped := strings.ReplaceAll(err.Error(), `"`, `\"`)
		s.safeSendText(client, []byte(fmt.Sprintf(`{"type":"file","action":"error","id":"%s","message":"%s"}`+"\n", id, errEscaped)))
		return
	}
	fileSize := fi.Size()

	start := map[string]any{
		"type":      "file",
		"action":    "start",
		"id":        id,
		"name":      name,
		"size":      fileSize,
		"md5":       md5Hex,
		"version":   1,
		"direction": "download",
	}

	if raw, err := json.Marshal(start); err == nil {
		s.safeSendText(client, append(raw, '\n'))
	}

	buf := make([]byte, 64*1024)
	var sentBytes int64
	for {
		nr, rerr := file.Read(buf)
		if nr > 0 {
			chunk := make([]byte, nr)
			copy(chunk, buf[:nr])
			if !s.safeSendBinary(client, chunk) {
				return
			}
			sentBytes += int64(nr)
		}
		if rerr != nil {
			break
		}
	}

	s.safeSendText(client, []byte(fmt.Sprintf(`{"type":"file","action":"complete","id":"%s","md5":"%s","bytes":%d}`+"\n", id, md5Hex, sentBytes)))
}

func (s *Server) processFileMessage(client *WSClient, payload map[string]any) {
	action, _ := payload["action"].(string)
	id, _ := payload["id"].(string)
	name, _ := payload["name"].(string)

	switch action {
	case "start":
		if id == "" || name == "" {
			s.safeSendText(client, []byte(`{"type":"file","action":"error","message":"missing file metadata"}`+"\n"))
			return
		}
		s.mu.Lock()
		dir := s.fileTransferDir()
		_ = os.MkdirAll(dir, 0755)
		tempPath := filepath.Join(dir, fmt.Sprintf(".upload_%s_%s.part", id, sanitizeFileName(name)))
		file, err := os.OpenFile(tempPath, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0644)
		if err != nil {
			s.mu.Unlock()
			errEscaped := strings.ReplaceAll(err.Error(), `"`, `\"`)
			s.safeSendText(client, []byte(fmt.Sprintf(`{"type":"file","action":"error","id":"%s","message":"%s"}`+"\n", id, errEscaped)))
			return
		}
		session := &fileSession{
			id:        id,
			name:      name,
			path:      filepath.Join(dir, sanitizeFileName(name)),
			tempPath:  tempPath,
			size:      toInt64(payload["size"]),
			md5Hex:    strings.ToLower(toString(payload["md5"])),
			direction: "upload",
			hasher:    md5.New(),
			file:      file,
		}
		s.fileSessions[client.ID] = session
		s.mu.Unlock()
		s.safeSendText(client, []byte(fmt.Sprintf(`{"type":"file","action":"ack","id":"%s"}`+"\n", id)))

	case "complete":
		s.mu.Lock()
		session := s.fileSessions[client.ID]
		delete(s.fileSessions, client.ID)
		s.mu.Unlock()
		if session == nil || session.id != id {
			return
		}
		if session.file != nil {
			_ = session.file.Close()
		}
		actualMD5 := hex.EncodeToString(session.hasher.Sum(nil))
		if session.md5Hex != "" && actualMD5 != session.md5Hex {
			_ = os.Remove(session.tempPath)
			s.safeSendText(client, []byte(fmt.Sprintf(`{"type":"file","action":"error","id":"%s","message":"md5 mismatch"}`+"\n", id)))
			return
		}
		if err := moveFile(session.tempPath, session.path); err != nil {
			_ = os.Remove(session.tempPath)
			errEscaped := strings.ReplaceAll(err.Error(), `"`, `\"`)
			s.safeSendText(client, []byte(fmt.Sprintf(`{"type":"file","action":"error","id":"%s","message":"%s"}`+"\n", id, errEscaped)))
			return
		}
		s.safeSendText(client, []byte(fmt.Sprintf(`{"type":"file","action":"complete","id":"%s","md5":"%s","bytes":%d}`+"\n", id, actualMD5, session.received)))

	case "download":
		if name == "" {
			s.safeSendText(client, []byte(`{"type":"file","action":"error","message":"missing file name"}`+"\n"))
			return
		}
		go s.handleWSDownload(client, id, name)
	}
}

func (s *Server) fileTransferDir() string {
	if cfg := config.Get(); cfg != nil {
		if cfg.LogFile != "" {
			base := filepath.Dir(cfg.LogFile)
			if base != "." {
				return filepath.Join(base, "airmouse-files")
			}
		}
	}
	return filepath.Join(os.TempDir(), "airmouse-files")
}

func (s *Server) handleBinaryFileChunk(client *WSClient, data []byte) bool {
	s.mu.Lock()
	session := s.fileSessions[client.ID]
	s.mu.Unlock()
	if session == nil || session.direction != "upload" || session.file == nil {
		return false
	}
	if _, err := session.file.Write(data); err != nil {
		select {
		case client.Send <- []byte(fmt.Sprintf(`{"type":"file","action":"error","id":"%s","message":"%s"}`+"\n", session.id, err.Error())):
		default:
		}
		return true
	}
	session.received += int64(len(data))
	if session.hasher != nil {
		_, _ = session.hasher.Write(data)
	}
	return true
}

func sanitizeFileName(name string) string {
	name = strings.ReplaceAll(name, "\\", "/")
	name = filepath.Base(name)
	name = strings.TrimSpace(name)
	if name == "." || name == "/" || name == "" {
		return "file.bin"
	}
	return name
}

func toString(v any) string {
	switch t := v.(type) {
	case string:
		return t
	case fmt.Stringer:
		return t.String()
	default:
		return fmt.Sprintf("%v", v)
	}
}

func toInt64(v any) int64 {
	switch t := v.(type) {
	case int64:
		return t
	case int:
		return int64(t)
	case float64:
		return int64(t)
	case json.Number:
		i, _ := t.Int64()
		return i
	default:
		return 0
	}
}

func firstFloat64(payload map[string]any, keys ...string) float64 {
	for _, key := range keys {
		if v, ok := payload[key]; ok {
			return toFloat64(v)
		}
	}
	return 0
}

func firstInt(payload map[string]any, keys ...string) int {
	for _, key := range keys {
		if v, ok := payload[key]; ok {
			return toInt(v)
		}
	}
	return 0
}

// GetStats returns server statistics.
func (s *Server) GetStats() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()

	var totalSent, totalRecv int64
	for _, c := range s.clients {
		totalSent += c.BytesSent
		totalRecv += c.BytesRecv
	}

	return map[string]interface{}{
		"clients":        len(s.clients),
		"total_clients":  s.totalClients,
		"bytes_sent":     totalSent,
		"bytes_recv":     totalRecv,
		"running":        s.running,
		"port":           s.port,
		"uptime_seconds": time.Since(s.startTime).Seconds(),
	}
}

// GetClientCount returns the number of connected clients.
func (s *Server) GetClientCount() int {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return len(s.clients)
}

// IsRunning returns true if the server is running.
func (s *Server) IsRunning() bool {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.running
}

// handleHTTPUpload handles multipart file uploads.
func (s *Server) handleHTTPUpload(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"error":"Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	err := r.ParseMultipartForm(32 << 20)
	if err != nil {
		http.Error(w, fmt.Sprintf(`{"error":"failed to parse form: %v"}`, err), http.StatusBadRequest)
		return
	}

	file, handler, err := r.FormFile("file")
	if err != nil {
		http.Error(w, `{"error":"missing file field"}`, http.StatusBadRequest)
		return
	}
	defer file.Close()

	dir := s.fileTransferDir()
	_ = os.MkdirAll(dir, 0755)
	filename := sanitizeFileName(handler.Filename)
	destPath := filepath.Join(dir, filename)

	destFile, err := os.OpenFile(destPath, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0644)
	if err != nil {
		http.Error(w, fmt.Sprintf(`{"error":"failed to write file: %v"}`, err), http.StatusInternalServerError)
		return
	}
	defer destFile.Close()

	hasher := md5.New()
	var written int64
	buf := make([]byte, 32*1024)
	for {
		nr, rerr := file.Read(buf)
		if nr > 0 {
			nw, werr := destFile.Write(buf[0:nr])
			if nw > 0 {
				written += int64(nw)
				_, _ = hasher.Write(buf[0:nw])
			}
			if werr != nil {
				http.Error(w, fmt.Sprintf(`{"error":"write error: %v"}`, werr), http.StatusInternalServerError)
				return
			}
		}
		if rerr != nil {
			break
		}
	}

	actualMD5 := hex.EncodeToString(hasher.Sum(nil))

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte(fmt.Sprintf(`{"status":"success","filename":"%s","bytes":%d,"md5":"%s"}`+"\n", filename, written, actualMD5)))
}

// handleHTTPDownload serves files from the transfer directory.
func (s *Server) handleHTTPDownload(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"error":"Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	filename := strings.TrimPrefix(r.URL.Path, "/download/")
	filename = sanitizeFileName(filename)
	if filename == "" || filename == "file.bin" {
		http.Error(w, `{"error":"invalid filename"}`, http.StatusBadRequest)
		return
	}

	dir := s.fileTransferDir()
	filePath := filepath.Join(dir, filename)

	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		http.Error(w, `{"error":"file not found"}`, http.StatusNotFound)
		return
	}

	http.ServeFile(w, r, filePath)
}

// handleScreenMirror handles screen mirroring WebSocket connections.
func (s *Server) handleScreenMirror(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		utils.LogError("Screen mirror WebSocket upgrade failed: %v", err)
		return
	}
	defer conn.Close()

	utils.LogInfo("Screen mirror client connected: %s", r.RemoteAddr)

	// Default parameters
	fps := 20
	scale := 0.5
	quality := 60
	displayIdx := 0

	// Parse initial query params
	if qFPS := r.URL.Query().Get("fps"); qFPS != "" {
		if val, err := strconv.Atoi(qFPS); err == nil && val >= 5 && val <= 60 {
			fps = val
		}
	}
	if qScale := r.URL.Query().Get("scale"); qScale != "" {
		if val, err := strconv.ParseFloat(qScale, 64); err == nil && val >= 0.1 && val <= 1.0 {
			scale = val
		}
	}
	if qQuality := r.URL.Query().Get("quality"); qQuality != "" {
		if qQuality == "low" {
			quality = 30
			scale = 0.3
		} else if qQuality == "medium" {
			quality = 60
			scale = 0.6
		} else if qQuality == "high" {
			quality = 90
			scale = 1.0
		} else if val, err := strconv.Atoi(qQuality); err == nil && val >= 10 && val <= 100 {
			quality = val
		}
	}
	if qDisplay := r.URL.Query().Get("display"); qDisplay != "" {
		if val, err := strconv.Atoi(qDisplay); err == nil && val >= 0 {
			displayIdx = val
		}
	}

	var settings struct {
		sync.Mutex
		fps        int
		scale      float64
		quality    int
		displayIdx int
		updated    chan struct{}
	}
	settings.fps = fps
	settings.scale = scale
	settings.quality = quality
	settings.displayIdx = displayIdx
	settings.updated = make(chan struct{}, 1)

	// Goroutine to read control messages from the client
	go func() {
		for {
			_, msg, err := conn.ReadMessage()
			if err != nil {
				return
			}
			var update struct {
				FPS     *int     `json:"fps"`
				Scale   *float64 `json:"scale"`
				Quality *string  `json:"quality"`
				Display *int     `json:"display"`
			}
			if err := json.Unmarshal(msg, &update); err == nil {
				settings.Lock()
				if update.FPS != nil && *update.FPS >= 5 && *update.FPS <= 60 {
					settings.fps = *update.FPS
				}
				if update.Scale != nil && *update.Scale >= 0.1 && *update.Scale <= 1.0 {
					settings.scale = *update.Scale
				}
				if update.Display != nil && *update.Display >= 0 {
					settings.displayIdx = *update.Display
				}
				if update.Quality != nil {
					q := *update.Quality
					if q == "low" {
						settings.quality = 30
						settings.scale = 0.3
					} else if q == "medium" {
						settings.quality = 60
						settings.scale = 0.6
					} else if q == "high" {
						settings.quality = 90
						settings.scale = 1.0
					} else {
						if val, err := strconv.Atoi(q); err == nil && val >= 10 && val <= 100 {
							settings.quality = val
						}
					}
				}
				settings.Unlock()
				select {
				case settings.updated <- struct{}{}:
				default:
				}
			}
		}
	}()

	ticker := time.NewTicker(time.Duration(1000/fps) * time.Millisecond)
	defer ticker.Stop()

	tmpFile := filepath.Join(os.TempDir(), fmt.Sprintf("airmouse_mirror_%s.jpg", utils.GenerateID()))
	defer os.Remove(tmpFile)

	for {
		select {
		case <-settings.updated:
			settings.Lock()
			currentFPS := settings.fps
			settings.Unlock()
			if currentFPS > 0 {
				ticker.Reset(time.Duration(1000/currentFPS) * time.Millisecond)
			}

		case <-ticker.C:
			settings.Lock()
			currDisplay := settings.displayIdx
			currScale := settings.scale
			currQuality := settings.quality
			settings.Unlock()

			// Capture screenshot natively
			imgData, err := captureDisplayScreenshot(currDisplay, currScale, currQuality)
			if err != nil {
				// Fallback to latestAndroidScreenFrame
				s.mu.RLock()
				imgData = s.latestAndroidScreenFrame
				stale := time.Since(s.latestAndroidScreenFrameTime) > 2*time.Second
				s.mu.RUnlock()

				if stale || len(imgData) == 0 {
					// Fallback to legacy command capture
					if err := captureScreenshot(tmpFile); err == nil {
						if data, readErr := os.ReadFile(tmpFile); readErr == nil {
							imgData = data
						}
					}
				}
			}

			if len(imgData) == 0 {
				continue
			}

			_ = conn.SetWriteDeadline(time.Now().Add(2 * time.Second))
			if err := conn.WriteMessage(gorilla.BinaryMessage, imgData); err != nil {
				utils.LogInfo("Screen mirror client disconnected: %v", err)
				return
			}
		}
	}
}

// captureDisplayScreenshot grabs a screenshot using the kbinani library, resizes it and encodes it.
func captureDisplayScreenshot(displayIdx int, scale float64, jpegQuality int) ([]byte, error) {
	numDisplays := screenshot.NumActiveDisplays()
	if numDisplays == 0 {
		return nil, fmt.Errorf("no active displays")
	}
	if displayIdx < 0 || displayIdx >= numDisplays {
		displayIdx = 0
	}

	img, err := screenshot.CaptureDisplay(displayIdx)
	if err != nil {
		return nil, err
	}

	var finalImg image.Image = img
	if scale > 0.0 && scale < 1.0 {
		w := uint(float64(img.Bounds().Dx()) * scale)
		h := uint(float64(img.Bounds().Dy()) * scale)
		if w > 0 && h > 0 {
			finalImg = resize.Resize(w, h, img, resize.NearestNeighbor)
		}
	}

	var buf bytes.Buffer
	err = jpeg.Encode(&buf, finalImg, &jpeg.Options{Quality: jpegQuality})
	if err != nil {
		return nil, err
	}

	return buf.Bytes(), nil
}

// captureScreenshot grabs a JPEG screenshot on Darwin, Windows, or Linux.
func captureScreenshot(path string) error {
	switch runtime.GOOS {
	case "darwin":
		cmd := exec.Command("screencapture", "-x", "-t", "jpeg", path)
		return cmd.Run()
	case "windows":
		psScript := fmt.Sprintf(`
Add-Type -AssemblyName System.Windows.Forms, System.Drawing
$screen = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
$bitmap = New-Object System.Drawing.Bitmap $screen.Width, $screen.Height
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.CopyFromScreen($screen.X, $screen.Y, 0, 0, $bitmap.Size)
$bitmap.Save('%s', [System.Drawing.Imaging.ImageFormat]::Jpeg)
$graphics.Dispose()
$bitmap.Dispose()
`, path)
		cmd := exec.Command("powershell", "-Command", psScript)
		return cmd.Run()
	case "linux":
		if err := exec.Command("scrot", "-z", path).Run(); err == nil {
			return nil
		}
		if err := exec.Command("gnome-screenshot", "-f", path).Run(); err == nil {
			return nil
		}
		if err := exec.Command("maim", path).Run(); err == nil {
			return nil
		}
		if err := exec.Command("import", "-window", "root", path).Run(); err == nil {
			return nil
		}
		return fmt.Errorf("no screenshot utility found (scrot, gnome-screenshot, maim, import)")
	default:
		return fmt.Errorf("unsupported platform: %s", runtime.GOOS)
	}
}
