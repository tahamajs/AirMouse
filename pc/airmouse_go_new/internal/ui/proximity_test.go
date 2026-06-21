package ui

import "testing"

func TestProximityModeLabel(t *testing.T) {
	if got := proximityModeLabel(true); got != "monitoring" {
		t.Fatalf("expected monitoring, got %q", got)
	}
	if got := proximityModeLabel(false); got != "idle" {
		t.Fatalf("expected idle, got %q", got)
	}
}
