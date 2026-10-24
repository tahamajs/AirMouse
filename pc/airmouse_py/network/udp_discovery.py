import json
import logging
import socket
import threading
import time
from typing import Callable, Optional

logger = logging.getLogger("UDPDiscovery")


class UDPDiscoveryServer:
    """
    Listens for UDP discovery broadcast messages from clients, validates
    the challenge nonce, and responds with server connection parameters.
    """

    def __init__(
        self,
        port: int = 8083,
        server_name: str = "AirMouse Python Server",
        tcp_port: int = 8080,
        ws_port: int = 8081,
        tls_enabled: bool = True,
        on_discovered: Optional[Callable[[str, int], None]] = None,
    ):
        self.port = port
        self.server_name = server_name
        self.tcp_port = tcp_port
        self.ws_port = ws_port
        self.tls_enabled = tls_enabled
        self.on_discovered = on_discovered
        self.running = False
        self._thread: Optional[threading.Thread] = None
        self._sock: Optional[socket.socket] = None

    def start(self) -> None:
        if self.running:
            return
        self.running = True
        self._sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self._sock.bind(("", self.port))
        self._sock.settimeout(1.0)

        self._thread = threading.Thread(target=self._listen_loop, daemon=True)
        self._thread.start()
        logger.info(f"UDP Discovery server listening on port {self.port}")

    def _listen_loop(self) -> None:
        while self.running:
            try:
                data, addr = self._sock.recvfrom(4096)
                if not data:
                    continue

                try:
                    payload = json.loads(data.decode("utf-8"))
                except Exception:
                    continue

                if payload.get("type") == "discover" and payload.get("v") == 2:
                    p = payload.get("payload", {})
                    nonce = p.get("nonce", "")
                    client_name = p.get("client_name", "Unknown")

                    if self.on_discovered:
                        self.on_discovered(client_name, addr[0])

                    # Challenge-Response with echo_nonce
                    response = {
                        "v": 2,
                        "type": "offer",
                        "seq": 1,
                        "ts": int(time.time() * 1000),
                        "payload": {
                            "server_name": self.server_name,
                            "instance_id": "py_airmouse_srv",
                            "tcp_port": self.tcp_port,
                            "ws_port": self.ws_port,
                            "tls": self.tls_enabled,
                            "echo_nonce": nonce,
                        },
                    }
                    resp_bytes = (json.dumps(response) + "\n").encode("utf-8")
                    self._sock.sendto(resp_bytes, addr)
            except socket.timeout:
                continue
            except Exception as e:
                if self.running:
                    logger.error(f"UDP discovery error: {e}")

    def stop(self) -> None:
        self.running = False
        if self._sock:
            try:
                self._sock.close()
            except Exception:
                pass
        if self._thread and self._thread.is_alive():
            self._thread.join(timeout=1.0)
        logger.info("UDP Discovery server stopped")
