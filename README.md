# Air Mouse Project

Air Mouse is a two-part embedded-systems project:

- an Android app that reads motion sensors, calibrates them, detects gestures, and sends control data
- a desktop Go server that receives the data, controls the mouse cursor, and shows a live dashboard

The project is built for the University of Tehran embedded-systems assignment and is centered on:

- motion sensing
- sensor calibration
- sensor fusion
- JSON/TCP communication
- ACK-based reliability for important actions
- desktop mouse control
- logging, debugging, and trace analysis

## Repository Layout

```text
code/
├── android/              # Android Studio project
│   ├── app/
│   └── docs/              # Android-side documentation and notes
├── pc/
│   ├── airmouse_go_new/   # Go desktop server
│   └── README.md
├── README.md              # This file
└── README_extra.md        # Extra notes and helpers
```

## What The Project Does

The phone acts as the input device. It reads sensor data, applies calibration and filtering, and sends motion and gesture events to the laptop.

The laptop runs the Go server. It:

- listens for Android clients
- accepts pairing and discovery requests
- parses JSON messages
- applies ACK handling for reliable commands
- controls the mouse cursor
- displays device status, protocol state, and logs in the UI

## Main Capabilities

### Android app

- motion-based cursor control
- click and scroll gesture detection
- calibration flow for sensors
- network discovery and pairing
- connection management
- runtime statistics and settings
- debug-friendly UI state

### Go server

- TCP / WebSocket-style desktop communication
- UDP discovery support
- QR and pairing-related status views
- live dashboard with logs and device information
- mouse control on the host machine
- connection and retry state visibility

## Communication Model

The current communication model is JSON-based.

### Typical flow

1. The Android app discovers or receives the laptop address.
2. The phone opens a connection to the Go server.
3. The app sends a `hello` or pairing message.
4. The server replies with a welcome/acknowledgement message.
5. Motion packets are sent continuously.
6. Click and scroll packets are treated as reliable actions and are acknowledged.

### Message types

Common message categories used by the project:

- `hello`
- `welcome`
- `move`
- `click`
- `doubleclick`
- `rightclick`
- `scroll`
- `ack`
- `ping` / `pong`
- `proximity`
- `control`

For the exact payload shapes, see:

- [`pc/airmouse_go_new/CommunicationProtocol.md`](./pc/airmouse_go_new/CommunicationProtocol.md)
- [`android/app/src/main/java/com/airmouse/network/README.md`](./android/app/src/main/java/com/airmouse/network/README.md)

## Sensor And Control Pipeline

The Android side is designed around the assignment requirements:

- raw motion sensors are read from the device
- calibration values are collected and applied
- orientation is filtered before it drives the cursor
- cursor movement is generated from phone rotation
- click and scroll gestures are detected separately from movement
- small hand jitter is suppressed by thresholds, dead zones, and cooldowns

The assignment reference docs in `docs/` include:

- [`docs/SENSOR_FUSION_EXPLAINED.md`](./docs/SENSOR_FUSION_EXPLAINED.md)
- [`docs/CALIBRATION_DETAILS.md`](./docs/CALIBRATION_DETAILS.md)
- [`docs/PERFETTO_ANSWERS.md`](./docs/PERFETTO_ANSWERS.md)
- [`docs/Network_Protocol_Specification.md`](./docs/Network_Protocol_Specification.md)
- [`docs/PROTOCOL_SPECIFICATION.md`](./docs/PROTOCOL_SPECIFICATION.md)

These are useful for both implementation choices and the final report.

## Desktop UI

The Go server UI is intended to show:

- server start/stop state
- listening ports
- connection status
- nearby or connected devices
- pairing and discovery information
- recent logs
- protocol / retry / ACK state

The UI is meant to stay readable during debugging, demoing, and pairing.

## Build And Run

### Android

Open the `android/` folder in Android Studio and run the app on an Android 10+ device.

If you already have the Android SDK configured, you can also build an APK from the project configuration in Android Studio.

### Go server

The desktop server is in `pc/airmouse_go_new/`.

Typical workflow:

```bash
cd pc/airmouse_go_new
make build
./airmouse-server
```

If you are just validating the code, run the project’s Go tests from the same folder.

### Networking

For phone-to-laptop communication:

- both devices must be on the same local network
- the server IP and port must match what the Android app uses
- Android needs `INTERNET` permission and cleartext access if the project uses non-TLS local communication

## Testing

The project includes both code-level and integration-style checks.

Recommended verification steps:

- build the Android app
- build and start the Go server
- confirm the phone can discover or connect to the server
- verify hello/welcome exchange
- verify cursor movement
- verify click and scroll acknowledgements
- check the live dashboard logs

For protocol-specific details, read:

- [`pc/airmouse_go_new/CommunicationProtocol.md`](./pc/airmouse_go_new/CommunicationProtocol.md)

## Perfetto And Report Notes

The assignment also includes tracing and profiling work.

Useful references in this repo:

- [`docs/PERFETTO_ANSWERS.md`](./docs/PERFETTO_ANSWERS.md)
- [`docs/Network_Protocol_Specification.md`](./docs/Network_Protocol_Specification.md)
- [`docs/PROJECT_COMPLETE_DOCUMENTATION.md`](./docs/PROJECT_COMPLETE_DOCUMENTATION.md)

Use these to document:

- sensor read latency
- filter CPU cost
- scheduling behavior
- thread interaction
- sampling interval vs actual data arrival

## Documentation Index

Some useful documentation files in the repo:

- [`pc/airmouse_go_new/README.md`](./pc/airmouse_go_new/README.md)
- [`pc/airmouse_go_new/CommunicationProtocol.md`](./pc/airmouse_go_new/CommunicationProtocol.md)
- [`android/app/src/main/java/com/airmouse/network/README.md`](./android/app/src/main/java/com/airmouse/network/README.md)
- [`android/app/src/main/java/com/airmouse/sensors/README.md`](./android/app/src/main/java/com/airmouse/sensors/README.md)
- [`android/app/src/main/java/com/airmouse/domain/README.md`](./android/app/src/main/java/com/airmouse/domain/README.md)
- [`android/app/src/main/java/com/airmouse/presentation/calibration/README.md`](./android/app/src/main/java/com/airmouse/presentation/calibration/README.md)

## Suggested Demo Flow

1. Start the Go server.
2. Open the Android app.
3. Pair the phone and laptop.
4. Show connection status on both sides.
5. Calibrate sensors.
6. Move the phone to move the cursor.
7. Trigger click and scroll gestures.
8. Open logs or dashboard status to show traffic and acknowledgements.

## Notes

- The docs in this repo are intentionally more detailed than a normal README because the assignment requires a lot of explanation.
- If a folder-level README exists, use it for module-specific details.
- If code and documentation disagree, prefer the source code and update the docs to match it.
