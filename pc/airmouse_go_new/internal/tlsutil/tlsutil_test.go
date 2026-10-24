package tlsutil

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/tls"
	"crypto/x509"
	"net"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

func TestGenerateOrLoadCert(t *testing.T) {
	tempDir, err := os.MkdirTemp("", "airmouse_tls_test_*")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	certPath := filepath.Join(tempDir, "server.crt")
	keyPath := filepath.Join(tempDir, "server.key")

	// 1. First run generates new certificate
	cert, err := GenerateOrLoadCert(certPath, keyPath, []string{"127.0.0.1", "custom.local"})
	if err != nil {
		t.Fatalf("GenerateOrLoadCert failed: %v", err)
	}

	if len(cert.Certificate) == 0 {
		t.Fatalf("Expected non-empty certificate")
	}

	x509Cert, err := x509.ParseCertificate(cert.Certificate[0])
	if err != nil {
		t.Fatalf("Failed to parse generated x509 certificate: %v", err)
	}

	// Verify key type is ECDSA P-256
	pubKey, ok := x509Cert.PublicKey.(*ecdsa.PublicKey)
	if !ok {
		t.Fatalf("Expected ECDSA public key, got %T", x509Cert.PublicKey)
	}
	if pubKey.Curve != elliptic.P256() {
		t.Fatalf("Expected P-256 curve, got %v", pubKey.Curve.Params().Name)
	}

	// Verify Subject & SAN
	if x509Cert.Subject.CommonName != "AirMouse Server" {
		t.Errorf("Expected CommonName 'AirMouse Server', got %q", x509Cert.Subject.CommonName)
	}

	hasCustomDNS := false
	for _, dns := range x509Cert.DNSNames {
		if dns == "custom.local" {
			hasCustomDNS = true
			break
		}
	}
	if !hasCustomDNS {
		t.Errorf("Expected DNS SAN 'custom.local' in %v", x509Cert.DNSNames)
	}

	// 2. Second run loads existing certificate from disk
	loadedCert, err := GenerateOrLoadCert(certPath, keyPath, nil)
	if err != nil {
		t.Fatalf("Failed to load existing cert from disk: %v", err)
	}
	if string(loadedCert.Certificate[0]) != string(cert.Certificate[0]) {
		t.Errorf("Loaded certificate bytes do not match originally generated certificate")
	}
}

func TestGetServerTLSConfig(t *testing.T) {
	tempDir, err := os.MkdirTemp("", "airmouse_tls_test_*")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	certPath := filepath.Join(tempDir, "srv.crt")
	keyPath := filepath.Join(tempDir, "srv.key")

	tlsCfg, err := GetServerTLSConfig(certPath, keyPath, []string{"localhost"})
	if err != nil {
		t.Fatalf("GetServerTLSConfig failed: %v", err)
	}

	if tlsCfg.MinVersion < tls.VersionTLS12 {
		t.Errorf("Expected MinVersion >= TLS 1.2, got 0x%04x", tlsCfg.MinVersion)
	}
	if len(tlsCfg.Certificates) == 0 {
		t.Errorf("Expected at least 1 certificate in tlsConfig")
	}
}

func TestGetCertFingerprint(t *testing.T) {
	tempDir, err := os.MkdirTemp("", "airmouse_tls_test_*")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	certPath := filepath.Join(tempDir, "fp.crt")
	keyPath := filepath.Join(tempDir, "fp.key")

	cert, err := GenerateOrLoadCert(certPath, keyPath, nil)
	if err != nil {
		t.Fatalf("Failed to generate cert: %v", err)
	}

	fp := GetCertFingerprint(cert)
	if !strings.HasPrefix(fp, "SHA256:") {
		t.Errorf("Expected fingerprint to start with 'SHA256:', got %q", fp)
	}

	// Expect 32 hex bytes separated by colons => "SHA256:" + (32 * 2 + 31) chars = 7 + 95 = 102 chars
	if len(fp) != 102 {
		t.Errorf("Expected fingerprint length 102, got %d (%s)", len(fp), fp)
	}
}

func TestTLSEnforcementRejectsCleartext(t *testing.T) {
	tempDir, err := os.MkdirTemp("", "airmouse_tls_test_*")
	if err != nil {
		t.Fatalf("Failed to create temp dir: %v", err)
	}
	defer os.RemoveAll(tempDir)

	certPath := filepath.Join(tempDir, "enf.crt")
	keyPath := filepath.Join(tempDir, "enf.key")

	tlsCfg, err := GetServerTLSConfig(certPath, keyPath, []string{"127.0.0.1"})
	if err != nil {
		t.Fatalf("Failed to get TLS config: %v", err)
	}

	listener, err := tls.Listen("tcp", "127.0.0.1:0", tlsCfg)
	if err != nil {
		t.Fatalf("Failed to listen on TLS: %v", err)
	}
	defer listener.Close()

	addr := listener.Addr().String()

	// Server accept goroutine
	acceptedChan := make(chan bool, 1)
	go func() {
		conn, err := listener.Accept()
		if err != nil {
			return
		}
		defer conn.Close()
		// Attempt reading or handshake
		buf := make([]byte, 128)
		_ = conn.SetReadDeadline(time.Now().Add(500 * time.Millisecond))
		_, readErr := conn.Read(buf)
		if readErr != nil {
			// Handshake failure or read failure
			acceptedChan <- false
			return
		}
		acceptedChan <- true
	}()

	// 1. Plain TCP (cleartext) connection MUST fail TLS handshake
	clearConn, err := net.DialTimeout("tcp", addr, 1*time.Second)
	if err != nil {
		t.Fatalf("Cleartext net.Dial failed: %v", err)
	}
	// Send plain bytes
	_, _ = clearConn.Write([]byte("GET / HTTP/1.1\r\n\r\n"))
	success := <-acceptedChan
	if success {
		t.Errorf("Expected cleartext connection to fail TLS handshake on server, but server read succeeded")
	}
	_ = clearConn.Close()

	// 2. TLS client connection MUST succeed
	tlsClientSuccess := make(chan bool, 1)
	go func() {
		conn, err := listener.Accept()
		if err != nil {
			tlsClientSuccess <- false
			return
		}
		defer conn.Close()
		buf := make([]byte, 16)
		_ = conn.SetReadDeadline(time.Now().Add(2 * time.Second))
		n, err := conn.Read(buf)
		if err == nil && string(buf[:n]) == "ping" {
			tlsClientSuccess <- true
		} else {
			tlsClientSuccess <- false
		}
	}()

	clientTLS := GetClientTLSConfig(true)
	tlsConn, err := tls.DialWithDialer(&net.Dialer{Timeout: 2 * time.Second}, "tcp", addr, clientTLS)
	if err != nil {
		t.Fatalf("TLS client dial failed: %v", err)
	}
	defer tlsConn.Close()

	_, err = tlsConn.Write([]byte("ping"))
	if err != nil {
		t.Fatalf("TLS client write failed: %v", err)
	}

	select {
	case ok := <-tlsClientSuccess:
		if !ok {
			t.Errorf("Expected TLS client handshake and read to succeed")
		}
	case <-time.After(3 * time.Second):
		t.Errorf("Timed out waiting for TLS client handshake")
	}
}
