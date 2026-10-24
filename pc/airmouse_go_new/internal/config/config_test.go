package config

import (
	"testing"
)

func TestDefaultSecuritySettings(t *testing.T) {
	cfg := loadOrDefault()

	if !cfg.AuthEnabled {
		t.Errorf("Expected AuthEnabled to be true by default, got false")
	}

	if cfg.AuthSecret == "" {
		t.Errorf("Expected AuthSecret to have a default non-empty value")
	}

	if !cfg.TLSEnabled {
		t.Errorf("Expected TLSEnabled to be true by default, got false")
	}

	if cfg.TLSCertPath == "" || cfg.TLSKeyPath == "" {
		t.Errorf("Expected non-empty TLSCertPath and TLSKeyPath defaults, got cert=%q key=%q", cfg.TLSCertPath, cfg.TLSKeyPath)
	}
}
