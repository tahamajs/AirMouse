package usb

import (
	"bufio"
	"encoding/json"
	"fmt"
	"os"
	"runtime"
	"sync"
	"time"

	"airmouse-go/control/common"
	"airmouse-go/control/mouse"
	"airmouse-go/control/syscmd"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	"airmouse-go/internal/utils"
)

type SerialDevice struct {
	Path        string
	File        *os.File
	Reader      *bufio.Reader
	Connected   bool
	ConnectedAt time.Time
	LastActive  time.Time
	BytesSent   int64
	BytesRecv   int64
	DeviceInfo  string
	VendorID    uint16
	ProductID   uint16
	BaudRate    int
	Approved    bool
	Fingerprint string
}

type Server struct {
	devices    map[string]*SerialDevice
	mouse      mouse.Controller
	deviceMgr  *device.Manager
	mu         sync.RWMutex
	running    bool
	baudRate   int
	dataBits   int
	stopBits   int
	parity     string
	callbacks  []func(event USBEvent)
	gadget     *USBGadget
	autoDetect bool
}

type USBMessage struct {
	Type    string          `json:"type"`
	Payload json.RawMessage `json:"payload,omitempty"`
	ID      *string         `json:"id,omitempty"`
}

type MovePayload struct {
	DX float64 `json:"dx"`
	DY float64 `json:"dy"`
}

type ScrollPayload struct {
	Delta int `json:"delta"`
}

type ClickPayload struct {
	Button string `json:"button"`
}

type USBEvent struct {
	Type      string // "connected", "disconnected", "data", "error"
	DeviceID  string
	Data      []byte
	Error     string
	Timestamp time.Time
}

func NewServer(mouseCtrl mouse.Controller, deviceMgr *device.Manager) *Server {
	cfg := DefaultGadgetConfig()
	return &Server{
		devices:    make(map[string]*SerialDevice),
		mouse:      mouseCtrl,
		deviceMgr:  deviceMgr,
		baudRate:   115200,
		dataBits:   8,
		stopBits:   1,
		parity:     "none",
		callbacks:  make([]func(USBEvent), 0),
		gadget:     NewUSBGadget(cfg),
		autoDetect: true,
	}
}

func (s *Server) SetSerialConfig(baudRate, dataBits, stopBits int, parity string) {
	s.baudRate = baudRate
	s.dataBits = dataBits
	s.stopBits = stopBits
	s.parity = parity
	utils.LogInfo("USB serial config updated: baud=%d bits=%d", baudRate, dataBits)
}

func (s *Server) EnableAutoDetect(enabled bool) {
	s.autoDetect = enabled
}

func (s *Server) Start() error {
	s.running = true

	if runtime.GOOS == "linux" {
		if err := s.gadget.Setup(); err != nil {
			utils.LogWarn("USB gadget setup failed: %v", err)
		}
	}

	if s.autoDetect {
		go s.scanSerialPorts()
	}

	utils.LogInfo("USB serial server started: baud_rate=%d auto_detect=%v", s.baudRate, s.autoDetect)
	return nil
}

func (s *Server) scanSerialPorts() {
	ticker := time.NewTicker(3 * time.Second)
	defer ticker.Stop()

	for s.running {
		ports := s.getSerialPorts()
		for _, port := range ports {
			s.tryConnectPort(port)
		}
		<-ticker.C
	}
}

func (s *Server) getSerialPorts() []string {
	var ports []string
	switch runtime.GOOS {
	case "linux":
		patterns := []string{"/dev/ttyACM", "/dev/ttyUSB", "/dev/ttyS"}
		for _, pattern := range patterns {
			for i := 0; i < 10; i++ {
				ports = append(ports, fmt.Sprintf("%s%d", pattern, i))
			}
		}
	case "darwin":
		for i := 0; i < 10; i++ {
			ports = append(ports, fmt.Sprintf("/dev/cu.usbmodem%d", i))
			ports = append(ports, fmt.Sprintf("/dev/cu.usbserial-%d", i))
		}
	case "windows":
		for i := 1; i <= 20; i++ {
			ports = append(ports, fmt.Sprintf("COM%d", i))
		}
	}
	return ports
}

