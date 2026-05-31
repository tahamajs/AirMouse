package config

import (
	"encoding/json"
	"os"
	"path/filepath"
	"sync"
)

type RepositoryConfig struct {
	DataDir string `json:"data_dir"`
}

var instance *RepositoryConfig
var once sync.Once

func Get() *RepositoryConfig {
	once.Do(func() {
		instance = loadOrDefault()
	})
	return instance
}

func loadOrDefault() *RepositoryConfig {
	cfg := &RepositoryConfig{
		DataDir: getDefaultDataDir(),
	}
	path := getConfigPath()
	if data, err := os.ReadFile(path); err == nil {
		_ = json.Unmarshal(data, cfg)
	}
	return cfg
}

func getDefaultDataDir() string {
	configDir, _ := os.UserConfigDir()
	return filepath.Join(configDir, "airmouse", "data")
}

func getConfigPath() string {
	configDir, _ := os.UserConfigDir()
	return filepath.Join(configDir, "airmouse", "repo_config.json")
}

func (c *RepositoryConfig) Save() error {
	data, err := json.MarshalIndent(c, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(getConfigPath(), data, 0644)
}