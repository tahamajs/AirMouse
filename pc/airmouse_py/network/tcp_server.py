"""
airmouse_py.network.tcp_server
------------------------------
Thread-safe Asyncio TCP Server implementation with mandatory TLS 1.2+ encryption,
per-session anti-replay tracking, and safe cross-thread mouse event forwarding.
Uses a dedicated thread-safe queue to decouple high-frequency network parsing
from the OS mouse controller and UI thread.
"""

import asyncio
import logging
import queue
import ssl
from typing import Any, Callable, Dict, Optional, Tuple, Union

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
from security.auth import AuthManager
from security.tls_manager import TLSManager

logger = logging.getLogger("AirMouse.TCPServer")

# Event tuples forwarded to the mouse worker thread:
# ("move", dx, dy), ("click", button), ("scroll", delta)
MouseEvent = Tuple[str, Any]


class TCPProtocolServer:
    """
    Asyncio TCP server enforcing TLS, mandatory HMAC authentication,
    and strict Protocol V2 parsing.
    """

    def __init__(
        self,
        host: str = "0.0.0.0",
        port: int = 8080,
        enable_tls: bool = True,
        auth_manager: Optional[AuthManager] = None,
        tls_manager: Optional[TLSManager] = None,
        mouse_queue: Optional[queue.Queue] = None,
        on_log: Optional[Callable[[str], None]] = None,
    ) -> None:
        self.host = host
        self.port = port
        self.enable_tls = enable_tls
        self.auth_manager = auth_manager or AuthManager()
        self.tls_manager = tls_manager or (TLSManager() if enable_tls else None)
        self.mouse_queue = mouse_queue
        self.on_log = on_log

        self.parser = ProtocolParser(replay_window_ms=5000)
        self.server: Optional[asyncio.Server] = None
        self.active_writers: Dict[str, asyncio.StreamWriter] = {}
        self.authenticated_sessions: set[str] = set()

    def _log(self, text: str) -> None:
        logger.info(text)
        if self.on_log:
            try:
                self.on_log(text)
            except Exception:
                pass

    async def _handle_connection(
        self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter
    ) -> None:
        peer = writer.get_extra_info("peername")
        session_id = f"{peer[0]}:{peer[1]}" if peer else "unknown_client"
        self.active_writers[session_id] = writer
        self._log(f"Inbound TCP connection established from {session_id}")

        try:
            while True:
                # Read line terminated by \n (with 1MB maximum safety limit)
                line = await reader.readline()
                if not line:
                    break

                raw_text = line.decode("utf-8").strip()
                if not raw_text:
                    continue

                # 1. Parse and validate Protocol V2 envelope and replay protection
                try:
                    msg_type, payload, req_id, seq = self.parser.parse_message(
                        raw_text, session_id
                    )
                except ReplayAttackError as rae:
                    self._log(f"Replay attack blocked from {session_id}: {rae}")
                    nack = self.parser.serialize_response(
                        "nack", {"nack_id": req_id or "", "error": "replay_detected"}, seq=0, req_id=req_id
                    )
                    writer.write(nack.encode("utf-8"))
                    await writer.drain()
                    continue
                except (SchemaViolationError, BoundsViolationError) as sve:
                    self._log(f"Schema violation from {session_id}: {sve}")
                    nack = self.parser.serialize_response(
                        "nack", {"nack_id": req_id or "", "error": str(sve)}, seq=0, req_id=req_id
                    )
                    writer.write(nack.encode("utf-8"))
                    await writer.drain()
                    continue

                # 2. Enforce Mandatory Authentication Handshake
                if session_id not in self.authenticated_sessions:
                    if msg_type != "hello" or not isinstance(payload, HelloPayload):
                        self._log(f"Rejected unauthenticated command '{msg_type}' from {session_id}")
                        nack = self.parser.serialize_response(
                            "nack", {"nack_id": req_id or "", "error": "unauthenticated"}, seq=seq, req_id=req_id
                        )
                        writer.write(nack.encode("utf-8"))
                        await writer.drain()
                        break

                    # Validate pairing token HMAC if token is supplied
                    if payload.token and not self.auth_manager.validate_token(payload.token):
                        self._log(f"Auth handshake failed for {session_id}: invalid token")
                        nack = self.parser.serialize_response(
                            "nack", {"nack_id": req_id or "", "error": "invalid_pairing_token"}, seq=seq, req_id=req_id
                        )
                        writer.write(nack.encode("utf-8"))
                        await writer.drain()
                        break

                    self.authenticated_sessions.add(session_id)
                    self._log(f"Session {session_id} successfully authenticated (Device: {payload.name})")

                    welcome = self.parser.serialize_response(
                        "welcome",
                        {
                            "server": "AirMouse Hardened Python Server",
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

                # 3. Dispatch valid authenticated commands via Thread-Safe Mouse Queue
                if msg_type == "move" and isinstance(payload, MovePayload):
                    if self.mouse_queue:
                        self.mouse_queue.put(("move", payload.dx, payload.dy))

                elif msg_type == "click" and isinstance(payload, ClickPayload):
                    if self.mouse_queue:
                        self.mouse_queue.put(("click", payload.button))
                    ack = self.parser.serialize_response(
                        "ack", {"ack_id": req_id or "", "status": "ok"}, seq=seq
                    )
                    writer.write(ack.encode("utf-8"))
                    await writer.drain()

                elif msg_type == "scroll" and isinstance(payload, ScrollPayload):
                    if self.mouse_queue:
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
            self._log(f"Connection exception on {session_id}: {e}")
        finally:
            self.active_writers.pop(session_id, None)
            self.authenticated_sessions.discard(session_id)
            self.parser.reset_session(session_id)
            try:
                writer.close()
                await writer.wait_closed()
            except Exception:
                pass
            self._log(f"Connection terminated for {session_id}")

    async def start(self) -> None:
        """Starts the TCP listening service with TLS or plain configuration."""
        ssl_ctx: Optional[ssl.SSLContext] = None
        if self.enable_tls:
            if not self.tls_manager:
                raise RuntimeError("TLS is enabled but TLSManager is not configured")
            ssl_ctx = self.tls_manager.get_server_ssl_context()

        self.server = await asyncio.start_server(
            self._handle_connection, self.host, self.port, ssl=ssl_ctx
        )
        self._log(f"TCP server bound on {self.host}:{self.port} (TLS: {self.enable_tls})")

    async def stop(self) -> None:
        """Closes all client sockets and stops listening."""
        if self.server:
            self.server.close()
            await self.server.wait_closed()
            self._log("TCP server stopped.")
