package websocket

import (
    "sync/atomic"
    "time"

    "github.com/gorilla/websocket"

    "airmouse-go/internal/domain/entity"
    "airmouse-go/internal/infra/logger"
)

type Client struct {
    id         string
    conn       *websocket.Conn
    send       chan []byte
    hub        *Hub
    lastPing   int64
    lastActive int64
    entity     *entity.Client
}

func NewClient(id string, conn *websocket.Conn, hub *Hub) *Client {
    now := time.Now()
    return &Client{
        id:         id,
        conn:       conn,
        send:       make(chan []byte, 256),
        hub:        hub,
        lastPing:   now.Unix(),
        lastActive: now.Unix(),
        entity: &entity.Client{
            ID:          id,
            Name:        "unknown",
            ConnectedAt: now,
            LastActive:  now,
            Transport:   "websocket",
            RemoteAddr:  conn.RemoteAddr().String(),
        },
    }
}

func (c *Client) readPump() {
    defer func() {
        c.hub.unregister <- c
        c.conn.Close()
    }()

    c.conn.SetReadLimit(4096)
    c.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
    c.conn.SetPongHandler(func(string) error {
        atomic.StoreInt64(&c.lastPing, time.Now().Unix())
        c.updateLastActive()
        c.conn.SetReadDeadline(time.Now().Add(60 * time.Second))
        if err := c.hub.connService.Heartbeat(c.id, 0); err != nil {
            logger.Error("Heartbeat failed: %v", err)
        }
        return nil
    })

    for {
        _, message, err := c.conn.ReadMessage()
        if err != nil {
            if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
                logger.Error("WebSocket read error: %v", err)
            }
            break
        }
        c.updateLastActive()
        c.processMessage(message)
    }
}

func (c *Client) writePump() {
    ticker := time.NewTicker(30 * time.Second)
    defer func() {
        ticker.Stop()
        c.conn.Close()
    }()

    for {
        select {
        case message, ok := <-c.send:
            c.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
            if !ok {
                c.conn.WriteMessage(websocket.CloseMessage, []byte{})
                return
            }

            if err := c.conn.WriteMessage(websocket.TextMessage, message); err != nil {
                logger.Error("WebSocket write error: %v", err)
                return
            }

        case <-ticker.C:
            c.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
            if err := c.conn.WriteMessage(websocket.PingMessage, nil); err != nil {
                logger.Error("WebSocket ping error: %v", err)
                return
            }
        }
    }
}

func (c *Client) processMessage(data []byte) {
    // Delegate to hub's handler
    // This is handled by the handler's processMessage method
}

func (c *Client) updateLastActive() {
    atomic.StoreInt64(&c.lastActive, time.Now().Unix())
    c.entity.LastActive = time.Now()
}

func (c *Client) GetLastActive() time.Time {
    return time.Unix(atomic.LoadInt64(&c.lastActive), 0)
}

func (c *Client) SendMessage(message []byte) {
    select {
    case c.send <- message:
    default:
        logger.Debug("Client send buffer full, dropping message: %s", c.id)
    }
}