func (s *Server) AddSerialPort(port string, baudRate int) error {
	if baudRate == 0 {
		baudRate = s.baudRate
	}
	dev := &SerialDevice{
		Path:      port,
		BaudRate:  baudRate,
		Connected: false,
	}
	s.mu.Lock()
	s.devices[port] = dev
	s.mu.Unlock()
	return s.tryConnectPort(port)
}

func (s *Server) tryConnectPort(path string) error {
	s.mu.RLock()
	dev, exists := s.devices[path]
	s.mu.RUnlock()
	if !exists {
		return fmt.Errorf("device not registered: %s", path)
	}
	if dev.Connected {
		return nil
	}
	file, err := os.OpenFile(path, os.O_RDWR, 0666)
	if err != nil {
		return err
	}
	if err := s.configurePort(file, dev.BaudRate); err != nil {
		file.Close()
		return err
	}
	dev.File = file
	dev.Reader = bufio.NewReader(file)
	dev.Connected = true
	dev.ConnectedAt = time.Now()
	dev.LastActive = time.Now()

	utils.LogInfo("USB serial device connected: path=%s baud=%d", path, dev.BaudRate)
	if s.deviceMgr != nil {
		_ = s.deviceMgr.RegisterDevice(path, device.TypeUSB, "USB Device")
	}
	s.triggerEvent(USBEvent{
		Type:      "connected",
		DeviceID:  path,
		Timestamp: time.Now(),
	})
	go s.handleDevice(dev)
	return nil
}

func (s *Server) configurePort(file *os.File, baudRate int) error {
	// Platform‑specific serial configuration – stub for now
	utils.LogDebug("Serial port configured: baud=%d", baudRate)
	return nil
}

func (s *Server) handleDevice(dev *SerialDevice) {
	defer func() {
		if dev.File != nil {
			dev.File.Close()
		}
		s.mu.Lock()
		dev.Connected = false
		s.mu.Unlock()
		utils.LogInfo("USB serial device disconnected: path=%s", dev.Path)
		if s.deviceMgr != nil {
			_ = s.deviceMgr.UnregisterDevice(dev.Path)
		}
		s.triggerEvent(USBEvent{
			Type:      "disconnected",
			DeviceID:  dev.Path,
			Timestamp: time.Now(),
		})
	}()

	for s.running && dev.Connected {
		line, err := dev.Reader.ReadString('\n')
		if err != nil {
			utils.LogDebug("USB read error: path=%s error=%v", dev.Path, err)
			break
		}
		dev.LastActive = time.Now()
		dev.BytesRecv += int64(len(line))
		if s.deviceMgr != nil {
			_ = s.deviceMgr.UpdateDeviceActivity(dev.Path, 0, int64(len(line)))
		}
		line = trimSpace(line)
		if line == "" {
			continue
		}
		var msg USBMessage
		if err := json.Unmarshal([]byte(line), &msg); err != nil {
			utils.LogDebug("Invalid USB message: %v", err)
			s.triggerEvent(USBEvent{
				Type:      "error",
				DeviceID:  dev.Path,
				Error:     err.Error(),
				Timestamp: time.Now(),
			})
			continue
		}
		s.mu.RLock()
		approved := dev.Approved
		s.mu.RUnlock()

		if !approved && msg.Type != "hello" && msg.Type != "ping" {
			utils.LogDebug("Ignoring USB %s while waiting for approval: path=%s", msg.Type, dev.Path)
			continue
		}

		s.processMessage(dev, &msg)
		s.triggerEvent(USBEvent{
			Type:      "data",
			DeviceID:  dev.Path,
			Data:      []byte(line),
			Timestamp: time.Now(),
		})
	}
}

