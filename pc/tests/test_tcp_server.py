import asyncio
from airmouse_server.tcp_server import AirMouseTCPServer


def test_close_connection_noop():
    """Calling close_connection on a non-existent address should not raise."""
    log_cb = lambda m, level='info': None
    stats_cb = lambda s: None
    server = AirMouseTCPServer(log_cb, stats_cb)

    # run the coroutine; should return quickly and not raise
    asyncio.run(server.close_connection(("127.0.0.1", 65000)))

    assert True
