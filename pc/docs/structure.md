## 1. Current Architecture (As‑Is)

The project currently contains **two overlapping architectures**:

### Active/Live Architecture (Works)
- **Entry point**: `cmd/airmouse-server/main.go`
- **UI Layer**: `ui/` package – Fyne‑based desktop GUI.
- **Application/Control Layer**: `control/` package – mouse controller, AI smoothers, predictors, gesture detection.
- **Protocol Layer**: `protocol/` package – TCP, WebSocket, UDP, Bluetooth, USB servers. Contains `ProtocolServer` that coordinates all protocols and the `device.Manager`.
- **Device Management**: `device/` package (not fully provided, but referenced) manages device states and persistence.
- **Configuration & Utils**: `config/`, `utils/`, `auth/` packages.

### Duplicate/Stub Architecture (Not Used)
- **Domain Layer**: `domain/` package – entities (`entity/`), service interfaces (`service/`), repository interfaces (`repository/`).
- **Infrastructure Layer**: `infra/` package – repository implementations (`repository/`), WebSocket hub (`websocket/`), HTTP router (`http/`), DTOs (`dto/`).
- **This layer is **not wired into `main.go`** and has many stubs/no‑ops.

Additionally, there are **ignored / experimental files** (`//go:build ignore`) like particle filter, duplicate mouse implementations, and a duplicate protocol server.

---

## 2. Recommended Clean Architecture

We will follow a **clean, layered architecture** with clear separation of concerns. I recommend **adopting the DDD‑style layer** (domain, application, infrastructure) **instead of** the current `protocol`/`control` approach, but **only if** you are willing to refactor. For most practical purposes, the existing `protocol`/`control` layer is simpler and already working. 

**I recommend the following structure** (which merges the best of both worlds):

```
airmouse-go/
├── cmd/
│   └── airmouse-server/          # Main entry point
├── internal/
│   ├── app/                      # Application orchestration
│   │   ├── server.go             # Creates ProtocolServer, UI, services
│   │   └── lifecycle.go          # Start/stop hooks
│   ├── domain/                   # Core business logic (entities, value objects)
│   │   ├── entity/               # Device, Client, Gesture, MouseProfile, Statistics
│   │   ├── repository/           # Interfaces for persistence/retrieval
│   │   └── service/              # Interfaces for use cases (MouseService, GestureService, ConnectionService)
│   ├── application/              # Implementation of services (use cases)
│   │   ├── mouse/
│   │   │   └── service.go        # Implements MouseService interface
│   │   ├── gesture/
│   │   │   └── service.go        # Implements GestureService (with recognizer)
│   │   └── connection/
│   │       └── service.go        # Implements ConnectionService
│   ├── infrastructure/           # External dependencies (repositories, protocol adapters)
│   │   ├── protocol/             # All protocol servers (TCP, WebSocket, UDP, Bluetooth, USB)
│   │   │   ├── tcp/
│   │   │   ├── websocket/
│   │   │   ├── udp/
│   │   │   ├── bluetooth/
│   │   │   ├── usb/
│   │   │   └── protocol.go       # ProtocolServer (orchestrates all)
│   │   ├── mouse/                # Platform‑specific mouse control (CoreGraphics, WinAPI, uinput)
│   │   │   ├── mouse.go          # MouseController interface
│   │   │   ├── darwin.go
│   │   │   ├── windows.go
│   │   │   └── linux.go
│   │   ├── repository/           # Concrete repository implementations (in‑memory, file, DB)
│   │   │   ├── client_repo.go
│   │   │   ├── gesture_repo.go
│   │   │   └── mouse_repo.go
│   │   ├── config/               # Configuration loader/saver
│   │   ├── auth/                 # Authentication (token validation)
│   │   ├── device/               # Device manager (could be merged with client repo)
│   │   └── utils/                # Helpers (ID generation, logging, IP, crypto)
│   ├── ui/                       # Fyne‑based GUI
│   │   ├── app.go                # Main window, tabs
│   │   ├── dashboard.go
│   │   ├── devices.go
│   │   ├── network.go
│   │   ├── gestures.go
│   │   ├── proximity.go
│   │   ├── analytics.go
│   │   ├── settings.go
│   │   ├── logs.go
│   │   ├── protocol_guide.go
│   │   ├── statusbar.go
│   │   ├── glass_card.go
│   │   ├── helpers.go
│   │   └── ... (other UI components)
│   └── pkg/                      # Shared libraries (if any)
│       └── ...
└── go.mod
```

