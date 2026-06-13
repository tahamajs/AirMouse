package utils

import (
    "crypto/rand"
    "encoding/hex"
    "strconv"
    "time"
)

// GenerateID returns a random 16‑byte hex string (32 characters).
func GenerateID() string {
    b := make([]byte, 16)
    if _, err := rand.Read(b); err != nil {
        // extremely unlikely; fallback to timestamp
        return strconv.FormatInt(time.Now().UnixNano(), 10)
    }
    return hex.EncodeToString(b)
}
