"""
Unit tests for CLI parsing, local IP resolution, and terminal QR code output.
"""

import io
import json
from unittest.mock import patch

from airmouse.cli import display_terminal_qr, get_local_ip, parse_args


def test_cli_argument_parser_defaults():
    args = parse_args([])
    assert args.host == "0.0.0.0"
    assert args.port == 8080
    assert args.ws_port == 8081
    assert args.udp_port == 8083
    assert args.no_tls is False
    assert args.no_smoothing is False
    assert args.sensitivity == 1.0
    assert args.qr is False


def test_cli_argument_parser_custom():
    custom_args = [
        "--host", "127.0.0.1",
        "--port", "9000",
        "--ws-port", "9001",
        "--udp-port", "9002",
        "--no-tls",
        "--no-smoothing",
        "--sensitivity", "2.5",
        "--qr",
        "--name", "My Office PC",
    ]
    args = parse_args(custom_args)
    assert args.host == "127.0.0.1"
    assert args.port == 9000
    assert args.ws_port == 9001
    assert args.udp_port == 9002
    assert args.no_tls is True
    assert args.no_smoothing is True
    assert args.sensitivity == 2.5
    assert args.qr is True
    assert args.name == "My Office PC"


def test_get_local_ip_returns_valid_string():
    ip = get_local_ip()
    assert isinstance(ip, str)
    assert len(ip.split(".")) == 4


def test_display_terminal_qr_executes_without_error():
    payload = json.dumps({"test": "data", "token": "abc"})
    with patch("sys.stdout", new_callable=io.StringIO) as mock_stdout:
        display_terminal_qr(payload)
        output = mock_stdout.getvalue()
        assert len(output) > 0