### Key Architectural Principles:
- **Dependency Inversion**: High‑level modules (UI, application services) depend on **interfaces** (domain/service, domain/repository), not on concrete implementations.
- **Separation of Concerns**:
  - **UI**: Only deals with presentation and user input. It calls application services.
  - **Application Services**: Orchestrate use cases, call repositories and infrastructure adapters.
  - **Domain**: Contains business rules, entities, and value objects (no external dependencies).
  - **Infrastructure**: Implements repositories, protocol servers, mouse control, etc.
- **Single Responsibility**: Each package has a clear purpose and minimal overlap.

---

## 3. Component Interactions (How They Talk to Each Other)

```
+------------------+          +---------------------------+
|   UI (fyne)      | -------> | Application Services      |
|   (ui/*)         |          | (mouse, gesture, connect) |
+------------------+          +-------------+-------------+
                                          |
                                          v
                              +---------------------------+
                              | Domain Entities & Repos   |
                              | (domain/entity, repo)     |
                              +-------------+-------------+
                                          |
                                          v
+------------------+          +---------------------------+
| Protocol Server  | <------> | Repository Implementations|
| (infra/protocol) |          | (infra/repository)        |
+------------------+          +---------------------------+
                                          |
                                          v
                              +---------------------------+
                              | External Systems          |
                              | (OS mouse, network, etc.) |
                              +---------------------------+
```

- **UI** calls services (e.g., `mouseService.Move()`, `connectionService.ListClients()`).
- **Services** use **repositories** to read/write data (clients, gestures, statistics) and call **infrastructure adapters** (e.g., `protocolServer.Broadcast()`, `mouseRepo.Move()`).
- **Protocol Servers** handle raw network connections, decode messages, and call back into the application (e.g., on new client, on mouse move, on gesture). They are **infrastructure** and should not contain business logic.

---

## 4. Which Files to Keep, Delete, or Complete

Based on the current state, here is a concrete action plan:

### ✅ **Keep (Active, working)**
- `cmd/airmouse-server/main.go`
- `ui/` (all `.go` files) – they already call the active services (but you may refactor them to call the new application services later).
- `control/` (all active files: `mouse.go`, `movement_predictor.go`, `pause.go`, plus platform‑specific `*_darwin.go`, `*_linux.go`, `*_windows.go`). Keep these for now if you stick with the old architecture. If you refactor, you can replace them with `infrastructure/mouse/`.
- `protocol/` (all active `*.go` except the ignored `server.go`). Keep `protocol.go`, `message.go`, and sub‑packages (tcp, websocket, udp, bluetooth, usb).
- `device/` – keep (used by protocol servers).
- `auth/` – keep.
- `config/` – keep.
- `utils/` – keep.
- `personalization/` – keep (if used).

### ❌ **Delete / Archive (Duplicates, stubs, ignore)**
- All `//go:build ignore` files:
  - `control/mouse_darwin.go`
  - `protocol/server.go`
  - `particlefilter/` (entire folder) – unless you plan to use it; then you could move it to `infrastructure/gesture/recognizer`.
  - `predictive/test.go` – rename to `kalman2d_test.go` if needed, but it's not integrated.
  - `control/predictor.go` (duplicate of `movement_predictor.go`).
- The **entire DDD‑style layer** (`domain/`, `infra/` except for `infra/mouse` and `infra/repository` if you decide to use them) – **if you are not going to refactor**. If you *are* going to refactor, then you should **complete** and **integrate** them, not delete.

### 🔧 **Complete (if you choose to adopt the new architecture)**
- `domain/entity/` – define all entities (already done, but ensure they match the active ones).
- `domain/repository/` – interfaces (already defined).
- `domain/service/` – interfaces (already defined).
- `infra/repository/` – implement all methods (already done for client and gesture; mouse repo needs real OS integration).
- `infra/websocket/` – if used, integrate with protocol; but I'd rather keep the existing `protocol/websocket`.
- `infra/http/` – if you want a separate HTTP server for health/metrics, it's fine.

---

## 5. Fully Completed File Structure (After Cleanup)

Here is the **final file tree** you should aim for (with no duplicates, all necessary files present and functional):

