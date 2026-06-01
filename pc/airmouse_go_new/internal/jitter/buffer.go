package jitter

import (
	"sync"
	"time"
)

type JitterBufferConfig struct {
	MaxLatency        time.Duration
	PredictionWindow  time.Duration
	BlendFactor       float64
	UseKalmanVelocity bool
	UseAcceleration   bool
}

func DefaultJitterBufferConfig() JitterBufferConfig {
	return JitterBufferConfig{
		MaxLatency:        100 * time.Millisecond,
		PredictionWindow:  20 * time.Millisecond,
		BlendFactor:       0.7,
		UseKalmanVelocity: true,
		UseAcceleration:   false,
	}
}

type JitterBuffer struct {
	cfg            JitterBufferConfig
	predictor      *DeadReckoningPredictor
	mu             sync.Mutex
	lastTime       time.Time
	lastDx, lastDy float64
	history        []MovementSample
	maxHistory     int
}

type MovementSample struct {
	Timestamp time.Time
	Dx, Dy    float64
}

func NewJitterBuffer(cfg JitterBufferConfig) *JitterBuffer {
	return &JitterBuffer{
		cfg:        cfg,
		predictor:  NewDeadReckoningPredictor(cfg.UseAcceleration),
		history:    make([]MovementSample, 0, 50),
		maxHistory: 50,
		lastTime:   time.Now(),
	}
}

func (jb *JitterBuffer) AddMovement(dx, dy float64, receivedAt time.Time) (smoothedDx, smoothedDy float64) {
	jb.mu.Lock()
	defer jb.mu.Unlock()
	now := time.Now()
	dt := now.Sub(jb.lastTime).Seconds()
	if dt < 0.001 {
		dt = 0.02
	}
	if jb.cfg.UseKalmanVelocity {
		jb.predictor.UpdateVelocity(dx, dy, dt)
	} else {
		jb.predictor.velocityX.Update(dx / dt)
		jb.predictor.velocityY.Update(dy / dt)
	}
	predDx, predDy := jb.predictor.Predict(jb.cfg.PredictionWindow.Seconds())
	latency := now.Sub(receivedAt)
	blend := jb.cfg.BlendFactor
	if latency > jb.cfg.MaxLatency {
		blend = 0.9
	} else if latency < 20*time.Millisecond {
		blend = 0.5
	}
	smoothedDx = (1-blend)*dx + blend*predDx
	smoothedDy = (1-blend)*dy + blend*predDy
	jb.history = append(jb.history, MovementSample{Timestamp: now, Dx: dx, Dy: dy})
	if len(jb.history) > jb.maxHistory {
		jb.history = jb.history[1:]
	}
	jb.lastTime = now
	jb.lastDx, jb.lastDy = dx, dy
	return
}

func (jb *JitterBuffer) PredictNow() (dx, dy float64) {
	jb.mu.Lock()
	defer jb.mu.Unlock()
	dt := time.Since(jb.lastTime).Seconds()
	if dt > 0.1 {
		dt = 0.1
	}
	return jb.predictor.Predict(dt)
}

func (jb *JitterBuffer) Reset() {
	jb.mu.Lock()
	defer jb.mu.Unlock()
	jb.predictor.Reset()
	jb.history = jb.history[:0]
	jb.lastTime = time.Now()
}
