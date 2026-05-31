airmouse-go/
├── cmd/
│   └── airmouse-server/
│       └── main.go
├── internal/
│   ├── adaptivesmoothing/
│   │   ├── bspline.go
│   │   ├── humanizer.go
│   │   ├── tremor.go
│   │   └── velocity.go
│   ├── auth/
│   │   └── manager.go
│   ├── config/
│   │   └── config.go
│   ├── control/
│   │   ├── gesture.go
│   │   ├── mouse.go
│   │   ├── mouse_darwin.go
│   │   ├── mouse_linux.go
│   │   ├── mouse_windows.go
│   │   ├── pause.go
│   │   ├── ai_smoother.go
│   │   └── ml_predictor.go
│   ├── device/
│   │   ├── authenticator.go
│   │   └── manager.go
│   ├── jitter/
│   │   ├── buffer.go
│   │   ├── kalman1d.go
│   │   ├── predictor.go
│   │   └── test.go
│   ├── particlefilter/
│   │   ├── filter.go
│   │   ├── recognizer.go
│   │   └── test.go
│   ├── personalization/
│   │   ├── collector.go
│   │   └── trainer_client.go
│   ├── predictive/
│   │   ├── kalman2d.go
│   │   ├── predictor.go
│   │   └── test.go
│   ├── predictiveml/
│   │   ├── predictor.go
│   │   └── trainer.go
│   ├── proximity/
│   │   ├── manager.go
│   │   ├── rssi_fusion.go
│   │   ├── darwin.go
│   │   ├── linux.go
│   │   └── windows.go
│   ├── protocol/
│   │   ├── server.go
│   │   ├── tcp.go
│   │   ├── udp.go
│   │   ├── websocket.go
│   │   ├── bluetooth/
│   │   │   ├── manager.go
│   │   │   ├── hid.go
│   │   │   └── serial.go
│   │   └── usb/
│   │       └── server.go
│   ├── sensorfusion/
│   │   ├── quaternion.go
│   │   ├── madgwick.go
│   │   ├── mahony.go
│   │   └── test.go
│   ├── sysaction/
│   │   ├── action.go
│   │   ├── darwin.go
│   │   ├── linux.go
│   │   ├── windows.go
│   │   └── robotgo.go
│   ├── ui/
│   │   ├── analytics.go
│   │   ├── app.go
│   │   ├── dashboard.go
│   │   ├── devices.go
│   │   ├── logs.go
│   │   ├── network.go
│   │   ├── settings.go
│   │   └── themes.go
│   └── utils/
│       ├── crypto.go
│       ├── file.go
│       ├── ip.go
│       ├── logger.go
│       ├── metrics.go
│       ├── random.go
│       ├── time.go
│       ├── validation.go
│       └── convert.go
├── go.mod
└── go.sum