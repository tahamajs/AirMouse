"""
AirMouse Core Library
~~~~~~~~~~~~~~~~~~~~~
Production-grade Python package for AirMouse servers and input processing.
"""

from core.jitter_buffer import UDPJitterBuffer
from core.one_euro import OneEuroFilter
from network.protocol import ProtocolParser
from security.auth import AuthManager
from airmouse.server import AirMouseServer

__version__ = "2.0.0"

__all__ = [
    "AirMouseServer",
    "OneEuroFilter",
    "UDPJitterBuffer",
    "AuthManager",
    "ProtocolParser",
]
