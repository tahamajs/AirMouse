"""
End-to-end tests for WebSocket server handling with TLS and authentication.
"""

import asyncio
import json
import ssl
import time
import pytest
import websockets

from airmouse.server import AirMouseServer
from security.auth import AuthManager
from security.tls_manager import TLSManager


def test_websocket_server_e2e_authenticated_session():
    async def _test():
        auth_mgr = AuthManager()
        token = auth_mgr.generate_token("ws_test_client")

        server = AirMouseServer(
            host="127.0.0.1",
            tcp_port=9200,
            ws_port=9201,
            udp_port=0,
            enable_tls=False,
            auth_manager=auth_mgr,
        )

        connected = []
        moves = []
        clicks = []
        server.on_connect(lambda s: connected.append(s))
        server.on_move(lambda dx, dy: moves.append((dx, dy)))
        server.on_click(lambda b: clicks.append(b))

        await server.start()

        uri = "ws://127.0.0.1:9201"
        async with websockets.connect(uri) as ws:
            now_ms = int(time.time() * 1000)

            # 1. Send Hello
            hello_msg = {
                "v": 2,
                "type": "hello",
                "seq": 1,
                "ts": now_ms,
                "payload": {
                    "device_id": "ws_client_1",
                    "name": "WS Client",
                    "version": "2.0.0",
                    "token": token,
                },
            }
            await ws.send(json.dumps(hello_msg))

            welcome_raw = await ws.recv()
            welcome = json.loads(welcome_raw)
            assert welcome["type"] == "welcome"
            assert welcome["payload"]["approved"] is True

            # 2. Move
            move_msg = {
                "v": 2,
                "type": "move",
                "seq": 2,
                "ts": now_ms + 10,
                "payload": {"dx": 18.0, "dy": -4.5, "dt": 0.016},
            }
            await ws.send(json.dumps(move_msg))

            # 3. Click
            click_msg = {
                "v": 2,
                "type": "click",
                "id": "c_ws",
                "seq": 3,
                "ts": now_ms + 20,
                "payload": {"button": "right"},
            }
            await ws.send(json.dumps(click_msg))

            ack_raw = await ws.recv()
            ack = json.loads(ack_raw)
            assert ack["type"] == "ack"
            assert ack["payload"]["ack_id"] == "c_ws"

            # 4. Scroll
            scroll_msg = {
                "v": 2,
                "type": "scroll",
                "id": "s_ws",
                "seq": 4,
                "ts": now_ms + 30,
                "payload": {"delta": -2.0},
            }
            await ws.send(json.dumps(scroll_msg))

            ack2_raw = await ws.recv()
            ack2 = json.loads(ack2_raw)
            assert ack2["type"] == "ack"
            assert ack2["payload"]["ack_id"] == "s_ws"

            # 5. Ping
            ping_msg = {
                "v": 2,
                "type": "ping",
                "seq": 5,
                "ts": now_ms + 40,
                "payload": {},
            }
            await ws.send(json.dumps(ping_msg))

            pong_raw = await ws.recv()
            pong = json.loads(pong_raw)
            assert pong["type"] == "pong"

        await asyncio.sleep(0.1)
        assert len(connected) == 1
        assert (18.0, -4.5) in moves
        assert "right" in clicks

        await server.stop()

    asyncio.run(_test())


def test_websocket_server_unauthenticated_rejection():
    async def _test():
        server = AirMouseServer(
            host="127.0.0.1",
            tcp_port=9202,
            ws_port=9203,
            udp_port=0,
            enable_tls=False,
        )
        await server.start()

        uri = "ws://127.0.0.1:9203"
        async with websockets.connect(uri) as ws:
            now_ms = int(time.time() * 1000)

            # Try moving before hello
            unauth_move = {
                "v": 2,
                "type": "move",
                "seq": 1,
                "ts": now_ms,
                "payload": {"dx": 1.0, "dy": 1.0},
            }
            await ws.send(json.dumps(unauth_move))

            resp_raw = await ws.recv()
            resp = json.loads(resp_raw)
            assert resp["type"] == "nack"
            assert resp["payload"]["error"] == "unauthenticated"

        await server.stop()

    asyncio.run(_test())
