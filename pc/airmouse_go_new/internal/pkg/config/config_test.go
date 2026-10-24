package config

import (
	"testing"
)

func TestSharedConfigGet(t *testing.T) {
	cfg := Get()
	if cfg == nil {
		t.Fatal("Expected non-nil shared config")
	}
	if cfg.Server.Port <= 0 {
		t.Errorf("Expected valid port, got %d", cfg.Server.Port)
	}
}
