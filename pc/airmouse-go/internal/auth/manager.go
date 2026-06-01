package auth

import (
    "fmt"
    "sync"
    "time"

    "github.com/golang-jwt/jwt/v5"
)

type Manager struct {
    secret       []byte
    pendingPairs map[string]time.Time
    mu           sync.RWMutex
}

func NewManager(secret string) *Manager {
    secretBytes := []byte(secret)
    if len(secretBytes) < 32 {
        secretBytes = append(secretBytes, []byte("airmouse-fallback-secret-key-2025")...)
        if len(secretBytes) > 32 {
            secretBytes = secretBytes[:32]
        }
    }
    return &Manager{
        secret:       secretBytes,
        pendingPairs: make(map[string]time.Time),
    }
}

func (m *Manager) GeneratePairingToken() (string, error) {
    token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
        "exp":  time.Now().Add(5 * time.Minute).Unix(),
        "iat":  time.Now().Unix(),
        "type": "pairing",
    })
    tokenString, err := token.SignedString(m.secret)
    if err != nil {
        return "", err
    }
    m.mu.Lock()
    m.pendingPairs[tokenString] = time.Now().Add(5 * time.Minute)
    m.mu.Unlock()
    return tokenString, nil
}

func (m *Manager) ValidatePairingToken(tokenString string) bool {
    token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
        return m.secret, nil
    })
    if err != nil || !token.Valid {
        return false
    }
    m.mu.Lock()
    expiry, exists := m.pendingPairs[tokenString]
    if exists && expiry.After(time.Now()) {
        delete(m.pendingPairs, tokenString)
        m.mu.Unlock()
        return true
    }
    m.mu.Unlock()
    return false
}

func (m *Manager) GetPairingQRData(wsURL string) (string, error) {
    token, err := m.GeneratePairingToken()
    if err != nil {
        return "", err
    }
    return fmt.Sprintf("airmouse://pair?token=%s&ws=%s", token, wsURL), nil
}