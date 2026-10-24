"""
Tests for embedded HTTP web server and web client static assets serving.
"""

import asyncio
import pytest
from airmouse import AirMouseServer


def test_embedded_web_server_serves_fullscreen_client():
    async def _test():
        server = AirMouseServer(
            host="127.0.0.1",
            tcp_port=9300,
            ws_port=9301,
            udp_port=0,
            web_port=9302,
            enable_tls=False,
        )
        await server.start()

        # Connect with asyncio stream client directly
        reader, writer = await asyncio.open_connection("127.0.0.1", 9302)
        writer.write(b"GET / HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n")
        await writer.drain()

        response = await reader.read(16384)
        writer.close()
        await writer.wait_closed()

        resp_text = response.decode("utf-8")
        assert "HTTP/1.1 200 OK" in resp_text
        assert "text/html" in resp_text
        assert "AirMouse" in resp_text
        assert "btnConnect" in resp_text
        assert "touchpad" in resp_text

        # Test static asset: style.css
        r_css, w_css = await asyncio.open_connection("127.0.0.1", 9302)
        w_css.write(b"GET /style.css HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n")
        await w_css.drain()
        css_data = await r_css.read(4096)
        w_css.close()
        await w_css.wait_closed()

        assert b"HTTP/1.1 200 OK" in css_data
        assert b"text/css" in css_data

        # Test static asset: app.js
        r_js, w_js = await asyncio.open_connection("127.0.0.1", 9302)
        w_js.write(b"GET /app.js HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n")
        await w_js.drain()
        js_data = await r_js.read(4096)
        w_js.close()
        await w_js.wait_closed()

        assert b"HTTP/1.1 200 OK" in js_data
        assert b"javascript" in js_data

        # Test 404
        r_404, w_404 = await asyncio.open_connection("127.0.0.1", 9302)
        w_404.write(b"GET /nonexistent.png HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n")
        await w_404.drain()
        not_found_data = await r_404.read(4096)
        w_404.close()
        await w_404.wait_closed()

        assert b"HTTP/1.1 404 Not Found" in not_found_data

        await server.stop()

    asyncio.run(_test())
