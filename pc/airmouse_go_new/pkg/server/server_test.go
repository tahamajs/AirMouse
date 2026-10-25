package server

import (
	"sync/atomic"
	"testing"
	"time"
)

func TestServerOptionsAndInitialization(t *testing.T) {
	srv, err := New(
		WithTCPPort(19080),
		WithWebSocketPort(19081),
		WithUDPPort(19082),
		WithSensitivity(1.5),
		WithVirtualMouse(),
		WithLogLevel("debug"),
		WithProtocols(true, false, false),
	)
	if err != nil {
		t.Fatalf("Failed to initialize server: %v", err)
	}

	cfg := srv.Config()
	if cfg.Port != 19080 || cfg.WebSocketPort != 19081 || cfg.UDPPort != 19082 {
		t.Errorf("Port mismatch in config: %+v", cfg)
	}
	if cfg.Sensitivity != 1.5 {
		t.Errorf("Sensitivity mismatch: %f", cfg.Sensitivity)
	}
	if !cfg.VirtualMouse {
		t.Errorf("VirtualMouse should be true")
	}
}

func TestVirtualMouseAndMotionHooks(t *testing.T) {
	srv, err := New(
		WithVirtualMouse(),
		WithSensitivity(1.0),
		WithProtocols(false, false, false), // no network listeners needed for unit test
	)
	if err != nil {
		t.Fatalf("Failed to create server: %v", err)
	}

	var moveCount int64
	var lastDX, lastDY float64
	srv.OnMove(func(dx, dy float64) {
		atomic.AddInt64(&moveCount, 1)
		lastDX = dx
		lastDY = dy
	})

	var clickCount int64
	var lastButton string
	srv.OnClick(func(button string) {
		atomic.AddInt64(&clickCount, 1)
		lastButton = button
	})

	var scrollCount int64
	srv.OnScroll(func(delta float64) {
		atomic.AddInt64(&scrollCount, 1)
	})

	// Invoke via hook mouse controller
	srv.hookMouse.Move(15.0, -10.0)
	srv.hookMouse.Click("left")
	srv.hookMouse.Click("right")
	srv.hookMouse.Scroll(3)

	if atomic.LoadInt64(&moveCount) != 1 {
		t.Errorf("Expected 1 move event, got %d", moveCount)
	}
	if lastDX != 15.0 || lastDY != -10.0 {
		t.Errorf("Move coordinates mismatch: dx=%.1f, dy=%.1f", lastDX, lastDY)
	}

	if atomic.LoadInt64(&clickCount) != 2 {
		t.Errorf("Expected 2 click events, got %d", clickCount)
	}
	if lastButton != "right" {
		t.Errorf("Expected last button right, got %s", lastButton)
	}

	if atomic.LoadInt64(&scrollCount) != 1 {
		t.Errorf("Expected 1 scroll event, got %d", scrollCount)
	}

	clicks, _, right, scroll := srv.hookMouse.Stats()
	if clicks != 1 || right != 1 || scroll != 3 {
		t.Errorf("Stats mismatch: clicks=%d, right=%d, scroll=%d", clicks, right, scroll)
	}

	x, y := srv.hookMouse.GetPosition()
	if x != 15.0 || y != -10.0 {
		t.Errorf("Position mismatch: x=%.1f, y=%.1f", x, y)
	}
}

func TestServerLifecycle(t *testing.T) {
	srv, err := New(
		WithTCPPort(18080),
		WithWebSocketPort(18081),
		WithUDPPort(18082),
		WithVirtualMouse(),
		WithProtocols(true, false, false),
	)
	if err != nil {
		t.Fatalf("Failed to initialize server: %v", err)
	}

	var started bool
	srv.OnServerStarted(func() {
		started = true
	})

	var stopped bool
	srv.OnServerStopped(func() {
		stopped = true
	})

	if srv.IsRunning() {
		t.Errorf("Server should not be running before Start")
	}

	if err := srv.Start(); err != nil {
		t.Fatalf("Failed to start server: %v", err)
	}

	if !srv.IsRunning() {
		t.Errorf("Server should report running after Start")
	}

	stats := srv.GetStatistics()
	if stats == nil {
		t.Errorf("Statistics should not be nil")
	}

	uptime := srv.GetUptime()
	if uptime < 0 {
		t.Errorf("Uptime should be non-negative")
	}

	time.Sleep(50 * time.Millisecond)

	if err := srv.Stop(); err != nil {
		t.Errorf("Stop failed: %v", err)
	}

	if srv.IsRunning() {
		t.Errorf("Server should not be running after Stop")
	}
	_ = started
	_ = stopped
}
