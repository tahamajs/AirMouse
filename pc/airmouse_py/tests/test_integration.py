"""
Comprehensive Integration Test Suite for AirMouse Python Server.
Covers 12 distinct, fully implemented end-to-end integration scenarios:
 1. Full Handshake Sequence & Welcome Response
 2. Rejection of Commands from Unauthenticated Clients
 3. Invalid Pairing Token Handling
 4. Cursor Movement Dispatch to Decoupled Mouse Actuator Queue
 5. Click Dispatch with Reliable Delivery ACK Confirmation
 6. Scroll Wheel Dispatch with Reliable Delivery ACK Confirmation
 7. Strict Sequence Monotonicity & Replay Protection Rejection
 8. Stale Timestamp Outside Sliding Window (+/- 5000ms) Blocked
 9. Out-of-Bounds Motion Delta (> 500px) Rejection
10. Rapid Burst of Motion Packets Throughput Verification
11. Ping / Pong Health Check and Heartbeat Roundtrip
12. Concurrent Multi-Client Isolation & Independent Replay Tracking
"""

import asyncio
import json
import queue
import time
import pytest
from network.tcp_server import TCPProtocolServer
from security.auth import AuthManager


@pytest.fixture
def auth_mgr(tmp_path):
    return AuthManager(secret_dir=str(tmp_path))


async def create_connected_client(port: int):
    return await asyncio.open_connection("127.0.0.1", port)


async def send_packet(writer, packet: dict):
    wire_data = json.dumps(packet) + "\n"
    writer.write(wire_data.encode("utf-8"))
    await writer.drain()


async def read_response(reader, timeout: float = 2.0) -> dict:
    line = await asyncio.wait_for(reader.readline(), timeout=timeout)
    return json.loads(line.decode("utf-8").strip())


# 1. Full Handshake Sequence & Welcome Response
def test_integration_01_handshake_welcome_flow(auth_mgr):
    async def _run():
        cmd_queue = queue.Queue()
        port = 9101
        server = TCPProtocolServer(host="127.0.0.1", port=port, enable_tls=False, auth_manager=auth_mgr, mouse_queue=cmd_queue)
        await server.start()
        try:
            reader, writer = await create_connected_client(port)
            token = auth_mgr.generate_token("device_alpha")
            now_ms = int(time.time() * 1000)

            hello = {
                "v": 2,
                "type": "hello",
                "seq": 1,
                "ts": now_ms,
                "payload": {"device_id": "device_alpha", "name": "Pixel 7 Pro", "version": "2.0.0", "token": token},
            }
            await send_packet(writer, hello)
            resp = await read_response(reader)

            assert resp["type"] == "welcome"
            assert resp["payload"]["approved"] is True
            assert "client_id" in resp["payload"]

            writer.close()
            await writer.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_run())


# 2. Rejection of Commands from Unauthenticated Clients
def test_integration_02_unauthenticated_command_rejected(auth_mgr):
    async def _run():
        cmd_queue = queue.Queue()
        port = 9102
        server = TCPProtocolServer(host="127.0.0.1", port=port, enable_tls=False, auth_manager=auth_mgr, mouse_queue=cmd_queue)
        await server.start()
        try:
            reader, writer = await create_connected_client(port)
            now_ms = int(time.time() * 1000)

            # Client tries to send 'move' without 'hello'
            move = {
                "v": 2,
                "type": "move",
                "seq": 1,
                "ts": now_ms,
                "payload": {"dx": 10.0, "dy": 20.0},
            }
            await send_packet(writer, move)
            resp = await read_response(reader)

            assert resp["type"] == "nack"
            assert resp["payload"]["error"] == "unauthenticated"
            assert cmd_queue.empty()

            writer.close()
            await writer.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_run())


# 3. Invalid Pairing Token Handling
def test_integration_03_invalid_token_rejected(auth_mgr):
    async def _run():
        cmd_queue = queue.Queue()
        port = 9103
        server = TCPProtocolServer(host="127.0.0.1", port=port, enable_tls=False, auth_manager=auth_mgr, mouse_queue=cmd_queue)
        await server.start()
        try:
            reader, writer = await create_connected_client(port)
            now_ms = int(time.time() * 1000)

            hello_tampered = {
                "v": 2,
                "type": "hello",
                "seq": 1,
                "ts": now_ms,
                "payload": {"device_id": "device_hacker", "name": "Fake Device", "version": "2.0.0", "token": "invalid_signature_hex_bad"},
            }
            await send_packet(writer, hello_tampered)
            resp = await read_response(reader)

            assert resp["type"] == "nack"
            assert resp["payload"]["error"] == "invalid_pairing_token"

            writer.close()
            await writer.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_run())


