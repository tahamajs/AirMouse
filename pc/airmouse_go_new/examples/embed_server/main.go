// Example embed_server demonstrates how to embed the AirMouse server into an external Go application
// with custom event callbacks, virtual or physical cursor control, and graceful shutdown.
package main

import (
	"fmt"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"airmouse-go/pkg/server"
)

func main() {
	fmt.Println("==================================================")
	fmt.Println("     AirMouse Embedded Server Example             ")
	fmt.Println("==================================================")

	// 1. Initialize embedded AirMouse server with custom configuration
	srv, err := server.New(
		server.WithTCPPort(8080),
		server.WithWebSocketPort(8081),
		server.WithUDPPort(8082),
		server.WithSensitivity(1.2),
		server.WithLogLevel("info"),
		// Use VirtualMouse to intercept movements in callbacks without moving the desktop cursor.
		// Omit server.WithVirtualMouse() if you want actual OS cursor movement.
		server.WithVirtualMouse(),
	)
	if err != nil {
		log.Fatalf("Failed to initialize AirMouse server: %v", err)
	}

	// 2. Register application lifecycle and motion callbacks
	srv.OnServerStarted(func() {
		fmt.Println("[EVENT] AirMouse server listener active.")
	})

	srv.OnServerStopped(func() {
		fmt.Println("[EVENT] AirMouse server listener stopped.")
	})

	srv.OnClientConnected(func(client server.ClientInfo) {
		fmt.Printf("[CLIENT CONNECTED] ID=%s Name=%q IP=%s Protocol=%s Model=%s\n",
			client.ID, client.Name, client.IPAddress, client.Type, client.DeviceModel)
	})

	srv.OnClientDisconnected(func(clientID string) {
		fmt.Printf("[CLIENT DISCONNECTED] ID=%s\n", clientID)
	})

	srv.OnMove(func(dx, dy float64) {
		fmt.Printf("[MOTION] dx=%.2f, dy=%.2f\n", dx, dy)
	})

	srv.OnClick(func(button string) {
		fmt.Printf("[CLICK] button=%s\n", button)
	})

	srv.OnScroll(func(delta float64) {
		fmt.Printf("[SCROLL] delta=%.2f\n", delta)
	})

	// 3. Start the server
	fmt.Println("Starting AirMouse protocol server...")
	if err := srv.Start(); err != nil {
		log.Fatalf("Server startup failed: %v", err)
	}

	cfg := srv.Config()
	fmt.Printf("AirMouse Server running!\n")
	fmt.Printf("  TCP:       :%d\n", cfg.Port)
	fmt.Printf("  WebSocket: :%d\n", cfg.WebSocketPort)
	fmt.Printf("  UDP:       :%d\n", cfg.UDPPort)
	fmt.Println("Press Ctrl+C to terminate gracefully.")

	// Background ticker to print periodic runtime stats
	ticker := time.NewTicker(10 * time.Second)
	defer ticker.Stop()
	go func() {
		for range ticker.C {
			stats := srv.GetStatistics()
			fmt.Printf("[STATS] Uptime: %v | Active Devices: %d\n",
				srv.GetUptime().Round(time.Second),
				len(srv.GetConnectedDevices()),
			)
			_ = stats
		}
	}()

	// 4. Wait for OS interrupt signal (Ctrl+C / SIGTERM)
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, os.Interrupt, syscall.SIGTERM)
	<-sigChan

	fmt.Println("\nInitiating graceful shutdown...")
	if err := srv.Stop(); err != nil {
		log.Printf("Shutdown error: %v", err)
	}
	fmt.Println("AirMouse server stopped cleanly.")
}
