package usb

import (
    "bufio"
    "encoding/json"
    "fmt"
    "io"
    "os"
    "sync"
    "time"

    "airmouse-go/internal/control"
    "airmouse-go/internal/device"
    "airmouse-go/internal/utils"
)

type SerialDevice struct {
    Path      string
    File      *os.File
    Reader    *bufio.Reader
    Writer    io.Writer
    Connected bool
}

type USBServer struct {
    devices   map[string]*SerialDevice
    mouse     control.MouseController
    deviceMgr *device.Manager
    mu        sync.RWMutex
    running   bool
}

type USBMessage struct {
    Type    string          `json:"type"`
    Payload json.RawMessage `json:"payload"`
    ID      string          `json:"id,omitempty"`
}

type MovePayload struct {
    DX int `json:"dx"`
    DY int `json:"dy"`
}

func NewUSBServer(mouse control.MouseController, deviceMgr *device.Manager) *USBServer {
    return &USBServer{
        devices:   make(map[string]*SerialDevice),
        mouse:     mouse,
        deviceMgr: deviceMgr,
    }
}

func (s *USBServer) Start() error {
    s.running = true
    go s.scanSerialPorts()
    utils.LogInfo("USB serial server started")
    return nil
}

func (s *USBServer) scanSerialPorts() {
    // Common serial port patterns
    ports := []string{
        "/dev/ttyACM0", "/dev/ttyACM1",  // Linux CDC ACM
        "/dev/ttyUSB0", "/dev/ttyUSB1",  // Linux USB serial
        "COM3", "COM4", "COM5",          // Windows
        "/dev/cu.usbmodem*",              // macOS
    }
    for s.running {
        for _, port := range ports {
            s.tryConnectPort(port)
        }
        time.Sleep(5 * time.Second)
    }
}

func (s *USBServer) tryConnectPort(path string) {
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

    device := &SerialDevice{
        Path:      path,
        File:      file,
        Reader:    bufio.NewReader(file),
        Writer:    file,
        Connected: true,
    }

    s.mu.Lock()
    s.devices[path] = device
    s.mu.Unlock()

    utils.LogInfo("USB serial device connected", "path", path)
    s.deviceMgr.RegisterDevice(path, device.TypeUSB, "USB Device")

    go s.handleDevice(device)
}

func (s *USBServer) handleDevice(dev *SerialDevice) {
    for s.running && dev.Connected {
        line, err := dev.Reader.ReadString('\n')
        if err != nil {
            break
        }
        var msg USBMessage
        if err := json.Unmarshal([]byte(line), &msg); err != nil {
            continue
        }
        s.processMessage(dev, &msg)
    }
    dev.File.Close()
    s.mu.Lock()
    delete(s.devices, dev.Path)
    s.mu.Unlock()
    utils.LogInfo("USB serial device disconnected", "path", dev.Path)
    s.deviceMgr.UnregisterDevice(dev.Path)
}

func (s *USBServer) processMessage(dev *SerialDevice, msg *USBMessage) {
    switch msg.Type {
    case "move":
        var p MovePayload
        if err := json.Unmarshal(msg.Payload, &p); err == nil {
            s.mouse.Move(float64(p.DX), float64(p.DY))
        }
    case "click":
        s.mouse.Click("left")
    case "rightclick":
        s.mouse.Click("right")
    case "doubleclick":
        s.mouse.DoubleClick()
    case "scroll":
        var p struct{ Delta int }
        if err := json.Unmarshal(msg.Payload, &p); err == nil {
            s.mouse.Scroll(p.Delta)
        }
    }
}

func (s *USBServer) Stop() {
    s.running = false
    s.mu.Lock()
    for _, dev := range s.devices {
        dev.File.Close()
    }
    s.devices = make(map[string]*SerialDevice)
    s.mu.Unlock()
    utils.LogInfo("USB serial server stopped")
}

func (s *USBServer) GetStats() map[string]interface{} {
    s.mu.RLock()
    defer s.mu.RUnlock()
    return map[string]interface{}{
        "devices": len(s.devices),
    }
}