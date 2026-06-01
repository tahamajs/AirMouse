package utils

import (
    "crypto/rand"
    "encoding/base64"
    "math/big"
)

const alphanumeric = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

// RandomString generates a cryptographically secure random string of given length.
func RandomString(n int) (string, error) {
    b := make([]byte, n)
    for i := range b {
        num, err := rand.Int(rand.Reader, big.NewInt(int64(len(alphanumeric))))
        if err != nil {
            return "", err
        }
        b[i] = alphanumeric[num.Int64()]
    }
    return string(b), nil
}

// RandomToken generates a random base64 token (32 bytes).
func RandomToken() (string, error) {
    b := make([]byte, 32)
    _, err := rand.Read(b)
    if err != nil {
        return "", err
    }
    return base64.URLEncoding.EncodeToString(b), nil
}