func (s *Server) processMessage(dev *SerialDevice, msg *USBMessage) {
	switch msg.Type {
	case "move":
		var p MovePayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			if s.mouse != nil {
				s.mouse.Move(p.DX, p.DY)
			}
		}
	case "click":
		var p ClickPayload
		button := "left"
		if err := json.Unmarshal(msg.Payload, &p); err == nil && p.Button != "" {
			button = p.Button
		}
		if s.mouse != nil {
			s.mouse.Click(button)
		}
	case "mouse_down":
		var p ClickPayload
		button := "left"
		if err := json.Unmarshal(msg.Payload, &p); err == nil && p.Button != "" {
			button = p.Button
		}
		if s.mouse != nil {
			s.mouse.MouseDown(button)
		}
	case "mouse_up":
		var p ClickPayload
		button := "left"
		if err := json.Unmarshal(msg.Payload, &p); err == nil && p.Button != "" {
			button = p.Button
		}
		if s.mouse != nil {
			s.mouse.MouseUp(button)
		}
	case "rightclick":
		if s.mouse != nil {
			s.mouse.Click("right")
		}
	case "doubleclick":
		if s.mouse != nil {
			s.mouse.DoubleClick()
		}
	case "scroll":
		var p ScrollPayload
		if err := json.Unmarshal(msg.Payload, &p); err == nil {
			if s.mouse != nil {
				s.mouse.Scroll(p.Delta)
			}
		}
	case "control":
		var payload map[string]interface{}
		if err := json.Unmarshal(msg.Payload, &payload); err == nil {
			command, _ := payload["command"].(string)
			switch command {
			case "mode_change":
				mode, _ := payload["mode"].(string)
				common.SetActiveMode(mode)
				utils.LogInfo("USB mode changed: path=%s mode=%s", dev.Path, mode)
			case "theme_change":
				theme, _ := payload["theme"].(string)
				common.SetActiveTheme(theme)
				utils.LogInfo("USB theme changed: path=%s theme=%s", dev.Path, theme)
			case "start", "touchpad_start", "resume", "resume_movement":
				common.SetMovementPaused(false)
				utils.LogInfo("Movement resumed by USB device: %s command=%s", dev.Path, command)
			case "touchpad_stop", "pause", "pause_movement":
				common.SetMovementPaused(true)
				utils.LogInfo("Movement paused by USB device: %s command=%s", dev.Path, command)
			case "show_desktop", "task_view", "switch_window", "lock_screen", "window_close", "zoom_in", "zoom_out", "zoom_reset",
				"volume_up", "volume_down", "mute", "play_pause", "next_track", "prev_track", "stop",
				"window_maximize", "window_minimize", "window_fullscreen",
				"browser_back", "browser_forward", "browser_refresh", "browser_home":
				go func(cmd string) {
					if err := syscmd.ExecuteSystemCommand(cmd); err != nil {
						utils.LogError("USB system command failed: path=%s command=%s err=%v", dev.Path, cmd, err)
					} else {
						utils.LogInfo("USB system command executed: path=%s command=%s", dev.Path, cmd)
					}
				}(command)
			}
		}
	case "presentation":
		var payload map[string]interface{}
		if err := json.Unmarshal(msg.Payload, &payload); err == nil {
			action, _ := payload["action"].(string)
			utils.LogInfo("USB presentation action received: path=%s action=%s", dev.Path, action)
			switch action {
			case "prev":
				_ = syscmd.ExecuteSystemCommand("prev_track")
			case "next":
				_ = syscmd.ExecuteSystemCommand("next_track")
			case "fullscreen":
				_ = syscmd.ExecuteSystemCommand("window_fullscreen")
			}
		}
	case "hello":
		utils.LogInfo("USB device identified: path=%s", dev.Path)
		// Extract device info for fingerprint
		var hello map[string]string
		var fingerprint string
		if len(msg.Payload) > 0 {
			if err := json.Unmarshal(msg.Payload, &hello); err == nil {
				name := hello["name"]
				version := hello["version"]
				deviceName := hello["device_name"]
				model := hello["model"]
				manufacturer := hello["manufacturer"]
				brand := hello["brand"]
				androidVersion := hello["android_version"]
				sdkInt := hello["sdk_int"]
				deviceIDValue := hello["device_id"]
				protocolName := hello["protocol"]
				transport := hello["transport"]
				fingerprint = device.StableDeviceID(deviceIDValue, name, version, deviceName, model, manufacturer, brand, androidVersion, sdkInt, protocolName, transport)
				if name != "" && s.deviceMgr != nil {
					_ = s.deviceMgr.UpdateDeviceName(dev.Path, name)
				}
			}
		}
		if fingerprint == "" {
			// Fallback: use device path as fingerprint
			fingerprint = dev.Path
		}

		// Upsert the device first so it is registered in DeviceManager
		if s.deviceMgr != nil {
			name := "USB Device"
			if len(msg.Payload) > 0 {
				if err := json.Unmarshal(msg.Payload, &hello); err == nil && hello["name"] != "" {
					name = hello["name"]
				}
			}
			_ = s.deviceMgr.UpsertDevice(fingerprint, device.TypeUSB, name, map[string]string{
				"fingerprint": fingerprint,
				"path":        dev.Path,
			})
		}

		s.mu.Lock()
		dev.Fingerprint = fingerprint
		s.mu.Unlock()

		// Check for auto‑approval
		if s.deviceMgr != nil && config.Get().IsTrustedDevice(fingerprint) {
			s.mu.Lock()
			dev.Approved = true
			s.mu.Unlock()

			_ = s.deviceMgr.UpdateDeviceStatus(fingerprint, device.StatusConnected)
			cfg := config.Get()
			response := fmt.Sprintf(`{"type":"welcome","payload":{"server":"%s","version":"%s"}}`+"\n",
				cfg.ServerName, cfg.Version)
			if _, err := dev.File.Write([]byte(response)); err == nil {
				dev.BytesSent += int64(len(response))
				if s.deviceMgr != nil {
					_ = s.deviceMgr.UpdateDeviceActivity(dev.Path, int64(len(response)), 0)
				}
				utils.LogInfo("USB device auto‑approved: %s (fingerprint: %s)", dev.Path, fingerprint)
			}
			// Clear any pending state
			common.SetMovementPaused(false)
			common.ClearPause()
			return
		}

		// If not auto‑approved, send welcome as pending
		s.mu.Lock()
		dev.Approved = false
		s.mu.Unlock()

		cfg := config.Get()
		response := fmt.Sprintf(`{"type":"welcome","payload":{"server":"%s","version":"%s"}}`+"\n",
			cfg.ServerName, cfg.Version)
		if _, err := dev.File.Write([]byte(response)); err == nil {
			dev.BytesSent += int64(len(response))
			if s.deviceMgr != nil {
				_ = s.deviceMgr.UpdateDeviceActivity(dev.Path, int64(len(response)), 0)
			}
		}
		if s.deviceMgr != nil {
			_ = s.deviceMgr.UpdateDeviceStatus(fingerprint, device.StatusPendingApproval)
		}
	case "ping":
		pong := []byte(`{"type":"pong"}` + "\n")
		if _, err := dev.File.Write(pong); err == nil {
			dev.BytesSent += int64(len(pong))
			if s.deviceMgr != nil {
				_ = s.deviceMgr.UpdateDeviceActivity(dev.Path, int64(len(pong)), 0)
			}
		}
	default:
		utils.LogDebug("Unknown USB message type: %s", msg.Type)
	}
	if msg.ID != nil && *msg.ID != "" {
		ack := fmt.Sprintf(`{"type":"ack","id":"%s"}`+"\n", *msg.ID)
		if _, err := dev.File.Write([]byte(ack)); err == nil {
			dev.BytesSent += int64(len(ack))
			if s.deviceMgr != nil {
				_ = s.deviceMgr.UpdateDeviceActivity(dev.Path, int64(len(ack)), 0)
			}
		}
	}
}

