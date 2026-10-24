import asyncio
import json
import logging
import queue
import threading
import time
import pytest
from network.tcp_server import TCPProtocolServer
from security.auth import AuthManager
from security.tls_manager import TLSManager


def test_end_to_end_client_to_actuator_queue():
    async def _run():
        cmd_queue = queue.Queue()
        auth_mgr = AuthManager()
        token = auth_mgr.generate_token("test_client")

        server = TCPProtocolServer(
            host="127.0.0.1",
            port=8999,
            enable_tls=False,
            auth_manager=auth_mgr,
            mouse_queue=cmd_queue,
        )
        await server.start()

        # Connect client
        reader, writer = await asyncio.open_connection("127.0.0.1", 8999)

        now_ms = int(time.time() * 1000)
        # Handshake
        hello_pkt = {
            "v": 2,
            "type": "hello",
            "seq": 1,
            "ts": now_ms,
            "payload": {
                "device_id": "test_id",
                "name": "Test Device",
                "version": "2.0.0",
                "token": token,
            },
        }
        writer.write((json.dumps(hello_pkt) + "\n").encode("utf-8"))
        await writer.drain()

        welcome_line = await reader.readline()
        welcome_obj = json.loads(welcome_line.decode("utf-8"))
        assert welcome_obj["type"] == "welcome"

        # Send move packet with simulated jitter
        move_pkt = {
            "v": 2,
            "type": "move",
            "seq": 2,
            "ts": now_ms + 10,
            "payload": {"dx": 15.5, "dy": -8.0, "dt": 0.016},
        }
        writer.write((json.dumps(move_pkt) + "\n").encode("utf-8"))
        await writer.drain()

        # Check queue receives command without invoking pynput directly in async loop
        received_cmd = None
        for _ in range(20):
            try:
                received_cmd = cmd_queue.get_nowait()
                break
            except queue.Empty:
                await asyncio.sleep(0.05)

        assert received_cmd == ("move", 15.5, -8.0)

        writer.close()
        await writer.wait_closed()
        await server.stop()

    asyncio.run(_run())
