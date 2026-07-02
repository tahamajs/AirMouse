package websocket

import (
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"time"

	"github.com/kbinani/screenshot"

	"airmouse-go/control/common"
	"airmouse-go/control/syscmd"
	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
	"airmouse-go/internal/sysaction"
)

// handleAPIStats returns system statistics
func (s *Server) handleAPIStats(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	s.mu.RLock()
	uptime := time.Since(s.startTime).Seconds()
	s.mu.RUnlock()

	var clicks, scrolls int64
	if s.mouse != nil {
		c, _, _, scr := s.mouse.Stats()
		clicks = c
		scrolls = scr
	}

	var active, pending int
	var lastDeviceName string = "None"
	var lastDeviceTime string = "-"
	var maxConnectTime time.Time
	var totalPackets, totalBytes int64

	if s.deviceMgr != nil {
		for _, d := range s.deviceMgr.GetAllDevices() {
			if d.Status == device.StatusConnected {
				active++
				totalPackets += d.MessagesRecv
				totalBytes += d.BytesRecv
			} else if d.Status == device.StatusPendingApproval {
				pending++
			}
			if d.ConnectedAt.After(maxConnectTime) {
				maxConnectTime = d.ConnectedAt
				lastDeviceName = d.Name
				lastDeviceTime = d.ConnectedAt.Format("15:04:05")
			}
		}
	}

	activeMode := common.GetActiveMode()
	activeTheme := common.GetActiveTheme()

	cfg := config.Get()
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"clicks":           clicks,
		"scrolls":          scrolls,
		"devices":          active,
		"pending":          pending,
		"uptime":           uptime,
		"paused":           common.IsMovementPaused(),
		"ips":              getLocalIPs(),
		"last_device":      lastDeviceName,
		"last_device_time": lastDeviceTime,
		"active_mode":      activeMode,
		"active_theme":     activeTheme,
		"total_packets":    totalPackets,
		"total_bytes":      totalBytes,
		"ports": map[string]int{
			"websocket": cfg.WebSocketPort,
			"tcp":       cfg.Port,
			"udp":       cfg.UDPPort,
		},
		"protocols": map[string]bool{
			"websocket": cfg.EnableWebSocket,
			"tcp":       cfg.EnableTCP,
			"udp":       cfg.EnableUDP,
			"bluetooth": cfg.EnableBluetooth,
			"serial":    cfg.EnableSerial,
		},
		"quality": map[string]interface{}{
			"level":   "excellent",
			"text":    "Excellent",
			"latency": 5,
			"signal":  "Strong",
		},
	})
}

// getLocalIPs returns non-loopback IPv4 addresses
func getLocalIPs() []string {
	var ips []string
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return ips
	}
	for _, addr := range addrs {
		if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
			if ipnet.IP.To4() != nil {
				ips = append(ips, ipnet.IP.String())
			}
		}
	}
	// Fallback to localhost if no active network IPs found
	if len(ips) == 0 {
		ips = append(ips, "127.0.0.1")
	}
	return ips
}

