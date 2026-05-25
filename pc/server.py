#!/usr/bin/env python3
import asyncio
import json
import pyautogui
import sys
import logging
import os
from datetime import datetime
from dataclasses import dataclass

# Load configuration
CONFIG_FILE = "config.json"
DEFAULT_CONFIG = {
    "host": "0.0.0.0",
    "port": 8080,
    "sensitivity": 0.5,
    "log_level": "INFO",
    "log_file": "airmouse.log"
}

def load_config():
    if os.path.exists(CONFIG_FILE):
        with open(CONFIG_FILE, 'r') as f:
            return json.load(f)
    else:
        with open(CONFIG_FILE, 'w') as f:
            json.dump(DEFAULT_CONFIG, f, indent=4)
        return DEFAULT_CONFIG

config = load_config()

# Setup logging
logging.basicConfig(
    level=getattr(logging, config["log_level"]),
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler(config["log_file"]),
        logging.StreamHandler()
    ]
)

@dataclass
class MouseController:
    sensitivity: float = config["sensitivity"]
    
    def __post_init__(self):
        pyautogui.FAILSAFE = True
        logging.info(f"Mouse sensitivity set to {self.sensitivity}")
    
    def move(self, dx, dy):
        dx = max(-50, min(50, dx * self.sensitivity))
        dy = max(-50, min(50, dy * self.sensitivity))
        pyautogui.moveRel(dx, dy, duration=0.0)
    
    def click(self, button='left'):
        pyautogui.click(button=button)
        logging.info(f"Click: {button}")
    
    def double_click(self):
        pyautogui.doubleClick()
        logging.info("Double-click")
    
    def scroll(self, delta):
        pyautogui.scroll(delta)
        logging.info(f"Scroll: {delta}")

class AirMouseServer:
    def __init__(self):
        self.host = config["host"]
        self.port = config["port"]
        self.mouse = MouseController()
        self.logger = logging.getLogger(__name__)
    
    async def handle_client(self, reader, writer):
        addr = writer.get_extra_info('peername')
        self.logger.info(f"Connected: {addr}")
        try:
            while True:
                data = await reader.readline()
                if not data:
                    break
                try:
                    msg = json.loads(data.decode().strip())
                    await self.process_message(msg, writer)
                except json.JSONDecodeError as e:
                    self.logger.warning(f"Invalid JSON: {e}")
        except Exception as e:
            self.logger.error(f"Error handling client {addr}: {e}")
        finally:
            writer.close()
            await writer.wait_closed()
            self.logger.info(f"Disconnected: {addr}")
    
    async def process_message(self, msg, writer):
        t = msg.get('type')
        if t == 'move':
            self.mouse.move(msg.get('dx', 0.0), msg.get('dy', 0.0))
        elif t == 'click':
            self.mouse.click()
            await self.send_ack(msg.get('id'), writer)
        elif t == 'doubleclick':
            self.mouse.double_click()
            await self.send_ack(msg.get('id'), writer)
        elif t == 'rightclick':
            self.mouse.click(button='right')
            await self.send_ack(msg.get('id'), writer)
        elif t == 'scroll':
            self.mouse.scroll(msg.get('delta', 0))
            await self.send_ack(msg.get('id'), writer)
        else:
            self.logger.warning(f"Unknown message type: {t}")
    
    async def send_ack(self, msg_id, writer):
        if msg_id is not None:
            ack = json.dumps({'type': 'ack', 'id': msg_id})
            writer.write(ack.encode() + b'\n')
            await writer.drain()
    
    async def start(self):
        server = await asyncio.start_server(self.handle_client, self.host, self.port)
        self.logger.info(f"Server listening on {self.host}:{self.port}")
        async with server:
            await server.serve_forever()

if __name__ == '__main__':
    server = AirMouseServer()
    try:
        asyncio.run(server.start())
    except KeyboardInterrupt:
        logging.info("Shutting down...")
        sys.exit(0)