package server

import (
	"fmt"
	"math"
	"sync"
	"sync/atomic"
	"time"

	"airmouse-go/control/mouse"
	"airmouse-go/control/predict"
	"airmouse-go/internal/auth"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	"airmouse-go/internal/logger"
	"airmouse-go/internal/protocol"
	"airmouse-go/internal/proximity"
)

// Server represents an embeddable AirMouse server instance that can be hosted inside any Go application.
type Server struct {
	cfg         Config
	mouseCtrl   mouse.Controller
	hookMouse   *hookingMouseController
	deviceMgr   *device.Manager
	authMgr     *auth.Manager
	proxMgr     *proximity.Manager
	protoServer *protocol.ProtocolServer

	mu        sync.RWMutex
	running   bool
	startTime time.Time

	// Client & Motion listener hooks
	onConnected    []func(ClientInfo)
	onDisconnected []func(string)
	onMove         []func(dx, dy float64)
	onClick        []func(button string)
	onScroll       []func(delta float64)
	onStart        []func()
	onStop         []func()
}

// New creates and initializes an AirMouse server with functional options.
func New(opts ...Option) (*Server, error) {
	o := &options{
		cfg: DefaultConfig(),
	}
	for _, opt := range opts {
		opt(o)
	}
	return newWithConfigAndCustomMouse(o.cfg, o.customCtrl)
}

// NewWithConfig creates an AirMouse server with explicit Config settings.
func NewWithConfig(cfg Config, opts ...Option) (*Server, error) {
	o := &options{
		cfg: cfg,
	}
	for _, opt := range opts {
		opt(o)
	}
	return newWithConfigAndCustomMouse(o.cfg, o.customCtrl)
}

func newWithConfigAndCustomMouse(cfg Config, customCtrl mouse.Controller) (*Server, error) {
	// Sync global internal config singleton
	intCfg := config.Get()
	intCfg.Host = cfg.Host
	intCfg.Port = cfg.Port
	intCfg.WebSocketPort = cfg.WebSocketPort
	intCfg.UDPPort = cfg.UDPPort
	intCfg.EnableTCP = cfg.EnableTCP
	intCfg.EnableWebSocket = cfg.EnableWebSocket
	intCfg.EnableUDP = cfg.EnableUDP
	intCfg.EnableBluetooth = cfg.EnableBluetooth
	intCfg.EnableSerial = cfg.EnableSerial
	intCfg.Sensitivity = cfg.Sensitivity
	intCfg.AuthEnabled = cfg.AuthEnabled
	intCfg.AuthSecret = cfg.AuthSecret
	intCfg.AuthTokens = cfg.AuthTokens
	intCfg.ProximityEnabled = cfg.ProximityEnabled

	// Initialize logger
	logger.Init(logger.Config{
		Level: cfg.LogLevel,
	})

	s := &Server{
		cfg:            cfg,
		onConnected:    make([]func(ClientInfo), 0),
		onDisconnected: make([]func(string), 0),
		onMove:         make([]func(float64, float64), 0),
		onClick:        make([]func(string), 0),
		onScroll:       make([]func(float64), 0),
		onStart:        make([]func(), 0),
		onStop:         make([]func(), 0),
	}

	// Setup mouse controller
	var underlyingCtrl mouse.Controller
	if customCtrl != nil {
		underlyingCtrl = customCtrl
	} else if !cfg.VirtualMouse {
		underlyingCtrl = mouse.NewController(cfg.Sensitivity)
	}

	s.hookMouse = newHookingMouseController(underlyingCtrl, s)
	s.mouseCtrl = s.hookMouse

	// Setup internal managers
	s.deviceMgr = device.NewManager()
	s.authMgr = auth.NewManager(cfg.AuthSecret)
	if cfg.AuthEnabled {
		for _, token := range cfg.AuthTokens {
			if token != "" {
				_, _ = s.authMgr.GenerateAuthToken(token, "")
			}
		}
	}

	if cfg.ProximityEnabled {
		s.proxMgr = proximity.NewManager()
	}

	// Setup Protocol Server
	s.protoServer = protocol.NewProtocolServerWithProximity(s.mouseCtrl, s.deviceMgr, s.authMgr, s.proxMgr)

	// Wire lifecycle listeners
	s.wireEvents()

	return s, nil
}

func (s *Server) wireEvents() {
	if s.protoServer != nil {
		s.protoServer.AddEventListener(func(event protocol.ServerEvent) {
			s.mu.RLock()
			defer s.mu.RUnlock()

			switch event.Type {
			case "start":
				for _, fn := range s.onStart {
					go fn()
				}
			case "stop":
				for _, fn := range s.onStop {
					go fn()
				}
			case "client_disconnected":
				for _, fn := range s.onDisconnected {
					go fn(event.ClientID)
				}
			}
		})
	}

	if s.deviceMgr != nil {
		s.deviceMgr.AddEventListener(func(event device.DeviceEvent) {
			s.mu.RLock()
			defer s.mu.RUnlock()

			switch event.Type {
			case "connected", "device_connected":
				dev := s.deviceMgr.GetDevice(event.DeviceID)
				info := fromDeviceInfo(dev)
				if info.ID == "" {
					info.ID = event.DeviceID
					info.Name = event.DeviceName
				}
				for _, fn := range s.onConnected {
					go fn(info)
				}
			case "disconnected", "device_disconnected":
				for _, fn := range s.onDisconnected {
					go fn(event.DeviceID)
				}
			}
		})
	}
}

