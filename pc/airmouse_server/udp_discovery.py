import socket
import json
import threading
from .config import CONFIG

class UDPDiscoveryServer:
    def __init__(self, callback, ip_provider):
        self.port = CONFIG["discovery_port"]
        self.callback = callback
        self.ip_provider = ip_provider
        self.socket = None
        self.running = False

    def start(self):
        if self.running:
            return
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.socket.bind(('', self.port))
        self.running = True
        threading.Thread(target=self._listen, daemon=True).start()
        self.callback(f"🔍 UDP discovery listening on port {self.port}")

    def _listen(self):
        while self.running:
            try:
                data, addr = self.socket.recvfrom(1024)
                msg = data.decode().strip()
                if msg == "AIRMOUSE_DISCOVER":
                    chosen_ip = self.ip_provider()
                    response = json.dumps({
                        "type": "discovery_response",
                        "port": CONFIG["port"],
                        "ip": chosen_ip
                    })
                    self.socket.sendto(response.encode(), addr)
                    self.callback(f"📡 Responded to discovery from {addr[0]} with IP {chosen_ip}")
            except Exception:
                pass

    def stop(self):
        self.running = False
        if self.socket:
            self.socket.close()