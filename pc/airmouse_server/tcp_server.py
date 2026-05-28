import asyncio
import json
import time
import socket
import traceback
from typing import Dict, Callable, Optional, Tuple
from .mouse_controller import MouseController
from .notification_manager import send_notification
from .config import CONFIG

class AirMouseTCPServer:
    def __init__(self, log_callback, stats_callback, connections_callback=None, move_callback=None):
        self.host = CONFIG.get("host", "0.0.0.0")
        self.port = CONFIG.get("port", 8080)
        self.mouse = MouseController()
        self.log = log_callback or (lambda msg, level="info": None)
        self.stats_callback = stats_callback
        self.connections_callback = connections_callback
        self.move_callback = move_callback

        self._server = None
        self._tasks: Dict[asyncio.Task, Tuple[str, int]] = {}  # task -> (ip, port)
        self.active_connections: Dict[Tuple[str, int], dict] = {}

    # ---------- Public API ----------
    async def start(self):
        retry_delay = 2
        while True:
            try:
                self._server = await asyncio.start_server(
                    self._handle_client, self.host, self.port
                )
                self.log(f"🚀 TCP server listening on {self.host}:{self.port}", "info")
                async with self._server:
                    await self._server.serve_forever()
                break
            except OSError as e:
                self.log(f"❌ Failed to start server: {e}. Retrying in {retry_delay}s...", "error")
                await asyncio.sleep(retry_delay)
                retry_delay = min(retry_delay * 2, 30)

    async def stop(self):
        if self._server:
            self._server.close()
            await self._server.wait_closed()
        # Cancel all client tasks
        for task in list(self._tasks.keys()):
            task.cancel()
        await asyncio.gather(*self._tasks.keys(), return_exceptions=True)
        self._tasks.clear()
        self.active_connections.clear()
        self.log("🛑 TCP server stopped", "info")

    def disconnect_client(self, addr: Tuple[str, int]):
        """Request a synchronous disconnect for `addr` from calling thread.
        Prefer scheduling `await close_connection(addr)` on the server loop for async safety.
        """
        info = self.active_connections.get(addr)
        if not info:
            return
        writer = info.get('writer')
        try:
            # best-effort close; caller may be outside event loop
            writer.close()
        except Exception:
            pass

    async def close_connection(self, addr: Tuple[str, int]):
        """Asynchronously close and cleanup a specific active connection.
        Safe to call from the server's asyncio loop or via `asyncio.run_coroutine_threadsafe`.
        """
        info = self.active_connections.get(addr)
        if not info:
            return
        writer = info.get('writer')
        try:
            writer.close()
            try:
                await writer.wait_closed()
            except Exception:
                pass
        except Exception:
            pass
        # final cleanup; remove from active map and update UI
        try:
            del self.active_connections[addr]
        except KeyError:
            pass
        self.log(f"🔌 Server closed connection {addr[0]}:{addr[1]}", "info")
        self._update_connections_list()

    def _update_connections_list(self):
        if self.connections_callback:
            addrs = []
            for addr, info in self.active_connections.items():
                name = info.get('name', 'Unknown')
                uptime = int(time.time() - info['start_time'])
                idle = int(time.time() - info['last_activity'])
                addrs.append(f"{name} | {addr[0]}:{addr[1]} | up {uptime}s | idle {idle}s")
            self.connections_callback(addrs)

    # ---------- Client handler ----------
    async def _handle_client(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter):
        addr = writer.get_extra_info('peername')
        if not addr:
            self.log("⚠️ Connection with no address rejected", "warning")
            writer.close()
            return

        start_time = time.time()
        client_info = {
            'writer': writer,
            'start_time': start_time,
            'bytes_sent': 0,
            'bytes_recv': 0,
            'last_activity': start_time,
            'name': None
        }
        self.active_connections[addr] = client_info
        self.log(f"✅ Connected: {addr[0]}:{addr[1]}", "info")
        send_notification("Client connected", f"{addr[0]}:{addr[1]} connected")
        self._update_connections_list()

        # Start watchdog
        watchdog_task = asyncio.create_task(self._connection_watchdog(addr, writer))

        try:
            while True:
                try:
                    data = await asyncio.wait_for(reader.readline(), timeout=10.0)
                except asyncio.TimeoutError:
                    continue
                except asyncio.CancelledError:
                    break

                if not data:
                    break

                client_info['last_activity'] = time.time()
                client_info['bytes_recv'] += len(data)

                try:
                    msg = json.loads(data.decode().strip())
                except json.JSONDecodeError:
                    self.log(f"⚠️ Invalid JSON from {addr[0]}:{addr[1]}", "warning")
                    continue

                # Process message
                await self._process_message(msg, writer, addr, client_info)

        except (ConnectionResetError, BrokenPipeError, OSError):
            self.log(f"🔌 Connection reset by {addr[0]}:{addr[1]}", "info")
        except Exception as e:
            self.log(f"❌ Unhandled error with {addr[0]}:{addr[1]}: {e}", "error")
            traceback.print_exc()
        finally:
            watchdog_task.cancel()
            try:
                del self.active_connections[addr]
            except KeyError:
                pass
            writer.close()
            await writer.wait_closed()
            uptime = int(time.time() - start_time)
            self.log(f"🔌 Disconnected: {addr[0]}:{addr[1]} (after {uptime}s)", "info")
            send_notification("Client disconnected", f"{addr[0]}:{addr[1]} disconnected after {uptime}s")
            self._update_connections_list()

    async def _connection_watchdog(self, addr, writer, timeout=10):
        while True:
            await asyncio.sleep(timeout)
            info = self.active_connections.get(addr)
            if info and time.time() - info['last_activity'] > timeout:
                self.log(f"🕒 Watchdog triggered for {addr[0]}:{addr[1]}, disconnecting", "warning")
                writer.close()
                break

    # ---------- Message processing ----------
    async def _process_message(self, msg: dict, writer, addr, client_info):
        t = msg.get('type')
        if t == 'move':
            dx, dy = msg.get('dx', 0.0), msg.get('dy', 0.0)
            self.mouse.move(dx, dy)
            if self.move_callback:
                self.move_callback(dx, dy)
        elif t == 'hello':
            name = msg.get('name', 'Unknown')
            client_info['name'] = name
            self.log(f"🖥️ Client {addr[0]}:{addr[1]} identified as '{name}'", "info")
            self._update_connections_list()
        elif t in ('click', 'doubleclick', 'rightclick', 'scroll'):
            # Handle click/scroll with ACK
            if t == 'click':
                self.mouse.click()
            elif t == 'doubleclick':
                self.mouse.double_click()
            elif t == 'rightclick':
                self.mouse.click(button='right')
            elif t == 'scroll':
                self.mouse.scroll(msg.get('delta', 0))
            await self._send_ack(msg.get('id'), writer)
            self.log(f"🖱️ {t} from {addr[0]}:{addr[1]}", "info")
        else:
            self.log(f"⚠️ Unknown message type '{t}' from {addr[0]}:{addr[1]}", "warning")
        self.stats_callback(self.mouse.get_stats())

    async def _send_ack(self, msg_id, writer):
        if msg_id is not None:
            ack = json.dumps({'type': 'ack', 'id': msg_id})
            writer.write(ack.encode() + b'\n')
            await writer.drain()
            # Update bytes sent (approximate)
            for addr, info in self.active_connections.items():
                if info['writer'] is writer:
                    info['bytes_sent'] += len(ack) + 1
                    break