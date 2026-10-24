import asyncio
import logging
import signal
import sys
from network.tcp_server import TCPProtocolServer
from network.udp_discovery import UDPDiscoveryServer
from network.ws_server import WebSocketProtocolServer
from security.auth import AuthManager

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger("AirMouseMain")


async def main():
    auth_mgr = AuthManager()
    token = auth_mgr.generate_token("default_client")

    logger.info("==================================================")
    logger.info("AirMouse Server (Production Python Edition)")
    logger.info(f"Generated Pairing Token: {token}")
    logger.info("==================================================")

    tcp_srv = TCPProtocolServer(host="0.0.0.0", port=8080, enable_tls=True, auth_manager=auth_mgr)
    ws_srv = WebSocketProtocolServer(host="0.0.0.0", port=8081, enable_tls=True, auth_manager=auth_mgr)
    udp_srv = UDPDiscoveryServer(port=8083, tcp_port=8080, ws_port=8081, tls_enabled=True)

    udp_srv.start()
    await tcp_srv.start()
    await ws_srv.start()

    logger.info("AirMouse Python server is ready and accepting clients.")

    stop_event = asyncio.Event()

    def _sig_handler():
        logger.info("Shutdown signal received...")
        stop_event.set()

    for sig in (signal.SIGINT, signal.SIGTERM):
        try:
            asyncio.get_running_loop().add_signal_handler(sig, _sig_handler)
        except NotImplementedError:
            pass

    await stop_event.wait()
    udp_srv.stop()
    await tcp_srv.stop()
    await ws_srv.stop()
    logger.info("Server clean shutdown completed.")


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass
