package auth

import (
    "crypto/rand"
    "crypto/hmac"
    "crypto/sha256"
    "encoding/hex"
    "encoding/base64"
    "encoding/json"
    "fmt"
    "strings"
    "sync"
    "time"
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

type tokenClaims struct {
    Exp  int64  `json:"exp"`
    Iat  int64  `json:"iat"`
    Type string `json:"type"`
    JTI  string `json:"jti"`
    DeviceID string `json:"device_id,omitempty"`
    DeviceName string `json:"device_name,omitempty"`
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
    claims := tokenClaims{
        Exp:  time.Now().Add(5 * time.Minute).Unix(),
        Iat:  time.Now().Unix(),
        Type: "pairing",
        JTI:  generateRandomID(),
    }
    tokenString, err := m.signClaims(claims)
    if err != nil {
        return "", err
    }
    m.mu.Lock()
    m.pendingPairs[tokenString] = time.Now().Add(5 * time.Minute)
    m.mu.Unlock()
    
    return tokenString, nil
}

func (m *Manager) ValidatePairingToken(tokenString string) bool {
    claims, err := m.parseClaims(tokenString)
    if err != nil || claims.Type != "pairing" {
        return false
    }
    m.mu.Lock()
    defer m.mu.Unlock()
    expiry, exists := m.pendingPairs[tokenString]
    if exists && expiry.After(time.Now()) && claims.Exp >= time.Now().Unix() {
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
    
    tokenString, err := m.signClaims(tokenClaims{
        Exp: now.Add(m.tokenTTL).Unix(),
        Iat: now.Unix(),
        Type: "auth",
        JTI: tokenID,
        DeviceID: deviceID,
        DeviceName: deviceName,
    })
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
    claims, err := m.parseClaims(tokenString)
    if err != nil {
        return false, "", ""
    }
    if claims.Type != "auth" || claims.Exp < time.Now().Unix() {
        return false, "", ""
    }
    deviceID, deviceName := claims.DeviceID, claims.DeviceName
    
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
    return fmt.Sprintf("airmouse://pair?token=%s&ws=%s&protocol=WEBSOCKET", token, wsURL), nil
}

func (m *Manager) signClaims(claims tokenClaims) (string, error) {
    body, err := json.Marshal(claims)
    if err != nil {
        return "", err
    }
    payload := base64.RawURLEncoding.EncodeToString(body)
    mac := hmac.New(sha256.New, m.secret)
    _, _ = mac.Write([]byte(payload))
    sig := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
    return payload + "." + sig, nil
}

func (m *Manager) parseClaims(tokenString string) (*tokenClaims, error) {
    parts := strings.Split(tokenString, ".")
    if len(parts) != 2 {
        return nil, fmt.Errorf("invalid token format")
    }
    payload, err := base64.RawURLEncoding.DecodeString(parts[0])
    if err != nil {
        return nil, err
    }
    mac := hmac.New(sha256.New, m.secret)
    _, _ = mac.Write([]byte(parts[0]))
    expected := mac.Sum(nil)
    sig, err := base64.RawURLEncoding.DecodeString(parts[1])
    if err != nil || !hmac.Equal(sig, expected) {
        return nil, fmt.Errorf("invalid token signature")
    }
    var claims tokenClaims
    if err := json.Unmarshal(payload, &claims); err != nil {
        return nil, err
    }
    return &claims, nil
}

func (m *Manager) ValidateToken(tokenString string) bool {
    return m.ValidatePairingToken(tokenString)
}

func generateRandomID() string {
    bytes := make([]byte, 16)
    rand.Read(bytes)
    return hex.EncodeToString(bytes)
}
