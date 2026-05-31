
## 📱 Android App: Clean MVVM Architecture

### 📁 Recommended Project Structure

```
app/
├── src/main/java/com/airmouse/
│   ├── di/                           # Dependency Injection (Hilt)
│   │   ├── AppModule.kt
│   │   ├── NetworkModule.kt
│   │   └── SensorModule.kt
│   ├── domain/                       # Clean Architecture: Domain Layer
│   │   ├── model/                    # Business models (pure Kotlin)
│   │   │   ├── MouseEvent.kt
│   │   │   ├── Gesture.kt
│   │   │   └── ConnectionConfig.kt
│   │   ├── repository/               # Interfaces for data operations
│   │   │   ├── IMouseRepository.kt
│   │   │   ├── IGestureRepository.kt
│   │   │   └── IConnectionRepository.kt
│   │   └── usecase/                  # Business logic (interactors)
│   │       ├── SendMovementUseCase.kt
│   │       ├── DetectGestureUseCase.kt
│   │       └── ConnectToServerUseCase.kt
│   ├── data/                         # Data Layer
│   │   ├── repository/               # Implementations of repository interfaces
│   │   │   ├── MouseRepositoryImpl.kt
│   │   │   └── ConnectionRepositoryImpl.kt
│   │   ├── datasource/               # Local & remote data sources
│   │   │   ├── local/                # Room database, DataStore
│   │   │   │   ├── PreferencesDao.kt
│   │   │   │   └── AppDatabase.kt
│   │   │   └── remote/               # WebSocket, Bluetooth, USB
│   │   │       ├── WebSocketDataSource.kt
│   │   │       ├── BluetoothDataSource.kt
│   │   │       └── UsbDataSource.kt
│   │   └── model/                    # Data models (DTOs)
│   │       ├── NetworkMessage.kt
│   │       └── SensorData.kt
│   ├── presentation/                 # Presentation Layer
│   │   ├── ui/                       # Jetpack Compose Screens
│   │   │   ├── home/
│   │   │   │   ├── HomeScreen.kt
│   │   │   │   ├── HomeViewModel.kt
│   │   │   │   └── HomeState.kt
│   │   │   ├── calibration/
│   │   │   │   ├── CalibrationScreen.kt
│   │   │   │   ├── CalibrationViewModel.kt
│   │   │   │   └── CalibrationState.kt
│   │   │   ├── gesture/
│   │   │   │   ├── GestureStudioScreen.kt
│   │   │   │   ├── GestureStudioViewModel.kt
│   │   │   │   └── GestureStudioState.kt
│   │   │   ├── settings/
│   │   │   │   ├── SettingsScreen.kt
│   │   │   │   ├── SettingsViewModel.kt
│   │   │   │   └── SettingsState.kt
│   │   │   └── components/           # Reusable Compose components
│   │   │       ├── ConnectionCard.kt
│   │   │       ├── SensorDataCard.kt
│   │   │       └── GestureChart.kt
│   │   ├── theme/                    # Material 3 theming
│   │   │   ├── Color.kt
│   │   │   ├── Theme.kt
│   │   │   └── Type.kt
│   │   └── navigation/               # Compose Navigation
│   │       ├── NavGraph.kt
│   │       └── Destinations.kt
│   ├── service/                      # Android Services
│   │   ├── SensorService.kt          # Foreground service for sensor collection
│   │   ├── GestureInferenceService.kt
│   │   ├── ProximityService.kt
│   │   ├── VoiceCommandService.kt
│   │   └── BluetoothHidService.kt
│   ├── utils/                        # Utility classes
│   │   ├── Extensions.kt
│   │   ├── Constants.kt
│   │   └── MathUtils.kt
│   └── AirMouseApplication.kt        # Application class
└── src/main/res/                     # Resources (icons, themes, etc.)
```

### 🧱 Core Principles

1. **Clean Architecture (Onion)**: The architecture follows concentric layers: UI → Domain → Data. Dependencies point inward, so the Domain layer (containing your business logic) remains independent of frameworks. For an Air Mouse app, this means the core logic for interpreting gestures and sending mouse commands is completely separate from Android‑specific code.

2. **MVVM with Jetpack Compose**: The presentation layer uses `ViewModel` to hold UI state (exposed as `StateFlow`) and handle user events. The View (Compose UI) observes this state via `collectAsState()`. This ensures the UI only displays data and sends events, while the `ViewModel` contains the presentation logic.

