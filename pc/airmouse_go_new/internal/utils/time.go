package utils

import "time"

// FormatDuration returns a human‑readable duration string.
func FormatDuration(d time.Duration) string {
	return d.String()
}

// ParseDuration safely parses a duration string, returning default on error.
func ParseDuration(s string, defaultDuration time.Duration) time.Duration {
	d, err := time.ParseDuration(s)
	if err != nil {
		return defaultDuration
	}
	return d
}

// NowMillis returns current time in milliseconds.
func NowMillis() int64 {
	return time.Now().UnixNano() / int64(time.Millisecond)
}
