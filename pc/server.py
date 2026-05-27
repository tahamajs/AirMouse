#!/usr/bin/env python3
"""
Air Mouse PC Server (Console Version) – with UDP discovery and ACK.
"""

import asyncio
import json
import pyautogui
import sys
import logging
import os
import socket
import threading
from dataclasses import dataclass
from typing import Optional, Dict

# ---------- Configuration ----------
CONFIG_FILE = "config.json"
DEFAULT_CONFIG = {
    "host": "0.0.0.0",
    "port": 8080,
    "discovery_port": 8081,
    "sensitivity": 0.5,
    "log_level": "INFO",
    "log_file": "airmouse.log"
}

def load_config() -> dict:
    if os.path.exists(CONFIG_FILE):
        with open(CONFIG_FILE, 'r') as f:
            return json.load(f)
    else:
        with open(CONFIG_FILE, 'w') as f:
            json.dump(DEFAULT_CONFIG, f, indent=4)
        return DEFAULT_CONFIG

config = load_config()

# ---------- Logging ----------
logging.basicConfig(
    level=getattr(logging, config["log_level"]),
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(config["log_file"]),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

# ---------- Mouse Controller ----------
@dataclass
class MouseController:
    sensitivity: float = config["sensitivity"]
    stats: Dict = None

    def __post_init__(self):
        pyautogui.FAILSAFE = True
        self.stats = {"clicks":0, "double_clicks":0, "right_clicks":0, "scrolls":0}
        logger.info(f"Mouse sensitivity = {self.sensitivity}")

    def move(self, dx: float, dy: float) -> None:
        dx = max(-50, min(50, dx * self.sensitivity))
        dy = max(-50, min(50, dy * self.sensitivity))
        pyautogui.moveRel(dx, dy, duration=0.0)

    def click(self, button: str = 'left') -> None:
        pyautogui.click(button=button)
        if button == 'left':
            self.stats["clicks"] += 1
        elif button == 'right':
            self.stats["right_clicks"] += 1
        logger.info(f"Click: {button} (total left: {self.stats['clicks']})")

    def double_click(self) -> None:
        pyautogui.doubleClick()
        self.stats["double_clicks"] += 1
        logger.info(f"Double-click (total: {self.stats['double_clicks']})")

    def scroll(self, delta: int) -> None:
        pyautogui.scroll(delta)
        self.stats["scrolls"] += 1
        logger.info(f"Scroll: {delta} (total: {self.stats['scrolls']})")

# ---------- UDP Discovery ----------
class UDPDiscovery:
    def __init__(self, port: int):
        self.port = port
        self.sock = None

    def start(self):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.bind(('', self.port))
        threading.Thread(target=self._listen, daemon=True).start()
        logger.info(f"UDP discovery listening on port {self.port}")

    def _listen(self):
        while True:
            try:
                data, addr = self.sock.recvfrom(1024)
                if data.decode().strip() == "AIRMOUSE_DISCOVER":
                    resp = json.dumps({"type":"discovery_response","port":config["port"]})
                    self.sock.sendto(resp.encode(), addr)
                    logger.info(f"Discovery response sent to {addr[0]}")
            except:
                break

    def stop(self):
        if self.sock:
            self.sock.close()

# ---------- TCP Server ----------
class AirMouseServer:
    def __init__(self):
        self.host = config["host"]
        self.port = config["port"]
        self.mouse = MouseController()
        self.udp = UDPDiscovery(config["discovery_port"])

    async def handle_client(self, reader, writer):
        addr = writer.get_extra_info('peername')
        logger.info(f"Connected: {addr}")
        try:
            while True:
                data = await reader.readline()
                if not data:
                    break
                try:
                    msg = json.loads(data.decode().strip())
                    await self._process_message(msg, writer)
                except json.JSONDecodeError:
                    logger.warning(f"Invalid JSON from {addr}")
        except Exception as e:
            logger.error(f"Error: {e}")
        finally:
            writer.close()
            await writer.wait_closed()
            logger.info(f"Disconnected: {addr}")

    async def _process_message(self, msg: dict, writer):
        t = msg.get('type')
        if t == 'move':
            self.mouse.move(msg.get('dx',0), msg.get('dy',0))
        elif t == 'click':
            self.mouse.click()
            await self._send_ack(msg.get('id'), writer)
        elif t == 'doubleclick':
            self.mouse.double_click()
            await self._send_ack(msg.get('id'), writer)
        elif t == 'rightclick':
            self.mouse.click(button='right')
            await self._send_ack(msg.get('id'), writer)
        elif t == 'scroll':
            self.mouse.scroll(msg.get('delta',0))
            await self._send_ack(msg.get('id'), writer)
        else:
            logger.warning(f"Unknown type: {t}")

    async def _send_ack(self, msg_id, writer):
        if msg_id:
            ack = json.dumps({'type':'ack','id':msg_id})
            writer.write(ack.encode()+b'\n')
            await writer.drain()

    async def start(self):
        self.udp.start()
        server = await asyncio.start_server(self.handle_client, self.host, self.port)
        logger.info(f"TCP server listening on {self.host}:{self.port}")
        async with server:
            await server.serve_forever()

    async def stop(self):
        self.udp.stop()
        # Server will be closed by context manager

if __name__ == "__main__":
    server = AirMouseServer()
    try:
        asyncio.run(server.start())
    except KeyboardInterrupt:
        logger.info("Shutting down...")
        sys.exit(0)