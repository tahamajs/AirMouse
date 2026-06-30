package websocket

import (
	"encoding/json"
	"net/http"
	"time"
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
	
	var devicesCount int
	if s.deviceMgr != nil {
		devicesCount = len(s.deviceMgr.GetActiveDevices())
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]interface{}{
		"clicks":  clicks,
		"scrolls": scrolls,
		"devices": devicesCount,
		"uptime":  uptime,
		"quality": map[string]interface{}{
			"level":   "excellent",
			"text":    "Excellent",
			"latency": 5,
			"signal":  "Strong",
		},
	})
}

// handleAPIDevices returns the list of connected devices
func (s *Server) handleAPIDevices(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var devicesList []map[string]interface{}
	if s.deviceMgr != nil {
		for _, d := range s.deviceMgr.GetActiveDevices() {
			devicesList = append(devicesList, map[string]interface{}{
				"id":        d.ID,
				"name":      d.Name,
				"type":      d.Type,
				"connected": true,
				"ip":        "Network",
			})
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(devicesList)
}
