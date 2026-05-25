#!/usr/bin/env python3
"""
Air Mouse PC Server (Console Version)
Listens for TCP connections from the Android app and controls the mouse.
Uses config.json for settings and logs to both file and console.
"""

import asyncio
import json
import pyautogui
import sys
import logging
import os
from dataclasses import dataclass
from typing import Optional

# ---------- Configuration ----------
CONFIG_FILE = "config.json"
DEFAULT_CONFIG = {
    "host": "0.0.0.0",
    "port": 8080,
    "sensitivity": 0.5,
    "log_level": "INFO",
    "log_file": "airmouse.log"
}

def load_config() -> dict:
    """Load configuration from JSON file, create default if missing."""
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

    def __post_init__(self):
        pyautogui.FAILSAFE = True
        logger.info(f"Mouse sensitivity set to {self.sensitivity}")

    def move(self, dx: float, dy: float) -> None:
        dx = max(-50, min(50, dx * self.sensitivity))
        dy = max(-50, min(50, dy * self.sensitivity))
        pyautogui.moveRel(dx, dy, duration=0.0)

    def click(self, button: str = 'left') -> None:
        pyautogui.click(button=button)
        logger.info(f"Click: {button}")

    def double_click(self) -> None:
        pyautogui.doubleClick()
        logger.info("Double-click")

    def scroll(self, delta: int) -> None:
        pyautogui.scroll(delta)
        logger.info(f"Scroll: {delta}")

# ---------- TCP Server ----------
class AirMouseServer:
    def __init__(self):
        self.host = config["host"]
        self.port = config["port"]
        self.mouse = MouseController()

    async def handle_client(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter):
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
                except json.JSONDecodeError as e:
                    logger.warning(f"Invalid JSON from {addr}: {e}")
        except Exception as e:
            logger.error(f"Error handling {addr}: {e}")
        finally:
            writer.close()
            await writer.wait_closed()
            logger.info(f"Disconnected: {addr}")

    async def _process_message(self, msg: dict, writer: asyncio.StreamWriter):
        t = msg.get('type')
        if t == 'move':
            self.mouse.move(msg.get('dx', 0.0), msg.get('dy', 0.0))
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
            self.mouse.scroll(msg.get('delta', 0))
            await self._send_ack(msg.get('id'), writer)
        else:
            logger.warning(f"Unknown message type: {t}")

    async def _send_ack(self, msg_id: Optional[int], writer: asyncio.StreamWriter):
        if msg_id is not None:
            ack = json.dumps({'type': 'ack', 'id': msg_id})
            writer.write(ack.encode() + b'\n')
            await writer.drain()

    async def start(self):
        server = await asyncio.start_server(self.handle_client, self.host, self.port)
        logger.info(f"Server listening on {self.host}:{self.port}")
        async with server:
            await server.serve_forever()

# ---------- Main ----------
if __name__ == '__main__':
    server = AirMouseServer()
    try:
        asyncio.run(server.start())
    except KeyboardInterrupt:
        logger.info("Shutting down...")
        sys.exit(0)