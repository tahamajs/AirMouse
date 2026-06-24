package auth

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strings"
	"sync"
	"time"
)

// Manager handles authentication tokens and pairing.
type Manager struct {
	secret       []byte
	pendingPairs map[string]time.Time
	tokens       map[string]*TokenInfo
	mu           sync.RWMutex
	maxTokens    int
	tokenTTL     time.Duration
	cleanupTick  *time.Ticker
	stopCleanup  chan struct{}
}

// TokenInfo stores information about an issued token.
type TokenInfo struct {
	Token      string    `json:"token"`
	DeviceID   string    `json:"device_id"`
	DeviceName string    `json:"device_name"`
	CreatedAt  time.Time `json:"created_at"`
	ExpiresAt  time.Time `json:"expires_at"`
	LastUsed   time.Time `json:"last_used"`
	UsageCount int       `json:"usage_count"`
	IsRevoked  bool      `json:"is_revoked"`
}

// tokenClaims represents the JWT‑style claims.
type tokenClaims struct {
	Exp        int64  `json:"exp"`
	Iat        int64  `json:"iat"`
	Type       string `json:"type"` // "pairing" or "auth"
	JTI        string `json:"jti"`  // unique token ID
	DeviceID   string `json:"device_id,omitempty"`
	DeviceName string `json:"device_name,omitempty"`
	Version    int    `json:"ver,omitempty"`
}

// NewManager creates a new authentication manager.
func NewManager(secret string) *Manager {
	secretBytes := []byte(secret)
	if len(secretBytes) < 32 {
		// Pad or extend secret to at least 32 bytes
		pad := []byte("airmouse-fallback-secret-key-2025")
		secretBytes = append(secretBytes, pad...)
		if len(secretBytes) > 32 {
			secretBytes = secretBytes[:32]
		}
	}

	m := &Manager{
		secret:       secretBytes,
		pendingPairs: make(map[string]time.Time),
		tokens:       make(map[string]*TokenInfo),
		maxTokens:    100,
		tokenTTL:     24 * time.Hour,
		stopCleanup:  make(chan struct{}),
	}

	// Start background cleanup goroutine
	go m.cleanupLoop()

	return m
}

// SetMaxTokens sets the maximum number of active tokens.
func (m *Manager) SetMaxTokens(max int) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.maxTokens = max
}

// SetTokenTTL sets the lifetime of auth tokens.
func (m *Manager) SetTokenTTL(ttl time.Duration) {
	m.mu.Lock()
	defer m.mu.Unlock()
	m.tokenTTL = ttl
}

// GeneratePairingToken creates a short‑lived pairing token.
func (m *Manager) GeneratePairingToken() (string, error) {
	claims := tokenClaims{
		Exp:     time.Now().Add(5 * time.Minute).Unix(),
		Iat:     time.Now().Unix(),
		Type:    "pairing",
		JTI:     generateRandomID(),
		Version: 1,
	}

	tokenString, err := m.signClaims(claims)
	if err != nil {
		return "", fmt.Errorf("failed to sign pairing token: %w", err)
	}

	m.mu.Lock()
	m.pendingPairs[tokenString] = time.Now().Add(5 * time.Minute)
	m.mu.Unlock()

	return tokenString, nil
}

// ValidatePairingToken validates and consumes a pairing token.
func (m *Manager) ValidatePairingToken(tokenString string) bool {
	claims, err := m.parseClaims(tokenString)
	if err != nil || claims.Type != "pairing" {
		return false
	}

	m.mu.Lock()
	defer m.mu.Unlock()

	expiry, exists := m.pendingPairs[tokenString]
	if !exists || time.Now().After(expiry) || time.Now().Unix() > claims.Exp {
		delete(m.pendingPairs, tokenString)
		return false
	}

	// Consume the token (one‑time use)
	delete(m.pendingPairs, tokenString)
	return true
}

// GenerateAuthToken creates a long‑lived authentication token for a device.
func (m *Manager) GenerateAuthToken(deviceID, deviceName string) (string, error) {
	m.mu.Lock()
	defer m.mu.Unlock()

	if len(m.tokens) >= m.maxTokens {
		m.cleanupExpiredTokens()
		if len(m.tokens) >= m.maxTokens {
			return "", fmt.Errorf("max tokens (%d) reached", m.maxTokens)
		}
	}

	tokenID := generateRandomID()
	now := time.Now()
	expiresAt := now.Add(m.tokenTTL)

	claims := tokenClaims{
		Exp:        expiresAt.Unix(),
		Iat:        now.Unix(),
		Type:       "auth",
		JTI:        tokenID,
		DeviceID:   deviceID,
		DeviceName: deviceName,
		Version:    1,
	}

	tokenString, err := m.signClaims(claims)
	if err != nil {
		return "", fmt.Errorf("failed to sign auth token: %w", err)
	}

	m.tokens[tokenString] = &TokenInfo{
		Token:      tokenString,
		DeviceID:   deviceID,
		DeviceName: deviceName,
		CreatedAt:  now,
		ExpiresAt:  expiresAt,
		LastUsed:   now,
		UsageCount: 0,
		IsRevoked:  false,
	}

	return tokenString, nil
}

