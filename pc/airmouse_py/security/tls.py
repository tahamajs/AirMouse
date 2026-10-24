"""
Security & TLS Certificate Management for AirMouse Protocol V2
"""
import os
import ssl
from pathlib import Path
from datetime import datetime, timedelta, timezone
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import serialization

def ensure_tls_certificates(cert_dir: Path = None) -> tuple[Path, Path]:
    """
    Ensure self-signed ECDSA P-256 certificate and private key exist.
    Generates them dynamically if absent and saves to ~/.airmouse/certs/ with 0600 permissions.
    """
    if cert_dir is None:
        cert_dir = Path.home() / ".airmouse" / "certs"
    cert_dir.mkdir(parents=True, exist_ok=True)
    
    cert_path = cert_dir / "server.crt"
    key_path = cert_dir / "server.key"
    
    if cert_path.exists() and key_path.exists():
        return cert_path, key_path
        
    private_key = ec.generate_private_key(ec.SECP256R1())
    
    subject = issuer = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, "US"),
        x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, "State"),
        x509.NameAttribute(NameOID.LOCALITY_NAME, "City"),
        x509.NameAttribute(NameOID.ORGANIZATION_NAME, "AirMouse"),
        x509.NameAttribute(NameOID.COMMON_NAME, "localhost"),
    ])
    
    now = datetime.now(timezone.utc)
    cert = (
        x509.CertificateBuilder()
        .subject_name(subject)
        .issuer_name(issuer)
        .public_key(private_key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(now - timedelta(days=1))
        .not_valid_after(now + timedelta(days=3650))
        .add_extension(
            x509.SubjectAlternativeName([
                x509.DNSName("localhost"),
                x509.IPAddress(ipaddress.IPv4Address("127.0.0.1")),
            ]),
            critical=False,
        )
        .sign(private_key, hashes.SHA256())
    )
    
    key_bytes = private_key.private_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption(),
    )
    cert_bytes = cert.public_bytes(serialization.Encoding.PEM)
    
    key_path.write_bytes(key_bytes)
    os.chmod(key_path, 0o600)
    
    cert_path.write_bytes(cert_bytes)
    os.chmod(cert_path, 0o644)
    
    return cert_path, key_path

import ipaddress
