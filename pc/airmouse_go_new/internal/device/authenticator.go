package device

import (
    "crypto/subtle"
    "sync"
    "time"

    "airmouse-go/internal/infra/logger"
)

type Authenticator struct {
    tokens      map[string]TokenInfo
    mu          sync.RWMutex
    maxTokens   int
    tokenTTL    time.Duration
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

func NewAuthenticator(tokens []string) *Authenticator {
    a := &Authenticator{
        tokens:    make(map[string]TokenInfo),
        maxTokens: 100,
        tokenTTL:  24 * time.Hour,
    }
    
    for _, token := range tokens {
        a.AddToken(token, "", "")
    }
    
    return a
}

func (a *Authenticator) SetMaxTokens(max int) {
    a.maxTokens = max
}

func (a *Authenticator) SetTokenTTL(ttl time.Duration) {
    a.tokenTTL = ttl
}

func (a *Authenticator) Authenticate(token string) bool {
    a.mu.RLock()
    defer a.mu.RUnlock()

    for valid, info := range a.tokens {
        if subtle.ConstantTimeCompare([]byte(token), []byte(valid)) == 1 {
            // Update usage
            info.LastUsed = time.Now()
            info.UsageCount++
            a.tokens[valid] = info
            return true
        }
    }
    return false
}

func (a *Authenticator) AuthenticateWithDevice(token string) (bool, string) {
    a.mu.RLock()
    defer a.mu.RUnlock()

    for valid, info := range a.tokens {
        if subtle.ConstantTimeCompare([]byte(token), []byte(valid)) == 1 {
            // Check expiry
            if time.Now().After(info.ExpiresAt) {
                delete(a.tokens, valid)
                return false, ""
            }
            
            info.LastUsed = time.Now()
            info.UsageCount++
            a.tokens[valid] = info
            return true, info.DeviceID
        }
    }
    return false, ""
}

func (a *Authenticator) AddToken(token, deviceID, deviceName string) bool {
    a.mu.Lock()
    defer a.mu.Unlock()

    if len(a.tokens) >= a.maxTokens {
        return false
    }

    a.tokens[token] = TokenInfo{
        Token:      token,
        DeviceID:   deviceID,
        DeviceName: deviceName,
        CreatedAt:  time.Now(),
        ExpiresAt:  time.Now().Add(a.tokenTTL),
        LastUsed:   time.Now(),
        UsageCount: 0,
    }
    
    logger.Debug("Token added for device: %s", deviceID)
    return true
}

func (a *Authenticator) RemoveToken(token string) bool {
    a.mu.Lock()
    defer a.mu.Unlock()

    if _, exists := a.tokens[token]; exists {
        delete(a.tokens, token)
        logger.Debug("Token removed")
        return true
    }
    return false
}

func (a *Authenticator) RevokeDeviceTokens(deviceID string) int {
    a.mu.Lock()
    defer a.mu.Unlock()

    removed := 0
    for token, info := range a.tokens {
        if info.DeviceID == deviceID {
            delete(a.tokens, token)
            removed++
        }
    }
    
    if removed > 0 {
        logger.Info("Revoked %d tokens for device: %s", removed, deviceID)
    }
    return removed
}

func (a *Authenticator) ListTokens() []TokenInfo {
    a.mu.RLock()
    defer a.mu.RUnlock()

    tokens := make([]TokenInfo, 0, len(a.tokens))
    for _, info := range a.tokens {
        tokens = append(tokens, info)
    }
    return tokens
}

func (a *Authenticator) CleanupExpiredTokens() int {
    a.mu.Lock()
    defer a.mu.Unlock()

    removed := 0
    for token, info := range a.tokens {
        if time.Now().After(info.ExpiresAt) {
            delete(a.tokens, token)
            removed++
        }
    }
    
    if removed > 0 {
        logger.Debug("Cleaned up %d expired tokens", removed)
    }
    return removed
}

func (a *Authenticator) ValidateToken(token string) bool {
    a.mu.RLock()
    defer a.mu.RUnlock()

    info, exists := a.tokens[token]
    if !exists {
        return false
    }
    
    if time.Now().After(info.ExpiresAt) {
        return false
    }
    
    return true
}

func (a *Authenticator) GetTokenInfo(token string) *TokenInfo {
    a.mu.RLock()
    defer a.mu.RUnlock()

    if info, exists := a.tokens[token]; exists {
        copy := info
        return &copy
    }
    return nil
}