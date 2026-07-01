package websocket

import (
	"encoding/json"
	"net"
	"net/http"
	"time"

	"airmouse-go/internal/config"
	"airmouse-go/internal/device"
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
	if s.deviceMgr != nil {
		for _, d := range s.deviceMgr.GetAllDevices() {
			if d.Status == device.StatusConnected {
				active++
			} else if d.Status == device.StatusPendingApproval {
				pending++
			}
		}
	}

	cfg := config.Get()
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"clicks":  clicks,
		"scrolls": scrolls,
		"devices": active,
		"pending": pending,
		"uptime":  uptime,
		"ips":     getLocalIPs(),
		"ports": map[string]int{
			"websocket": cfg.WebSocketPort,
			"tcp":       cfg.Port,
			"udp":       cfg.UDPPort,
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
		currentCfg.ServerName = newCfg.ServerName
		currentCfg.Sensitivity = newCfg.Sensitivity
		currentCfg.SmoothingEnabled = newCfg.SmoothingEnabled
		currentCfg.AccelerationEnabled = newCfg.AccelerationEnabled
		currentCfg.AccelerationFactor = newCfg.AccelerationFactor
		currentCfg.MaxAcceleration = newCfg.MaxAcceleration
		currentCfg.HapticEnabled = newCfg.HapticEnabled

		currentCfg.EnableAISmoothing = newCfg.EnableAISmoothing
		currentCfg.EnablePredictive = newCfg.EnablePredictive
		currentCfg.PredictiveBlendFactor = newCfg.PredictiveBlendFactor
		currentCfg.EnableJitterCompensation = newCfg.EnableJitterCompensation
		currentCfg.JitterBlendFactor = newCfg.JitterBlendFactor

		currentCfg.ClickThreshold = newCfg.ClickThreshold
		currentCfg.ScrollThreshold = newCfg.ScrollThreshold
		currentCfg.DoubleClickInterval = newCfg.DoubleClickInterval
		currentCfg.RightClickTilt = newCfg.RightClickTilt

		currentCfg.ProximityEnabled = newCfg.ProximityEnabled
		currentCfg.BLEEnabled = newCfg.BLEEnabled
		currentCfg.HIDProxyEnabled = newCfg.HIDProxyEnabled

		currentCfg.AuthEnabled = newCfg.AuthEnabled
		
		currentCfg.Save()
		
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{"success": true})
		return
	}

	http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
}