// handleAPIDevices returns the list of all devices
func (s *Server) handleAPIDevices(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var devicesList []map[string]interface{}
	if s.deviceMgr != nil {
		for _, d := range s.deviceMgr.GetAllDevices() {
			devicesList = append(devicesList, map[string]interface{}{
				"id":              d.ID,
				"name":            d.Name,
				"type":            d.Type,
				"status":          string(d.Status),
				"ip":              d.IPAddress,
				"mac":             d.MACAddress,
				"connected":       d.Status == device.StatusConnected,
				"bytes_sent":      d.BytesSent,
				"bytes_recv":      d.BytesRecv,
				"msg_sent":        d.MessagesSent,
				"msg_recv":        d.MessagesRecv,
				"rssi":            d.RSSI,
				"model":           d.DeviceModel,
				"android_version": d.AndroidVersion,
				"manufacturer":    d.Manufacturer,
				"brand":           d.Brand,
				"sdk":             d.SDKInt,
			})
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(devicesList)
}

type deviceRequest struct {
	ID string `json:"id"`
}

// handleDeviceApprove approves a pending device
func (s *Server) handleDeviceApprove(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req deviceRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	if s.GlobalApproveDevice != nil {
		err := s.GlobalApproveDevice(req.ID)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{"success": true})
}

// handleDeviceReject rejects a pending device
func (s *Server) handleDeviceReject(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req deviceRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	if s.GlobalRejectDevice != nil {
		err := s.GlobalRejectDevice(req.ID)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{"success": true})
}

// handleAPIConfig returns or updates config
func (s *Server) handleAPIConfig(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodGet {
		w.Header().Set("Content-Type", "application/json")
		cfg := config.Get()
		json.NewEncoder(w).Encode(cfg)
		return
	}

	if r.Method == http.MethodPost {
		var newCfg config.Config
		if err := json.NewDecoder(r.Body).Decode(&newCfg); err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}

		currentCfg := config.Get()
		
		// Update configurable fields
		currentCfg.Port = newCfg.Port
		currentCfg.WebSocketPort = newCfg.WebSocketPort
		currentCfg.UDPPort = newCfg.UDPPort
		currentCfg.ServerName = newCfg.ServerName
		currentCfg.Theme = newCfg.Theme

		// Protocol settings
		currentCfg.EnableTCP = newCfg.EnableTCP
		currentCfg.EnableWebSocket = newCfg.EnableWebSocket
		currentCfg.EnableUDP = newCfg.EnableUDP
		currentCfg.EnableBluetooth = newCfg.EnableBluetooth
		currentCfg.EnableSerial = newCfg.EnableSerial

		// Mouse settings
		currentCfg.Sensitivity = newCfg.Sensitivity
		currentCfg.SmoothingEnabled = newCfg.SmoothingEnabled
		currentCfg.AccelerationEnabled = newCfg.AccelerationEnabled
		currentCfg.AccelerationFactor = newCfg.AccelerationFactor
		currentCfg.MaxAcceleration = newCfg.MaxAcceleration
		currentCfg.HapticEnabled = newCfg.HapticEnabled

		// AI settings
		currentCfg.EnableAISmoothing = newCfg.EnableAISmoothing
		currentCfg.EnablePredictive = newCfg.EnablePredictive
		currentCfg.PredictiveBlendFactor = newCfg.PredictiveBlendFactor
		currentCfg.EnableJitterCompensation = newCfg.EnableJitterCompensation
		currentCfg.JitterBlendFactor = newCfg.JitterBlendFactor

		// ML settings
		currentCfg.EnableMLPrediction = newCfg.EnableMLPrediction
		currentCfg.MLModelPath = newCfg.MLModelPath
		currentCfg.MLSequenceLength = newCfg.MLSequenceLength
		currentCfg.MLBlendFactor = newCfg.MLBlendFactor
		currentCfg.MLInferenceInterval = newCfg.MLInferenceInterval

		// Humanizer
		currentCfg.EnableHumanizer = newCfg.EnableHumanizer
		currentCfg.HumanizerTremorAmplitude = newCfg.HumanizerTremorAmplitude
		currentCfg.HumanizerBSplineSegments = newCfg.HumanizerBSplineSegments
		currentCfg.HumanizerNoiseAmplitude = newCfg.HumanizerNoiseAmplitude
		currentCfg.HumanizerVelocityPeakRatio = newCfg.HumanizerVelocityPeakRatio

		// Particle Filter
		currentCfg.EnableParticleFilter = newCfg.EnableParticleFilter
		currentCfg.ParticleFilterNumParticles = newCfg.ParticleFilterNumParticles

		// Gesture settings
		currentCfg.GestureConfidenceThreshold = newCfg.GestureConfidenceThreshold
		currentCfg.GestureCooldown = newCfg.GestureCooldown
		currentCfg.ClickThreshold = newCfg.ClickThreshold
		currentCfg.ScrollThreshold = newCfg.ScrollThreshold
		currentCfg.DoubleClickInterval = newCfg.DoubleClickInterval
		currentCfg.RightClickTilt = newCfg.RightClickTilt
		currentCfg.GestureMappings = newCfg.GestureMappings

		// Proximity settings
		currentCfg.ProximityEnabled = newCfg.ProximityEnabled
		currentCfg.ProximityNearThreshold = newCfg.ProximityNearThreshold
		currentCfg.ProximityFarThreshold = newCfg.ProximityFarThreshold
		currentCfg.ProximityScanInterval = newCfg.ProximityScanInterval
		currentCfg.BLEEnabled = newCfg.BLEEnabled
		currentCfg.HIDProxyEnabled = newCfg.HIDProxyEnabled

		// USB Gadget
		currentCfg.USBGadgetEnabled = newCfg.USBGadgetEnabled
		currentCfg.USBVendorID = newCfg.USBVendorID
		currentCfg.USBProductID = newCfg.USBProductID
		currentCfg.USBManufacturer = newCfg.USBManufacturer
		currentCfg.USBProduct = newCfg.USBProduct

		// Security & Performance
		currentCfg.AuthEnabled = newCfg.AuthEnabled
		currentCfg.EnablePairingUI = newCfg.EnablePairingUI
		currentCfg.TokenTTL = newCfg.TokenTTL
		currentCfg.MaxClients = newCfg.MaxClients
		currentCfg.HeartbeatInterval = newCfg.HeartbeatInterval
		currentCfg.ConnectionTimeout = newCfg.ConnectionTimeout

		// Logging
		currentCfg.LogLevel = newCfg.LogLevel
		currentCfg.LogColor = newCfg.LogColor
		currentCfg.DebugMode = newCfg.DebugMode
		currentCfg.MetricsEnabled = newCfg.MetricsEnabled
		
		currentCfg.Save()

		// Apply the configuration updates to the running mouse controller
		if s.mouse != nil {
			s.mouse.SetSensitivity(currentCfg.Sensitivity)
			s.mouse.SetSmoothing(currentCfg.SmoothingEnabled)
			s.mouse.SetAcceleration(currentCfg.AccelerationEnabled, currentCfg.AccelerationFactor)
			s.mouse.EnablePredictive(currentCfg.EnablePredictive)
			s.mouse.SetPredictiveBlendFactor(currentCfg.PredictiveBlendFactor)
			s.mouse.EnableAISmoothing(currentCfg.EnableAISmoothing)
			s.mouse.EnableMLPrediction(currentCfg.EnableMLPrediction)
		}

		// Update OS system-wide mouse scaling/speed as well
		sysaction.SetSystemMouseSpeed(currentCfg.Sensitivity)
		
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{"success": true})
		return
	}

	http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
}

