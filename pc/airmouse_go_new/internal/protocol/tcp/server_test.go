package tcp

import (
	"bytes"
	"io"
	"net"
	"strings"
	"testing"
	"time"

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
func (f *fakeMouse) Stats() (int64, int64, int64, int64) {
	return 0, 0, 0, 0
}
func (f *fakeMouse) SetSensitivity(float64)          {}
func (f *fakeMouse) GetSensitivity() float64         { return 1 }
func (f *fakeMouse) SetSmoothing(bool)               {}
func (f *fakeMouse) SetAcceleration(bool, float64)   {}
func (f *fakeMouse) EnablePredictive(bool)           {}
func (f *fakeMouse) SetPredictiveBlendFactor(float64) {}
func (f *fakeMouse) EnableAISmoothing(bool)          {}
func (f *fakeMouse) SetAISmoother(*control.AISmoother) {}
func (f *fakeMouse) EnableMLPrediction(bool)         {}
func (f *fakeMouse) SetMLBlendFactor(float64)        {}

type fakeAddr string

func (a fakeAddr) Network() string { return "tcp" }
func (a fakeAddr) String() string  { return string(a) }

type fakeConn struct {
	remoteAddr net.Addr
	writes     bytes.Buffer
	readData   []string
	readIndex  int
	closed     bool
}

func newFakeConn(remote string, lines ...string) *fakeConn {
	return &fakeConn{
		remoteAddr: fakeAddr(remote),
		readData:   lines,
	}
}

func (c *fakeConn) Read(b []byte) (int, error) {
	if c.readIndex >= len(c.readData) {
		return 0, io.EOF
	}
	n := copy(b, c.readData[c.readIndex])
	c.readIndex++
	return n, nil
}

func (c *fakeConn) Write(b []byte) (int, error) { return c.writes.Write(b) }
func (c *fakeConn) Close() error                { c.closed = true; return nil }
func (c *fakeConn) LocalAddr() net.Addr         { return fakeAddr("local") }
func (c *fakeConn) RemoteAddr() net.Addr        { return c.remoteAddr }
func (c *fakeConn) SetDeadline(time.Time) error  { return nil }
func (c *fakeConn) SetReadDeadline(time.Time) error {
	return nil
}
func (c *fakeConn) SetWriteDeadline(time.Time) error { return nil }

func TestProcessLineMoveAndAck(t *testing.T) {
	mouse := &fakeMouse{}
	server := &Server{
		mouse:     mouse,
		deviceMgr: device.NewManager(),
		clients:   map[string]*Client{},
	}
	conn := newFakeConn("client-1")
	client := &Client{ID: "client-1", Conn: conn}

	server.processLine(client, []byte(`{"type":"move","dx":4.5,"dy":-2.25}`))
	if len(mouse.moveCalls) != 1 {
		t.Fatalf("moveCalls = %d, want 1", len(mouse.moveCalls))
	}
	if got := mouse.moveCalls[0]; got.dx != 4.5 || got.dy != -2.25 {
		t.Fatalf("moveCalls[0] = %+v, want dx=4.5 dy=-2.25", got)
	}
	if conn.writes.Len() != 0 {
		t.Fatalf("expected no ack for move, got %q", conn.writes.String())
	}

	server.processLine(client, []byte(`{"type":"click","button":"left","id":11}`))
	server.processLine(client, []byte(`{"type":"doubleclick","id":12}`))
	server.processLine(client, []byte(`{"type":"rightclick","id":13}`))
	if len(mouse.clickCalls) != 2 || mouse.clickCalls[0] != "left" || mouse.clickCalls[1] != "right" {
		t.Fatalf("clickCalls = %v, want [left right]", mouse.clickCalls)
	}
	if mouse.doubleCalls != 1 {
		t.Fatalf("doubleCalls = %d, want 1", mouse.doubleCalls)
	}

	server.processLine(client, []byte(`{"type":"scroll","delta":-1,"id":12}`))
	if len(mouse.scrollCalls) != 1 || mouse.scrollCalls[0] != -1 {
		t.Fatalf("scrollCalls = %v, want [-1]", mouse.scrollCalls)
	}
	if got := conn.writes.String(); !strings.Contains(got, `"type":"ack"`) || !strings.Contains(got, `"id":"12"`) {
		t.Fatalf("ack write = %q, want ack with id 12", got)
	}
}

func TestProcessLineHelloAndControl(t *testing.T) {
	mouse := &fakeMouse{}
	server := &Server{
		mouse:     mouse,
		deviceMgr: device.NewManager(),
		clients:   map[string]*Client{},
	}
	conn := newFakeConn("client-2")
	client := &Client{ID: "client-2", Conn: conn}

	server.deviceMgr.RegisterDevice(client.ID, device.TypeTCP, "Unknown")
	server.processLine(client, []byte(`{"type":"hello","name":"Pixel","version":"3.0"}`))
	if client.Name != "Pixel" {
		t.Fatalf("client.Name = %q, want Pixel", client.Name)
	}

	server.processLine(client, []byte(`{"type":"ping"}`))
	if got := conn.writes.String(); !strings.Contains(got, `"type":"pong"`) {
		t.Fatalf("expected pong response, got %q", got)
	}

	server.processLine(client, []byte(`{"type":"control","command":"pause_movement"}`))
	if !control.IsMovementPaused() {
		t.Fatalf("movement should be paused after control message")
	}
	server.processLine(client, []byte(`{"type":"control","command":"resume_movement"}`))
	if control.IsMovementPaused() {
		t.Fatalf("movement should be resumed after control message")
	}
}
