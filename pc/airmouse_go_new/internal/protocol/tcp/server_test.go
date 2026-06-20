package tcp

import (
	"bufio"
	"net"
	"strings"
	"testing"
	"time"

	"airmouse-go/internal/adaptivesmoothing"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
)

type fakeMouse struct {
	moves   [][2]float64
	clicks  []string
	scrolls []int
}

func (f *fakeMouse) Move(dx, dy float64) {
	f.moves = append(f.moves, [2]float64{dx, dy})
}

func (f *fakeMouse) Click(button string) { f.clicks = append(f.clicks, button) }
func (f *fakeMouse) DoubleClick()        { f.clicks = append(f.clicks, "double") }
func (f *fakeMouse) Scroll(delta int)    { f.scrolls = append(f.scrolls, delta) }
func (f *fakeMouse) Stats() (int64, int64, int64, int64) {
	return 0, 0, 0, 0
}
func (f *fakeMouse) SetSensitivity(float64)                               {}
func (f *fakeMouse) GetSensitivity() float64                              { return 1 }
func (f *fakeMouse) SetSmoothing(bool)                                    {}
func (f *fakeMouse) SetAcceleration(bool, float64)                        {}
func (f *fakeMouse) EnablePredictive(bool)                                {}
func (f *fakeMouse) SetPredictiveBlendFactor(float64)                     {}
func (f *fakeMouse) EnableAISmoothing(bool)                               {}
func (f *fakeMouse) SetAISmoother(*control.AISmoother)                    {}
func (f *fakeMouse) EnableMLPrediction(bool)                              {}
func (f *fakeMouse) SetMLBlendFactor(float64)                             {}
func (f *fakeMouse) EnableHumanizer(bool)                                 {}
func (f *fakeMouse) SetHumanizerConfig(adaptivesmoothing.HumanizerConfig) {}
func (f *fakeMouse) ResetStats()                                          {}
func (f *fakeMouse) GetPosition() (float64, float64)                      { return 0, 0 }

func TestTCPProcessLine_SimulatedAndroidSession(t *testing.T) {
	mouse := &fakeMouse{}
	s := NewServer("127.0.0.1", 0, mouse, device.NewManager())

	serverConn, clientConn := net.Pipe()
	defer serverConn.Close()
	defer clientConn.Close()

	client := &Client{
		ID:          "client-1",
		Name:        "Unknown",
		Conn:        serverConn,
		ConnectedAt: time.Now(),
		LastActive:  time.Now(),
	}

	// hello -> welcome
	s.processLine(client, []byte(`{"type":"hello","name":"Pixel 8","version":"3.0"}`+"\n"))
	serverConn.SetReadDeadline(time.Now().Add(500 * time.Millisecond))
	welcome, err := bufio.NewReader(clientConn).ReadString('\n')
	if err != nil {
		t.Fatalf("expected welcome from server, got error: %v", err)
	}
	if !strings.Contains(welcome, `"type":"welcome"`) {
		t.Fatalf("expected welcome message, got %q", welcome)
	}

	// move with Android aliases -> forwarded to mouse
	s.processLine(client, []byte(`{"type":"move","DeltaX":7.5,"DeltaY":-2.25}`+"\n"))
	if len(mouse.moves) != 1 {
		t.Fatalf("expected 1 move, got %d", len(mouse.moves))
	}
	if mouse.moves[0][0] != 7.5 || mouse.moves[0][1] != -2.25 {
		t.Fatalf("unexpected move forwarded: %+v", mouse.moves[0])
	}

	// click -> ack
	s.processLine(client, []byte(`{"type":"click","button":"left","id":"msg-7"}`+"\n"))
	ack, err := bufio.NewReader(clientConn).ReadString('\n')
	if err != nil {
		t.Fatalf("expected ack from server, got error: %v", err)
	}
	if !strings.Contains(ack, `"type":"ack"`) || !strings.Contains(ack, `"id":"msg-7"`) {
		t.Fatalf("unexpected ack payload: %q", ack)
	}
	if len(mouse.clicks) != 1 || mouse.clicks[0] != "left" {
		t.Fatalf("unexpected clicks: %#v", mouse.clicks)
	}

	// scroll alias -> ack and scroll forwarded
	s.processLine(client, []byte(`{"type":"scroll","Scroll":-3,"id":"msg-8"}`+"\n"))
	ack, err = bufio.NewReader(clientConn).ReadString('\n')
	if err != nil {
		t.Fatalf("expected scroll ack from server, got error: %v", err)
	}
	if !strings.Contains(ack, `"id":"msg-8"`) {
		t.Fatalf("unexpected scroll ack payload: %q", ack)
	}
	if len(mouse.scrolls) != 1 || mouse.scrolls[0] != -3 {
		t.Fatalf("unexpected scrolls: %#v", mouse.scrolls)
	}
}
