package tlsutil

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/sha256"
	"crypto/tls"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/hex"
	"encoding/pem"
	"fmt"
	"math/big"
	"net"
	"os"
	"path/filepath"
	"strings"
	"time"
)

// GenerateOrLoadCert loads an existing ECDSA certificate/key pair or generates a new one on first run.
func GenerateOrLoadCert(certPath, keyPath string, hostList []string) (tls.Certificate, error) {
	if certPath != "" && keyPath != "" {
		if _, err1 := os.Stat(certPath); err1 == nil {
			if _, err2 := os.Stat(keyPath); err2 == nil {
				return tls.LoadX509KeyPair(certPath, keyPath)
			}
		}
	}

	privKey, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		return tls.Certificate{}, fmt.Errorf("tlsutil: failed to generate ECDSA private key: %w", err)
	}

	notBefore := time.Now().Add(-1 * time.Hour)
	notAfter := notBefore.Add(365 * 24 * time.Hour)

	serialNumberLimit := new(big.Int).Lsh(big.NewInt(1), 128)
	serialNumber, err := rand.Int(rand.Reader, serialNumberLimit)
	if err != nil {
		return tls.Certificate{}, fmt.Errorf("tlsutil: failed to generate serial number: %w", err)
	}

	template := x509.Certificate{
		SerialNumber: serialNumber,
		Subject: pkix.Name{
			Organization: []string{"AirMouse Security"},
			CommonName:   "AirMouse Server",
		},
		NotBefore:             notBefore,
		NotAfter:              notAfter,
		KeyUsage:              x509.KeyUsageKeyEncipherment | x509.KeyUsageDigitalSignature,
		ExtKeyUsage:           []x509.ExtKeyUsage{x509.ExtKeyUsageServerAuth, x509.ExtKeyUsageClientAuth},
		BasicConstraintsValid: true,
	}

	template.IPAddresses = append(template.IPAddresses, net.ParseIP("127.0.0.1"), net.ParseIP("::1"))
	template.DNSNames = append(template.DNSNames, "localhost")

	for _, h := range hostList {
		if ip := net.ParseIP(h); ip != nil {
			template.IPAddresses = append(template.IPAddresses, ip)
		} else if h != "" && h != "0.0.0.0" {
			template.DNSNames = append(template.DNSNames, h)
		}
	}

	derBytes, err := x509.CreateCertificate(rand.Reader, &template, &template, &privKey.PublicKey, privKey)
	if err != nil {
		return tls.Certificate{}, fmt.Errorf("tlsutil: failed to generate certificate: %w", err)
	}

	certPEM := pem.EncodeToMemory(&pem.Block{Type: "CERTIFICATE", Bytes: derBytes})

	privBytes, err := x509.MarshalECPrivateKey(privKey)
	if err != nil {
		return tls.Certificate{}, fmt.Errorf("tlsutil: failed to marshal private key: %w", err)
	}
	keyPEM := pem.EncodeToMemory(&pem.Block{Type: "EC PRIVATE KEY", Bytes: privBytes})

	if certPath != "" && keyPath != "" {
		_ = os.MkdirAll(filepath.Dir(certPath), 0700)
		_ = os.MkdirAll(filepath.Dir(keyPath), 0700)
		_ = os.WriteFile(certPath, certPEM, 0600)
		_ = os.WriteFile(keyPath, keyPEM, 0600)
	}

	return tls.X509KeyPair(certPEM, keyPEM)
}

// GetServerTLSConfig returns a *tls.Config enforcing TLS 1.2+ with ECDSA P-256 certificate.
func GetServerTLSConfig(certPath, keyPath string, hostList []string) (*tls.Config, error) {
	cert, err := GenerateOrLoadCert(certPath, keyPath, hostList)
	if err != nil {
		return nil, fmt.Errorf("tlsutil: failed to get cert: %w", err)
	}

	return &tls.Config{
		Certificates: []tls.Certificate{cert},
		MinVersion:   tls.VersionTLS12,
		CurvePreferences: []tls.CurveID{
			tls.X25519,
			tls.CurveP256,
		},
		CipherSuites: []uint16{
			tls.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
			tls.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
			tls.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305,
		},
	}, nil
}

// GetClientTLSConfig returns a client *tls.Config for testing or internal dialers.
func GetClientTLSConfig(insecureSkipVerify bool) *tls.Config {
	return &tls.Config{
		InsecureSkipVerify: insecureSkipVerify,
		MinVersion:         tls.VersionTLS12,
	}
}

// GetCertFingerprint returns the SHA-256 fingerprint of the certificate in "SHA256:xx:xx:..." format.
func GetCertFingerprint(cert tls.Certificate) string {
	if len(cert.Certificate) == 0 {
		return ""
	}
	hash := sha256.Sum256(cert.Certificate[0])
	var hexParts []string
	for _, b := range hash {
		hexParts = append(hexParts, fmt.Sprintf("%02x", b))
	}
	return "SHA256:" + strings.Join(hexParts, ":")
}

// GetRawFingerprintHex returns the raw SHA-256 hex string.
func GetRawFingerprintHex(cert tls.Certificate) string {
	if len(cert.Certificate) == 0 {
		return ""
	}
	hash := sha256.Sum256(cert.Certificate[0])
	return hex.EncodeToString(hash[:])
}

// WrapListener wraps a net.Listener with TLS using the provided configuration.
func WrapListener(l net.Listener, tlsConfig *tls.Config) net.Listener {
	return tls.NewListener(l, tlsConfig)
}