// ValidateAuthToken validates an authentication token and returns device info.
func (m *Manager) ValidateAuthToken(tokenString string) (valid bool, deviceID string, deviceName string) {
	claims, err := m.parseClaims(tokenString)
	if err != nil {
		return false, "", ""
	}

	if claims.Type != "auth" {
		return false, "", ""
	}

	if time.Now().Unix() > claims.Exp {
		return false, "", ""
	}

	m.mu.Lock()
	defer m.mu.Unlock()

	info, exists := m.tokens[tokenString]
	if !exists || info.IsRevoked {
		return false, "", ""
	}

	info.LastUsed = time.Now()
	info.UsageCount++

	return true, claims.DeviceID, claims.DeviceName
}

// ValidateToken is a convenience method that tries pairing then auth.
func (m *Manager) ValidateToken(tokenString string) bool {
	if m.ValidatePairingToken(tokenString) {
		return true
	}
	valid, _, _ := m.ValidateAuthToken(tokenString)
	return valid
}

// ValidateTokenWithInfo validates a token and returns full info.
func (m *Manager) ValidateTokenWithInfo(tokenString string) (*TokenInfo, error) {
	// Try pairing first
	if m.ValidatePairingToken(tokenString) {
		return &TokenInfo{
			Token:      tokenString,
			CreatedAt:  time.Now(),
			ExpiresAt:  time.Now().Add(5 * time.Minute),
			LastUsed:   time.Now(),
			UsageCount: 1,
			IsRevoked:  false,
		}, nil
	}

	// Try auth
	m.mu.RLock()
	info, exists := m.tokens[tokenString]
	m.mu.RUnlock()

	if !exists || info.IsRevoked {
		return nil, fmt.Errorf("invalid or revoked token")
	}

	if time.Now().After(info.ExpiresAt) {
		return nil, fmt.Errorf("token expired")
	}

	m.mu.Lock()
	info.LastUsed = time.Now()
	info.UsageCount++
	m.mu.Unlock()

	return info, nil
}

// RevokeToken revokes a single token.
func (m *Manager) RevokeToken(tokenString string) bool {
	m.mu.Lock()
	defer m.mu.Unlock()

	if info, exists := m.tokens[tokenString]; exists {
		info.IsRevoked = true
		delete(m.tokens, tokenString)
		return true
	}
	return false
}

// RevokeDeviceTokens revokes all tokens for a device.
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

// RefreshToken extends the expiry of a valid token.
func (m *Manager) RefreshToken(tokenString string) (string, error) {
	m.mu.Lock()
	defer m.mu.Unlock()

	info, exists := m.tokens[tokenString]
	if !exists || info.IsRevoked {
		return "", fmt.Errorf("token not found or revoked")
	}

	if time.Now().After(info.ExpiresAt) {
		return "", fmt.Errorf("token expired")
	}

	// Generate new token
	newTTL := m.tokenTTL
	if newTTL <= 0 {
		newTTL = 24 * time.Hour
	}
	newExpiry := time.Now().Add(newTTL)

	claims := tokenClaims{
		Exp:        newExpiry.Unix(),
		Iat:        time.Now().Unix(),
		Type:       "auth",
		JTI:        generateRandomID(),
		DeviceID:   info.DeviceID,
		DeviceName: info.DeviceName,
		Version:    1,
	}

	newToken, err := m.signClaims(claims)
	if err != nil {
		return "", fmt.Errorf("failed to sign refreshed token: %w", err)
	}

	// Remove old token, add new one
	delete(m.tokens, tokenString)
	m.tokens[newToken] = &TokenInfo{
		Token:      newToken,
		DeviceID:   info.DeviceID,
		DeviceName: info.DeviceName,
		CreatedAt:  time.Now(),
		ExpiresAt:  newExpiry,
		LastUsed:   time.Now(),
		UsageCount: info.UsageCount + 1,
		IsRevoked:  false,
	}

	return newToken, nil
}

// ListTokens returns all active tokens.
func (m *Manager) ListTokens() []*TokenInfo {
	m.mu.RLock()
	defer m.mu.RUnlock()

	tokens := make([]*TokenInfo, 0, len(m.tokens))
	for _, info := range m.tokens {
		if !info.IsRevoked {
			tokens = append(tokens, info)
		}
	}
	return tokens
}

// GetTokenInfo returns information about a specific token.
func (m *Manager) GetTokenInfo(tokenString string) (*TokenInfo, bool) {
	m.mu.RLock()
	defer m.mu.RUnlock()
	info, exists := m.tokens[tokenString]
	return info, exists
}

// Cleanup removes expired tokens and returns the count removed.
func (m *Manager) Cleanup() int {
	m.mu.Lock()
	defer m.mu.Unlock()
	before := len(m.tokens) + len(m.pendingPairs)
	m.cleanupExpiredTokens()
	after := len(m.tokens) + len(m.pendingPairs)
	return before - after
}

