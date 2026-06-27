package http

import (
    "encoding/json"
    "fmt"
    "net/http"
    "runtime"
    "time"

    "airmouse-go/internal/utils"
)

type webSocketStats interface {
    GetStats() map[string]interface{}
    GetConnectedClients() []interface{}
}

// Router wraps the HTTP serve mux with additional configuration.
type Router struct {
    mux     *http.ServeMux
    hub     webSocketStats
    middlewares []Middleware
}

// NewRouter creates a new HTTP router with the given WebSocket hub.
func NewRouter(hub webSocketStats) *Router {
    r := &Router{
        mux:     http.NewServeMux(),
        hub:     hub,
        middlewares: []Middleware{
            CORSMiddleware,
            RecoverMiddleware,
            LoggingMiddleware,
        },
    }
    r.setupRoutes()
    return r
}

// setupRoutes registers all HTTP endpoints.
func (r *Router) setupRoutes() {
    // WebSocket endpoint
    r.mux.HandleFunc("/ws", func(w http.ResponseWriter, req *http.Request) {
        http.Error(w, "websocket handler not configured here", http.StatusNotImplemented)
    })

    // Health check
    r.mux.HandleFunc("/health", healthHandler)

    // Prometheus-style metrics
    r.mux.HandleFunc("/metrics", metricsHandler)

    // API endpoints
    r.mux.HandleFunc("/api/status", r.statusHandler())
    r.mux.HandleFunc("/api/clients", r.clientsHandler())
    r.mux.HandleFunc("/api/stats", r.statsHandler())

    // Static files (optional)
    r.mux.Handle("/static/", http.StripPrefix("/static/", http.FileServer(http.Dir("web/static"))))
}

// Handler returns the fully wrapped http.Handler with all middlewares applied.
func (r *Router) Handler() http.Handler {
    var handler http.Handler = r.mux
    for i := len(r.middlewares) - 1; i >= 0; i-- {
        handler = r.middlewares[i](handler)
    }
    return handler
}

// Start starts the HTTP server on the given address.
func (r *Router) Start(addr string) error {
    utils.LogInfo("Starting HTTP server on %s", addr)
    server := &http.Server{
        Addr:         addr,
        Handler:      r.Handler(),
        ReadTimeout:  10 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  60 * time.Second,
    }
    return server.ListenAndServe()
}

// --- Handlers ---

func healthHandler(w http.ResponseWriter, r *http.Request) {
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusOK)
    json.NewEncoder(w).Encode(map[string]interface{}{
        "status":    "ok",
        "timestamp": time.Now().Unix(),
    })
}

func metricsHandler(w http.ResponseWriter, r *http.Request) {
    var memStats runtime.MemStats
    runtime.ReadMemStats(&memStats)

    w.Header().Set("Content-Type", "text/plain")
    w.WriteHeader(http.StatusOK)

    // In production, you would use a proper Prometheus registry.
    // For now, we output simple gauge metrics.
    w.Write([]byte("# HELP airmouse_clients Connected clients\n"))
    w.Write([]byte("# TYPE airmouse_clients gauge\n"))
    w.Write([]byte("airmouse_clients 0\n\n"))

    w.Write([]byte("# HELP airmouse_goroutines Number of goroutines\n"))
    w.Write([]byte("# TYPE airmouse_goroutines gauge\n"))
    w.Write([]byte(fmt.Sprintf("airmouse_goroutines %d\n\n", runtime.NumGoroutine())))

    w.Write([]byte("# HELP airmouse_memory_alloc_bytes Memory allocated (bytes)\n"))
    w.Write([]byte("# TYPE airmouse_memory_alloc_bytes gauge\n"))
    w.Write([]byte(fmt.Sprintf("airmouse_memory_alloc_bytes %d\n", memStats.Alloc)))
}

func (r *Router) statusHandler() http.HandlerFunc {
    return func(w http.ResponseWriter, req *http.Request) {
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode(map[string]interface{}{
            "status": "running",
            "stats":  r.hub.GetStats(),
        })
    }
}

func (r *Router) clientsHandler() http.HandlerFunc {
    return func(w http.ResponseWriter, req *http.Request) {
        clients := r.hub.GetConnectedClients()
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode(map[string]interface{}{
            "clients": clients,
            "count":   len(clients),
        })
    }
}

func (r *Router) statsHandler() http.HandlerFunc {
    return func(w http.ResponseWriter, req *http.Request) {
        stats := r.hub.GetStats()
        w.Header().Set("Content-Type", "application/json")
        json.NewEncoder(w).Encode(stats)
    }
}
