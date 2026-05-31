package device

import "crypto/subtle"

// Authenticator provides simple token‑based authentication.
type Authenticator struct {
	tokens map[string]bool
}

// NewAuthenticator creates an authenticator with the given list of tokens.
func NewAuthenticator(tokens []string) *Authenticator {
	tokenMap := make(map[string]bool, len(tokens))
	for _, t := range tokens {
		tokenMap[t] = true
	}
	return &Authenticator{tokens: tokenMap}
}

// Authenticate checks whether the provided token is valid.
// It uses constant‑time comparison to prevent timing attacks.
func (a *Authenticator) Authenticate(token string) bool {
	for valid := range a.tokens {
		if subtle.ConstantTimeCompare([]byte(token), []byte(valid)) == 1 {
			return true
		}
	}
	return false
}