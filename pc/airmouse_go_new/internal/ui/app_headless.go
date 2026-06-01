//go:build !gui

package ui

import (
	"airmouse-go/internal/config"
	"airmouse-go/internal/control"
	"airmouse-go/internal/device"
	"airmouse-go/internal/protocol"
)

// App is a lightweight headless stub used when the GUI build tag is not enabled.
type App struct{}

func NewApp(cfg *config.Config, server *protocol.ProtocolServer, mouse control.MouseController, deviceMgr *device.Manager) *App {
	return &App{}
}

func (a *App) Run() error {
	return nil
}

func (a *App) Stop() {}
