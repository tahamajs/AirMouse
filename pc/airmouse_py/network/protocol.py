"""
airmouse_py.network.protocol
----------------------------
Defines strict Pydantic models for Protocol V2 serialization, validation,
and session-scoped replay attack prevention.
Rejects extra/unknown fields and enforces numeric bounds on motion and scroll deltas.
Provides backward-compatibility aliases for legacy adapters.
"""

import math
import time
from typing import Any, Dict, List, Literal, Optional, Tuple, Union
from pydantic import BaseModel, ConfigDict, Field, field_validator


class ProtocolError(Exception):
    """Base exception for all protocol validation failures."""
    pass


class ReplayAttackError(ProtocolError):
    """Raised when sequence counter is non-monotonic or timestamp is outside the replay window."""
    pass


class SchemaViolationError(ProtocolError):
    """Raised when incoming JSON structure violates required envelope or payload contracts."""
    pass


class BoundsViolationError(ProtocolError):
    """Raised when coordinate deltas or scroll values exceed safety bounds or are NaN/Inf."""
    pass


# Backward compatibility aliases
ReplayError = ReplayAttackError
SchemaError = SchemaViolationError
BoundsError = BoundsViolationError


class MovePayload(BaseModel):
    """Payload representing 2D cursor displacement."""
    model_config = ConfigDict(extra="forbid")

    dx: float = Field(..., description="Horizontal displacement delta")
    dy: float = Field(..., description="Vertical displacement delta")
    dt: Optional[float] = Field(None, description="Sampling time interval in seconds")

    @field_validator("dx", "dy")
    @classmethod
    def validate_motion_bounds(cls, value: float) -> float:
        if math.isnan(value) or math.isinf(value):
            raise BoundsViolationError("Coordinate delta must be a finite number (not NaN or Inf)")
        if abs(value) > 500.0:
            raise BoundsViolationError(f"Motion delta {value} exceeds absolute limit of 500.0 pixels")
        return value


class ClickPayload(BaseModel):
    """Payload representing mouse button clicks."""
    model_config = ConfigDict(extra="forbid")

    button: Literal["left", "right", "middle", "double"] = "left"


class ScrollPayload(BaseModel):
    """Payload representing wheel scrolling."""
    model_config = ConfigDict(extra="forbid")

    delta: float = Field(..., description="Scroll wheel ticks")

    @field_validator("delta")
    @classmethod
    def validate_scroll_bounds(cls, value: float) -> float:
        if math.isnan(value) or math.isinf(value):
            raise BoundsViolationError("Scroll delta must be finite")
        if abs(value) > 10.0:
            raise BoundsViolationError(f"Scroll delta {value} exceeds absolute limit of 10.0")
        return value


class KeyPayload(BaseModel):
    """Payload representing keyboard events."""
    model_config = ConfigDict(extra="forbid")

    key: str
    modifiers: List[str] = Field(default_factory=list)


class HelloPayload(BaseModel):
    """Client handshake payload containing pairing token."""
    model_config = ConfigDict(extra="ignore")

    device_id: str = ""
    name: str = "AirMouse Client"
    version: str = "2.0.0"
    token: str = ""
    device: Optional[str] = None
    android_version: Optional[str] = None
    model: Optional[str] = None
    manufacturer: Optional[str] = None
    brand: Optional[str] = None
    device_name: Optional[str] = None
    sdk_int: Optional[str] = None
    protocol: Optional[str] = None
    transport: Optional[str] = None


class WelcomePayload(BaseModel):
    """Server handshake approval response."""
    model_config = ConfigDict(extra="forbid")

    server: str
    version: str
    client_id: str
    approved: bool


class AckPayload(BaseModel):
    """Receipt confirmation for reliable commands."""
    model_config = ConfigDict(extra="forbid")

    ack_id: str
    status: Literal["ok", "failed"] = "ok"


class NackPayload(BaseModel):
    """Error or rejection receipt."""
    model_config = ConfigDict(extra="forbid")

    nack_id: str
    error: str
    expected_seq: Optional[int] = None


class DiscoverPayload(BaseModel):
    """UDP discovery broadcast message."""
    model_config = ConfigDict(extra="forbid")

    client_name: str
    nonce: str


class OfferPayload(BaseModel):
    """UDP discovery unicast reply."""
    model_config = ConfigDict(extra="forbid")

    server_name: str
    instance_id: str
    tcp_port: int
    ws_port: int
    tls: bool
    echo_nonce: str


class EmptyPayload(BaseModel):
    """Empty payload for ping, pong, and keep-alive messages."""
    model_config = ConfigDict(extra="forbid")


