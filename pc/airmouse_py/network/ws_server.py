import asyncio
import logging
import ssl
from typing import Callable, Dict, Optional
import websockets
from core.mouse_controller import MouseController
from network.protocol import BoundsError, ProtocolHandler, ProtocolError, ReplayError, SchemaError
from security.auth import AuthManager
from security.tls_manager import TLSManager

logger = logging.getLogger("WSServer")


class WebSocketProtocolServer:
    """Asyncio WebSocket Server supporting browser and mobile clients over wss://."""

    def __init__(
        self,
        host: str = "0.0.0.0",
        port: int = 8081,
        enable_tls: bool = True,
        auth_manager: Optional[AuthManager] = None,
        mouse_controller: Optional[MouseController] = None,
        on_log: Optional[Callable[[str], None]] = None,
    ):
        self.host = host
        self.port = port
        self.enable_tls = enable_tls
        self.auth_manager = auth_manager or AuthManager()
        self.mouse = mouse_controller or MouseController()
        self.on_log = on_log

        self.tls_manager = TLSManager() if enable_tls else None
        self.protocol_handler = ProtocolHandler()
        self.server: Optional[websockets.server.WebSocketServer] = None
        self.authenticated_sessions: set[str] = set()

    def _log(self, message: str) -> None:
        logger.info(message)
        if self.on_log:
            self.on_log(message)

    async def handle_ws(self, websocket: websockets.WebSocketServerProtocol) -> None:
        session_id = f"{websocket.remote_address[0]}:{websocket.remote_address[1]}"
        self._log(f"WS client connected from {session_id}")

        try:
            async for raw_msg in websocket:
                try:
                    msg_type, payload, req_id, seq = self.protocol_handler.decode_and_validate(
                        raw_msg, session_id
                    )
                except ReplayError as re:
                    resp = self.protocol_handler.create_response(
                        "nack", {"error": "replay_detected"}, seq=0, req_id=req_id
                    )
                    await websocket.send(resp)
                    continue
                except (SchemaError, BoundsError) as se:
                    resp = self.protocol_handler.create_response(
                        "nack", {"error": str(se)}, seq=0, req_id=req_id
                    )
                    await websocket.send(resp)
                    continue

                if session_id not in self.authenticated_sessions:
                    if msg_type != "hello":
                        resp = self.protocol_handler.create_response(
                            "nack", {"error": "unauthenticated"}, seq=seq, req_id=req_id
                        )
                        await websocket.send(resp)
                        break

                    token = payload.get("token", "")
                    if token and not self.auth_manager.validate_token(token):
                        resp = self.protocol_handler.create_response(
                            "nack", {"error": "invalid_token"}, seq=seq, req_id=req_id
                        )
                        await websocket.send(resp)
                        break

                    self.authenticated_sessions.add(session_id)
                    welcome = self.protocol_handler.create_response(
                        "welcome",
                        {"server": "AirMouse WS Python", "version": "1.0.1", "client_id": session_id, "approved": True},
                        seq=seq,
                        req_id=req_id,
                    )
                    await websocket.send(welcome)
                    continue

                if msg_type == "move":
                    self.mouse.move(float(payload.get("dx", 0.0)), float(payload.get("dy", 0.0)))
                elif msg_type == "click":
                    self.mouse.click(payload.get("button", "left"))
                    ack = self.protocol_handler.create_response(
                        "ack", {"ack_id": req_id or "", "status": "ok"}, seq=seq
                    )
                    await websocket.send(ack)
                elif msg_type == "scroll":
                    self.mouse.scroll(float(payload.get("delta", 0.0)))
                    ack = self.protocol_handler.create_response(
                        "ack", {"ack_id": req_id or "", "status": "ok"}, seq=seq
                    )
                    await websocket.send(ack)

        except websockets.exceptions.ConnectionClosed:
            pass
        finally:
            self.authenticated_sessions.discard(session_id)
            self.protocol_handler.reset_session(session_id)
            self._log(f"WS client disconnected: {session_id}")

    async def start(self) -> None:
        ssl_ctx: Optional[ssl.SSLContext] = None
        if self.enable_tls and self.tls_manager:
            ssl_ctx = self.tls_manager.get_server_ssl_context()

        self.server = await websockets.serve(
            self.handle_ws, self.host, self.port, ssl=ssl_ctx
        )
        self._log(f"WebSocket server running on {self.host}:{self.port} (TLS={self.enable_tls})")

    async def stop(self) -> None:
        if self.server:
            self.server.close()
            await self.server.wait_closed()
            self._log("WebSocket server stopped")
