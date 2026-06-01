package jitter

import (
	"testing"
	"time"
)

func TestJitterBuffer(t *testing.T) {
	cfg := DefaultJitterBufferConfig()
	jb := NewJitterBuffer(cfg)
	now := time.Now()
	for i := 0; i < 10; i++ {
		smDx, smDy := jb.AddMovement(10.0, 5.0, now)
		t.Logf("smoothed: (%.2f, %.2f)", smDx, smDy)
	}
}
