package utils

import (
    "crypto/rand"
    "encoding/hex"
)

// GenerateID returns a random 16‑byte hex string.
func GenerateID() string {
    b := make([]byte, 16)
    _, _ = rand.Read(b)
    return hex.EncodeToString(b)
}
