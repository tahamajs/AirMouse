package http

import (
	"net/http"
	"time"

	"airmouse-go/internal/utils"
)

// Middleware defines the signature for HTTP middleware.
type Middleware func(http.Handler) http.Handler

// ChainMiddleware applies middlewares in the given order (first applied, last executed).
func ChainMiddleware(h http.Handler, middlewares ...Middleware) http.Handler {
	for i := len(middlewares) - 1; i >= 0; i-- {
		h = middlewares[i](h)
	}
	return h
}

// LoggingMiddleware logs every request with method, path, and duration.
func LoggingMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		next.ServeHTTP(w, r)
		utils.LogInfo("HTTP %s %s took %s", r.Method, r.URL.Path, time.Since(start))
	})
}

// RecoverMiddleware catches panics and returns a 500 error without crashing the server.
func RecoverMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if err := recover(); err != nil {
				utils.LogError("HTTP panic: %v", err)
				w.WriteHeader(http.StatusInternalServerError)
				w.Write([]byte(`{"error":"internal server error"}`))
			}
		}()
		next.ServeHTTP(w, r)
	})
}

// CORSMiddleware adds CORS headers to allow cross‑origin requests (useful for development).
func CORSMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")

		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}

		next.ServeHTTP(w, r)
	})
}

// RateLimitMiddleware limits requests to a given number per second (per client, using a token bucket).
// Note: This simple implementation uses a global ticker; for per‑client limiting, use a more advanced approach.
func RateLimitMiddleware(requestsPerSecond int) Middleware {
	if requestsPerSecond <= 0 {
		requestsPerSecond = 1
	}
	ticker := time.NewTicker(time.Second / time.Duration(requestsPerSecond))
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			<-ticker.C
			next.ServeHTTP(w, r)
		})
	}
}

// BasicAuthMiddleware adds simple HTTP Basic authentication (optional).
func BasicAuthMiddleware(username, password string) Middleware {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			user, pass, ok := r.BasicAuth()
			if !ok || user != username || pass != password {
				w.Header().Set("WWW-Authenticate", `Basic realm="Restricted"`)
				w.WriteHeader(http.StatusUnauthorized)
				w.Write([]byte(`{"error":"unauthorized"}`))
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
