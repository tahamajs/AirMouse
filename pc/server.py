#!/usr/bin/env python3
import asyncio
import json
import pyautogui
import sys
from dataclasses import dataclass

@dataclass
class MouseController:
    sensitivity: float = 0.5

    def __post_init__(self):
        pyautogui.FAILSAFE = True

    def move(self, dx: float, dy: float):
        dx = max(-50, min(50, dx * self.sensitivity))
        dy = max(-50, min(50, dy * self.sensitivity))
        pyautogui.moveRel(dx, dy, duration=0.0)

    def click(self):
        pyautogui.click()

    def scroll(self, delta: int):
        pyautogui.scroll(delta)

class AirMouseServer:
    def __init__(self, host='0.0.0.0', port=8080):
        self.host = host
        self.port = port
        self.mouse = MouseController()

    async def handle_client(self, reader, writer):
        addr = writer.get_extra_info('peername')
        print(f"Connected: {addr}")
        try:
            while True:
                data = await reader.readline()
                if not data:
                    break
                try:
                    msg = json.loads(data.decode().strip())
                    await self.process_message(msg, writer)
                except json.JSONDecodeError:
                    print("Invalid JSON")
        except Exception as e:
            print(f"Error: {e}")
        finally:
            writer.close()
            await writer.wait_closed()
            print(f"Disconnected: {addr}")

    async def process_message(self, msg, writer):
        t = msg.get('type')
        if t == 'move':
            self.mouse.move(msg.get('dx', 0.0), msg.get('dy', 0.0))
        elif t == 'click':
            self.mouse.click()
            await self.send_ack(msg.get('id'), writer)
        elif t == 'scroll':
            self.mouse.scroll(msg.get('delta', 0))
            await self.send_ack(msg.get('id'), writer)

    async def send_ack(self, msg_id, writer):
        if msg_id is not None:
            ack = json.dumps({'type': 'ack', 'id': msg_id})
            writer.write(ack.encode() + b'\n')
            await writer.drain()

    async def start(self):
        server = await asyncio.start_server(self.handle_client, self.host, self.port)
        print(f"Server listening on {self.host}:{self.port}")
        async with server:
            await server.serve_forever()

if __name__ == '__main__':
    server = AirMouseServer()
    try:
        asyncio.run(server.start())
    except KeyboardInterrupt:
        print("\nShutting down...")
        sys.exit(0)