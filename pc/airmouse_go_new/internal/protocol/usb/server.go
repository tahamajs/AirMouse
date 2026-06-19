package usb

import (
    "bufio"
    "encoding/json"
    "fmt"
    "os"
    "runtime"
    "sync"
    "time"

    "airmouse-go/internal/control"
    "airmouse-go/internal/device"
    "airmouse-go/internal/infra/logger"
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
    gadget      *USBGadget
    autoDetect  bool
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

func NewServer(mouse control.MouseController, deviceMgr *device.Manager) *Server {
    cfg := DefaultGadgetConfig()
    return &Server{
        devices:    make(map[string]*SerialDevice),
        mouse:      mouse,
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
    logger.Info("USB serial config updated: baud=%d bits=%d", baudRate, dataBits)
}

func (s *Server) EnableAutoDetect(enabled bool) {
    s.autoDetect = enabled
}

func (s *Server) Start() error {
    s.running = true
    
    // Start USB gadget if on Linux
    if runtime.GOOS == "linux" {
        if err := s.gadget.Setup(); err != nil {
            logger.Warn("USB gadget setup failed: %v", err)
        }
    }
    
    // Start serial port scanning if auto-detect enabled
    if s.autoDetect {
        go s.scanSerialPorts()
    }
    
    logger.Info("USB serial server started: baud_rate=%d auto_detect=%v", s.baudRate, s.autoDetect)
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
    
    // Configure serial port
    if err := s.configurePort(file, dev.BaudRate); err != nil {
        file.Close()
        return err
    }
    
    dev.File = file
    dev.Reader = bufio.NewReader(file)
    dev.Connected = true
    dev.ConnectedAt = time.Now()
    dev.LastActive = time.Now()
    
    logger.Info("USB serial device connected: path=%s baud=%d", path, dev.BaudRate)
    s.deviceMgr.RegisterDevice(path, device.TypeUSB, "USB Device")
    s.triggerEvent(USBEvent{
        Type:      "connected",
        DeviceID:  path,
        Timestamp: time.Now(),
    })
    
    go s.handleDevice(dev)
    return nil
}

func (s *Server) configurePort(file *os.File, baudRate int) error {
    // Platform-specific serial configuration
    // In production, use appropriate syscalls or libraries
    logger.Debug("Serial port configured: baud=%d", baudRate)
    return nil
}

func (s *Server) handleDevice(dev *SerialDevice) {
    defer func() {
        dev.File.Close()
        s.mu.Lock()
        dev.Connected = false
        s.mu.Unlock()
        
        logger.Info("USB serial device disconnected: path=%s", dev.Path)
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
            logger.Debug("USB read error: path=%s error=%v", dev.Path, err)
            break
        }
        
        dev.LastActive = time.Now()
        dev.BytesRecv += int64(len(line))
        
        line = trimSpace(line)
        if line == "" {
            continue
        }
        
        var msg USBMessage
        if err := json.Unmarshal([]byte(line), &msg); err != nil {
            logger.Debug("Invalid USB message: %v", err)
            s.triggerEvent(USBEvent{
                Type:      "error",
                DeviceID:  dev.Path,
                Error:     err.Error(),
                Timestamp: time.Now(),
            })
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
            s.mouse.Move(p.DX, p.DY)
        }
        
    case "click":
        var p ClickPayload
        button := "left"
        if err := json.Unmarshal(msg.Payload, &p); err == nil && p.Button != "" {
            button = p.Button
        }
        s.mouse.Click(button)
        
    case "rightclick":
        s.mouse.Click("right")
        
    case "doubleclick":
        s.mouse.DoubleClick()
        
    case "scroll":
        var p ScrollPayload
        if err := json.Unmarshal(msg.Payload, &p); err == nil {
            s.mouse.Scroll(p.Delta)
        }
        
    case "hello":
        logger.Info("USB device identified: path=%s", dev.Path)
        if len(msg.Payload) > 0 {
            var hello map[string]string
            if err := json.Unmarshal(msg.Payload, &hello); err == nil {
                if name, ok := hello["name"]; ok {
                    s.deviceMgr.UpdateDeviceName(dev.Path, name)
                }
            }
        }
        response := `{"type":"welcome","payload":{"server":"AirMouse","version":"3.0"}}` + "\n"
        dev.File.Write([]byte(response))
        dev.BytesSent += int64(len(response))
        
    case "ping":
        dev.File.Write([]byte(`{"type":"pong"}` + "\n"))
        
    default:
        logger.Debug("Unknown USB message type: %s", msg.Type)
    }
    
    // Send ACK if ID present
    if msg.ID != nil && *msg.ID != "" {
        ack := fmt.Sprintf(`{"type":"ack","id":"%s"}`+"\n", *msg.ID)
        dev.File.Write([]byte(ack))
        dev.BytesSent += int64(len(ack))
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
    }
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
    
    logger.Info("USB serial server stopped")
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
    
    stats := map[string]interface{}{
        "devices":      len(s.devices),
        "connected":    connectedCount,
        "bytes_sent":   totalSent,
        "bytes_recv":   totalRecv,
        "running":      s.running,
        "baud_rate":    s.baudRate,
        "data_bits":    s.dataBits,
        "stop_bits":    s.stopBits,
        "parity":       s.parity,
        "auto_detect":  s.autoDetect,
        "gadget":       s.gadget.GetStatus(),
    }
    
    return stats
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
