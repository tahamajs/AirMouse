import socket
from zeroconf import Zeroconf, ServiceInfo
from .config import CONFIG

class MDNSAdvertiser:
    def __init__(self, log_callback):
        self.log = log_callback
        self.zeroconf = None
        self.info = None

    def start(self, ip, port):
        try:
            self.zeroconf = Zeroconf()
            service_type = "_airmouse._tcp.local."
            service_name = f"{CONFIG['mDNS_name']}.{service_type}"
            self.info = ServiceInfo(
                service_type,
                service_name,
                addresses=[socket.inet_aton(ip)],
                port=port,
                properties={"version": "1.0"},
                server=f"{CONFIG['mDNS_name']}.local."
            )
            self.zeroconf.register_service(self.info)
            self.log(f"🌐 mDNS advertised as {CONFIG['mDNS_name']}.local:{port}")
        except Exception as e:
            self.log(f"⚠️ mDNS failed: {e}")

    def stop(self):
        if self.zeroconf and self.info:
            self.zeroconf.unregister_service(self.info)
            self.zeroconf.close()
            self.log("🛑 mDNS advertisement stopped")