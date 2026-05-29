package main

import (
	"airmouse-go/config"
	"airmouse-go/control"
	"airmouse-go/server"
	"airmouse-go/ui"
	"fmt"
	"fyne.io/fyne/v2"
	"fyne.io/fyne/v2/app"
	"os"
)

func main() {
	cfg, err := config.Load("config.json")
	if err != nil {
		fmt.Fprintf(os.Stderr, "config error: %v\n", err)
		os.Exit(1)
	}

	mouseCtrl := control.NewMouseController(cfg.Sensitivity)
	a := app.NewWithID("com.airmouse.server")
	w := a.NewWindow("Air Mouse Pro Server")

	// Build UI with all backend components
	ui.Setup(w, a, cfg, mouseCtrl)

	// Start background services
	go server.StartUDPDiscovery(cfg.DiscoveryPort, func() string { return cfg.SelectedIP }, ui.Log)
	go server.StartMDNS(cfg.MDNSName, cfg.SelectedIP, cfg.Port, ui.Log)

	w.ShowAndRun()
}