# 4. Cursor Movement Dispatch to Decoupled Mouse Actuator Queue
def test_integration_04_mouse_move_dispatched(auth_mgr):
    async def _run():
        cmd_queue = queue.Queue()
        port = 9104
        server = TCPProtocolServer(host="127.0.0.1", port=port, enable_tls=False, auth_manager=auth_mgr, mouse_queue=cmd_queue)
        await server.start()
        try:
            reader, writer = await create_connected_client(port)
            token = auth_mgr.generate_token("dev4")
            now_ms = int(time.time() * 1000)

            # Authenticate
            await send_packet(writer, {
                "v": 2, "type": "hello", "seq": 1, "ts": now_ms,
                "payload": {"device_id": "dev4", "name": "Tester", "version": "2.0.0", "token": token}
            })
            await read_response(reader)

            # Move command
            await send_packet(writer, {
                "v": 2, "type": "move", "seq": 2, "ts": now_ms + 10,
                "payload": {"dx": -42.5, "dy": 100.25, "dt": 0.016}
            })

            # Check queue
            cmd = None
            for _ in range(20):
                if not cmd_queue.empty():
                    cmd = cmd_queue.get_nowait()
                    break
                await asyncio.sleep(0.02)

            assert cmd == ("move", -42.5, 100.25)

            writer.close()
            await writer.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_run())


# 5. Click Dispatch with Reliable Delivery ACK Confirmation
def test_integration_05_mouse_click_and_ack(auth_mgr):
    async def _run():
        cmd_queue = queue.Queue()
        port = 9105
        server = TCPProtocolServer(host="127.0.0.1", port=port, enable_tls=False, auth_manager=auth_mgr, mouse_queue=cmd_queue)
        await server.start()
        try:
            reader, writer = await create_connected_client(port)
            token = auth_mgr.generate_token("dev5")
            now_ms = int(time.time() * 1000)

            await send_packet(writer, {
                "v": 2, "type": "hello", "seq": 1, "ts": now_ms,
                "payload": {"device_id": "dev5", "name": "Tester", "version": "2.0.0", "token": token}
            })
            await read_response(reader)

            # Send right click with reliable UUID
            click_id = "click-uuid-999"
            await send_packet(writer, {
                "v": 2, "type": "click", "seq": 2, "ts": now_ms + 10, "id": click_id,
                "payload": {"button": "right"}
            })

            # Server must respond with ACK matching ID
            ack_resp = await read_response(reader)
            assert ack_resp["type"] == "ack"
            assert ack_resp["payload"]["ack_id"] == click_id
            assert ack_resp["payload"]["status"] == "ok"

            # Actuator queue must receive click action
            cmd = cmd_queue.get_nowait()
            assert cmd == ("click", "right")

            writer.close()
            await writer.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_run())


# 6. Scroll Wheel Dispatch with Reliable Delivery ACK Confirmation
def test_integration_06_scroll_and_ack(auth_mgr):
    async def _run():
        cmd_queue = queue.Queue()
        port = 9106
        server = TCPProtocolServer(host="127.0.0.1", port=port, enable_tls=False, auth_manager=auth_mgr, mouse_queue=cmd_queue)
        await server.start()
        try:
            reader, writer = await create_connected_client(port)
            token = auth_mgr.generate_token("dev6")
            now_ms = int(time.time() * 1000)

            await send_packet(writer, {
                "v": 2, "type": "hello", "seq": 1, "ts": now_ms,
                "payload": {"device_id": "dev6", "name": "Tester", "version": "2.0.0", "token": token}
            })
            await read_response(reader)

            # Send scroll command
            scroll_id = "scroll-uuid-123"
            await send_packet(writer, {
                "v": 2, "type": "scroll", "seq": 2, "ts": now_ms + 10, "id": scroll_id,
                "payload": {"delta": -3.5}
            })

            ack_resp = await read_response(reader)
            assert ack_resp["type"] == "ack"
            assert ack_resp["payload"]["ack_id"] == scroll_id
            assert ack_resp["payload"]["status"] == "ok"

            cmd = cmd_queue.get_nowait()
            assert cmd == ("scroll", -3.5)

            writer.close()
            await writer.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_run())