3. **Repository Pattern**: The `repository` interfaces in the Domain layer are implemented in the Data layer. This abstracts the source of data (network, Bluetooth, USB, local storage). For example, an `IMovementRepository` can have a `WebSocketMovementDataSource` and a `BluetoothMovementDataSource`, both interchangeable without changing the business logic.

4. **Dependency Injection (Hilt)**: All dependencies (repositories, use cases, data sources, services) are injected via Hilt. This makes the code testable and reduces boilerplate.

5. **Kotlin Flows for Asynchronous Data**: Use `StateFlow` for UI state, `SharedFlow` for one‑time events (e.g., `showToast`), and `Flow` for continuous data streams (sensor data, incoming WebSocket messages). This integrates seamlessly with Compose.

### 🔌 Communication Protocols (Choice Matrix)

Select the protocol based on your specific requirements (Wi‑Fi reliability, latency tolerance, security needs, etc.):

| Protocol | Overhead | Latency | Pros | Cons | Best for |
|----------|----------|---------|------|------|----------|
| **WebSocket (TCP)** | 2–14 bytes | Low (stable) | Reliable, ordered, firewall‑friendly, simple API | Head‑of‑line blocking, TCP’s reliability can add jitter | General use on good Wi‑Fi (your primary choice) |
| **UDP + custom ACK** | 8 bytes | Very low | Lowest latency, no retransmission overhead | Unreliable, packets may arrive out of order | Gaming / ultra‑low‑latency requirements |
| **QUIC** | Low (similar to TCP‑TLS) | Lower than TCP | Combines reliability and low latency, 0‑RTT handshake | Emerging, library support varies | Remote connections over the internet |
| **USB HID** | <10 bytes | Extremely low | Direct cable, no network issues | Wired only, requires OTG adapter | Professional / stationary use |
| **Bluetooth (HID/GATT)** | 5–10 bytes | Low (5‑20 ms) | Wireless, standardised HID profile | Range limited, pairing required | Casual / mobile use |

For most cases, **WebSocket over TCP** is the best balance of simplicity and performance. If you later need to support weak networks, **QUIC** is a great upgrade because it avoids head‑of‑line blocking and allows 0‑RTT connection resumption. USB and Bluetooth HID modes are excellent for local, direct connections.

### 📡 Message Protocol (Binary for Performance)

**Strong recommendation:** Use **Protocol Buffers (protobuf)** or **MessagePack** instead of JSON. These binary formats are significantly more compact and faster to parse, which is crucial for real‑time applications. Here is a minimal protobuf schema:

```protobuf
syntax = "proto3";

message Movement {
    float dx = 1;
    float dy = 2;
}

message Click {
    string button = 1;
}

message Scroll {
    int32 delta = 1;
}

message Command {
    oneof command_type {
        Movement move = 1;
        Click click = 2;
        DoubleClick double_click = 3;
        Scroll scroll = 4;
        Hello hello = 5;
    }
}
```

This reduces packet size by 30–50% compared to JSON, lowering latency and saving battery on the phone.

### 🔐 Security & Data Persistence

*   **Authentication**: Use a short‑lived JWT token generated by the server and exchanged via a QR code. The token is included in the WebSocket upgrade request (`ws://server:8080/ws?token=...`). This prevents unauthorised clients from controlling the mouse.
*   **Encryption**: On the local network, you may skip TLS for performance, but for any remote connection you must use WSS (WebSocket over TLS) or QUIC (which has built‑in TLS 1.3).
*   **Local Storage (Room)**: Store user preferences (sensitivity, thresholds), saved gestures (as templates), and calibration data in a local Room database.
*   **DataStore**: Use `Preferences DataStore` for simple key‑value settings (e.g., last used IP, theme).

### 🤖 Sensor Data Collection & Gesture Recognition

*   **Foreground Service + SensorManager**: Use a foreground service (with a persistent notification) to collect gyroscope and accelerometer data, even when the app is in the background. The service registers listeners with `SensorManager` at the fastest possible rate (`SENSOR_DELAY_FASTEST`).
*   **Gesture Recognition Pipeline**:
    1.  **Data Buffer**: Maintain a sliding window of the last `N` sensor events (e.g., 50 samples).
    2.  **Feature Extraction**: Compute angular velocity (gyro), linear acceleration, and orientation (using `SensorManager.getRotationMatrixFromVector`).
    3.  **Classification**: Feed the features into a TensorFlow Lite model (trained on‑device) that outputs a gesture label and confidence.
    4.  **Post‑processing**: Apply a confidence threshold (≥0.7) and a cooldown (500 ms) to avoid duplicate triggers.
