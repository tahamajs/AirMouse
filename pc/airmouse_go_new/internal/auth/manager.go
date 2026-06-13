package auth

import (
    "crypto/rand"
    "encoding/hex"
    "fmt"
    "sync"
    "time"

    "github.com/golang-jwt/jwt/v5"
)

type Manager struct {
    secret       []byte
    pendingPairs map[string]time.Time
    mu           sync.RWMutex
    tokens       map[string]*TokenInfo
    maxTokens    int
    tokenTTL     time.Duration
}

type TokenInfo struct {
    Token      string
    DeviceID   string
    DeviceName string
    CreatedAt  time.Time
    ExpiresAt  time.Time
    LastUsed   time.Time
    UsageCount int
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
        tokens:       make(map[string]*TokenInfo),
        maxTokens:    100,
        tokenTTL:     24 * time.Hour,
    }
}

func (m *Manager) SetMaxTokens(max int) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.maxTokens = max
}

func (m *Manager) SetTokenTTL(ttl time.Duration) {
    m.mu.Lock()
    defer m.mu.Unlock()
    m.tokenTTL = ttl
}

func (m *Manager) GeneratePairingToken() (string, error) {
    token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
        "exp":  time.Now().Add(5 * time.Minute).Unix(),
        "iat":  time.Now().Unix(),
        "type": "pairing",
        "jti":  generateRandomID(),
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
    defer m.mu.Unlock()
    
    expiry, exists := m.pendingPairs[tokenString]
    if exists && expiry.After(time.Now()) {
        delete(m.pendingPairs, tokenString)
        return true
    }
    return false
}

func (m *Manager) GenerateAuthToken(deviceID, deviceName string) (string, error) {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    if len(m.tokens) >= m.maxTokens {
        m.cleanupExpiredTokens()
        if len(m.tokens) >= m.maxTokens {
            return "", fmt.Errorf("max tokens reached")
        }
    }
    
    tokenID := generateRandomID()
    now := time.Now()
    
    token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
        "exp":      now.Add(m.tokenTTL).Unix(),
        "iat":      now.Unix(),
        "type":     "auth",
        "device_id": deviceID,
        "device_name": deviceName,
        "jti":      tokenID,
    })
    
    tokenString, err := token.SignedString(m.secret)
    if err != nil {
        return "", err
    }
    
    m.tokens[tokenString] = &TokenInfo{
        Token:      tokenString,
        DeviceID:   deviceID,
        DeviceName: deviceName,
        CreatedAt:  now,
        ExpiresAt:  now.Add(m.tokenTTL),
        LastUsed:   now,
        UsageCount: 0,
    }
    
    return tokenString, nil
}

func (m *Manager) ValidateAuthToken(tokenString string) (bool, string, string) {
    token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
        return m.secret, nil
    })
    if err != nil || !token.Valid {
        return false, "", ""
    }
    
    claims, ok := token.Claims.(jwt.MapClaims)
    if !ok {
        return false, "", ""
    }
    
    tokenType, ok := claims["type"].(string)
    if !ok || tokenType != "auth" {
        return false, "", ""
    }
    
    deviceID, _ := claims["device_id"].(string)
    deviceName, _ := claims["device_name"].(string)
    
    m.mu.Lock()
    defer m.mu.Unlock()
    
    if info, exists := m.tokens[tokenString]; exists {
        info.LastUsed = time.Now()
        info.UsageCount++
        return true, deviceID, deviceName
    }
    
    return true, deviceID, deviceName
}

func (m *Manager) RevokeToken(tokenString string) bool {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    if _, exists := m.tokens[tokenString]; exists {
        delete(m.tokens, tokenString)
        return true
    }
    return false
}

func (m *Manager) RevokeDeviceTokens(deviceID string) int {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    removed := 0
    for token, info := range m.tokens {
        if info.DeviceID == deviceID {
            delete(m.tokens, token)
            removed++
        }
    }
    return removed
}

func (m *Manager) ListTokens() []*TokenInfo {
    m.mu.RLock()
    defer m.mu.RUnlock()
    
    tokens := make([]*TokenInfo, 0, len(m.tokens))
    for _, info := range m.tokens {
        tokens = append(tokens, info)
    }
    return tokens
}

func (m *Manager) cleanupExpiredTokens() {
    now := time.Now()
    for token, info := range m.tokens {
        if now.After(info.ExpiresAt) {
            delete(m.tokens, token)
        }
    }
    for token, expiry := range m.pendingPairs {
        if now.After(expiry) {
            delete(m.pendingPairs, token)
        }
    }
}

func (m *Manager) Cleanup() int {
    m.mu.Lock()
    defer m.mu.Unlock()
    
    before := len(m.tokens) + len(m.pendingPairs)
    m.cleanupExpiredTokens()
    after := len(m.tokens) + len(m.pendingPairs)
    return before - after
}

func (m *Manager) GetPairingQRData(wsURL string) (string, error) {
    token, err := m.GeneratePairingToken()
    if err != nil {
        return "", err
    }
    return fmt.Sprintf("airmouse://pair?token=%s&ws=%s", token, wsURL), nil
}

func (m *Manager) ValidateToken(tokenString string) bool {
    return m.ValidatePairingToken(tokenString)
}

func generateRandomID() string {
    bytes := make([]byte, 16)
    rand.Read(bytes)
    return hex.EncodeToString(bytes)
}