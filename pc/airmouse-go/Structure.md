airmouse-go/
├── cmd/
│   └── airmouse-server/            # Main entry point
│       └── main.go
├── internal/                       # Private application code
│   ├── auth/                       # Authentication & pairing logic
│   │   ├── manager.go
│   │   └── token.go
│   ├── config/                     # Configuration management
│   │   └── config.go
│   ├── control/                    # Mouse control logic
│   │   ├── mouse.go
│   │   ├── mouse_darwin.go
│   │   ├── mouse_linux.go
│   │   └── mouse_windows.go
│   ├── device/                     # Device registry
│   │   └── manager.go
│   ├── protocol/                   # Network protocol handlers
│   │   ├── server.go
│   │   ├── tcp.go
│   │   └── websocket.go
│   └── utils/                      # Utility functions
│       └── logger.go
├── go.mod
├── go.sum
└── README.md