*   **Optimisation**: To reduce battery drain, lower the sensor sampling rate (`SENSOR_DELAY_UI`) when the phone is idle (detected via accelerometer variance).

### 💡 Additional Considerations

*   **Testing**: Use the `test` source set with JUnit, MockK, and Robolectric. Mock external dependencies (WebSocket, Bluetooth) to test the use cases in isolation.
*   **Error Handling**: Use `Either` or a sealed class (`Result<T>`) to represent failures (network errors, sensor unavailable). Propagate these to the UI layer.
*   **Multi‑Module Setup**: Separate features into independent modules for better build times and team scalability: `:core:domain`, `:core:data`, `:feature:home`, `:feature:calibration`, etc.

The Android app is now designed to be **scalable, testable, and maintainable** while delivering a smooth real‑time mouse control experience.

---

## 🖥️ Go Server: Clean Architecture with Modular Design

### 📁 Recommended Project Structure

```
airmouse-go/
├── cmd/
│   └── airmouse-server/
│       └── main.go                 # Entry point, DI container
├── internal/
│   ├── domain/                     # Domain Layer (pure business logic)
│   │   ├── entity/                 # Core business objects
│   │   │   ├── mouse.go
│   │   │   ├── gesture.go
│   │   │   └── client.go
│   │   ├── repository/             # Repository interfaces (for data access)
│   │   │   ├── mouse_repository.go
│   │   │   ├── gesture_repository.go
│   │   │   └── client_repository.go
│   │   └── service/                # Business logic / Use Cases
│   │       ├── mouse_service.go
│   │       ├── gesture_service.go
│   │       └── connection_service.go
│   ├── repository/                 # Data Layer
│   │   ├── mouse_repository_impl.go
│   │   ├── gesture_repository_impl.go
│   │   ├── client_repository_impl.go
│   │   └── config/                 # Configuration (JSON, env)
│   │       └── config.go
│   ├── handler/                    # Delivery Layer (HTTP, WebSocket)
│   │   ├── websocket/
│   │   │   ├── handler.go
│   │   │   ├── client.go
│   │   │   └── hub.go
│   │   ├── http/
│   │   │   ├── router.go
│   │   │   └── middleware.go
│   │   └── dto/                    # Data Transfer Objects
│   │       ├── message.go
│   │       └── response.go
│   ├── infra/                      # Infrastructure (external dependencies)
│   │   ├── mouse/                  # Platform‑specific mouse control
│   │   │   ├── mouse.go            # Interface
│   │   │   ├── windows.go
│   │   │   ├── darwin.go
│   │   │   └── linux.go
│   │   ├── bluetooth/              # BLE, HID
│   │   │   └── manager.go
│   │   ├── usb/                    # USB gadget mode
│   │   │   └── gadget.go
│   │   └── logger/                 # Structured logging
│   │       └── logger.go
│   └── pkg/                        # Shared utilities
│       ├── errors/                 # Custom error types
│       ├── utils/                  # Helper functions
│       └── config/                 # Configuration loader
├── pkg/                            # Public packages (if any)
├── go.mod
├── go.sum
└── Makefile
```

### 🏗️ Core Principles (Clean Architecture)

*   **Layered Design**: The architecture is divided into `domain` (business rules), `repository` (data access), `handler` (delivery), and `infra` (external dependencies). Dependencies point inward, so the `domain` layer knows nothing about WebSockets, databases, or OS‑specific mouse code.
*   **Dependency Injection**: In `main.go`, you manually wire all dependencies. The `infra` implementations are passed to the constructors of the repository and service layers. This makes the code testable (mock implementations can be substituted).
*   **Concurrency**: Use goroutines and channels for handling multiple clients. The WebSocket hub pattern (from `gorilla/websocket`) is perfect for broadcasting messages to all connected clients.
*   **Configuration**: Use a `config.go` that reads from a JSON file and environment variables. All config values are passed to the services at startup.

### 🧭 Communication Protocol Recommendations (Go Server)