// OnClientConnected registers a listener invoked when a client successfully connects and authenticates.
func (s *Server) OnClientConnected(fn func(client ClientInfo)) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if fn != nil {
		s.onConnected = append(s.onConnected, fn)
	}
}

// OnClientDisconnected registers a listener invoked when a client disconnects.
func (s *Server) OnClientDisconnected(fn func(clientID string)) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if fn != nil {
		s.onDisconnected = append(s.onDisconnected, fn)
	}
}

// OnMove registers a listener invoked whenever a movement event is processed.
func (s *Server) OnMove(fn func(dx, dy float64)) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if fn != nil {
		s.onMove = append(s.onMove, fn)
	}
}

// OnClick registers a listener invoked whenever a mouse click event is processed.
func (s *Server) OnClick(fn func(button string)) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if fn != nil {
		s.onClick = append(s.onClick, fn)
	}
}

// OnScroll registers a listener invoked whenever a mouse scroll event is processed.
func (s *Server) OnScroll(fn func(delta float64)) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if fn != nil {
		s.onScroll = append(s.onScroll, fn)
	}
}

// OnServerStarted registers a listener invoked when the server successfully starts.
func (s *Server) OnServerStarted(fn func()) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if fn != nil {
		s.onStart = append(s.onStart, fn)
	}
}

// OnServerStopped registers a listener invoked when the server stops.
func (s *Server) OnServerStopped(fn func()) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if fn != nil {
		s.onStop = append(s.onStop, fn)
	}
}

// Start boots all enabled protocol servers (TCP, WebSocket, UDP).
func (s *Server) Start() error {
	s.mu.Lock()
	if s.running {
		s.mu.Unlock()
		return fmt.Errorf("server is already running")
	}
	s.startTime = time.Now()
	s.running = true
	s.mu.Unlock()

	return s.protoServer.Start()
}

// Stop gracefully terminates all active connections and server listeners.
func (s *Server) Stop() error {
	s.mu.Lock()
	if !s.running {
		s.mu.Unlock()
		return nil
	}
	s.running = false
	s.mu.Unlock()

	if s.protoServer != nil {
		s.protoServer.Stop()
	}
	if s.deviceMgr != nil {
		s.deviceMgr.Stop()
	}
	return nil
}

// IsRunning returns whether the server is currently actively listening.
func (s *Server) IsRunning() bool {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.running
}

// GetUptime returns how long the server has been running.
func (s *Server) GetUptime() time.Duration {
	s.mu.RLock()
	defer s.mu.RUnlock()
	if !s.running {
		return 0
	}
	return time.Since(s.startTime)
}

// GetStatistics aggregates traffic, device count, and protocol uptime.
func (s *Server) GetStatistics() map[string]interface{} {
	if s.protoServer != nil {
		return s.protoServer.GetStatistics()
	}
	return map[string]interface{}{}
}

// GetConnectedDevices returns the list of all currently active clients.
func (s *Server) GetConnectedDevices() []ClientInfo {
	if s.deviceMgr == nil {
		return []ClientInfo{}
	}
	active := s.deviceMgr.GetActiveDevices()
	res := make([]ClientInfo, len(active))
	for i, d := range active {
		res[i] = fromDeviceInfo(d)
	}
	return res
}

// ApproveDevice approves a pending device by ID.
func (s *Server) ApproveDevice(deviceID string) error {
	if s.protoServer == nil {
		return fmt.Errorf("server not initialized")
	}
	return s.protoServer.ApproveDevice(deviceID)
}

// BlockDevice blocks a device by ID.
func (s *Server) BlockDevice(deviceID string) error {
	if s.protoServer == nil {
		return fmt.Errorf("server not initialized")
	}
	return s.protoServer.BlockDevice(deviceID)
}

// DisconnectDevice terminates connection with a device by ID.
func (s *Server) DisconnectDevice(deviceID string) error {
	if s.protoServer == nil {
		return fmt.Errorf("server not initialized")
	}
	return s.protoServer.DisconnectDevice(deviceID)
}

// Config returns a copy of current configuration settings.
func (s *Server) Config() Config {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.cfg
}

// ------------------------------------------------------------
// Hooking Mouse Controller
// ------------------------------------------------------------

type hookingMouseController struct {
	inner  mouse.Controller
	server *Server

	mu          sync.RWMutex
	posX, posY  float64
	sensitivity float64
	clickCount  int64
	doubleCount int64
	rightCount  int64
	scrollCount int64
}