# 7. Strict Sequence Monotonicity & Replay Protection Rejection
def test_integration_07_replay_protection_sequence_check(auth_mgr):
    async def _run():
        cmd_queue = queue.Queue()
        port = 9107
        server = TCPProtocolServer(host="127.0.0.1", port=port, enable_tls=False, auth_manager=auth_mgr, mouse_queue=cmd_queue)
        await server.start()
        try:
            reader, writer = await create_connected_client(port)
            token = auth_mgr.generate_token("dev7")
            now_ms = int(time.time() * 1000)

            await send_packet(writer, {
                "v": 2, "type": "hello", "seq": 5, "ts": now_ms,
                "payload": {"device_id": "dev7", "name": "Tester", "version": "2.0.0", "token": token}
            })
            await read_response(reader)

            # Send move with sequence 10
            await send_packet(writer, {
                "v": 2, "type": "move", "seq": 10, "ts": now_ms + 10,
                "payload": {"dx": 1.0, "dy": 1.0}
            })

            # Attempt replaying sequence 10 or retrograde sequence 8
            await send_packet(writer, {
                "v": 2, "type": "move", "seq": 10, "ts": now_ms + 20,
                "payload": {"dx": 1.0, "dy": 1.0}
            })
            resp = await read_response(reader)
            assert resp["type"] == "nack"
            assert resp["payload"]["error"] == "replay_detected"

            # Retrograde sequence 8 must also fail
            await send_packet(writer, {
                "v": 2, "type": "move", "seq": 8, "ts": now_ms + 30,
                "payload": {"dx": 1.0, "dy": 1.0}
            })
            resp2 = await read_response(reader)
            assert resp2["type"] == "nack"
            assert resp2["payload"]["error"] == "replay_detected"

            writer.close()
            await writer.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_run())


# 8. Stale Timestamp Outside Sliding Window (+/- 5000ms) Blocked
def test_integration_08_stale_timestamp_rejected(auth_mgr):
    async def _run():
        cmd_queue = queue.Queue()
        port = 9108
        server = TCPProtocolServer(host="127.0.0.1", port=port, enable_tls=False, auth_manager=auth_mgr, mouse_queue=cmd_queue)
        await server.start()
        try:
            reader, writer = await create_connected_client(port)
            token = auth_mgr.generate_token("dev8")
            now_ms = int(time.time() * 1000)

            await send_packet(writer, {
                "v": 2, "type": "hello", "seq": 1, "ts": now_ms,
                "payload": {"device_id": "dev8", "name": "Tester", "version": "2.0.0", "token": token}
            })
            await read_response(reader)

            # Send packet timestamped 15 seconds in the past (> 5000ms window)
            stale_ms = now_ms - 15000
            await send_packet(writer, {
                "v": 2, "type": "move", "seq": 2, "ts": stale_ms,
                "payload": {"dx": 2.0, "dy": 2.0}
            })
            resp = await read_response(reader)
            assert resp["type"] == "nack"
            assert resp["payload"]["error"] == "replay_detected"

            writer.close()
            await writer.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_run())


# 9. Out-of-Bounds Motion Delta (> 500px) Rejection
def test_integration_09_bounds_violation_rejected(auth_mgr):
    async def _run():
        cmd_queue = queue.Queue()
        port = 9109
        server = TCPProtocolServer(host="127.0.0.1", port=port, enable_tls=False, auth_manager=auth_mgr, mouse_queue=cmd_queue)
        await server.start()
        try:
            reader, writer = await create_connected_client(port)
            token = auth_mgr.generate_token("dev9")
            now_ms = int(time.time() * 1000)

            await send_packet(writer, {
                "v": 2, "type": "hello", "seq": 1, "ts": now_ms,
                "payload": {"device_id": "dev9", "name": "Tester", "version": "2.0.0", "token": token}
            })
            await read_response(reader)

            # Extreme out-of-bounds dx = 1000.0 (max allowed is 500.0)
            await send_packet(writer, {
                "v": 2, "type": "move", "seq": 2, "ts": now_ms + 10,
                "payload": {"dx": 1000.0, "dy": 0.0}
            })
            resp = await read_response(reader)
            assert resp["type"] == "nack"
            assert "exceeds absolute limit of 500.0 pixels" in resp["payload"]["error"]

            writer.close()
            await writer.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_run())


