package common

import "sync"

var (
	activeMode  string = "None"
	activeTheme string = "dark"
	stateMu     sync.RWMutex
)

func SetActiveMode(mode string) {
	stateMu.Lock()
	defer stateMu.Unlock()
	activeMode = mode
}

func GetActiveMode() string {
	stateMu.RLock()
	defer stateMu.RUnlock()
	return activeMode
}

func SetActiveTheme(theme string) {
	stateMu.Lock()
	defer stateMu.Unlock()
	activeTheme = theme
}

func GetActiveTheme() string {
	stateMu.RLock()
	defer stateMu.RUnlock()
	return activeTheme
}