// cleanupLoop runs in the background to automatically remove expired tokens.
func (m *Manager) cleanupLoop() {
	ticker := time.NewTicker(5 * time.Minute)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			m.Cleanup()
		case <-m.stopCleanup:
			return
		}
	}
}

// StopCleanup stops the background cleanup goroutine.
func (m *Manager) StopCleanup() {
	close(m.stopCleanup)
}

// GetPairingQRData generates a pairing URL with a token.
func (m *Manager) GetPairingQRData(wsURL string) (string, error) {
	token, err := m.GeneratePairingToken()
	if err != nil {
		return "", err
	}
	return fmt.Sprintf("airmouse://pair?token=%s&ws=%s&protocol=WEBSOCKET", token, wsURL), nil
}

// GetPairingData returns pairing data as a map (for API responses).
func (m *Manager) GetPairingData(wsURL string) (map[string]interface{}, error) {
	token, err := m.GeneratePairingToken()
	if err != nil {
		return nil, err
	}
	return map[string]interface{}{
		"token":       token,
		"ws_url":      wsURL,
		"protocol":    "WEBSOCKET",
		"expires_in":  300, // 5 minutes in seconds
		"pairing_url": fmt.Sprintf("airmouse://pair?token=%s&ws=%s&protocol=WEBSOCKET", token, wsURL),
	}, nil
}

// --- Internal methods ---

// cleanupExpiredTokens removes expired tokens (must be called with lock held).
func (m *Manager) cleanupExpiredTokens() {
	now := time.Now()
	for token, info := range m.tokens {
		if now.After(info.ExpiresAt) || info.IsRevoked {
			delete(m.tokens, token)
		}
	}
	for token, expiry := range m.pendingPairs {
		if now.After(expiry) {
			delete(m.pendingPairs, token)
		}
	}
}

// signClaims creates a signed token from claims.
func (m *Manager) signClaims(claims tokenClaims) (string, error) {
	body, err := json.Marshal(claims)
	if err != nil {
		return "", err
	}
	payload := base64.RawURLEncoding.EncodeToString(body)

	mac := hmac.New(sha256.New, m.secret)
	if _, err := mac.Write([]byte(payload)); err != nil {
		return "", err
	}
	sig := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))

	return payload + "." + sig, nil
}

// parseClaims parses and validates a signed token.
func (m *Manager) parseClaims(tokenString string) (*tokenClaims, error) {
	parts := strings.Split(tokenString, ".")
	if len(parts) != 2 {
		return nil, fmt.Errorf("invalid token format")
	}

	payload, err := base64.RawURLEncoding.DecodeString(parts[0])
	if err != nil {
		return nil, fmt.Errorf("invalid payload: %w", err)
	}

	mac := hmac.New(sha256.New, m.secret)
	if _, err := mac.Write([]byte(parts[0])); err != nil {
		return nil, err
	}
	expected := mac.Sum(nil)

	sig, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return nil, fmt.Errorf("invalid signature: %w", err)
	}

	if !hmac.Equal(sig, expected) {
		return nil, fmt.Errorf("invalid signature")
	}

	var claims tokenClaims
	if err := json.Unmarshal(payload, &claims); err != nil {
		return nil, fmt.Errorf("invalid claims: %w", err)
	}
	return &claims, nil
}

// generateRandomID creates a cryptographically secure random hex string.
func generateRandomID() string {
	bytes := make([]byte, 16)
	if _, err := rand.Read(bytes); err != nil {
		// Fallback: use timestamp + random (less secure but safe)
		return fmt.Sprintf("%x", time.Now().UnixNano())
	}
	return hex.EncodeToString(bytes)
}

// --- Helper methods for external use ---

// IsTokenExpired checks if a token is expired without validation.
func (m *Manager) IsTokenExpired(tokenString string) bool {
	claims, err := m.parseClaims(tokenString)
	if err != nil {
		return true
	}
	return time.Now().Unix() > claims.Exp
}

// GetTokenType returns the type of token ("pairing" or "auth").
func (m *Manager) GetTokenType(tokenString string) (string, error) {
	claims, err := m.parseClaims(tokenString)
	if err != nil {
		return "", err
	}
	return claims.Type, nil
}

// HasPendingPairing checks if a pairing token exists (without consuming it).
func (m *Manager) HasPendingPairing(tokenString string) bool {
	m.mu.RLock()
	defer m.mu.RUnlock()
	expiry, exists := m.pendingPairs[tokenString]
	return exists && time.Now().Before(expiry)
}

// PurgeAll removes all tokens (use with caution).
func (m *Manager) PurgeAll() int {
	m.mu.Lock()
	defer m.mu.Unlock()
	count := len(m.tokens) + len(m.pendingPairs)
	m.tokens = make(map[string]*TokenInfo)
	m.pendingPairs = make(map[string]time.Time)
	return count
}