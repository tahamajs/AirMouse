package config

import (
	"encoding/json"
	"os"
)

type Config struct {
	Host            string  `json:"host"`
	Port            int     `json:"port"`
	DiscoveryPort   int     `json:"discovery_port"`
	Sensitivity     float64 `json:"sensitivity"`
	AccentColor     string  `json:"accent_color"`
	SelectedIP      string  `json:"selected_ip"`
	ManualIPEnabled bool    `json:"manual_ip_enabled"`
	ManualIPValue   string  `json:"manual_ip_value"`
	MDNSName        string  `json:"mDNS_name"`
	AlwaysOnTop     bool    `json:"always_on_top"`
}

func Load(path string) (*Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return &Config{
			Host:          "0.0.0.0",
			Port:          8080,
			DiscoveryPort: 8081,
			Sensitivity:   0.5,
			AccentColor:   "#007acc",
			MDNSName:      "airmouse",
		}, nil
	}
	var cfg Config
	if err := json.Unmarshal(data, &cfg); err != nil {
		return nil, err
	}
	return &cfg, nil
}

func Save(path string, cfg *Config) error {
	data, err := json.MarshalIndent(cfg, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(path, data, 0644)
}