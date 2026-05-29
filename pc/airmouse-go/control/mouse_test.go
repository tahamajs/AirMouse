package control_test

import (
	"testing"

	"airmouse-go/control"
)

type mockMouse struct {
	clicks int64
}

func (m *mockMouse) Move(dx, dy float64)      {}
func (m *mockMouse) Click(button string)        { if button == "left" { m.clicks++ } }
func (m *mockMouse) DoubleClick()               {}
func (m *mockMouse) Scroll(delta int)           {}
func (m *mockMouse) Stats() (int64, int64, int64, int64) {
	return m.clicks, 0, 0, 0
}
func (m *mockMouse) SetSensitivity(s float64)   {}

func TestMouseClick(t *testing.T) {
	m := &mockMouse{}
	m.Click("left")
	c, _, _, _ := m.Stats()
	if c != 1 {
		t.Errorf("expected 1 click, got %d", c)
	}
}