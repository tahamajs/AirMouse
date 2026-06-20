package ui

import (
	"strings"
	"testing"
	"time"

	"airmouse-go/internal/device"
)

func TestEmptyOrDash(t *testing.T) {
	if got := emptyOrDash(""); got != "-" {
		t.Fatalf("expected dash for empty value, got %q", got)
	}
	if got := emptyOrDash("   "); got != "-" {
		t.Fatalf("expected dash for whitespace value, got %q", got)
	}
	if got := emptyOrDash("192.168.1.10"); got != "192.168.1.10" {
		t.Fatalf("expected original value, got %q", got)
	}
}

func TestFormatRecentLogEntry(t *testing.T) {
	ts := time.Date(2026, 6, 20, 19, 44, 59, 0, time.UTC)
	got := formatRecentLogEntry(ts, "info", " server started ")
	if !strings.Contains(got, "19:44:59 [INFO] server started") {
		t.Fatalf("unexpected formatted log entry: %q", got)
	}
}

func TestTruncateRecentLogsKeepsNewestEntries(t *testing.T) {
	input := []string{"1", "2", "3", "4", "5"}
	got := truncateRecentLogs(input, 3)
	want := []string{"3", "4", "5"}

	if len(got) != len(want) {
		t.Fatalf("expected %d entries, got %d", len(want), len(got))
	}
	for i := range want {
		if got[i] != want[i] {
			t.Fatalf("expected %v, got %v", want, got)
		}
	}
}

func TestDeviceNamesForLog(t *testing.T) {
	devices := []*device.DeviceInfo{
		{Name: "Phone", Type: device.TypeWebSocket},
		{Name: "Tablet", Type: device.TypeBluetooth},
	}

	got := deviceNamesForLog(devices)
	if len(got) != 2 {
		t.Fatalf("expected 2 entries, got %d", len(got))
	}
	if got[0] != "Phone (WebSocket)" || got[1] != "Tablet (Bluetooth)" {
		t.Fatalf("unexpected device log names: %v", got)
	}
}
