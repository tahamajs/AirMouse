package http

import (
	"net/http"

	"airmouse-go/internal/handler/websocket"
)

type Router struct {
	mux *http.ServeMux
}

func NewRouter(wsHandler *websocket.Handler) *Router {
	r := &Router{mux: http.NewServeMux()}
	r.mux.Handle("/ws", wsHandler)
	r.mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("OK"))
	})
	return r
}

func (r *Router) Handler() http.Handler {
	return r.mux
}