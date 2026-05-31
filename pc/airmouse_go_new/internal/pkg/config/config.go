package config

import (
	"encoding/json"
	"os"
	"path/filepath"
	"sync"
)

type SharedConfig struct {
	Server struct {
		Host string `json:"host"`
		Port int    `json:"port"`
	} `json:"server"`
	Features struct {
		EnableAI      bool `json:"enable_ai"`
		EnableBluetooth bool `json:"enable_bluetooth"`
	} `json:"features"`
}

var instance *SharedConfig
var once sync.Once

func Get() *SharedConfig {
	once.Do(func() {
		instance = loadOrDefault()
	})
	return instance
}

func loadOrDefault() *SharedConfig {
	cfg := &SharedConfig{}
	cfg.Server.Host = "0.0.0.0"
	cfg.Server.Port = 8080
	cfg.Features.EnableAI = false
	cfg.Features.EnableBluetooth = true

	configDir, _ := os.UserConfigDir()
	path := filepath.Join(configDir, "airmouse", "shared_config.json")
	if data, err := os.ReadFile(path); err == nil {
		_ = json.Unmarshal(data, cfg)
	}
	return cfg
}

func (c *SharedConfig) Save() error {
	configDir, _ := os.UserConfigDir()
	path := filepath.Join(configDir, "airmouse", "shared_config.json")
	data, err := json.MarshalIndent(c, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(path, data, 0644)
}