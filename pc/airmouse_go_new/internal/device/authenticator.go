package device

import (
	"crypto/subtle"
	"sync"
	"time"
)

// ------------------------------------------------------------
//  Authenticator
// ------------------------------------------------------------

type Authenticator struct {
	tokens    map[string]TokenInfo
	mu        sync.RWMutex
	maxTokens int
	tokenTTL  time.Duration
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

// NewAuthenticator creates a new authenticator with optional initial tokens
func NewAuthenticator(tokens []string) *Authenticator {
	a := &Authenticator{
		tokens:    make(map[string]TokenInfo),
		maxTokens: 100,
		tokenTTL:  24 * time.Hour,
	}

	if tokens != nil {
		for _, token := range tokens {
			if token != "" {
				a.AddToken(token, "", "")
			}
		}
	}

	return a
}

// SetMaxTokens sets the maximum number of tokens allowed
func (a *Authenticator) SetMaxTokens(max int) {
	if max <= 0 {
		max = 100
	}
	a.mu.Lock()
	defer a.mu.Unlock()
	a.maxTokens = max
}

// SetTokenTTL sets the token time-to-live
func (a *Authenticator) SetTokenTTL(ttl time.Duration) {
	if ttl <= 0 {
		ttl = 24 * time.Hour
	}
	a.mu.Lock()
	defer a.mu.Unlock()
	a.tokenTTL = ttl
}

// Authenticate checks if a token is valid
func (a *Authenticator) Authenticate(token string) bool {
	if token == "" {
		return false
	}
	a.mu.RLock()
	defer a.mu.RUnlock()

	for valid, info := range a.tokens {
		if subtle.ConstantTimeCompare([]byte(token), []byte(valid)) == 1 {
			// Update usage (need to unlock and relock to write)
			a.mu.RUnlock()
			a.mu.Lock()
			info.LastUsed = time.Now()
			info.UsageCount++
			a.tokens[valid] = info
			a.mu.Unlock()
			a.mu.RLock()
			return true
		}
	}
	return false
}

// AuthenticateWithDevice authenticates a token and returns the associated device ID
func (a *Authenticator) AuthenticateWithDevice(token string) (bool, string) {
	if token == "" {
		return false, ""
	}
	a.mu.RLock()
	defer a.mu.RUnlock()

	for valid, info := range a.tokens {
		if subtle.ConstantTimeCompare([]byte(token), []byte(valid)) == 1 {
			// Check expiry
			if time.Now().After(info.ExpiresAt) {
				// Expired - remove it
				a.mu.RUnlock()
				a.mu.Lock()
				delete(a.tokens, valid)
				a.mu.Unlock()
				a.mu.RLock()
				return false, ""
			}

			// Update usage
			a.mu.RUnlock()
			a.mu.Lock()
			info.LastUsed = time.Now()
			info.UsageCount++
			a.tokens[valid] = info
			a.mu.Unlock()
			a.mu.RLock()
			return true, info.DeviceID
		}
	}
	return false, ""
}

// AddToken adds a new token
func (a *Authenticator) AddToken(token, deviceID, deviceName string) bool {
	if token == "" {
		return false
	}
	a.mu.Lock()
	defer a.mu.Unlock()

	if len(a.tokens) >= a.maxTokens {
		return false
	}

	// Check if token already exists
	if _, exists := a.tokens[token]; exists {
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

	// Use logDebug from manager.go (shared)
	logDebug("Token added for device: %s", deviceID)
	return true
}

// RemoveToken removes a token
func (a *Authenticator) RemoveToken(token string) bool {
	if token == "" {
		return false
	}
	a.mu.Lock()
	defer a.mu.Unlock()

	if _, exists := a.tokens[token]; exists {
		delete(a.tokens, token)
		logDebug("Token removed")
		return true
	}
	return false
}

// RevokeDeviceTokens revokes all tokens for a device
func (a *Authenticator) RevokeDeviceTokens(deviceID string) int {
	if deviceID == "" {
		return 0
	}
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
		logInfo("Revoked %d tokens for device: %s", removed, deviceID)
	}
	return removed
}

// ListTokens returns all tokens
func (a *Authenticator) ListTokens() []TokenInfo {
	a.mu.RLock()
	defer a.mu.RUnlock()

	tokens := make([]TokenInfo, 0, len(a.tokens))
	for _, info := range a.tokens {
		tokens = append(tokens, info)
	}
	return tokens
}

// CleanupExpiredTokens removes all expired tokens
func (a *Authenticator) CleanupExpiredTokens() int {
	a.mu.Lock()
	defer a.mu.Unlock()

	removed := 0
	now := time.Now()
	for token, info := range a.tokens {
		if now.After(info.ExpiresAt) {
			delete(a.tokens, token)
			removed++
		}
	}

	if removed > 0 {
		logDebug("Cleaned up %d expired tokens", removed)
	}
	return removed
}

// ValidateToken checks if a token is valid and not expired
func (a *Authenticator) ValidateToken(token string) bool {
	if token == "" {
		return false
	}
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

// GetTokenInfo returns information about a token
func (a *Authenticator) GetTokenInfo(token string) *TokenInfo {
	if token == "" {
		return nil
	}
	a.mu.RLock()
	defer a.mu.RUnlock()

	if info, exists := a.tokens[token]; exists {
		copy := info
		return &copy
	}
	return nil
}

// GetTokenCount returns the number of active tokens
func (a *Authenticator) GetTokenCount() int {
	a.mu.RLock()
	defer a.mu.RUnlock()
	return len(a.tokens)
}

// IsInitialized returns true if the authenticator is ready
func (a *Authenticator) IsInitialized() bool {
	return a.tokens != nil
}

// RefreshToken extends the expiry of a token
func (a *Authenticator) RefreshToken(token string, duration time.Duration) bool {
	if token == "" || duration <= 0 {
		return false
	}
	a.mu.Lock()
	defer a.mu.Unlock()

	info, exists := a.tokens[token]
	if !exists {
		return false
	}

	if time.Now().After(info.ExpiresAt) {
		return false
	}

	info.ExpiresAt = time.Now().Add(duration)
	info.LastUsed = time.Now()
	a.tokens[token] = info
	return true
}

// ClearAllTokens removes all tokens
func (a *Authenticator) ClearAllTokens() int {
	a.mu.Lock()
	defer a.mu.Unlock()

	count := len(a.tokens)
	a.tokens = make(map[string]TokenInfo)
	logInfo("Cleared all %d tokens", count)
	return count
}