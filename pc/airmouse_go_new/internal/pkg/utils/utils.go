package utils

import (
	"crypto/rand"
	"encoding/hex"
	"math"
	"net"
	"time"
)

// GenerateID generates a random 16‑byte hex ID.
func GenerateID() string {
	b := make([]byte, 16)
	rand.Read(b)
	return hex.EncodeToString(b)
}

// Clamp limits a value to a range.
func Clamp(value, min, max float64) float64 {
	if value < min {
		return min
	}
	if value > max {
		return max
	}
	return value
}

// MapRange maps a value from one range to another.
func MapRange(value, fromLow, fromHigh, toLow, toHigh float64) float64 {
	return toLow + (value-fromLow)*(toHigh-toLow)/(fromHigh-fromLow)
}

// GetLocalIP returns the first non‑loopback IPv4 address.
func GetLocalIP() string {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return "127.0.0.1"
	}
	for _, addr := range addrs {
		if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() && ipnet.IP.To4() != nil {
			return ipnet.IP.String()
		}
	}
	return "127.0.0.1"
}

// TimeNowMs returns the current time in milliseconds.
func TimeNowMs() int64 {
	return time.Now().UnixMilli()
}

// FilterMovingAverage implements a simple moving average filter.
func FilterMovingAverage(values []float64, window int) float64 {
	if len(values) == 0 {
		return 0
	}
	if window > len(values) {
		window = len(values)
	}
	sum := 0.0
	for i := len(values) - window; i < len(values); i++ {
		sum += values[i]
	}
	return sum / float64(window)
}

// RadToDeg converts radians to degrees.
func RadToDeg(rad float64) float64 {
	return rad * 180 / math.Pi
}

// DegToRad converts degrees to radians.
func DegToRad(deg float64) float64 {
	return deg * math.Pi / 180
}