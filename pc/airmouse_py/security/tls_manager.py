"""
airmouse_py.security.tls_manager
--------------------------------
Handles persistent generation, storage, and loading of self-signed TLS certificates.
Ensures that server certificates and private keys persist across reboots so that
Android TLS pinning and persistent trust stores do not break on server restart.
Enforces 0600 file permissions on private keys.
"""

import datetime
import ipaddress
import logging
import os
from pathlib import Path
import ssl
from typing import List, Optional

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID

logger = logging.getLogger("AirMouse.TLS")


class TLSManager:
    """
    Manages persistent X.509 certificate and private key lifecycle.
    Loads existing credentials from disk or creates new persistent credentials
    with strict 0600 file permissions if none exist.
    """

    def __init__(self, cert_dir: Optional[str] = None) -> None:
        if cert_dir is None:
            self.cert_dir = Path.home() / ".airmouse" / "certs"
        else:
            self.cert_dir = Path(cert_dir)

        # Create base directory with 0700 permissions
        self.cert_dir.mkdir(parents=True, exist_ok=True)
        try:
            os.chmod(self.cert_dir, 0o700)
        except OSError as e:
            logger.warning(f"Could not enforce 0700 on cert directory: {e}")

        self.cert_path = self.cert_dir / "server.crt"
        self.key_path = self.cert_dir / "server.key"

    def ensure_certificate(self, additional_ips: Optional[List[str]] = None) -> None:
        """
        Ensures that a valid persistent certificate and key exist on disk.
        If both files already exist, does not overwrite them, preserving TLS pinning.
        """
        if self.cert_path.exists() and self.key_path.exists():
            logger.debug(f"Loaded existing TLS credentials from {self.cert_dir}")
            return

        logger.info(f"Generating new persistent self-signed TLS certificate at {self.cert_dir}")

        # Generate RSA 2048 private key
        private_key = rsa.generate_private_key(
            public_exponent=65537,
            key_size=2048,
        )

        subject = issuer = x509.Name(
            [
                x509.NameAttribute(NameOID.COUNTRY_NAME, "US"),
                x509.NameAttribute(NameOID.ORGANIZATION_NAME, "AirMouse"),
                x509.NameAttribute(NameOID.COMMON_NAME, "AirMouse Desktop Server"),
            ]
        )

        # Populate Subject Alternative Names (SANs) for LAN loopback and local subnets
        alt_names: List[x509.GeneralName] = [
            x509.DNSName("localhost"),
            x509.IPAddress(ipaddress.IPv4Address("127.0.0.1")),
            x509.IPAddress(ipaddress.IPv6Address("::1")),
        ]

        if additional_ips:
            for ip_entry in additional_ips:
                try:
                    alt_names.append(x509.IPAddress(ipaddress.ip_address(ip_entry)))
                except ValueError:
                    alt_names.append(x509.DNSName(ip_entry))

        now = datetime.datetime.now(datetime.timezone.utc)
        certificate = (
            x509.CertificateBuilder()
            .subject_name(subject)
            .issuer_name(issuer)
            .public_key(private_key.public_key())
            .serial_number(x509.random_serial_number())
            .not_valid_before(now - datetime.timedelta(hours=1))
            .not_valid_after(now + datetime.timedelta(days=730))  # 2 years validity
            .add_extension(x509.SubjectAlternativeName(alt_names), critical=False)
            .sign(private_key, hashes.SHA256())
        )

        # Write private key with strict 0600 file permissions
        key_bytes = private_key.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.TraditionalOpenSSL,
            encryption_algorithm=serialization.NoEncryption(),
        )

        # Ensure file exists before chmod
        self.key_path.touch(mode=0o600, exist_ok=True)
        try:
            os.chmod(self.key_path, 0o600)
        except OSError:
            pass

        with open(self.key_path, "wb") as f:
            f.write(key_bytes)

        # Write certificate (readable by client)
        cert_bytes = certificate.public_bytes(serialization.Encoding.PEM)
        with open(self.cert_path, "wb") as f:
            f.write(cert_bytes)

        logger.info("Persistent TLS certificate and key written successfully.")

    def get_server_ssl_context(self) -> ssl.SSLContext:
        """
        Creates and returns a server-side SSLContext configured for TLS 1.2+
        using the persistent certificate and key.
        """
        self.ensure_certificate()
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        context.load_cert_chain(certfile=str(self.cert_path), keyfile=str(self.key_path))
        context.minimum_version = ssl.TLSVersion.TLSv1_2
        return context

    def get_client_ssl_context(self) -> ssl.SSLContext:
        """
        Creates and returns a client-side SSLContext trusting the self-signed server certificate.
        Used for internal integration testing.
        """
        self.ensure_certificate()
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
        context.load_verify_locations(cafile=str(self.cert_path))
        context.check_hostname = False
        return context