class WireEnvelope(BaseModel):
    """
    Standard wire envelope for Protocol V2.
    Strictly forbids unexpected fields at the envelope level.
    """
    model_config = ConfigDict(extra="forbid")

    v: Literal[2] = 2
    type: str
    seq: int = Field(..., ge=0, description="Monotonically increasing sequence number")
    ts: int = Field(..., description="Unix timestamp in milliseconds")
    id: Optional[str] = None
    payload: Dict[str, Any]


class SessionTracker:
    """
    Maintains per-connection sequence counters and validates timestamp freshness
    to prevent cross-session interference and replay attacks.
    """

    def __init__(self, replay_window_ms: int = 5000) -> None:
        self.replay_window_ms = replay_window_ms
        self._session_sequences: Dict[str, int] = {}

    def reset_session(self, session_id: str) -> None:
        """Removes session state when a client disconnects."""
        self._session_sequences.pop(session_id, None)

    def validate_envelope(
        self, envelope: WireEnvelope, session_id: str
    ) -> None:
        """
        Validates timing freshness against sliding window and checks strict
        per-session monotonic sequence progression.
        """
        current_time_ms = int(time.time() * 1000)
        if abs(current_time_ms - envelope.ts) > self.replay_window_ms:
            raise ReplayAttackError(
                f"Timestamp {envelope.ts} outside replay window (+/- {self.replay_window_ms}ms, current: {current_time_ms})"
            )

        last_seq = self._session_sequences.get(session_id, -1)
        if envelope.seq <= last_seq:
            raise ReplayAttackError(
                f"Session {session_id}: Duplicate or out-of-order sequence {envelope.seq} (last seen: {last_seq})"
            )
        self._session_sequences[session_id] = envelope.seq


class ProtocolParser:
    """
    Facade parser using Pydantic models to validate wire envelopes and type-specific payloads.
    """

    def __init__(self, replay_window_ms: int = 5000) -> None:
        self.tracker = SessionTracker(replay_window_ms=replay_window_ms)

    def reset_session(self, session_id: str) -> None:
        self.tracker.reset_session(session_id)

    def parse_message(
        self, raw_json: str, session_id: str
    ) -> Tuple[str, BaseModel, Optional[str], int]:
        try:
            envelope = WireEnvelope.model_validate_json(raw_json)
        except Exception as e:
            raise SchemaViolationError(f"Envelope validation error: {e}") from e

        self.tracker.validate_envelope(envelope, session_id)

        typed_payload: BaseModel
        try:
            if envelope.type == "move":
                typed_payload = MovePayload.model_validate(envelope.payload)
            elif envelope.type == "click":
                typed_payload = ClickPayload.model_validate(envelope.payload)
            elif envelope.type == "scroll":
                typed_payload = ScrollPayload.model_validate(envelope.payload)
            elif envelope.type == "key":
                typed_payload = KeyPayload.model_validate(envelope.payload)
            elif envelope.type == "hello":
                typed_payload = HelloPayload.model_validate(envelope.payload)
            elif envelope.type == "welcome":
                typed_payload = WelcomePayload.model_validate(envelope.payload)
            elif envelope.type == "ack":
                typed_payload = AckPayload.model_validate(envelope.payload)
            elif envelope.type == "nack":
                typed_payload = NackPayload.model_validate(envelope.payload)
            elif envelope.type == "discover":
                typed_payload = DiscoverPayload.model_validate(envelope.payload)
            elif envelope.type == "offer":
                typed_payload = OfferPayload.model_validate(envelope.payload)
            elif envelope.type in {"ping", "pong"}:
                typed_payload = EmptyPayload.model_validate(envelope.payload)
            else:
                raise SchemaViolationError(f"Unknown message type: {envelope.type}")
        except BoundsViolationError:
            raise
        except Exception as e:
            raise SchemaViolationError(f"Payload schema validation failed for {envelope.type}: {e}") from e

        return envelope.type, typed_payload, envelope.id, envelope.seq

    # Compatibility method for legacy dict-based callers
    def decode_and_validate(
        self, raw_json: str, session_id: str
    ) -> Tuple[str, Dict[str, Any], Optional[str], int]:
        msg_type, model_obj, req_id, seq = self.parse_message(raw_json, session_id)
        return msg_type, model_obj.model_dump(), req_id, seq

    @staticmethod
    def serialize_response(
        msg_type: str,
        payload: Union[BaseModel, Dict[str, Any]],
        seq: int = 0,
        req_id: Optional[str] = None,
    ) -> str:
        payload_dict = payload.model_dump() if isinstance(payload, BaseModel) else payload
        env = WireEnvelope(
            v=2,
            type=msg_type,
            seq=seq,
            ts=int(time.time() * 1000),
            id=req_id,
            payload=payload_dict,
        )
        return env.model_dump_json() + "\n"

    create_response = serialize_response


# ProtocolHandler alias for backward compatibility
ProtocolHandler = ProtocolParser
