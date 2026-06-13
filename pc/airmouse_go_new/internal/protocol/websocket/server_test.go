package websocket

import (
	"strings"
	"testing"

	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/proximity"
)

type wsFakeMouse struct {
	moveCalls   []struct{ dx, dy float64 }
	clickCalls  []string
	doubleCalls int
	scrollCalls []int
}

func (f *wsFakeMouse) Move(dx, dy float64) { f.moveCalls = append(f.moveCalls, struct{ dx, dy float64 }{dx, dy}) }
func (f *wsFakeMouse) Click(button string) { f.clickCalls = append(f.clickCalls, button) }
func (f *wsFakeMouse) DoubleClick()        { f.doubleCalls++ }
func (f *wsFakeMouse) Scroll(delta int)    { f.scrollCalls = append(f.scrollCalls, delta) }
func (f *wsFakeMouse) Stats() (int64, int64, int64, int64) {
	return 0, 0, 0, 0
}
func (f *wsFakeMouse) SetSensitivity(float64)            {}
func (f *wsFakeMouse) GetSensitivity() float64           { return 1 }
func (f *wsFakeMouse) SetSmoothing(bool)                 {}
func (f *wsFakeMouse) SetAcceleration(bool, float64)     {}
func (f *wsFakeMouse) EnablePredictive(bool)             {}
func (f *wsFakeMouse) SetPredictiveBlendFactor(float64)  {}
func (f *wsFakeMouse) EnableAISmoothing(bool)            {}
func (f *wsFakeMouse) SetAISmoother(*control.AISmoother) {}
func (f *wsFakeMouse) EnableMLPrediction(bool)           {}
func (f *wsFakeMouse) SetMLBlendFactor(float64)          {}

func TestProcessMessageRoutesAndroidCommands(t *testing.T) {
	mouse := &wsFakeMouse{}
	deviceMgr := device.NewManager()
	s := &Server{
		clients:   map[string]*WSClient{},
		mouse:     mouse,
		deviceMgr: deviceMgr,
	}
	client := &WSClient{ID: "ws-1", Send: make(chan []byte, 8)}
	s.deviceMgr.RegisterDevice(client.ID, device.TypeWebSocket, "Android")

	s.processMessage(client, "hello", map[string]any{"name": "Pixel 8", "version": "3.0"}, nil)
	s.processMessage(client, "move", map[string]any{"dx": 5.5, "dy": -1.25}, nil)
	s.processMessage(client, "click", map[string]any{"button": "left"}, nil)
	s.processMessage(client, "doubleclick", map[string]any{}, nil)
	s.processMessage(client, "rightclick", map[string]any{}, nil)
	s.processMessage(client, "scroll", map[string]any{"delta": -2}, nil)
	s.processMessage(client, "gesture", map[string]any{"gesture": "ThumbsUp", "confidence": 0.91}, nil)
	s.processMessage(client, "proximity", map[string]any{"is_near": true, "distance": 1.23}, nil)
	s.processMessage(client, "control", map[string]any{"command": "pause_movement"}, nil)
	if !control.IsMovementPaused() {
		t.Fatal("expected movement to be paused")
	}
	s.processMessage(client, "control", map[string]any{"command": "resume_movement"}, nil)
	if control.IsMovementPaused() {
		t.Fatal("expected movement to be resumed")
	}

	if len(mouse.moveCalls) != 1 {
		t.Fatalf("moveCalls = %d, want 1", len(mouse.moveCalls))
	}
	if len(mouse.clickCalls) != 2 || mouse.clickCalls[0] != "left" || mouse.clickCalls[1] != "right" {
		t.Fatalf("clickCalls = %v, want [left right]", mouse.clickCalls)
	}
	if mouse.doubleCalls != 1 {
		t.Fatalf("doubleCalls = %d, want 1", mouse.doubleCalls)
	}
	if len(mouse.scrollCalls) != 1 || mouse.scrollCalls[0] != -2 {
		t.Fatalf("scrollCalls = %v, want [-2]", mouse.scrollCalls)
	}
	if got := len(client.Send); got != 1 {
		t.Fatalf("expected welcome message, got %d messages", got)
	}
	if got := <-client.Send; !strings.Contains(string(got), `"type":"welcome"`) {
		t.Fatalf("first send = %q, want welcome", string(got))
	}
}

func TestProcessMessageSendsAckForCriticalCommands(t *testing.T) {
	mouse := &wsFakeMouse{}
	s := &Server{
		clients:   map[string]*WSClient{},
		mouse:     mouse,
		deviceMgr: device.NewManager(),
	}
	client := &WSClient{ID: "ws-2", Send: make(chan []byte, 8)}
	s.processMessage(client, "click", map[string]any{"button": "left"}, ptr("abc"))

	select {
	case msg := <-client.Send:
		if !strings.Contains(string(msg), `"type":"ack"`) || !strings.Contains(string(msg), `"id":"abc"`) {
			t.Fatalf("ack = %q, want id abc", string(msg))
		}
	default:
		t.Fatal("expected ack to be queued")
	}
}

func ptr(s string) *string { return &s }

func TestProcessMessagePingPongAndUnknown(t *testing.T) {
	s := &Server{
		clients:   map[string]*WSClient{},
		mouse:     &wsFakeMouse{},
		deviceMgr: device.NewManager(),
	}
	client := &WSClient{ID: "ws-3", Send: make(chan []byte, 8)}
	s.processMessage(client, "ping", map[string]any{}, nil)
	s.processMessage(client, "pong", map[string]any{}, nil)
	s.processMessage(client, "unknown", map[string]any{}, nil)

	select {
	case msg := <-client.Send:
		if !strings.Contains(string(msg), `"type":"pong"`) {
			t.Fatalf("expected pong, got %q", string(msg))
		}
	default:
		t.Fatal("expected pong to be queued")
	}
}

func TestSetProximityManagerStoresManager(t *testing.T) {
	s := &Server{}
	pm := proximity.NewManager()
	s.SetProximityManager(pm)
	if s.proximityMgr != pm {
		t.Fatal("SetProximityManager did not store the manager")
	}
}
