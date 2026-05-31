airmouse-go/
├── cmd/
│   └── airmouse-server/
│       └── main.go
├── internal/
│   ├── domain/
│   │   ├── entity/
│   │   │   ├── mouse.go
│   │   │   ├── gesture.go
│   │   │   └── client.go
│   │   ├── repository/
│   │   │   ├── mouse_repository.go
│   │   │   ├── gesture_repository.go
│   │   │   └── client_repository.go
│   │   └── service/
│   │       ├── mouse_service.go
│   │       ├── gesture_service.go
│   │       └── connection_service.go
│   ├── repository/
│   │   ├── mouse_repository_impl.go
│   │   ├── gesture_repository_impl.go
│   │   ├── client_repository_impl.go
│   │   └── config/
│   │       └── config.go
│   ├── handler/
│   │   ├── websocket/
│   │   │   ├── hub.go
│   │   │   ├── client.go
│   │   │   └── handler.go
│   │   ├── http/
│   │   │   ├── router.go
│   │   │   └── middleware.go
│   │   └── dto/
│   │       └── message.go
│   ├── infra/
│   │   ├── mouse/
│   │   │   ├── mouse.go          # interface
│   │   │   ├── windows.go
│   │   │   ├── darwin.go
│   │   │   └── linux.go
│   │   ├── bluetooth/
│   │   │   └── manager.go
│   │   ├── usb/
│   │   │   └── gadget.go
│   │   └── logger/
│   │       └── logger.go
│   └── pkg/
│       ├── errors/
│       │   └── errors.go
│       ├── utils/
│       │   └── utils.go
│       └── config/
│           └── config.go
├── go.mod
├── go.sum
└── Makefile