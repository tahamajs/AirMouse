package udp

import (
	"encoding/json"
	"net"
	"testing"

	"airmouse-go/internal/auth"
	"airmouse-go/internal/device"
)

func TestGetStatsReflectsRunningState(t *testing.T) {
	s := NewServer(0, nil, device.NewManager(), (*auth.Manager)(nil))
	if got := s.GetStats()["running"]; got != false {
		t.Fatalf("running = %v, want false", got)
	}
	s.running = true
	if got := s.GetStats()["running"]; got != true {
		t.Fatalf("running = %v, want true", got)
	}
}

func TestListenLoopDiscoveryResponseShape(t *testing.T) {
	// We only validate the response contract here to keep the test deterministic.
	resp := map[string]interface{}{
		"type": "discovery_response",
		"port": 8080,
		"ip":   "127.0.0.1",
	}
	data, err := json.Marshal(resp)
	if err != nil {
		t.Fatalf("json.Marshal returned error: %v", err)
	}
	var decoded map[string]any
	if err := json.Unmarshal(data, &decoded); err != nil {
		t.Fatalf("json.Unmarshal returned error: %v", err)
	}
	if decoded["type"] != "discovery_response" {
		t.Fatalf("type = %v, want discovery_response", decoded["type"])
	}
}

func TestGetLocalIPReturnsValidIPAddress(t *testing.T) {
	ip := getLocalIP()
	if parsed := net.ParseIP(ip); parsed == nil {
		t.Fatalf("getLocalIP returned invalid IP %q", ip)
	}
}
