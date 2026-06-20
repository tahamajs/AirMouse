package entity

import (
	"encoding/json"
	"strconv"
	"time"
)

// GestureType enumerates predefined gesture names.
type GestureType string

const (
	GestureNone       GestureType = "none"
	GestureSwipeLeft  GestureType = "swipe_left"
	GestureSwipeRight GestureType = "swipe_right"
	GestureSwipeUp    GestureType = "swipe_up"
	GestureSwipeDown  GestureType = "swipe_down"
	GestureCircleCW   GestureType = "circle_cw"
	GestureCircleCCW  GestureType = "circle_ccw"
	GestureThumbsUp   GestureType = "thumbs_up"
	GestureThumbsDown GestureType = "thumbs_down"
	GestureDoubleTap  GestureType = "double_tap"
	GestureCustom     GestureType = "custom"
)

// Gesture represents a recognised gesture.
type Gesture struct {
	Type       GestureType `json:"type"`
	Name       string      `json:"name"`             // for custom gestures
	Confidence float64     `json:"confidence"`       // 0..1
	Timestamp  int64       `json:"timestamp"`        // Unix millis
	Velocity   float64     `json:"velocity"`         // pixels per second (if applicable)
	Duration   int64       `json:"duration"`         // milliseconds
	Points     []Point     `json:"points,omitempty"` // raw trajectory
}

// Point is a 2D coordinate with timestamp.
type Point struct {
	X, Y float64
	Time int64
}

// GestureTemplate stores a recorded gesture for later recognition.
type GestureTemplate struct {
	ID          string                 `json:"id"`
	Name        string                 `json:"name"`
	Type        GestureType            `json:"type"`
	Description string                 `json:"description,omitempty"`
	Data        []byte                 `json:"data"` // serialised feature vectors (e.g., protobuf)
	Metadata    map[string]interface{} `json:"metadata"`
	CreatedAt   int64                  `json:"created_at"`
	UpdatedAt   int64                  `json:"updated_at"`
	Version     int                    `json:"version"`
}

// TrainingSample represents a single data point for online learning.
type TrainingSample struct {
	Timestamp              int64   `json:"timestamp"`
	GyroX                  float64 `json:"gyro_x"`
	GyroY                  float64 `json:"gyro_y"`
	GyroZ                  float64 `json:"gyro_z"`
	AccelX                 float64 `json:"accel_x"`
	AccelY                 float64 `json:"accel_y"`
	AccelZ                 float64 `json:"accel_z"`
	GestureLabel           string  `json:"gesture_label"`
}

// NewGesture creates a new recognised gesture.
func NewGesture(gestureType GestureType, confidence float64) *Gesture {
	return &Gesture{
		Type:       gestureType,
		Confidence: confidence,
		Timestamp:  time.Now().UnixMilli(),
	}
}

// IsHighConfidence returns true if confidence is above the default threshold (0.7).
func (g *Gesture) IsHighConfidence() bool {
	return g.Confidence >= 0.7
}

// GetAction maps common gestures to system action identifiers.
func (g *Gesture) GetAction() string {
	switch g.Type {
	case GestureThumbsUp:
		return "play_pause"
	case GestureThumbsDown:
		return "stop"
	case GestureSwipeLeft:
		return "prev_track"
	case GestureSwipeRight:
		return "next_track"
	case GestureSwipeUp:
		return "volume_up"
	case GestureSwipeDown:
		return "volume_down"
	case GestureDoubleTap:
		return "double_click"
	case GestureCircleCW:
		return "volume_up"
	case GestureCircleCCW:
		return "volume_down"
	default:
		return string(g.Type)
	}
}

// NewGestureTemplate creates a new template with a unique ID.
func NewGestureTemplate(name string, gestureType GestureType) *GestureTemplate {
	now := time.Now().UnixMilli()
	return &GestureTemplate{
		ID:        name + "-" + strconv.FormatInt(now, 10),
		Name:      name,
		Type:      gestureType,
		Metadata:  make(map[string]interface{}),
		CreatedAt: now,
		UpdatedAt: now,
		Version:   1,
	}
}

// IsValid checks if the template has sufficient data.
func (t *GestureTemplate) IsValid() bool {
	return t.Name != "" && len(t.Data) > 20
}

// UpdateData replaces the template data and bumps the version.
func (t *GestureTemplate) UpdateData(data []byte) {
	t.Data = data
	t.UpdatedAt = time.Now().UnixMilli()
	t.Version++
}

// ToJSON returns the JSON representation of the template.
func (t *GestureTemplate) ToJSON() ([]byte, error) {
	return json.Marshal(t)
}
