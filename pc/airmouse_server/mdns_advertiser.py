import socket
from zeroconf import Zeroconf, ServiceInfo
from .config import CONFIG

class MDNSAdvertiser:
    def __init__(self, log_callback):
        self.log = log_callback
        self.zeroconf = None
        self.info = None

    def start(self, ip: str, port: int):
        try:
            self.zeroconf = Zeroconf()
            service_type = "_airmouse._tcp.local."
            name = f"{CONFIG.get('mDNS_name', 'airmouse')}.{service_type}"
            self.info = ServiceInfo(
                service_type,
                name,
                addresses=[socket.inet_aton(ip)],
                port=port,
                properties={"version": "1.0"},
                server=f"{CONFIG.get('mDNS_name', 'airmouse')}.local."
            )
            self.zeroconf.register_service(self.info)
            self.log(f"🌐 mDNS advertised as {CONFIG.get('mDNS_name', 'airmouse')}.local:{port}")
        except Exception as e:
            self.log(f"⚠️ mDNS failed: {e}")

    def stop(self):
        if self.zeroconf and self.info:
            self.zeroconf.unregister_service(self.info)
            self.zeroconf.close()
            self.log("🛑 mDNS advertisement stopped")