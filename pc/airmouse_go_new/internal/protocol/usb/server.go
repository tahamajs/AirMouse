package usb

import (
    "bufio"
    "encoding/json"
    "fmt"
    "os"
    "runtime"
    "strings"
    "sync"
    "time"

    "airmouse-go/internal/control"
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
}

type Server struct {
    devices     map[string]*SerialDevice
    mouse       control.MouseController
    deviceMgr   *device.Manager
    mu          sync.RWMutex
    running     bool
    baudRate    int
    dataBits    int
    stopBits    int
    parity      string
    callbacks   []func(event USBEvent)
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
    Type      string    // "connected", "disconnected", "data"
    DeviceID  string
    Timestamp time.Time
}

func NewServer(mouse control.MouseController, deviceMgr *device.Manager) *Server {
    return &Server{
        devices:   make(map[string]*SerialDevice),
        mouse:     mouse,
        deviceMgr: deviceMgr,
        baudRate:  115200,
        dataBits:  8,
        stopBits:  1,
        parity:    "none",
        callbacks: make([]func(USBEvent), 0),
    }
}

func (s *Server) SetSerialConfig(baudRate, dataBits, stopBits int, parity string) {
    s.baudRate = baudRate
    s.dataBits = dataBits
    s.stopBits = stopBits
    s.parity = parity
    utils.LogInfo("USB serial config updated", "baud", baudRate, "bits", dataBits)
}

func (s *Server) Start() error {
    s.running = true
    go s.scanSerialPorts()
    utils.LogInfo("USB serial server started", "baud_rate", s.baudRate)
    return nil
}

func (s *Server) scanSerialPorts() {
    ports := s.getSerialPorts()
    ticker := time.NewTicker(3 * time.Second)
    defer ticker.Stop()
    
    for s.running {
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

func (s *Server) tryConnectPort(path string) {
    s.mu.RLock()
    _, exists := s.devices[path]
    s.mu.RUnlock()
    if exists {
        return
    }
    
    file, err := os.OpenFile(path, os.O_RDWR, 0666)
    if err != nil {
        return
    }
    
    // Configure serial port (platform-specific)
    if err := s.configurePort(file); err != nil {
        file.Close()
        return
    }
    
    dev := &SerialDevice{
        Path:        path,
        File:        file,
        Reader:      bufio.NewReader(file),
        Connected:   true,
        ConnectedAt: time.Now(),
        LastActive:  time.Now(),
    }
    
    s.mu.Lock()
    s.devices[path] = dev
    s.mu.Unlock()
    
    utils.LogInfo("USB serial device connected", "path", path)
    s.deviceMgr.RegisterDevice(path, device.TypeUSB, "USB Device")
    s.triggerEvent(USBEvent{
        Type:      "connected",
        DeviceID:  path,
        Timestamp: time.Now(),
    })
    
    go s.handleDevice(dev)
}

func (s *Server) configurePort(file *os.File) error {
    // Platform-specific serial configuration would go here
    // For now, assume it's properly configured
    return nil
}

func (s *Server) handleDevice(dev *SerialDevice) {
    defer func() {
        dev.File.Close()
        s.mu.Lock()
        delete(s.devices, dev.Path)
        s.mu.Unlock()
        utils.LogInfo("USB serial device disconnected", "path", dev.Path)
        s.deviceMgr.UnregisterDevice(dev.Path)
        s.triggerEvent(USBEvent{
            Type:      "disconnected",
            DeviceID:  dev.Path,
            Timestamp: time.Now(),
        })
    }()
    
    for s.running && dev.Connected {
        line, err := dev.Reader.ReadString('\n')
        if err != nil {
            break
        }
        
        dev.LastActive = time.Now()
        dev.BytesRecv += int64(len(line))
        
        line = strings.TrimSpace(line)
        if line == "" {
            continue
        }
        
        var msg USBMessage
        if err := json.Unmarshal([]byte(line), &msg); err != nil {
            utils.LogDebug("Invalid USB message", "error", err, "path", dev.Path)
            continue
        }
        
        s.processMessage(dev, &msg)
        s.triggerEvent(USBEvent{
            Type:      "data",
            DeviceID:  dev.Path,
            Timestamp: time.Now(),
        })
    }
}

func (s *Server) processMessage(dev *SerialDevice, msg *USBMessage) {
    switch msg.Type {
    case "move":
        var p MovePayload
        if err := json.Unmarshal(msg.Payload, &p); err == nil {
            s.mouse.Move(p.DX, p.DY)
            utils.LogDebug("USB move", "dx", p.DX, "dy", p.DY)
        }
        
    case "click":
        var p ClickPayload
        button := "left"
        if err := json.Unmarshal(msg.Payload, &p); err == nil && p.Button != "" {
            button = p.Button
        }
        s.mouse.Click(button)
        utils.LogDebug("USB click", "button", button)
        
    case "rightclick":
        s.mouse.Click("right")
        utils.LogDebug("USB right click")
        
    case "doubleclick":
        s.mouse.DoubleClick()
        utils.LogDebug("USB double click")
        
    case "scroll":
        var p ScrollPayload
        if err := json.Unmarshal(msg.Payload, &p); err == nil {
            s.mouse.Scroll(p.Delta)
            utils.LogDebug("USB scroll", "delta", p.Delta)
        }
        
    case "hello":
        utils.LogInfo("USB device identified", "path", dev.Path)
        // Send welcome response
        response := `{"type":"welcome","payload":{"server":"AirMouse","version":"3.0"}}` + "\n"
        dev.File.Write([]byte(response))
        dev.BytesSent += int64(len(response))
        
    case "ping":
        dev.File.Write([]byte(`{"type":"pong"}` + "\n"))
        
    default:
        utils.LogDebug("Unknown USB message type", "type", msg.Type)
    }
    
    // Send ACK if ID present
    if msg.ID != nil && *msg.ID != "" {
        ack := fmt.Sprintf(`{"type":"ack","id":"%s"}`+"\n", *msg.ID)
        dev.File.Write([]byte(ack))
        dev.BytesSent += int64(len(ack))
    }
}

func (s *Server) Stop() {
    s.running = false
    s.mu.Lock()
    defer s.mu.Unlock()
    
    for path, dev := range s.devices {
        dev.File.Close()
        delete(s.devices, path)
        utils.LogInfo("USB device closed", "path", path)
    }
    s.devices = make(map[string]*SerialDevice)
    utils.LogInfo("USB serial server stopped")
}

func (s *Server) GetStats() map[string]interface{} {
    s.mu.RLock()
    defer s.mu.RUnlock()
    
    var totalSent, totalRecv int64
    for _, dev := range s.devices {
        totalSent += dev.BytesSent
        totalRecv += dev.BytesRecv
    }
    
    return map[string]interface{}{
        "devices":      len(s.devices),
        "bytes_sent":   totalSent,
        "bytes_recv":   totalRecv,
        "running":      s.running,
        "baud_rate":    s.baudRate,
        "data_bits":    s.dataBits,
        "stop_bits":    s.stopBits,
        "parity":       s.parity,
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

func (s *Server) SendMessage(devicePath string, msg interface{}) error {
    s.mu.RLock()
    dev, exists := s.devices[devicePath]
    s.mu.RUnlock()
    
    if !exists {
        return fmt.Errorf("device not found: %s", devicePath)
    }
    
    data, err := json.Marshal(msg)
    if err != nil {
        return err
    }
    
    _, err = dev.File.Write(append(data, '\n'))
    if err == nil {
        dev.BytesSent += int64(len(data) + 1)
    }
    return err
}