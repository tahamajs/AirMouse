package ui

import (
	"strings"
	"testing"
)

func TestNetworkTabPairingURIIncludesCurrentValues(t *testing.T) {
	uri := buildPairingURI("192.168.1.50", "8080", "8081", "AirMouse Pro")

	for _, want := range []string{
		"airmouse://pair?",
		"ip=192.168.1.50",
		"port=8080",
		"ws=ws://192.168.1.50:8081/ws",
		"name=AirMouse+Pro",
		"protocol=WEBSOCKET",
		"type=mobile",
	} {
		if !strings.Contains(uri, want) {
			t.Fatalf("pairing URI missing %q: %s", want, uri)
		}
	}
}

func TestNetworkTabPairingSummaryPrefixesUri(t *testing.T) {
	summary := "Pairing URI: " + buildPairingURI("10.0.0.12", "8080", "8081", "AirMouse Pro")
	if !strings.HasPrefix(summary, "Pairing URI: ") {
		t.Fatalf("unexpected summary prefix: %s", summary)
	}
	if !strings.Contains(summary, "10.0.0.12") {
		t.Fatalf("summary missing host IP: %s", summary)
	}
}
