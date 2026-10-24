"""
Tests for TLS certificate generation, loading, permissions, and SSL context generation.
"""

import os
import ssl
import tempfile
import pytest

from security.tls_manager import TLSManager


def test_tls_manager_certificate_generation_and_caching():
    with tempfile.TemporaryDirectory() as tmp_dir:
        tls_mgr = TLSManager(cert_dir=tmp_dir)

        # First run: generates certificates
        tls_mgr.ensure_certificate(additional_ips=["192.168.1.100"])

        assert tls_mgr.cert_path.exists()
        assert tls_mgr.key_path.exists()

        # Check permissions on Unix
        stat_key = os.stat(tls_mgr.key_path)
        assert (stat_key.st_mode & 0o777) == 0o600

        # Read contents
        cert_data = tls_mgr.cert_path.read_bytes()
        assert b"BEGIN CERTIFICATE" in cert_data

        key_data = tls_mgr.key_path.read_bytes()
        assert b"BEGIN RSA PRIVATE KEY" in key_data

        # Server SSL context
        server_ctx = tls_mgr.get_server_ssl_context()
        assert isinstance(server_ctx, ssl.SSLContext)
        assert server_ctx.minimum_version == ssl.TLSVersion.TLSv1_2

        # Client SSL context
        client_ctx = tls_mgr.get_client_ssl_context()
        assert isinstance(client_ctx, ssl.SSLContext)

        # Call ensure_certificate again: should be idempotent and not regenerate
        mtime_before = tls_mgr.cert_path.stat().st_mtime
        tls_mgr.ensure_certificate()
        mtime_after = tls_mgr.cert_path.stat().st_mtime
        assert mtime_before == mtime_after
