package device

import "crypto/subtle"

type Authenticator struct {
	tokens map[string]bool
}

func NewAuthenticator(tokens []string) *Authenticator {
	tokenMap := make(map[string]bool)
	for _, t := range tokens {
		tokenMap[t] = true
	}
	return &Authenticator{tokens: tokenMap}
}

func (a *Authenticator) Authenticate(token string) bool {
	for valid := range a.tokens {
		if subtle.ConstantTimeCompare([]byte(token), []byte(valid)) == 1 {
			return true
		}
	}
	return false
}