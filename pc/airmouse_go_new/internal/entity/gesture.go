package entity

import (
	"encoding/json"
	"strconv"
	"time"
)

// GestureType enumerates predefined gesture names
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
	GestureLongPress  GestureType = "long_press"
	GesturePinchIn    GestureType = "pinch_in"
	GesturePinchOut   GestureType = "pinch_out"
	GestureRotateCW   GestureType = "rotate_cw"
	GestureRotateCCW  GestureType = "rotate_ccw"
	GestureShake      GestureType = "shake"
	GestureCustom     GestureType = "custom"
)

// GestureAction maps gestures to system actions
var GestureActionMap = map[GestureType]string{
	GestureSwipeLeft:  "media_prev",
	GestureSwipeRight: "media_next",
	GestureSwipeUp:    "volume_up",
	GestureSwipeDown:  "volume_down",
	GestureCircleCW:   "volume_up",
	GestureCircleCCW:  "volume_down",
	GestureThumbsUp:   "play_pause",
	GestureThumbsDown: "stop",
	GestureDoubleTap:  "play_pause",
	GestureLongPress:  "right_click",
	GesturePinchIn:    "zoom_out",
	GesturePinchOut:   "zoom_in",
	GestureShake:      "undo",
}

// Gesture represents a recognised gesture
type Gesture struct {
	Type       GestureType `json:"type"`
	Name       string      `json:"name"`
	Confidence float64     `json:"confidence"`
	Timestamp  int64       `json:"timestamp"`
	Velocity   float64     `json:"velocity"`
	Duration   int64       `json:"duration"`
	Points     []Point     `json:"points,omitempty"`
	Action     string      `json:"action,omitempty"`
}

// Point is a 2D coordinate with timestamp
type Point struct {
	X    float64 `json:"x"`
	Y    float64 `json:"y"`
	Time int64   `json:"time"`
}

// GestureTemplate stores a recorded gesture for recognition
type GestureTemplate struct {
	ID          string                 `json:"id"`
	Name        string                 `json:"name"`
	Type        GestureType            `json:"type"`
	Description string                 `json:"description,omitempty"`
	Data        []byte                 `json:"data"`
	Metadata    map[string]interface{} `json:"metadata"`
	CreatedAt   int64                  `json:"created_at"`
	UpdatedAt   int64                  `json:"updated_at"`
	Version     int                    `json:"version"`
	UsageCount  int                    `json:"usage_count"`
	AvgScore    float64                `json:"avg_score"`
}

// TrainingSample represents a single data point for online learning
type TrainingSample struct {
	Timestamp    int64   `json:"timestamp"`
	GyroX        float64 `json:"gyro_x"`
	GyroY        float64 `json:"gyro_y"`
	GyroZ        float64 `json:"gyro_z"`
	AccelX       float64 `json:"accel_x"`
	AccelY       float64 `json:"accel_y"`
	AccelZ       float64 `json:"accel_z"`
	GestureLabel string  `json:"gesture_label"`
	Confidence   float64 `json:"confidence"`
}

// NewGesture creates a new recognised gesture
func NewGesture(gestureType GestureType, confidence float64) *Gesture {
	return &Gesture{
		Type:       gestureType,
		Confidence: confidence,
		Timestamp:  time.Now().UnixMilli(),
		Action:     GestureActionMap[gestureType],
	}
}

// IsHighConfidence returns true if confidence is above threshold
func (g *Gesture) IsHighConfidence(threshold float64) bool {
	if threshold <= 0 {
		threshold = 0.7
	}
	return g.Confidence >= threshold
}

// GetAction returns the mapped system action
func (g *Gesture) GetAction() string {
	if g.Action != "" {
		return g.Action
	}
	return GestureActionMap[g.Type]
}

// NewGestureTemplate creates a new template with unique ID
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

// IsValid checks if the template has sufficient data
func (t *GestureTemplate) IsValid() bool {
	return t.Name != "" && len(t.Data) > 20
}

// UpdateData replaces the template data and bumps version
func (t *GestureTemplate) UpdateData(data []byte) {
	t.Data = data
	t.UpdatedAt = time.Now().UnixMilli()
	t.Version++
}

// RecordUsage increments usage count and updates average score
func (t *GestureTemplate) RecordUsage(score float64) {
	t.UsageCount++
	t.AvgScore = (t.AvgScore*float64(t.UsageCount-1) + score) / float64(t.UsageCount)
}

// ToJSON returns JSON representation
func (t *GestureTemplate) ToJSON() ([]byte, error) {
	return json.Marshal(t)
}

// FromJSON creates template from JSON
func FromJSON(data []byte) (*GestureTemplate, error) {
	var t GestureTemplate
	if err := json.Unmarshal(data, &t); err != nil {
		return nil, err
	}
	return &t, nil
}