func (s *Server) SendMessage(devicePath string, msg interface{}) error {
	s.mu.RLock()
	dev, exists := s.devices[devicePath]
	s.mu.RUnlock()
	if !exists || !dev.Connected {
		return fmt.Errorf("device not connected: %s", devicePath)
	}
	data, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	_, err = dev.File.Write(append(data, '\n'))
	if err == nil {
		dev.BytesSent += int64(len(data) + 1)
		if s.deviceMgr != nil {
			_ = s.deviceMgr.UpdateDeviceActivity(devicePath, int64(len(data)+1), 0)
		}
	}
	return err
}

func (s *Server) ApproveDevice(deviceID string) error {
	s.mu.Lock()
	var dev *SerialDevice
	for _, d := range s.devices {
		if d.Path == deviceID || d.Fingerprint == deviceID {
			dev = d
			break
		}
	}
	s.mu.Unlock()

	if dev == nil {
		return fmt.Errorf("usb device not found: %s", deviceID)
	}

	s.mu.Lock()
	if dev.Approved {
		s.mu.Unlock()
		return nil
	}
	dev.Approved = true
	s.mu.Unlock()

	cfg := config.Get()
	response := fmt.Sprintf(`{"type":"welcome","payload":{"server":"%s","version":"%s"}}`+"\n",
		cfg.ServerName, cfg.Version)
	
	s.mu.Lock()
	var err error
	if dev.File != nil {
		_, err = dev.File.Write([]byte(response))
		if err == nil {
			dev.BytesSent += int64(len(response))
			if s.deviceMgr != nil {
				_ = s.deviceMgr.UpdateDeviceActivity(dev.Path, int64(len(response)), 0)
			}
		}
	}
	s.mu.Unlock()

	if s.deviceMgr != nil {
		_ = s.deviceMgr.UpdateDeviceStatus(dev.Fingerprint, device.StatusConnected)
	}
	common.SetMovementPaused(false)
	common.ClearPause()
	
	utils.LogInfo("USB device approved: %s (fingerprint: %s)", dev.Path, dev.Fingerprint)
	return err
}

