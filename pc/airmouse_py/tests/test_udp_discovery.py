"""
Comprehensive tests for UDP discovery server and challenge-response handling.
"""

import json
import socket
import time
import pytest

from network.protocol import DiscoverPayload, OfferPayload
from network.udp_discovery import UDPDiscoveryServer


def test_udp_discovery_discover_and_offer():
    port = 9883
    received_discoveries = []

    def on_disc(client_ip, client_port):
        received_discoveries.append((client_ip, client_port))

    server = UDPDiscoveryServer(
        port=port,
        server_name="Test AirMouse Server",
        tcp_port=9880,
        ws_port=9881,
        tls_enabled=True,
        on_discovered=on_disc,
    )
    server.start()
    time.sleep(0.1)

    try:
        client_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        client_sock.settimeout(2.0)

        # 1. Discover packet matching Protocol V2 schema
        test_nonce = "test-challenge-nonce-12345"
        now_ms = int(time.time() * 1000)
        discover_packet = {
            "v": 2,
            "type": "discover",
            "seq": 1,
            "ts": now_ms,
            "payload": {
                "client_name": "Android Test Client",
                "nonce": test_nonce,
            },
        }

        client_sock.sendto(json.dumps(discover_packet).encode("utf-8"), ("127.0.0.1", port))

        # Receive offer response
        data, addr = client_sock.recvfrom(4096)
        response = json.loads(data.decode("utf-8"))

        assert response["v"] == 2
        assert response["type"] == "offer"
        assert response["payload"]["server_name"] == "Test AirMouse Server"
        assert response["payload"]["tcp_port"] == 9880
        assert response["payload"]["ws_port"] == 9881
        assert response["payload"]["tls"] is True
        assert response["payload"]["echo_nonce"] == test_nonce

        # Callback check
        time.sleep(0.05)
        assert len(received_discoveries) >= 1

    finally:
        server.stop()
        client_sock.close()


def test_udp_discovery_rejects_invalid_json():
    port = 9884
    server = UDPDiscoveryServer(port=port)
    server.start()
    time.sleep(0.1)

    try:
        client_sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        client_sock.settimeout(0.5)

        # Malformed packet
        client_sock.sendto(b"not a valid json {", ("127.0.0.1", port))

        # Non-discover message
        wrong_packet = {"v": 2, "type": "ping", "seq": 1, "ts": int(time.time() * 1000), "payload": {}}
        client_sock.sendto(json.dumps(wrong_packet).encode("utf-8"), ("127.0.0.1", port))

        with pytest.raises(socket.timeout):
            client_sock.recvfrom(1024)
    finally:
        server.stop()
        client_sock.close()
