# Security Policy

## Supported Versions

The following versions of **AirMouse** currently receive security updates and vulnerability patches:

| Version | Supported | Protocol Support | TLS Requirements |
|---------|-----------|------------------|------------------|
| **2.1.x** | :white_check_mark: Yes | Protocol V2 Canonical | TLS 1.2+ mandatory |
| **2.0.x** | :white_check_mark: Yes | Protocol V2 Canonical | TLS 1.2+ mandatory |
| **1.0.x** | :x: Deprecated | Protocol V1 Legacy | Unencrypted (Deprecated) |

---

## Reporting a Vulnerability

We take the security of AirMouse and its users very seriously. If you discover a security vulnerability or suspect an exploitable flaw within the wire protocol, desktop actuator, or network server implementations, **please do not disclose it publicly on the issue tracker**.

### How to Report
Please report security vulnerabilities by emailing the project maintainers or creating a **GitHub Private Security Advisory**:

1. Navigate to the repository's **Security** tab on GitHub.
2. Select **Advisories** &rarr; **New draft advisory**.
3. Detail the nature of the vulnerability:
   - Affected component(s) (`pc/airmouse_py`, `pc/airmouse_go_new`, `android/`, `web-client/`)
   - Attack vector and prerequisite access
   - Minimal reproducible proof-of-concept (PoC)
   - Proposed mitigation or patch if available

### Response Timeline
- **Initial Response**: Within 48 hours of report submission.
- **Vulnerability Assessment**: Within 5 business days, confirming reproduction and severity triage.
- **Patch & Release**: Critical security fixes will be prioritized, released as an expedited patch tag (e.g., `v2.1.1`), and published to PyPI.

---

## Known Cryptographic Design & Threat Controls

AirMouse enforces defense-in-depth principles as documented in [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md):

1. **HMAC Authentication**: Every incoming TCP / WebSocket connection must validate a secret pairing token before injecting OS-level mouse/keyboard events.
2. **Replay Attack Mitigation**: Each packet envelope contains a monotonic sequence number (`seq`) and timestamp (`ts`). Replay attacks or out-of-order packets outside the acceptable window (±5000ms) are immediately dropped.
3. **Actuator Numeric Clamping**: Packet displacements are strictly bounded (`|dx|, |dy| <= 500`) to prevent numeric overflow or runaway cursor attacks.
4. **Local Network Segregation**: Servers bind to LAN interfaces and reject unauthenticated remote connections.
