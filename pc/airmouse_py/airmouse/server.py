"""
airmouse.server
---------------
Asyncio TCP & WebSocket dual-stack server with TLS 1.2+ encryption,
UDP discovery responder with challenge-response nonces,
thread-safe mouse command queue and actuator thread,
and event callbacks: on_connect, on_disconnect, on_move, on_click.
"""

import asyncio
import logging
import queue
import socket
import ssl
import threading
import time
from typing import Any, Callable, Dict, Optional, Set, Tuple

from core.actuator import MouseActuatorThread
from core.mouse_controller import MouseController
from network.protocol import (
    BoundsViolationError,
    ClickPayload,
    HelloPayload,
    MovePayload,
    ProtocolParser,
    ReplayAttackError,
    SchemaViolationError,
    ScrollPayload,
)
from network.udp_discovery import UDPDiscoveryServer
from security.auth import AuthManager
from security.tls_manager import TLSManager

try:
    import websockets
    import websockets.server
    WEBSOCKETS_AVAILABLE = True
except ImportError:
    WEBSOCKETS_AVAILABLE = False

logger = logging.getLogger("AirMouseServer")


class AirMouseServer:
    """
    High-level AirMouse server combining TCP, WebSocket, and UDP discovery services.
    Decouples networking from OS mouse actuation using an internal thread-safe queue.
    """

    def __init__(
        self,
        host: str = "0.0.0.0",
        tcp_port: int = 8080,
        ws_port: int = 8081,
        udp_port: int = 8083,
        enable_tls: bool = True,
        auth_manager: Optional[AuthManager] = None,
        tls_manager: Optional[TLSManager] = None,
        sensitivity: float = 1.0,
        smoothing: bool = True,
        server_name: str = "AirMouse Desktop Server",
    ) -> None:
        self.host = host
        self.tcp_port = tcp_port
        self.ws_port = ws_port
        self.udp_port = udp_port
        self.enable_tls = enable_tls
        self.server_name = server_name

        self.auth_manager = auth_manager or AuthManager()
        self.tls_manager = tls_manager or (TLSManager() if enable_tls else None)

        # Thread-safe queue for OS mouse actuation
        self.mouse_queue: queue.Queue = queue.Queue()
        self.actuator = MouseActuatorThread(
            command_queue=self.mouse_queue,
            sensitivity=sensitivity,
            smoothing=smoothing,
        )
        self._actuator_thread: Optional[threading.Thread] = None

        # Protocol parser with per-session sequence and replay attack checks
        self.parser = ProtocolParser(replay_window_ms=5000)

        # UDP Discovery
        self.udp_discovery: Optional[UDPDiscoveryServer] = None

        # Server handles
        self._tcp_server: Optional[asyncio.Server] = None
        self._ws_server: Optional[Any] = None
        self._is_running = False

        # Session tracking
        self.authenticated_sessions: Set[str] = set()
        self.active_tcp_writers: Dict[str, asyncio.StreamWriter] = {}

        # Event callbacks
        self._on_connect_callbacks: list[Callable[[str], None]] = []
        self._on_disconnect_callbacks: list[Callable[[str], None]] = []
        self._on_move_callbacks: list[Callable[[float, float], None]] = []
        self._on_click_callbacks: list[Callable[[str], None]] = []

    def on_connect(self, callback: Callable[[str], None]) -> None:
        """Register a callback invoked when a client connects and authenticates."""
        self._on_connect_callbacks.append(callback)

    def on_disconnect(self, callback: Callable[[str], None]) -> None:
        """Register a callback invoked when a client disconnects."""
        self._on_disconnect_callbacks.append(callback)

    def on_move(self, callback: Callable[[float, float], None]) -> None:
        """Register a callback invoked when a motion event is processed."""
        self._on_move_callbacks.append(callback)

    def on_click(self, callback: Callable[[str], None]) -> None:
        """Register a callback invoked when a mouse click event is processed."""
        self._on_click_callbacks.append(callback)

    def _trigger_connect(self, session_id: str) -> None:
        for cb in self._on_connect_callbacks:
            try:
                cb(session_id)
            except Exception as e:
                logger.error(f"Error in on_connect callback: {e}")

    def _trigger_disconnect(self, session_id: str) -> None:
        for cb in self._on_disconnect_callbacks:
            try:
                cb(session_id)
            except Exception as e:
                logger.error(f"Error in on_disconnect callback: {e}")

    def _trigger_move(self, dx: float, dy: float) -> None:
        for cb in self._on_move_callbacks:
            try:
                cb(dx, dy)
            except Exception as e:
                logger.error(f"Error in on_move callback: {e}")

    def _trigger_click(self, button: str) -> None:
        for cb in self._on_click_callbacks:
            try:
                cb(button)
            except Exception as e:
                logger.error(f"Error in on_click callback: {e}")

    async def _handle_tcp_client(
        self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter
    ) -> None:
        peer = writer.get_extra_info("peername")
        session_id = f"{peer[0]}:{peer[1]}" if peer else f"tcp_{id(writer)}"
        self.active_tcp_writers[session_id] = writer
        logger.info(f"TCP client connected from {session_id}")

        try:
            while True:
                line = await reader.readline()
                if not line:
                    break

                raw_json = line.decode("utf-8").strip()
                if not raw_json:
                    continue

                try:
                    msg_type, payload, req_id, seq = self.parser.parse_message(
                        raw_json, session_id
                    )
                except ReplayAttackError as re:
                    logger.warning(f"Replay rejected for {session_id}: {re}")
                    nack = self.parser.serialize_response(
                        "nack", {"nack_id": "", "error": "replay_detected"}, seq=0
                    )
                    writer.write(nack.encode("utf-8"))
                    await writer.drain()
                    continue
                except (SchemaViolationError, BoundsViolationError) as se:
                    logger.warning(f"Schema violation from {session_id}: {se}")
                    nack = self.parser.serialize_response(
                        "nack", {"nack_id": "", "error": str(se)}, seq=0
                    )
                    writer.write(nack.encode("utf-8"))
                    await writer.drain()
                    continue

                # Authentication check
                if session_id not in self.authenticated_sessions:
                    if msg_type != "hello" or not isinstance(payload, HelloPayload):
                        logger.warning(f"Unauthenticated packet {msg_type} from {session_id}")
                        nack = self.parser.serialize_response(
                            "nack", {"nack_id": req_id or "", "error": "unauthenticated"}, seq=seq
                        )
                        writer.write(nack.encode("utf-8"))
                        await writer.drain()
                        break

                    if not self.auth_manager.validate_token(payload.token):
                        logger.warning(f"Invalid auth token from {session_id}")
                        nack = self.parser.serialize_response(
                            "nack", {"nack_id": req_id or "", "error": "invalid_token"}, seq=seq
                        )
                        writer.write(nack.encode("utf-8"))
                        await writer.drain()
                        break

                    self.authenticated_sessions.add(session_id)
                    logger.info(f"Client {session_id} successfully authenticated ({payload.name})")
                    self._trigger_connect(session_id)

                    welcome = self.parser.serialize_response(
                        "welcome",
                        {
                            "server": self.server_name,
                            "version": "2.0.0",
                            "client_id": session_id,
                            "approved": True,
                        },
                        seq=seq,
                        req_id=req_id,
                    )
                    writer.write(welcome.encode("utf-8"))
                    await writer.drain()
                    continue

                # Authenticated event dispatch
                if msg_type == "move" and isinstance(payload, MovePayload):
                    self.mouse_queue.put(("move", payload.dx, payload.dy))
                    self._trigger_move(payload.dx, payload.dy)

                elif msg_type == "click" and isinstance(payload, ClickPayload):
                    self.mouse_queue.put(("click", payload.button))
                    self._trigger_click(payload.button)
                    ack = self.parser.serialize_response(
                        "ack", {"ack_id": req_id or "", "status": "ok"}, seq=seq
                    )
                    writer.write(ack.encode("utf-8"))
                    await writer.drain()

                elif msg_type == "scroll" and isinstance(payload, ScrollPayload):
                    self.mouse_queue.put(("scroll", payload.delta))
                    ack = self.parser.serialize_response(
                        "ack", {"ack_id": req_id or "", "status": "ok"}, seq=seq
                    )
                    writer.write(ack.encode("utf-8"))
                    await writer.drain()

                elif msg_type == "ping":
                    pong = self.parser.serialize_response("pong", {}, seq=seq, req_id=req_id)
                    writer.write(pong.encode("utf-8"))
                    await writer.drain()

        except asyncio.CancelledError:
            pass
        except Exception as e:
            logger.error(f"TCP exception on {session_id}: {e}")
        finally:
            self.active_tcp_writers.pop(session_id, None)
            was_auth = session_id in self.authenticated_sessions
            self.authenticated_sessions.discard(session_id)
            self.parser.reset_session(session_id)
            try:
                writer.close()
                await writer.wait_closed()
            except Exception:
                pass
            if was_auth:
                self._trigger_disconnect(session_id)
            logger.info(f"TCP connection closed for {session_id}")

    async def _handle_ws_client(self, websocket: Any) -> None:
        session_id = f"{websocket.remote_address[0]}:{websocket.remote_address[1]}"
        logger.info(f"WebSocket client connected from {session_id}")

        try:
            async for raw_msg in websocket:
                try:
                    msg_type, payload, req_id, seq = self.parser.parse_message(
                        raw_msg, session_id
                    )
                except ReplayAttackError:
                    resp = self.parser.serialize_response(
                        "nack", {"nack_id": "", "error": "replay_detected"}, seq=0
                    )
                    await websocket.send(resp.strip())
                    continue
                except (SchemaViolationError, BoundsViolationError) as se:
                    resp = self.parser.serialize_response(
                        "nack", {"nack_id": "", "error": str(se)}, seq=0
                    )
                    await websocket.send(resp.strip())
                    continue

                if session_id not in self.authenticated_sessions:
                    if msg_type != "hello" or not isinstance(payload, HelloPayload):
                        resp = self.parser.serialize_response(
                            "nack", {"nack_id": req_id or "", "error": "unauthenticated"}, seq=seq
                        )
                        await websocket.send(resp.strip())
                        break

                    if not self.auth_manager.validate_token(payload.token):
                        resp = self.parser.serialize_response(
                            "nack", {"nack_id": req_id or "", "error": "invalid_token"}, seq=seq
                        )
                        await websocket.send(resp.strip())
                        break

                    self.authenticated_sessions.add(session_id)
                    self._trigger_connect(session_id)

                    welcome = self.parser.serialize_response(
                        "welcome",
                        {
                            "server": self.server_name,
                            "version": "2.0.0",
                            "client_id": session_id,
                            "approved": True,
                        },
                        seq=seq,
                        req_id=req_id,
                    )
                    await websocket.send(welcome.strip())
                    continue

                if msg_type == "move" and isinstance(payload, MovePayload):
                    self.mouse_queue.put(("move", payload.dx, payload.dy))
                    self._trigger_move(payload.dx, payload.dy)

                elif msg_type == "click" and isinstance(payload, ClickPayload):
                    self.mouse_queue.put(("click", payload.button))
                    self._trigger_click(payload.button)
                    ack = self.parser.serialize_response(
                        "ack", {"ack_id": req_id or "", "status": "ok"}, seq=seq
                    )
                    await websocket.send(ack.strip())

                elif msg_type == "scroll" and isinstance(payload, ScrollPayload):
                    self.mouse_queue.put(("scroll", payload.delta))
                    ack = self.parser.serialize_response(
                        "ack", {"ack_id": req_id or "", "status": "ok"}, seq=seq
                    )
                    await websocket.send(ack.strip())

                elif msg_type == "ping":
                    pong = self.parser.serialize_response("pong", {}, seq=seq, req_id=req_id)
                    await websocket.send(pong.strip())

        except Exception as e:
            logger.debug(f"WS connection error: {e}")
        finally:
            was_auth = session_id in self.authenticated_sessions
            self.authenticated_sessions.discard(session_id)
            self.parser.reset_session(session_id)
            if was_auth:
                self._trigger_disconnect(session_id)
            logger.info(f"WS client disconnected: {session_id}")

    async def start(self) -> None:
        """Starts mouse actuator thread, UDP discovery, TCP server, and WebSocket server."""
        if self._is_running:
            return
        self._is_running = True

        # 1. Start mouse actuator worker thread
        self._actuator_thread = threading.Thread(
            target=self.actuator.start,
            name="AirMouse-ActuatorThread",
            daemon=True,
        )
        self._actuator_thread.start()

        # 2. Setup TLS context if enabled
        ssl_ctx: Optional[ssl.SSLContext] = None
        if self.enable_tls:
            if not self.tls_manager:
                self.tls_manager = TLSManager()
            ssl_ctx = self.tls_manager.get_server_ssl_context()

        # 3. Start UDP discovery responder
        if self.udp_port > 0:
            self.udp_discovery = UDPDiscoveryServer(
                port=self.udp_port,
                server_name=self.server_name,
                tcp_port=self.tcp_port,
                ws_port=self.ws_port,
                tls_enabled=self.enable_tls,
            )
            self.udp_discovery.start()
            logger.info(f"UDP Discovery service started on port {self.udp_port}")

        # 4. Start TCP server
        self._tcp_server = await asyncio.start_server(
            self._handle_tcp_client,
            self.host,
            self.tcp_port,
            ssl=ssl_ctx,
        )
        logger.info(
            f"TCP Protocol server listening on {self.host}:{self.tcp_port} (TLS={self.enable_tls})"
        )

        # 5. Start WebSocket server
        if WEBSOCKETS_AVAILABLE and self.ws_port > 0:
            self._ws_server = await websockets.serve(
                self._handle_ws_client,
                self.host,
                self.ws_port,
                ssl=ssl_ctx,
            )
            logger.info(
                f"WebSocket server listening on {self.host}:{self.ws_port} (TLS={self.enable_tls})"
            )

    async def stop(self) -> None:
        """Stops all servers, UDP discovery, and the mouse actuator worker thread."""
        self._is_running = False

        # Close TCP server
        if self._tcp_server:
            self._tcp_server.close()
            await self._tcp_server.wait_closed()
            self._tcp_server = None

        # Close WebSocket server
        if self._ws_server:
            self._ws_server.close()
            await self._ws_server.wait_closed()
            self._ws_server = None

        # Stop UDP discovery
        if self.udp_discovery:
            self.udp_discovery.stop()
            self.udp_discovery = None

        # Stop mouse actuator thread
        self.actuator.stop()
        if self._actuator_thread and self._actuator_thread.is_alive():
            self._actuator_thread.join(timeout=1.0)
            self._actuator_thread = None

        logger.info("AirMouseServer stopped cleanly.")
