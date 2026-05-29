package config_test

import (
	"os"
	"testing"

	"airmouse-go/config"
)

func TestLoadDefaults(t *testing.T) {
	cfg, err := config.Load("nonexistent.json")
	if err != nil {
		t.Fatal(err)
	}
	if cfg.Port != 8080 {
		t.Errorf("expected default port 8080, got %d", cfg.Port)
	}
}

func TestSaveAndLoad(t *testing.T) {
	path := "test_config.json"
	defer os.Remove(path)
	cfg := &config.Config{Port: 9999, Sensitivity: 0.8}
	if err := config.Save(path, cfg); err != nil {
		t.Fatal(err)
	}
	loaded, err := config.Load(path)
	if err != nil {
		t.Fatal(err)
	}
	if loaded.Port != 9999 || loaded.Sensitivity != 0.8 {
		t.Errorf("loaded config mismatch: %+v", loaded)
	}
}