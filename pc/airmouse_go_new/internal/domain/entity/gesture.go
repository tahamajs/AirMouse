package entity

// GestureType represents the type of gesture detected.
type GestureType int

const (
	GestureNone GestureType = iota
	GestureClick
	GestureDoubleClick
	GestureRightClick
	GestureScrollUp
	GestureScrollDown
	GestureSwipeLeft
	GestureSwipeRight
)

// String returns a human-readable representation.
func (g GestureType) String() string {
	switch g {
	case GestureClick:
		return "click"
	case GestureDoubleClick:
		return "doubleclick"
	case GestureRightClick:
		return "rightclick"
	case GestureScrollUp:
		return "scroll_up"
	case GestureScrollDown:
		return "scroll_down"
	case GestureSwipeLeft:
		return "swipe_left"
	case GestureSwipeRight:
		return "swipe_right"
	default:
		return "none"
	}
}

// Gesture represents a recognised gesture with confidence.
type Gesture struct {
	Type       GestureType
	Confidence float64
	Timestamp  int64
}

// NewGesture creates a new gesture.
func NewGesture(gestureType GestureType, confidence float64) *Gesture {
	return &Gesture{
		Type:       gestureType,
		Confidence: confidence,
		Timestamp:  time.Now().UnixMilli(),
	}
}