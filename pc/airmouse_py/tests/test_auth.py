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
