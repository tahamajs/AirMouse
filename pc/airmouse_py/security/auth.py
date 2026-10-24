"""
airmouse_py.security.auth
-------------------------
Persistent HMAC-SHA256 authentication and pairing token manager.
Stores the master 32-byte cryptographic secret key in `~/.airmouse/secret.key`
with strict 0600 file permissions, ensuring keys persist across server restarts
while preventing unauthorized file system access.
"""

import hashlib
import hmac
import logging
import os
from pathlib import Path
import secrets
import time
from typing import Dict, Optional

logger = logging.getLogger("AirMouse.Auth")


class AuthManager:
    """
    Manages persistent master secret storage and HMAC-SHA256 token validation.
    """

    def __init__(
        self,
        secret_dir: Optional[str] = None,
        token_ttl_seconds: int = 86400,
    ) -> None:
        if secret_dir is None:
            self.base_dir = Path.home() / ".airmouse"
        else:
            self.base_dir = Path(secret_dir)

        self.base_dir.mkdir(parents=True, exist_ok=True)
        try:
            os.chmod(self.base_dir, 0o700)
        except OSError:
            pass

        self.secret_path = self.base_dir / "secret.key"
        self.token_ttl_seconds = token_ttl_seconds
        self.active_tokens: Dict[str, float] = {}

        # Load or generate persistent secret
        self.secret = self._load_or_create_secret()

    def _load_or_create_secret(self) -> bytes:
        """
        Loads the 32-byte master secret from disk or securely generates a new one.
        Ensures file permissions are locked to 0600.
        """
        if self.secret_path.exists():
            try:
                secret_data = self.secret_path.read_bytes()
                if len(secret_data) >= 32:
                    logger.debug(f"Loaded persistent master secret from {self.secret_path}")
                    return secret_data[:32]
            except Exception as e:
                logger.warning(f"Failed to read existing secret file, regenerating: {e}")

        # Generate a new 32-byte cryptographically secure random key
        new_secret = secrets.token_bytes(32)

        # Write to disk with 0600 permissions
        self.secret_path.touch(mode=0o600, exist_ok=True)
        try:
            os.chmod(self.secret_path, 0o600)
        except OSError:
            pass

        with open(self.secret_path, "wb") as f:
            f.write(new_secret)

        logger.info(f"Initialized new persistent pairing secret at {self.secret_path}")
        return new_secret

    def generate_token(self, client_id: str) -> str:
        """
        Generates a signed HMAC-SHA256 pairing token formatted as:
        <client_id>.<timestamp>.<nonce>.<hmac_signature>
        """
        nonce = secrets.token_hex(16)
        timestamp = str(int(time.time()))
        message = f"{client_id}:{timestamp}:{nonce}".encode("utf-8")
        signature = hmac.new(self.secret, message, hashlib.sha256).hexdigest()
        token = f"{client_id}.{timestamp}.{nonce}.{signature}"

        self.active_tokens[token] = time.time() + self.token_ttl_seconds
        return token

    def validate_token(self, token: str) -> bool:
        """
        Validates token format, expiration window, and cryptographic HMAC signature.
        Uses constant-time comparison to prevent timing side-channel attacks.
        """
        if not token or not isinstance(token, str):
            return False

        parts = token.split(".")
        if len(parts) != 4:
            return False

        client_id, timestamp_str, nonce, signature = parts
        try:
            token_timestamp = int(timestamp_str)
        except ValueError:
            return False

        current_time = time.time()
        # Verify TTL expiration and reject tokens claiming to be far in the future (+300s window)
        if current_time - token_timestamp > self.token_ttl_seconds or token_timestamp > current_time + 300:
            return False

        message = f"{client_id}:{timestamp_str}:{nonce}".encode("utf-8")
        expected_sig = hmac.new(self.secret, message, hashlib.sha256).hexdigest()

        # Constant-time comparison
        return hmac.compare_digest(signature, expected_sig)

    def rotate_secret(self) -> None:
        """
        Forces a rotation of the master secret key and overwrites the persisted file.
        Invalidates all previously issued pairing tokens.
        """
        self.secret = secrets.token_bytes(32)
        with open(self.secret_path, "wb") as f:
            f.write(self.secret)
        self.active_tokens.clear()
        logger.info("Master secret rotated and persistent keyfile updated.")
