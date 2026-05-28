import asyncio
import json
import time
from typing import Dict
from .mouse_controller import MouseController
from .notification_manager import send_notification
from .config import CONFIG

class AirMouseTCPServer:
    def __init__(self, log_callback, stats_callback, connections_callback=None, move_callback=None):
        self.host = CONFIG.get("host", "0.0.0.0")
        self.port = CONFIG.get("port", 8080)
        self.mouse = MouseController()
        self.log_callback = log_callback
        self.stats_callback = stats_callback
        self.connections_callback = connections_callback
        self.move_callback = move_callback
        self._server = None
        self.active_connections: Dict[tuple, dict] = {}

    def log(self, msg):
        if self.log_callback:
            self.log_callback(msg)

    def update_stats(self):
        if self.stats_callback:
            self.stats_callback(self.mouse.get_stats())
        if self.connections_callback:
            addrs = [f"{a[0]}:{a[1]}" for a in self.active_connections]
            self.connections_callback(addrs)

    async def handle_client(self, reader, writer):
        addr = writer.get_extra_info('peername')
        start_time = time.time()
        self.active_connections[addr] = {
            'writer': writer,
            'start_time': start_time,
            'bytes_sent': 0,
            'bytes_recv': 0
        }
        self.log(f"✅ Connected: {addr[0]}:{addr[1]} (active: {len(self.active_connections)})")
        send_notification("Client connected", f"{addr[0]}:{addr[1]} connected")
        self.update_stats()
        try:
            while True:
                data = await reader.readline()
                if not data:
                    break
                self.active_connections[addr]['bytes_recv'] += len(data)
                try:
                    msg = json.loads(data.decode().strip())
                    await self._process_message(msg, writer)
                except json.JSONDecodeError:
                    self.log(f"⚠️ Invalid JSON from {addr}")
        except Exception as e:
            self.log(f"❌ Error: {e}")
        finally:
            duration = time.time() - self.active_connections[addr]['start_time']
            self.log(f"🔌 Disconnected: {addr[0]}:{addr[1]} (after {duration:.0f}s)")
            send_notification("Client disconnected", f"{addr[0]}:{addr[1]} disconnected after {duration:.0f}s")
            try:
                del self.active_connections[addr]
            except KeyError:
                pass
            writer.close()
            await writer.wait_closed()
            self.update_stats()

    async def _process_message(self, msg, writer):
        t = msg.get('type')
        if t == 'move':
            dx, dy = msg.get('dx', 0), msg.get('dy', 0)
            self.mouse.move(dx, dy)
            if self.move_callback:
                self.move_callback(dx, dy)
        elif t in ('click', 'doubleclick', 'rightclick', 'scroll'):
            if t == 'click':
                self.mouse.click()
            elif t == 'doubleclick':
                self.mouse.double_click()
            elif t == 'rightclick':
                self.mouse.click(button='right')
            elif t == 'scroll':
                self.mouse.scroll(msg.get('delta', 0))
            await self._send_ack(msg.get('id'), writer)
            self.log(f"🖱️ {t}")
        else:
            self.log(f"⚠️ Unknown message type: {t}")
        self.update_stats()

    async def _send_ack(self, msg_id, writer):
        if msg_id:
            ack = json.dumps({'type': 'ack', 'id': msg_id})
            writer.write(ack.encode() + b'\n')
            await writer.drain()
            for addr, info in self.active_connections.items():
                if info['writer'] is writer:
                    info['bytes_sent'] += len(ack) + 1
                    break

    async def start(self):
        self._server = await asyncio.start_server(self.handle_client, self.host, self.port)
        self.log(f"🚀 TCP server listening on {self.host}:{self.port}")
        async with self._server:
            await self._server.serve_forever()

    async def stop(self):
        if self._server:
            self._server.close()
            await self._server.wait_closed()
            self.log("🛑 TCP server stopped")


if t == 'hello':
    name = msg.get('name', 'Unknown')
    self.active_connections[addr]['name'] = name
    self.log(f"🖥️ Client identified as '{name}'")


    addrs = [f"{info.get('name','Unknown')} ({a[0]}:{a[1]})" for a, info in self.active_connections.items()]