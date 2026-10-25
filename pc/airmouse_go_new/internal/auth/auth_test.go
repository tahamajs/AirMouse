package auth

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"strings"
	"testing"
	"time"
)

func TestHMACSHA256TokenGenerationAndValidation(t *testing.T) {
	secret := "test-secret-key-that-is-at-least-32-bytes-long!"
	mgr := NewManager(secret)
	defer mgr.StopCleanup()

	// Generate Auth Token
	deviceID := "device-uuid-1234"
	deviceName := "Pixel 8 Pro"
	token, err := mgr.GenerateAuthToken(deviceID, deviceName)
	if err != nil {
		t.Fatalf("GenerateAuthToken failed: %v", err)
	}

	if token == "" {
		t.Fatalf("Expected non-empty token")
	}

	// Validate token format: payload.signature
	parts := strings.Split(token, ".")
	if len(parts) != 2 {
		t.Fatalf("Expected token to have format payload.signature, got %d parts", len(parts))
	}

	// Verify HMAC-SHA256 signature manually
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(parts[0]))
	expectedSig := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
	if parts[1] != expectedSig {
		t.Errorf("Signature mismatch: expected %s, got %s", expectedSig, parts[1])
	}

	// Validate with manager
	valid, retDevID, retDevName := mgr.ValidateAuthToken(token)
	if !valid {
		t.Errorf("Expected token to be valid")
	}
	if retDevID != deviceID {
		t.Errorf("Expected device ID %q, got %q", deviceID, retDevID)
	}
	if retDevName != deviceName {
		t.Errorf("Expected device name %q, got %q", deviceName, retDevName)
	}

	// Convenience ValidateToken method
	if !mgr.ValidateToken(token) {
		t.Errorf("Expected ValidateToken to return true")
	}
}

func TestTamperedTokenRejection(t *testing.T) {
	secret := "test-secret-key-that-is-at-least-32-bytes-long!"
	mgr := NewManager(secret)
	defer mgr.StopCleanup()

	token, err := mgr.GenerateAuthToken("dev-1", "Phone")
	if err != nil {
		t.Fatalf("GenerateAuthToken failed: %v", err)
	}

	parts := strings.Split(token, ".")

	// 1. Tamper with payload
	tamperedPayload := parts[0] + "A"
	tamperedToken1 := tamperedPayload + "." + parts[1]
	if mgr.ValidateToken(tamperedToken1) {
		t.Errorf("Expected validation to fail for tampered payload")
	}

	// 2. Tamper with signature
	tamperedSig := parts[1]
	if strings.HasSuffix(tamperedSig, "a") {
		tamperedSig = tamperedSig[:len(tamperedSig)-1] + "b"
	} else {
		tamperedSig = tamperedSig[:len(tamperedSig)-1] + "a"
	}
	tamperedToken2 := parts[0] + "." + tamperedSig
	if mgr.ValidateToken(tamperedToken2) {
		t.Errorf("Expected validation to fail for tampered signature")
	}

	// 3. Different secret
	otherMgr := NewManager("a-different-secret-key-that-is-32-bytes!!")
	defer otherMgr.StopCleanup()
	if otherMgr.ValidateToken(token) {
		t.Errorf("Expected validation to fail with a different manager secret")
	}
}

func TestPairingTokenOneTimeUse(t *testing.T) {
	secret := "test-secret-key-that-is-at-least-32-bytes-long!"
	mgr := NewManager(secret)
	defer mgr.StopCleanup()

	pairToken, err := mgr.GeneratePairingToken()
	if err != nil {
		t.Fatalf("GeneratePairingToken failed: %v", err)
	}

	// First validation must succeed
	if !mgr.ValidatePairingToken(pairToken) {
		t.Errorf("Expected first ValidatePairingToken to succeed")
	}

	// Second validation must fail (one-time use / consumed)
	if mgr.ValidatePairingToken(pairToken) {
		t.Errorf("Expected second ValidatePairingToken to fail (consumed)")
	}
}

func TestExpiredTokenRejection(t *testing.T) {
	secret := "test-secret-key-that-is-at-least-32-bytes-long!"
	mgr := NewManager(secret)
	defer mgr.StopCleanup()

	// Set TTL to 1ms
	mgr.SetTokenTTL(1 * time.Millisecond)
	token, err := mgr.GenerateAuthToken("dev-fast-expire", "Phone")
	if err != nil {
		t.Fatalf("GenerateAuthToken failed: %v", err)
	}

	time.Sleep(10 * time.Millisecond)

	valid, _, _ := mgr.ValidateAuthToken(token)
	if valid {
		t.Errorf("Expected expired token to be invalid")
	}
}

func TestRevokeToken(t *testing.T) {
	secret := "test-secret-key-that-is-at-least-32-bytes-long!"
	mgr := NewManager(secret)
	defer mgr.StopCleanup()

	token, err := mgr.GenerateAuthToken("dev-to-revoke", "Phone")
	if err != nil {
		t.Fatalf("GenerateAuthToken failed: %v", err)
	}

	if !mgr.ValidateToken(token) {
		t.Fatalf("Token should initially be valid")
	}

	if !mgr.RevokeToken(token) {
		t.Fatalf("RevokeToken failed")
	}

	if mgr.ValidateToken(token) {
		t.Errorf("Revoked token must not be valid")
	}
}

func TestFutureTokenRejection(t *testing.T) {
	secret := "test-secret-key-that-is-at-least-32-bytes-long!"
	mgr := NewManager(secret)
	defer mgr.StopCleanup()

	// Forge claims with Iat 10 minutes in the future
	futureClaims := tokenClaims{
		Exp:        time.Now().Add(24 * time.Hour).Unix(),
		Iat:        time.Now().Add(10 * time.Minute).Unix(),
		Type:       "auth",
		JTI:        "future-token-id",
		DeviceID:   "future-dev",
		DeviceName: "Future Phone",
		Version:    1,
	}

	futureToken, err := mgr.signClaims(futureClaims)
	if err != nil {
		t.Fatalf("signClaims failed: %v", err)
	}

	valid, _, _ := mgr.ValidateAuthToken(futureToken)
	if valid {
		t.Errorf("Expected token with future iat to be rejected")
	}
}

func TestMalformedTokens(t *testing.T) {
	secret := "test-secret-key-that-is-at-least-32-bytes-long!"
	mgr := NewManager(secret)
	defer mgr.StopCleanup()

	testCases := []string{
		"",
		"nodot",
		"too.many.dots.here",
		"invalidbase64!@#.invalidsig!@#",
		"eyJhbGciOiJIUzI1NiJ9.dGVzdA", // signature length mismatch
	}

	for _, tc := range testCases {
		if mgr.ValidateToken(tc) {
			t.Errorf("Expected malformed token %q to be rejected", tc)
		}
	}
}
