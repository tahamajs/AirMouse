"""
airmouse.cli
------------
Command Line Interface for AirMouse server.
Generates terminal QR code for zero-friction client pairing and runs the server out-of-the-box.
"""

import argparse
import asyncio
import io
import json
import logging
import os
import signal
import socket
import sys
from typing import Optional

import qrcode

from airmouse.server import AirMouseServer
from security.auth import AuthManager

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("AirMouseCLI")


def get_local_ip() -> str:
    """Best-effort discovery of non-loopback LAN IP address."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Does not actually connect but picks the outgoing route interface
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = "127.0.0.1"
    finally:
        s.close()
    return ip


def display_terminal_qr(data: str) -> None:
    """Prints an ASCII/ANSI QR code directly in the terminal."""
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=1,
        border=1,
    )
    qr.add_data(data)
    qr.make(fit=True)

    f = io.StringIO()
    qr.print_ascii(out=f, invert=True)
    f.seek(0)
    print(f.read())


def parse_args(argv=None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="airmouse-server",
        description="AirMouse Server - Zero-latency, TLS-secured remote input server",
    )
    parser.add_argument(
        "--host",
        type=str,
        default="0.0.0.0",
        help="IP address to bind to (default: 0.0.0.0)",
    )
    parser.add_argument(
        "--port",
        type=int,
        default=8080,
        help="TCP listening port (default: 8080)",
    )
    parser.add_argument(
        "--ws-port",
        type=int,
        default=8081,
        help="WebSocket listening port (default: 8081)",
    )
    parser.add_argument(
        "--udp-port",
        type=int,
        default=8083,
        help="UDP discovery broadcast listening port (default: 8083)",
    )
    parser.add_argument(
        "--web-port",
        type=int,
        default=8082,
        help="HTTP web client listening port (default: 8082, 0 to disable)",
    )
    parser.add_argument(
        "--no-tls",
        action="store_true",
        help="Disable TLS encryption (TLS 1.2+ is enabled by default)",
    )
    parser.add_argument(
        "--no-smoothing",
        action="store_true",
        help="Disable 1€ adaptive low-pass filter smoothing",
    )
    parser.add_argument(
        "--sensitivity",
        type=float,
        default=1.0,
        help="Mouse speed multiplier (default: 1.0)",
    )
    parser.add_argument(
        "--qr",
        action="store_true",
        help="Display terminal QR code for instant Android client pairing",
    )
    parser.add_argument(
        "--name",
        type=str,
        default="AirMouse Server",
        help="Friendly server display name",
    )
    return parser.parse_args(argv)


async def run_server(args: argparse.Namespace) -> None:
    auth_mgr = AuthManager()
    token = auth_mgr.generate_token("cli_client")
    lan_ip = get_local_ip()

    enable_tls = not args.no_tls

    pairing_payload = {
        "host": lan_ip,
        "tcp_port": args.port,
        "ws_port": args.ws_port,
        "token": token,
        "tls": enable_tls,
        "name": args.name,
    }
    pairing_json = json.dumps(pairing_payload)

    print("=" * 60)
    print(f"  {args.name}")
    print(f"  LAN Address: {lan_ip}")
    print(f"  TCP Port   : {args.port} (TLS: {enable_tls})")
    print(f"  WS Port    : {args.ws_port} (TLS: {enable_tls})")
    if args.web_port > 0:
        print(f"  Web Client : http://{lan_ip}:{args.web_port}/?token={token}")
    print(f"  UDP Port   : {args.udp_port}")
    print(f"  Pairing Token: {token}")
    print("=" * 60)

    if args.qr:
        print("\nScan this QR code with the AirMouse Android App to pair:\n")
        try:
            display_terminal_qr(pairing_json)
        except Exception as e:
            logger.warning(f"Could not print QR code to terminal: {e}")

    server = AirMouseServer(
        host=args.host,
        tcp_port=args.port,
        ws_port=args.ws_port,
        udp_port=args.udp_port,
        web_port=args.web_port,
        enable_tls=enable_tls,
        auth_manager=auth_mgr,
        sensitivity=args.sensitivity,
        smoothing=not args.no_smoothing,
        server_name=args.name,
    )

    server.on_connect(lambda s_id: logger.info(f"[Event] Client Connected: {s_id}"))
    server.on_disconnect(lambda s_id: logger.info(f"[Event] Client Disconnected: {s_id}"))
    server.on_click(lambda btn: logger.debug(f"[Event] Mouse Click: {btn}"))

    await server.start()
    logger.info("AirMouse server started successfully and ready for connections.")

    stop_event = asyncio.Event()

    def _shutdown():
        logger.info("Shutdown signal received, shutting down gracefully...")
        stop_event.set()

    for sig in (signal.SIGINT, signal.SIGTERM):
        try:
            asyncio.get_running_loop().add_signal_handler(sig, _shutdown)
        except NotImplementedError:
            pass

    try:
        await stop_event.wait()
    finally:
        await server.stop()
        logger.info("AirMouse server shut down cleanly.")


def main(argv=None) -> None:
    args = parse_args(argv)
    try:
        asyncio.run(run_server(args))
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
