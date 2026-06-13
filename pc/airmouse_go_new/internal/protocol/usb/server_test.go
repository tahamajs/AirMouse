//go:build linux

package usb

import (
	"errors"
	"os"
	"testing"

	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
)

type fakeMouse struct {
	moveCalls   []struct{ dx, dy float64 }
	clickCalls  []string
	doubleCalls int
	scrollCalls []int
}

func (f *fakeMouse) Move(dx, dy float64) { f.moveCalls = append(f.moveCalls, struct{ dx, dy float64 }{dx, dy}) }
func (f *fakeMouse) Click(button string)  { f.clickCalls = append(f.clickCalls, button) }
func (f *fakeMouse) DoubleClick()        { f.doubleCalls++ }
func (f *fakeMouse) Scroll(delta int)     { f.scrollCalls = append(f.scrollCalls, delta) }
func (f *fakeMouse) Stats() (int64, int64, int64, int64) { return 0, 0, 0, 0 }
func (f *fakeMouse) SetSensitivity(float64)              {}
func (f *fakeMouse) GetSensitivity() float64             { return 1 }
func (f *fakeMouse) SetSmoothing(bool)                   {}
func (f *fakeMouse) SetAcceleration(bool, float64)       {}
func (f *fakeMouse) EnablePredictive(bool)               {}
func (f *fakeMouse) SetPredictiveBlendFactor(float64)    {}
func (f *fakeMouse) EnableAISmoothing(bool)              {}
func (f *fakeMouse) SetAISmoother(*control.AISmoother)   {}
func (f *fakeMouse) EnableMLPrediction(bool)             {}
func (f *fakeMouse) SetMLBlendFactor(float64)            {}

func TestProcessMessageRoutesActions(t *testing.T) {
	mouse := &fakeMouse{}
	s := &Server{
		devices:   map[string]*SerialDevice{},
		mouse:     mouse,
		deviceMgr: device.NewManager(),
	}

	s.processMessage(&SerialDevice{Path: "dev1"}, &USBMessage{Type: "move", Payload: []byte(`{"DX":3,"DY":-2}`)})
	s.processMessage(&SerialDevice{Path: "dev1"}, &USBMessage{Type: "click"})
	s.processMessage(&SerialDevice{Path: "dev1"}, &USBMessage{Type: "rightclick"})
	s.processMessage(&SerialDevice{Path: "dev1"}, &USBMessage{Type: "doubleclick"})
	s.processMessage(&SerialDevice{Path: "dev1"}, &USBMessage{Type: "scroll", Payload: []byte(`{"Delta":4}`)})

	if len(mouse.moveCalls) != 1 {
		t.Fatalf("moveCalls = %d, want 1", len(mouse.moveCalls))
	}
	if len(mouse.clickCalls) != 2 || mouse.clickCalls[0] != "left" || mouse.clickCalls[1] != "right" {
		t.Fatalf("clickCalls = %v, want [left right]", mouse.clickCalls)
	}
	if mouse.doubleCalls != 1 {
		t.Fatalf("doubleCalls = %d, want 1", mouse.doubleCalls)
	}
	if len(mouse.scrollCalls) != 1 || mouse.scrollCalls[0] != 4 {
		t.Fatalf("scrollCalls = %v, want [4]", mouse.scrollCalls)
	}
}

func TestGetStatsAndStop(t *testing.T) {
	s := &Server{
		devices:   map[string]*SerialDevice{},
		mouse:     &fakeMouse{},
		deviceMgr: device.NewManager(),
	}
	if got := s.GetStats()["devices"]; got != 0 {
		t.Fatalf("devices = %v, want 0", got)
	}
	s.devices["dev1"] = &SerialDevice{File: mustTempFile(t), Path: "dev1"}
	if got := s.GetStats()["devices"]; got != 1 {
		t.Fatalf("devices = %v, want 1", got)
	}
	s.Stop()
	if got := s.GetStats()["devices"]; got != 0 {
		t.Fatalf("devices = %v, want 0 after stop", got)
	}
}

func mustTempFile(t *testing.T) *os.File {
	t.Helper()
	f, err := os.CreateTemp("", "airmouse-usb-test-*")
	if err != nil {
		t.Fatalf("CreateTemp failed: %v", err)
	}
	t.Cleanup(func() {
		_ = os.Remove(f.Name())
	})
	return f
}

var errUnused = errors.New("unused")