# 10. Rapid Burst of Motion Packets Throughput Verification
def test_integration_10_rapid_motion_burst(auth_mgr):
    async def _run():
        cmd_queue = queue.Queue()
        port = 9110
        server = TCPProtocolServer(host="127.0.0.1", port=port, enable_tls=False, auth_manager=auth_mgr, mouse_queue=cmd_queue)
        await server.start()
        try:
            reader, writer = await create_connected_client(port)
            token = auth_mgr.generate_token("dev10")
            now_ms = int(time.time() * 1000)

            await send_packet(writer, {
                "v": 2, "type": "hello", "seq": 1, "ts": now_ms,
                "payload": {"device_id": "dev10", "name": "Burst Tester", "version": "2.0.0", "token": token}
            })
            await read_response(reader)

            # Send burst of 50 packets rapidly
            packet_count = 50
            for i in range(1, packet_count + 1):
                await send_packet(writer, {
                    "v": 2, "type": "move", "seq": 1 + i, "ts": now_ms + i,
                    "payload": {"dx": 1.5, "dy": -2.0}
                })

            # Verify queue processed all packets
            drain_count = 0
            for _ in range(50):
                while not cmd_queue.empty():
                    cmd = cmd_queue.get_nowait()
                    if cmd == ("move", 1.5, -2.0):
                        drain_count += 1
                if drain_count >= packet_count:
                    break
                await asyncio.sleep(0.02)

            assert drain_count == packet_count

            writer.close()
            await writer.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_run())


# 11. Ping / Pong Health Check and Heartbeat Roundtrip
def test_integration_11_ping_pong_heartbeat(auth_mgr):
    async def _run():
        cmd_queue = queue.Queue()
        port = 9111
        server = TCPProtocolServer(host="127.0.0.1", port=port, enable_tls=False, auth_manager=auth_mgr, mouse_queue=cmd_queue)
        await server.start()
        try:
            reader, writer = await create_connected_client(port)
            token = auth_mgr.generate_token("dev11")
            now_ms = int(time.time() * 1000)

            await send_packet(writer, {
                "v": 2, "type": "hello", "seq": 1, "ts": now_ms,
                "payload": {"device_id": "dev11", "name": "Heartbeat Tester", "version": "2.0.0", "token": token}
            })
            await read_response(reader)

            # Send ping
            ping_id = "ping-12345"
            await send_packet(writer, {
                "v": 2, "type": "ping", "seq": 2, "ts": now_ms + 10, "id": ping_id,
                "payload": {}
            })

            pong_resp = await read_response(reader)
            assert pong_resp["type"] == "pong"
            assert pong_resp["id"] == ping_id

            writer.close()
            await writer.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_run())


# 12. Concurrent Multi-Client Isolation & Independent Replay Tracking
def test_integration_12_concurrent_clients_session_isolation(auth_mgr):
    async def _run():
        cmd_queue = queue.Queue()
        port = 9112
        server = TCPProtocolServer(host="127.0.0.1", port=port, enable_tls=False, auth_manager=auth_mgr, mouse_queue=cmd_queue)
        await server.start()
        try:
            # Client A
            rA, wA = await create_connected_client(port)
            tA = auth_mgr.generate_token("clientA")
            now_ms = int(time.time() * 1000)
            await send_packet(wA, {
                "v": 2, "type": "hello", "seq": 1, "ts": now_ms,
                "payload": {"device_id": "clientA", "name": "Phone A", "version": "2.0.0", "token": tA}
            })
            respA = await read_response(rA)
            assert respA["type"] == "welcome"

            # Client B
            rB, wB = await create_connected_client(port)
            tB = auth_mgr.generate_token("clientB")
            await send_packet(wB, {
                "v": 2, "type": "hello", "seq": 1, "ts": now_ms,
                "payload": {"device_id": "clientB", "name": "Phone B", "version": "2.0.0", "token": tB}
            })
            respB = await read_response(rB)
            assert respB["type"] == "welcome"

            # Both clients send seq=2 simultaneously; session isolation ensures no collision
            await send_packet(wA, {
                "v": 2, "type": "move", "seq": 2, "ts": now_ms + 10,
                "payload": {"dx": 10.0, "dy": 10.0}
            })
            await send_packet(wB, {
                "v": 2, "type": "move", "seq": 2, "ts": now_ms + 10,
                "payload": {"dx": 20.0, "dy": 20.0}
            })

            # Check that both moves reached the command queue
            received_moves = []
            for _ in range(20):
                while not cmd_queue.empty():
                    received_moves.append(cmd_queue.get_nowait())
                if len(received_moves) >= 2:
                    break
                await asyncio.sleep(0.02)

            assert ("move", 10.0, 10.0) in received_moves
            assert ("move", 20.0, 20.0) in received_moves

            wA.close()
            wB.close()
            await wA.wait_closed()
            await wB.wait_closed()
        finally:
            await server.stop()

    asyncio.run(_run())
