#!/usr/bin/env python3
import asyncio
import json
import pyautogui
import sys
import threading
import tkinter as tk
from tkinter import scrolledtext
from dataclasses import dataclass

@dataclass
class MouseController:
    sensitivity: float = 0.5
    def move(self, dx, dy):
        dx = max(-50, min(50, dx * self.sensitivity))
        dy = max(-50, min(50, dy * self.sensitivity))
        pyautogui.moveRel(dx, dy, duration=0.0)
    def click(self): pyautogui.click()
    def scroll(self, delta): pyautogui.scroll(delta)

class AirMouseServer:
    def __init__(self, host='0.0.0.0', port=8080, log_callback=None):
        self.host = host
        self.port = port
        self.mouse = MouseController()
        self.log_callback = log_callback
    def log(self, msg):
        if self.log_callback:
            self.log_callback(msg)
        else:
            print(msg)
    async def handle_client(self, reader, writer):
        addr = writer.get_extra_info('peername')
        self.log(f"Connected: {addr}")
        try:
            while True:
                data = await reader.readline()
                if not data: break
                msg = json.loads(data.decode().strip())
                t = msg.get('type')
                if t == 'move':
                    self.mouse.move(msg.get('dx',0), msg.get('dy',0))
                elif t == 'click':
                    self.mouse.click()
                    await self.send_ack(msg.get('id'), writer)
                    self.log("Click performed")
                elif t == 'scroll':
                    self.mouse.scroll(msg.get('delta',0))
                    await self.send_ack(msg.get('id'), writer)
                    self.log(f"Scroll {msg.get('delta')}")
        except Exception as e:
            self.log(f"Error: {e}")
        finally:
            writer.close()
            await writer.wait_closed()
            self.log(f"Disconnected: {addr}")
    async def send_ack(self, msg_id, writer):
        if msg_id:
            ack = json.dumps({'type':'ack','id':msg_id})
            writer.write(ack.encode()+b'\n')
            await writer.drain()
    async def start(self):
        server = await asyncio.start_server(self.handle_client, self.host, self.port)
        self.log(f"Server listening on {self.host}:{self.port}")
        async with server:
            await server.serve_forever()

class ServerGUI:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("Air Mouse Server")
        self.root.geometry("500x400")
        self.text_area = scrolledtext.ScrolledText(self.root, wrap=tk.WORD, width=60, height=20)
        self.text_area.pack(padx=10, pady=10, fill=tk.BOTH, expand=True)
        self.start_button = tk.Button(self.root, text="Start Server", command=self.start_server)
        self.start_button.pack(pady=5)
        self.stop_button = tk.Button(self.root, text="Stop Server", command=self.stop_server, state=tk.DISABLED)
        self.stop_button.pack(pady=5)
        self.server = None
        self.loop = None
        self.server_task = None
    def log(self, msg):
        self.text_area.insert(tk.END, f"{msg}\n")
        self.text_area.see(tk.END)
    def start_server(self):
        self.start_button.config(state=tk.DISABLED)
        self.stop_button.config(state=tk.NORMAL)
        self.loop = asyncio.new_event_loop()
        self.server = AirMouseServer(log_callback=self.log)
        def run():
            asyncio.set_event_loop(self.loop)
            self.loop.run_until_complete(self.server.start())
        self.server_task = threading.Thread(target=run, daemon=True)
        self.server_task.start()
    def stop_server(self):
        if self.loop:
            self.loop.call_soon_threadsafe(self.loop.stop)
        self.start_button.config(state=tk.NORMAL)
        self.stop_button.config(state=tk.DISABLED)
        self.log("Server stopped")
    def run(self):
        self.root.mainloop()

if __name__ == "__main__":
    gui = ServerGUI()
    gui.run()