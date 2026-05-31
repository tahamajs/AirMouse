package utils

import (
	"time"
)

// FormatDuration returns a human‑readable duration string (e.g., "2h3m4s").
func FormatDuration(d time.Duration) string {
	return d.String()
}

// ParseDuration safely parses a duration string, returning default if error.
func ParseDuration(s string, defaultDuration time.Duration) time.Duration {
	d, err := time.ParseDuration(s)
	if err != nil {
		return defaultDuration
	}
	return d
}