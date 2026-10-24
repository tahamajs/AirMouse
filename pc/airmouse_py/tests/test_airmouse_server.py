"""
Unit and integration tests for AirMouse public API.
"""

import asyncio
import json
import socket
import time
import pytest

from airmouse import (
    AirMouseServer,
    AuthManager,
    OneEuroFilter,
    ProtocolParser,
    UDPJitterBuffer,
)
from network.protocol import MovePayload


def test_public_api_exports():
    """Verify that all core classes are accessible from top-level airmouse package."""
    assert AirMouseServer is not None
    assert OneEuroFilter is not None
    assert UDPJitterBuffer is not None
    assert AuthManager is not None
    assert ProtocolParser is not None


def test_protocol_parser_android_spec():
    """Verify ProtocolParser strictly validates Android AirMouseProtocolMessages.kt structures."""
    parser = ProtocolParser(replay_window_ms=5000)
    now_ms = int(time.time() * 1000)

    # Move message matching Android AirMouseProtocolMessages.move(12.5f, -3.2f, 0.016f)
    move_envelope = {
        "v": 2,
        "type": "move",
        "seq": 100,
        "ts": now_ms,
        "payload": {
            "dx": 12.5,
            "dy": -3.2,
            "dt": 0.016,
        },
    }
    msg_type, payload, req_id, seq = parser.parse_message(
        json.dumps(move_envelope), "android_session"
    )
    assert msg_type == "move"
    assert isinstance(payload, MovePayload)
    assert payload.dx == 12.5
    assert payload.dy == -3.2
    assert payload.dt == 0.016
    assert seq == 100

    # Reliable click matching Android AirMouseProtocolMessages.reliableClick("click", "req-1", "left")
    click_envelope = {
        "v": 2,
        "type": "click",
        "id": "req-1",
        "seq": 101,
        "ts": now_ms + 10,
        "payload": {
            "button": "left",
        },
    }
    msg_type, payload, req_id, seq = parser.parse_message(
        json.dumps(click_envelope), "android_session"
    )
    assert msg_type == "click"
    assert req_id == "req-1"
    assert seq == 101


def test_airmouse_server_event_callbacks_and_queue():
    """Test AirMouseServer full lifecycle: connect, hello, move, click, callbacks."""
    async def _test():
        auth_mgr = AuthManager()
        token = auth_mgr.generate_token("android_client_unit")

        server = AirMouseServer(
            host="127.0.0.1",
            tcp_port=9123,
            ws_port=9124,
            udp_port=0,  # disable udp in this test
            enable_tls=False,
            auth_manager=auth_mgr,
        )

        connected_sessions = []
        disconnected_sessions = []
        moves = []
        clicks = []

        server.on_connect(lambda s: connected_sessions.append(s))
        server.on_disconnect(lambda s: disconnected_sessions.append(s))
        server.on_move(lambda dx, dy: moves.append((dx, dy)))
        server.on_click(lambda b: clicks.append(b))

        await server.start()

        reader, writer = await asyncio.open_connection("127.0.0.1", 9123)

        now_ms = int(time.time() * 1000)

        # Handshake
        hello_msg = {
            "v": 2,
            "type": "hello",
            "seq": 1,
            "ts": now_ms,
            "payload": {
                "device_id": "pixel_8",
                "name": "Pixel 8 Pro",
                "version": "2.0.0",
                "token": token,
            },
        }
        writer.write((json.dumps(hello_msg) + "\n").encode("utf-8"))
        await writer.drain()

        welcome_raw = await reader.readline()
        welcome = json.loads(welcome_raw.decode("utf-8"))
        assert welcome["type"] == "welcome"
        assert welcome["payload"]["approved"] is True

        assert len(connected_sessions) == 1

        # Motion
        move_msg = {
            "v": 2,
            "type": "move",
            "seq": 2,
            "ts": now_ms + 10,
            "payload": {"dx": 2.5, "dy": -1.5, "dt": 0.016},
        }
        writer.write((json.dumps(move_msg) + "\n").encode("utf-8"))
        await writer.drain()

        # Click
        click_msg = {
            "v": 2,
            "type": "click",
            "id": "c1",
            "seq": 3,
            "ts": now_ms + 20,
            "payload": {"button": "left"},
        }
        writer.write((json.dumps(click_msg) + "\n").encode("utf-8"))
        await writer.drain()

        ack_raw = await reader.readline()
        ack = json.loads(ack_raw.decode("utf-8"))
        assert ack["type"] == "ack"
        assert ack["payload"]["ack_id"] == "c1"

        # Give a moment for callback and queue propagation
        await asyncio.sleep(0.1)

        assert (2.5, -1.5) in moves
        assert "left" in clicks

        # Disconnect
        writer.close()
        await writer.wait_closed()
        await asyncio.sleep(0.1)

        assert len(disconnected_sessions) == 1

        await server.stop()

    asyncio.run(_test())
