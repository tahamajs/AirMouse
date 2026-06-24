package websocket

import (
	"testing"

	"airmouse-go/internal/adaptivesmoothing"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
)

type fakeWSMouse struct {
	moves   [][2]float64
	clicks  []string
	scrolls []int
}

func (f *fakeWSMouse) Move(dx, dy float64) { f.moves = append(f.moves, [2]float64{dx, dy}) }
func (f *fakeWSMouse) Click(button string) { f.clicks = append(f.clicks, button) }
func (f *fakeWSMouse) DoubleClick()        { f.clicks = append(f.clicks, "double") }
func (f *fakeWSMouse) Scroll(delta int)    { f.scrolls = append(f.scrolls, delta) }
func (f *fakeWSMouse) Stats() (int64, int64, int64, int64) {
	return 0, 0, 0, 0
}
func (f *fakeWSMouse) SetSensitivity(float64)                               {}
func (f *fakeWSMouse) GetSensitivity() float64                              { return 1 }
func (f *fakeWSMouse) SetSmoothing(bool)                                    {}
func (f *fakeWSMouse) SetAcceleration(bool, float64)                        {}
func (f *fakeWSMouse) EnablePredictive(bool)                                {}
func (f *fakeWSMouse) SetPredictiveBlendFactor(float64)                     {}
func (f *fakeWSMouse) EnableAISmoothing(bool)                               {}
func (f *fakeWSMouse) SetAISmoother(*control.AISmoother)                    {}
func (f *fakeWSMouse) EnableMLPrediction(bool)                              {}
func (f *fakeWSMouse) SetMLBlendFactor(float64)                             {}
func (f *fakeWSMouse) EnableHumanizer(bool)                                 {}
func (f *fakeWSMouse) SetHumanizerConfig(adaptivesmoothing.HumanizerConfig) {}
func (f *fakeWSMouse) ResetStats()                                          {}
func (f *fakeWSMouse) GetPosition() (float64, float64)                      { return 0, 0 }

func TestWebSocketProcessMessage_SimulatedAndroidSession(t *testing.T) {
	mouse := &fakeWSMouse{}
	s := NewServer(0, mouse, device.NewManager(), nil)
	client := &WSClient{
		ID:         "ws-1",
		Name:       "Unknown",
		Send:       make(chan []byte, 8),
		BinarySend: make(chan []byte, 2),
	}

	s.processMessage(client, "hello", map[string]any{
		"name":    "Pixel 8",
		"version": "3.0",
	}, nil)

	if client.Approved.Load() {
		t.Fatal("expected client to remain pending until approved")
	}
	select {
	case msg := <-client.Send:
		t.Fatalf("did not expect welcome before approval, got %q", string(msg))
	default:
	}

	client.DeviceID = "device-1"
	s.mu.Lock()
	s.clients["ws-1"] = client
	s.mu.Unlock()
	if err := s.ApproveDevice("device-1"); err != nil {
		t.Fatalf("approve device: %v", err)
	}
	select {
	case msg := <-client.Send:
		if got := string(msg); got == "" || got[0] != '{' {
			t.Fatalf("expected welcome JSON, got %q", got)
		}
	default:
		t.Fatal("expected welcome message to be queued")
	}

	s.processMessage(client, "move", map[string]any{
		"DeltaX": 4.5,
		"DeltaY": -1.25,
	}, nil)
	if len(mouse.moves) != 1 {
		t.Fatalf("expected 1 move, got %d", len(mouse.moves))
	}
	if mouse.moves[0][0] != 4.5 || mouse.moves[0][1] != -1.25 {
		t.Fatalf("unexpected move forwarded: %+v", mouse.moves[0])
	}

	s.processMessage(client, "click", map[string]any{
		"button": "left",
	}, strPtr("msg-1"))
	select {
	case msg := <-client.Send:
		if got := string(msg); got == "" || got[0] != '{' {
			t.Fatalf("expected ack JSON, got %q", got)
		}
	default:
		t.Fatal("expected ack to be queued")
	}
	if len(mouse.clicks) != 1 || mouse.clicks[0] != "left" {
		t.Fatalf("unexpected click forwarded: %#v", mouse.clicks)
	}

	s.processMessage(client, "scroll", map[string]any{
		"Scroll": -3,
	}, strPtr("msg-2"))
	select {
	case msg := <-client.Send:
		if got := string(msg); got == "" || got[0] != '{' {
			t.Fatalf("expected scroll ack JSON, got %q", got)
		}
	default:
		t.Fatal("expected scroll ack to be queued")
	}
	if len(mouse.scrolls) != 1 || mouse.scrolls[0] != -3 {
		t.Fatalf("unexpected scroll forwarded: %#v", mouse.scrolls)
	}
}

func strPtr(v string) *string { return &v }
