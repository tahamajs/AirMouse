package utils

import "time"

func FormatDuration(d time.Duration) string {
    return d.String()
}

func ParseDuration(s string, defaultDuration time.Duration) time.Duration {
    d, err := time.ParseDuration(s)
    if err != nil {
        return defaultDuration
    }
    return d
}

func NowMillis() int64 {
    return time.Now().UnixNano() / int64(time.Millisecond)
}