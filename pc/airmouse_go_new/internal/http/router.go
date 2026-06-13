package http

import (
    "encoding/json"
    "net/http"
    "runtime"
    "time"

    "airmouse-go/internal/handler/websocket"
    "airmouse-go/internal/infra/logger"
)

func NewRouter(hub *websocket.Hub) *http.ServeMux {
    mux := http.NewServeMux()
    
    // WebSocket endpoint
    mux.HandleFunc("/ws", websocket.WebSocketHandler(hub))
    
    // Health check
    mux.HandleFunc("/health", healthHandler)
    
    // Metrics
    mux.HandleFunc("/metrics", metricsHandler)
    
    // Status
    mux.HandleFunc("/api/status", statusHandler(hub))
    
    // API endpoints
    mux.HandleFunc("/api/clients", clientsHandler(hub))
    mux.HandleFunc("/api/stats", statsHandler(hub))
    
    // Static files
    mux.Handle("/static/", http.StripPrefix("/static/", http.FileServer(http.Dir("web/static"))))
    
    return mux
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusOK)
    json.NewEncoder(w).Encode(map[string]interface{}{
        "status": "ok",
        "timestamp": time.Now().Unix(),
    })
}

func metricsHandler(w http.ResponseWriter, r *http.Request) {
    var memStats runtime.MemStats
    runtime.ReadMemStats(&memStats)
    
    w.Header().Set("Content-Type", "text/plain")
    w.WriteHeader(http.StatusOK)
    
    w.Write([]byte("# HELP airmouse_clients Connected clients\n"))
    w.Write([]byte("# TYPE airmouse_clients gauge\n"))
    w.Write([]byte("airmouse_clients 0\n\n"))
    
    w.Write([]byte("# HELP airmouse_goroutines Number of goroutines\n"))
    w.Write([]byte("# TYPE airmouse_goroutines gauge\n"))
    w.Write([]byte("airmouse_goroutines " + string(runtime.NumGoroutine()) + "\n\n"))
    
    w.Write([]byte("# HELP airmouse_memory_alloc_bytes Memory allocated\n"))
    w.Write([]byte("# TYPE airmouse_memory_alloc_bytes gauge\n"))
    w.Write([]byte("airmouse_memory_alloc_bytes " + string(memStats.Alloc) + "\n"))
}

func statusHandler(hub *websocket.Hub) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        stats := hub.GetStats()
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode(map[string]interface{}{
            "status": "running",
            "stats": stats,
            "uptime": time.Since(stats.StartTime).Seconds(),
        })
    }
}

func clientsHandler(hub *websocket.Hub) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        clients := hub.GetConnectedClients()
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode(map[string]interface{}{
            "clients": clients,
            "count": len(clients),
        })
    }
}

func statsHandler(hub *websocket.Hub) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        stats := hub.GetStats()
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode(stats)
    }
}