func newHookingMouseController(inner mouse.Controller, s *Server) *hookingMouseController {
	return &hookingMouseController{
		inner:       inner,
		server:      s,
		sensitivity: s.cfg.Sensitivity,
	}
}

func (h *hookingMouseController) Move(dx, dy float64) {
	// Dispatch registered onMove listeners
	h.server.mu.RLock()
	listeners := make([]func(float64, float64), len(h.server.onMove))
	copy(listeners, h.server.onMove)
	h.server.mu.RUnlock()

	for _, fn := range listeners {
		fn(dx, dy)
	}

	if h.inner != nil {
		h.inner.Move(dx, dy)
	} else {
		h.mu.Lock()
		h.posX += dx
		h.posY += dy
		h.mu.Unlock()
	}
}

func (h *hookingMouseController) Click(button string) {
	h.server.mu.RLock()
	listeners := make([]func(string), len(h.server.onClick))
	copy(listeners, h.server.onClick)
	h.server.mu.RUnlock()

	for _, fn := range listeners {
		fn(button)
	}

	if h.inner != nil {
		h.inner.Click(button)
	} else {
		switch button {
		case "left":
			atomic.AddInt64(&h.clickCount, 1)
		case "right":
			atomic.AddInt64(&h.rightCount, 1)
		case "double":
			atomic.AddInt64(&h.doubleCount, 1)
		}
	}
}

func (h *hookingMouseController) Scroll(delta int) {
	h.server.mu.RLock()
	listeners := make([]func(float64), len(h.server.onScroll))
	copy(listeners, h.server.onScroll)
	h.server.mu.RUnlock()

	for _, fn := range listeners {
		fn(float64(delta))
	}

	if h.inner != nil {
		h.inner.Scroll(delta)
	} else {
		atomic.AddInt64(&h.scrollCount, int64(math.Abs(float64(delta))))
	}
}

func (h *hookingMouseController) MouseDown(button string) {
	if h.inner != nil {
		h.inner.MouseDown(button)
	}
}

func (h *hookingMouseController) MouseUp(button string) {
	if h.inner != nil {
		h.inner.MouseUp(button)
	}
}

func (h *hookingMouseController) DoubleClick() {
	h.Click("double")
}

func (h *hookingMouseController) Stats() (clicks, dbl, right, scroll int64) {
	if h.inner != nil {
		return h.inner.Stats()
	}
	return atomic.LoadInt64(&h.clickCount),
		atomic.LoadInt64(&h.doubleCount),
		atomic.LoadInt64(&h.rightCount),
		atomic.LoadInt64(&h.scrollCount)
}

func (h *hookingMouseController) SetSensitivity(s float64) {
	h.mu.Lock()
	h.sensitivity = s
	h.mu.Unlock()
	if h.inner != nil {
		h.inner.SetSensitivity(s)
	}
}

func (h *hookingMouseController) GetSensitivity() float64 {
	if h.inner != nil {
		return h.inner.GetSensitivity()
	}
	h.mu.RLock()
	defer h.mu.RUnlock()
	return h.sensitivity
}

func (h *hookingMouseController) SetSmoothing(enabled bool) {
	if h.inner != nil {
		h.inner.SetSmoothing(enabled)
	}
}

func (h *hookingMouseController) SetAcceleration(enabled bool, factor float64) {
	if h.inner != nil {
		h.inner.SetAcceleration(enabled, factor)
	}
}

func (h *hookingMouseController) EnablePredictive(enabled bool) {
	if h.inner != nil {
		h.inner.EnablePredictive(enabled)
	}
}

func (h *hookingMouseController) SetPredictiveBlendFactor(factor float64) {
	if h.inner != nil {
		h.inner.SetPredictiveBlendFactor(factor)
	}
}

func (h *hookingMouseController) EnableAISmoothing(enabled bool) {
	if h.inner != nil {
		h.inner.EnableAISmoothing(enabled)
	}
}

func (h *hookingMouseController) SetAISmoother(s *predict.AISmoother) {
	if h.inner != nil {
		h.inner.SetAISmoother(s)
	}
}

func (h *hookingMouseController) EnableMLPrediction(enabled bool) {
	if h.inner != nil {
		h.inner.EnableMLPrediction(enabled)
	}
}

func (h *hookingMouseController) SetMLBlendFactor(factor float64) {
	if h.inner != nil {
		h.inner.SetMLBlendFactor(factor)
	}
}

func (h *hookingMouseController) ResetStats() {
	if h.inner != nil {
		h.inner.ResetStats()
	} else {
		atomic.StoreInt64(&h.clickCount, 0)
		atomic.StoreInt64(&h.doubleCount, 0)
		atomic.StoreInt64(&h.rightCount, 0)
		atomic.StoreInt64(&h.scrollCount, 0)
	}
}

func (h *hookingMouseController) GetPosition() (x, y float64) {
	if h.inner != nil {
		return h.inner.GetPosition()
	}
	h.mu.RLock()
	defer h.mu.RUnlock()
	return h.posX, h.posY
}