```
airmouse-go/
├── cmd/
│   └── airmouse-server/
│       └── main.go                        # Initializes app, starts UI, protocol, services
├── internal/
│   ├── app/
│   │   ├── app.go                         # Application bootstrap, dependency injection
│   │   └── lifecycle.go                   # Start/stop hooks (optional)
│   ├── ui/                                # All UI files (no changes needed)
│   │   ├── about.go
│   │   ├── analytics.go
│   │   ├── app.go                         # Main window, tabs, menu
│   │   ├── connection.go                  # Quality widget
│   │   ├── dashboard.go
│   │   ├── devices.go
│   │   ├── gestures.go
│   │   ├── glass_card.go
│   │   ├── helpers.go
│   │   ├── icons.go
│   │   ├── logs.go
│   │   ├── network.go
│   │   ├── pairing.go
│   │   ├── premium_dashboard.go
│   │   ├── premium_theme.go
│   │   ├── protocol_guide.go
│   │   ├── proximity.go
│   │   ├── settings.go
│   │   ├── shortcuts.go
│   │   ├── speedchart.go
│   │   ├── statusbar.go
│   │   └── themes.go
│   ├── domain/                            # Core business entities and repository/service interfaces
│   │   ├── entity/
│   │   │   ├── client.go
│   │   │   ├── device.go                  # (if not already in device package)
│   │   │   ├── gesture.go
│   │   │   ├── mouse.go
│   │   │   └── statistics.go
│   │   ├── repository/
│   │   │   ├── client_repository.go       # Interface
│   │   │   ├── gesture_repository.go      # Interface
│   │   │   └── mouse_repository.go        # Interface
│   │   └── service/
│   │       ├── connection_service.go      # Interface
│   │       ├── gesture_service.go         # Interface
│   │       └── mouse_service.go           # Interface
│   ├── application/                       # Implementations of services (use cases)
│   │   ├── mouse/
│   │   │   └── service.go                 # MouseService impl (using repos and mouse controller)
│   │   ├── gesture/
│   │   │   ├── service.go                 # GestureService impl (with recognizer)
│   │   │   └── recognizer.go              # (optional) internal recognizer
│   │   └── connection/
│   │       └── service.go                 # ConnectionService impl (using client repo)
│   ├── infrastructure/                    # External adapters
│   │   ├── protocol/                      # All protocol servers (keep current)
│   │   │   ├── tcp/
│   │   │   ├── websocket/
│   │   │   ├── udp/
│   │   │   ├── bluetooth/
│   │   │   ├── usb/
│   │   │   └── protocol.go                # ProtocolServer (orchestrates all, calls services?)
│   │   ├── mouse/                         # Platform‑specific mouse control
│   │   │   ├── mouse.go                   # Interface and common logic
│   │   │   ├── darwin.go
│   │   │   ├── windows.go
│   │   │   └── linux.go
│   │   ├── repository/                    # Repository implementations
│   │   │   ├── client_repository_impl.go
│   │   │   ├── gesture_repository_impl.go
│   │   │   └── mouse_repository_impl.go   # Uses mouse controller from infra/mouse
│   │   ├── config/                        # Configuration (keep)
│   │   ├── auth/                          # Auth (keep)
│   │   ├── device/                        # Device manager (could be replaced by client repo)
│   │   └── utils/                         # Helpers (keep)
│   └── pkg/                               # (optional) shared libraries
│       └── ...
├── go.mod
└── go.sum
```

---

## 6. Recommended Migration Path

1. **First, stabilise the current working version** – fix the critical bugs (WebSocket error propagation, data races, welcome message) so that the app connects reliably.
2. **Decide which architecture to keep** – either keep the simple `protocol`/`control` approach or adopt the clean DDD architecture.
3. **If you adopt the DDD architecture**:
   - Move all protocol servers to `infrastructure/protocol/` and make them implement a `Broadcaster` interface.
   - Inject `ConnectionService` and `MouseService` into the protocol handlers.
   - Replace direct calls to `device.Manager` with calls to `ConnectionService` and `ClientRepository`.
   - Refactor the UI to use the new services (this is a bigger effort).
4. **Delete all duplicate/ignored files** to reduce confusion.
5. **Write comprehensive unit and integration tests** for the critical paths.

---

## 7. Summary

The **cleanest, most maintainable structure** is one where:
- UI depends only on **application services**.
- Services depend on **repository interfaces** and **infrastructure adapters**.
- Repositories and protocol servers are **pluggable** and **testable**.
- There are **no duplicate** implementations and **no ignored files** in the main build.

I have provided the full file layout and the reasoning. If you need more detail on any specific file (e.g., the exact implementation of `mouse_repository_impl.go` that calls the OS APIs), I can provide that too. Let me know which part you want to tackle first.