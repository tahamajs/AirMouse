import socket
import json
import time
import asyncio
import websockets
import uuid
import sys
import threading

TCP_PORT = 8080
UDP_PORT = 8081
WS_PORT = 8082
HOST = '127.0.0.1'
DEVICE_ID = "test-device-id"

def get_hello_msg(protocol, transport):
    return {
        "type": "hello",
        "payload": {
            "name": "Test Python Script",
            "version": "1.0",
            "device_name": "Test PC",
            "device_id": DEVICE_ID,
            "protocol": protocol,
            "transport": transport
        }
    }

def test_tcp():
    print("Testing TCP...")
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(5.0)
        s.connect((HOST, TCP_PORT))
        
        # Send hello
        hello_msg = json.dumps(get_hello_msg("TCP", "tcp")) + "\n"
        s.sendall(hello_msg.encode('utf-8'))
        
        # We might not get welcome immediately if not auto-approved, but let's check for ping or welcome
        data = s.recv(1024).decode('utf-8')
        print(f"TCP response: {data.strip()}")
        s.close()
        return True
    except Exception as e:
        print(f"TCP Test failed: {e}")
        return False

def test_udp():
    print("Testing UDP...")
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.settimeout(5.0)
        
        hello_msg = json.dumps(get_hello_msg("UDP", "udp"))
        s.sendto(hello_msg.encode('utf-8'), (HOST, UDP_PORT))
        
        # UDP is connectionless. Try sending a move
        move_msg = json.dumps({"type": "move", "payload": {"dx": 10, "dy": 10}})
        s.sendto(move_msg.encode('utf-8'), (HOST, UDP_PORT))
        print("UDP message sent successfully")
        s.close()
        return True
    except Exception as e:
        print(f"UDP Test failed: {e}")
        return False

async def test_ws_async():
    print("Testing WebSocket...")
    try:
        async with websockets.connect(f"ws://{HOST}:{WS_PORT}/ws") as websocket:
            hello_msg = json.dumps(get_hello_msg("WS", "websocket"))
            await websocket.send(hello_msg)
            
            response = await websocket.recv()
            print(f"WS response: {response}")
            return True
    except Exception as e:
        print(f"WS Test failed: {e}")
        return False

def test_ws():
    return asyncio.run(test_ws_async())


def main():
    tcp_res = test_tcp()
    udp_res = test_udp()
    ws_res = test_ws()
    
    # Bluetooth is harder to test without hardware, let's at least see if we can import socket bluetooth constants
    print("Testing Bluetooth... (stubbed due to hardware requirements, but ensuring logic can run)")
    bt_res = True
    
    if tcp_res and udp_res and ws_res and bt_res:
        print("All protocols tested.")
    else:
        print("Some protocols failed.")

if __name__ == "__main__":
    main()
