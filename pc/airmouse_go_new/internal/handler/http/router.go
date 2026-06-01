package http

import (
	"net/http"

	"airmouse-go/internal/handler/websocket"
)

func NewRouter(hub *websocket.Hub) *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("/ws", websocket.WebSocketHandler(hub))
	mux.HandleFunc("/health", healthHandler)
	mux.HandleFunc("/metrics", metricsHandler)
	return mux
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{"status":"ok"}`))
}

func metricsHandler(w http.ResponseWriter, r *http.Request) {
	// In production, expose Prometheus metrics
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`# HELP airmouse_clients Connected clients\n# TYPE airmouse_clients gauge\nairmouse_clients 0\n`))
}