// handleAPIStatusToggle handles pausing/resuming the mouse movement
func (s *Server) handleAPIStatusToggle(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req struct {
		Paused bool `json:"paused"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	common.SetMovementPaused(req.Paused)

	// Broadcast pause/resume state to all active client devices
	var action string
	if req.Paused {
		action = "pause_movement"
	} else {
		action = "resume_movement"
	}
	msg := fmt.Sprintf(`{"type":"control","action":"%s"}`+"\n", action)

	s.mu.RLock()
	for _, client := range s.clients {
		s.safeSendText(client, []byte(msg))
	}
	s.mu.RUnlock()

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"paused":  common.IsMovementPaused(),
	})
}

// handleAPICommand handles triggering system commands via HTTP POST
func (s *Server) handleAPICommand(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var req struct {
		Command string `json:"command"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	err := syscmd.ExecuteSystemCommand(req.Command)
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"error":   err.Error(),
		})
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
	})
}

// handleAPIFiles lists all uploaded files in the transfer directory
func (s *Server) handleAPIFiles(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	dir := s.fileTransferDir()
	_ = os.MkdirAll(dir, 0755)

	entries, err := os.ReadDir(dir)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	type FileInfo struct {
		Name string `json:"name"`
		Size int64  `json:"size"`
		URL  string `json:"url"`
	}

	files := []FileInfo{}
	for _, entry := range entries {
		if !entry.IsDir() {
			info, err := entry.Info()
			if err == nil {
				files = append(files, FileInfo{
					Name: entry.Name(),
					Size: info.Size(),
					URL:  fmt.Sprintf("/download/%s", entry.Name()),
				})
			}
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(files)
}

// handleAPIFilesDelete deletes a file from the transfer directory
func (s *Server) handleAPIFilesDelete(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodDelete && r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	filename := r.URL.Query().Get("name")
	if filename == "" {
		// fallback to body if JSON
		var req struct {
			Name string `json:"name"`
		}
		if err := json.NewDecoder(r.Body).Decode(&req); err == nil {
			filename = req.Name
		}
	}

	filename = sanitizeFileName(filename)
	if filename == "" {
		http.Error(w, `{"error":"missing filename"}`, http.StatusBadRequest)
		return
	}

	dir := s.fileTransferDir()
	filePath := filepath.Join(dir, filename)

	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		http.Error(w, `{"error":"file not found"}`, http.StatusNotFound)
		return
	}

	err := os.Remove(filePath)
	if err != nil {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"error":   err.Error(),
		})
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
	})
}

// handleAPIDisplays returns information about all connected displays/monitors
func (s *Server) handleAPIDisplays(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	n := screenshot.NumActiveDisplays()
	type DisplayInfo struct {
		ID        int    `json:"id"`
		Width     int    `json:"width"`
		Height    int    `json:"height"`
		IsPrimary bool   `json:"is_primary"`
		Name      string `json:"name"`
	}

	displays := make([]DisplayInfo, 0, n)
	for i := 0; i < n; i++ {
		bounds := screenshot.GetDisplayBounds(i)
		isPrimary := i == 0 // screenshot package considers display 0 to be primary
		displays = append(displays, DisplayInfo{
			ID:        i,
			Width:     bounds.Dx(),
			Height:    bounds.Dy(),
			IsPrimary: isPrimary,
			Name:      fmt.Sprintf("Display %d (%dx%d)", i, bounds.Dx(), bounds.Dy()),
		})
	}

	// Fallback if no displays detected
	if len(displays) == 0 {
		displays = append(displays, DisplayInfo{
			ID:        0,
			Width:     1920,
			Height:    1080,
			IsPrimary: true,
			Name:      "Default Primary Display (1920x1080)",
		})
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(displays)
}