func (s *Server) SendHIDReport(dx, dy int16, buttons byte, wheel int8) error {
	if runtime.GOOS != "linux" {
		return fmt.Errorf("HID reports only supported on Linux")
	}
	return s.gadget.SendMouseReport(dx, dy, buttons, wheel)
}

func (s *Server) Stop() {
	s.running = false
	if runtime.GOOS == "linux" {
		s.gadget.Teardown()
	}
	s.mu.Lock()
	for path, dev := range s.devices {
		if dev.Connected {
			dev.File.Close()
		}
		delete(s.devices, path)
	}
	s.mu.Unlock()
	utils.LogInfo("USB serial server stopped")
}

func (s *Server) GetStats() map[string]interface{} {
	s.mu.RLock()
	defer s.mu.RUnlock()
	var totalSent, totalRecv int64
	var connectedCount int
	for _, dev := range s.devices {
		if dev.Connected {
			connectedCount++
			totalSent += dev.BytesSent
			totalRecv += dev.BytesRecv
		}
	}
	return map[string]interface{}{
		"devices":     len(s.devices),
		"connected":   connectedCount,
		"bytes_sent":  totalSent,
		"bytes_recv":  totalRecv,
		"running":     s.running,
		"baud_rate":   s.baudRate,
		"data_bits":   s.dataBits,
		"stop_bits":   s.stopBits,
		"parity":      s.parity,
		"auto_detect": s.autoDetect,
		"gadget":      s.gadget.GetStatus(),
	}
}

func (s *Server) AddEventListener(callback func(event USBEvent)) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.callbacks = append(s.callbacks, callback)
}

func (s *Server) triggerEvent(event USBEvent) {
	s.mu.RLock()
	callbacks := make([]func(USBEvent), len(s.callbacks))
	copy(callbacks, s.callbacks)
	s.mu.RUnlock()
	for _, cb := range callbacks {
		go cb(event)
	}
}

func trimSpace(s string) string {
	for len(s) > 0 && (s[0] == ' ' || s[0] == '\t' || s[0] == '\n' || s[0] == '\r') {
		s = s[1:]
	}
	for len(s) > 0 && (s[len(s)-1] == ' ' || s[len(s)-1] == '\t' || s[len(s)-1] == '\n' || s[len(s)-1] == '\r') {
		s = s[:len(s)-1]
	}
	return s
}
