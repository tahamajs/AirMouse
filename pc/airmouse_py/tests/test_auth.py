import time
from security.auth import AuthManager


def test_auth_token_generation_and_validation():
    manager = AuthManager()
    token = manager.generate_token("test_device_001")
    assert manager.validate_token(token) is True


def test_auth_token_tampered():
    manager = AuthManager()
    token = manager.generate_token("test_device_001")
    tampered = token[:-4] + "ffff"
    assert manager.validate_token(tampered) is False


def test_auth_token_secret_rotation():
    manager = AuthManager()
    token = manager.generate_token("test_device_001")
    assert manager.validate_token(token) is True
    manager.rotate_secret()
    assert manager.validate_token(token) is False


def test_auth_token_future_and_expired(tmp_path):
    manager = AuthManager(secret_dir=str(tmp_path), token_ttl_seconds=10)
    # Token claiming to be issued in the future (> 300s)
    future_ts = str(int(time.time()) + 500)
    msg = f"test_dev:{future_ts}:fake_nonce".encode("utf-8")
    import hashlib, hmac
    sig = hmac.new(manager.secret, msg, hashlib.sha256).hexdigest()
    future_token = f"test_dev.{future_ts}.fake_nonce.{sig}"
    assert manager.validate_token(future_token) is False

    # Malformed tokens
    assert manager.validate_token("") is False
    assert manager.validate_token("abc.def") is False
    assert manager.validate_token("a.b.c.d.e") is False
    assert manager.validate_token("dev.not_an_int.nonce.sig") is False