The server is designed to support multiple protocols simultaneously, allowing the client to choose the best one. The recommended primary protocol is **WebSocket over TCP** because of its simplicity and reliability.

| Protocol | Library | Use Case |
|----------|---------|----------|
| **WebSocket** | `github.com/gorilla/websocket` | Primary protocol (Wi‑Fi, internet) |
| **QUIC** | `github.com/quic-go/quic-go` | Upgrade for low‑latency remote connections |
| **UDP** | `net` | Custom UDP + lightweight ACK |
| **TCP** | `net` | Plain TCP fallback |
| **Bluetooth** | `github.com/go-ble/ble` | BLE GATT service (discovery, not for data) |
| **USB** | `github.com/karalabe/hid` | USB gadget mode (emulate HID device) |

You can implement a `Transport` interface and register all active transports in a central `Hub`. Each transport receives incoming messages, decodes them, and forwards them to the appropriate use case (e.g., `MouseService.Move`). Responses are sent back through the same transport.

### 📦 Message Processing Pipeline

1.  **WebSocket Handler**: Upgrades HTTP connection, reads messages as binary (protobuf), and passes them to a `MessageProcessor`.
2.  **MessageProcessor**: Decodes the protobuf message and determines the type (move, click, hello). It then invokes the appropriate use case (e.g., `mouseService.Move`).
3.  **Use Case (Service)**: Contains the business logic (e.g., apply sensitivity, filtering, gesture detection). It may call a repository (e.g., to store statistics).
4.  **Repository**: Accesses infrastructure (e.g., in‑memory storage for statistics, configuration).
5.  **Response**: The use case returns a result, which the `MessageProcessor` encodes back to protobuf and sends to the WebSocket client.

### 🔧 Infrastructure Implementations

*   **Mouse Control**: Platform‑specific files (`mouse_windows.go`, `mouse_darwin.go`, `mouse_linux.go`) implement the `Mouse` interface using native APIs (Win32 API, CoreGraphics, uinput).
*   **Bluetooth**: The `bluetooth` package implements GATT services for discovery and pairing. It does not stream cursor data (use WebSocket for that).
*   **USB Gadget Mode**: The `usb` package uses `linux/configfs` (on supported devices) to emulate a USB HID mouse without any client software on the PC.
*   **Logging**: Structured logging with `slog` (Go 1.21+) or a third‑party library like `zerolog`. Each log entry includes a correlation ID to trace a request across multiple services.

### 🤖 AI and Personalization

*   **ONNX Runtime**: Use `github.com/yalue/onnxruntime_go` to load and execute the LSTM model for trajectory prediction. The inference runs in a separate goroutine (using a worker pool).
*   **Data Collector**: The `personalization` package collects movement samples (position, velocity, timestamp) and stores them in a circular buffer. When enough samples are collected, it triggers a HTTP call to a Python fine‑tuning service (running on `localhost:5001`). The updated model is then loaded via ONNX Runtime.

### 🧪 Testing & Observability

*   **Unit Tests**: Write tests for each use case using the `testing` package and mocks for repositories.
*   **Integration Tests**: Test the WebSocket handler with a real client.
*   **Metrics**: Expose Prometheus metrics (requests per second, active connections, latency) on an `/metrics` endpoint.
*   **Health Check**: Provide a `/health` endpoint that returns `200 OK` when the server is ready.

The Go server is now designed to be **modular, highly performant, and horizontally scalable**. Each layer is decoupled, allowing you to replace any component (e.g., switch from WebSocket to QUIC) without affecting the business logic. This architecture supports high‑concurrency (thousands of simultaneous clients) while maintaining sub‑20ms latency for cursor movement.

---

## 🤝 Cross-Project Synchronisation

To keep the Android app and Go server in sync, follow these guidelines:

*   **Shared Protocol Buffer Schema**: Maintain a single `airmouse.proto` file in a separate repository. Both projects generate code from this file (using the protobuf compiler). This ensures that message structures never become misaligned.
*   **API Versioning**: Prefix your WebSocket endpoint with a version, e.g., `/ws/v1/`. This allows you to introduce breaking changes later.
*   **Continuous Integration**: Set up a CI pipeline that, on any change to the `.proto` file, automatically regenerates the code for both projects and runs the full test suite.
*   **Version Tags**: Tag releases with semantic versioning (e.g., `v1.2.3`) and update the Android app’s build script to require a minimum server version.

