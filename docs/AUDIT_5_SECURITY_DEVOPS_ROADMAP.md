# Deep Technical Audit: Security, CI/CD & DevOps Pipeline

**Audit Date**: October 2026  
**Auditor**: Lead Security Architect & DevOps Specialist  
**Scope**: Cryptographic Integrity, Threat Modeling, CI/CD Automation, Binary Packaging, Release Security

---

## 1. Executive Summary

As an input synthesis solution operating over local Wi-Fi and Bluetooth networks, AirMouse operates in an environment where **unauthorized access translates directly into remote arbitrary input control** over the host PC (keystroke injection, arbitrary mouse manipulation, and potential Remote Code Execution via OS run commands or terminal injection).

This audit reviews the cryptographic defenses of Protocol V2, assesses automated build & test coverage, and presents a definitive **Vulnerability & Remediation Roadmap**.

---

## 2. Cryptographic & Protocol Threat Modeling

```
Attacker on Same LAN
       │
       ▼ (ARP Spoofing / Packet Sniffing)
[AirMouse Client]  ───(Plain WS or Static PSK)───►  [AirMouse Desktop Host]
                                                          │
                                                    (OS Input Synthesis)
                                                          ▼
                                                [Arbitrary Keystrokes]
```

### 2.1 Threat Vectors & Critical Flaws

| Vulnerability ID | Severity | Category | Description |
|---|---|---|---|
| **VULN-SEC-01** | **CRITICAL** | Authorization | **Static Pre-Shared Key (PSK) Longevity**: Pairing QR codes encode a static HMAC secret without session renegotiation. Any entity capturing the QR code (or reading local preferences) retains permanent control until server restart. |
| **VULN-SEC-02** | **HIGH** | Integrity | **Timing Side-Channel in Signature Verification**: Signature checks in some server endpoints use standard string equality (`==`) rather than constant-time comparison (`subtle.ConstantTimeCompare` in Go, `hmac.compare_digest` in Python), exposing HMAC verification to timing attacks. |
| **VULN-SEC-03** | **HIGH** | Network Security | **LAN Broadcast Discovery Poisoning**: UDP discovery broadcast replies (`DISCOVER_REPLY`) are unauthenticated. A rogue device on the LAN can broadcast fake host IPs, redirecting clients to malicious proxy servers. |
| **VULN-SEC-04** | **MEDIUM** | Input Sanitization | **Keystroke / Macro Injection**: Keyboard text input packets transmit raw unicode strings directly into OS key simulators without escaping terminal control characters or dangerous OS shortcut combinations (e.g., `Win+R`, `Cmd+Space`, `Alt+F4`). |

---

## 3. DevOps, Build System & CI/CD Integrity

### 3.1 Binary Artifact Verification
* **Strengths**: The release workflow generates centralized SHA-256 checksums (`releases/SHA256SUMS.txt`) across Android, Windows, macOS, and Linux binaries.
* **Weaknesses**:
  - **Missing Code Signatures**:
    - macOS binaries (`.dmg`, `.app`, or standalone Mach-O) are ad-hoc signed and lack Apple Developer ID Notarization. On macOS Sequoia and Sonoma, Gatekeeper blocks execution with "App is damaged and cannot be opened" unless the user runs `xattr -cr`.
    - Windows `.exe` binaries are unsigned, triggering Windows SmartScreen "Unknown Publisher" warnings.
  - **Cross-Compilation Inconsistencies**: Compiling Go for Linux from macOS without proper CGO flags requires testing on genuine glibc environments (Ubuntu 22.04 LTS) to prevent runtime dynamic linker errors (`/lib64/ld-linux-x86-64.so.2: not found` on Alpine/Musl).

### 3.2 Automated Testing & Quality Gates
* **Weakness**:
  - Current unit tests verify protocol serialization and basic math, but lack:
    - **Automated UI screenshot regression tests** on Android across various screen ratios (foldable, tablet, phone).
    - **End-to-End latency benchmarks** simulating 10,000 packets under simulated 5% packet loss and 20ms jitter.
    - **Fuzz testing** for malformed or truncated Protocol V2 packets feeding into the Go/Python parsers.

---

## 4. Prioritized Remediation Roadmap Matrix

| Priority | Component | Target Vulnerability / Weakness | Required Action | Target Milestone |
|---|---|---|---|---|
| **P0** | Go / Python Server | Constant-time HMAC comparison | Replace string equality with `subtle.ConstantTimeCompare` / `hmac.compare_digest`. | v1.0.1 |
| **P0** | Android Build | R8 Full Mode & Obfuscation | Enable `minifyEnabled true`, `shrinkResources true`, and verify `<20MB` DEX. | v1.0.0 (Current) |
| **P1** | Security | Ephemeral Session Keys | Integrate X25519 ECDH key exchange on handshake; retire static raw PSKs. | v1.1.0 |
| **P1** | Desktop Daemons | SecureInput detection (macOS/Win) | Notify mobile UI when host OS enters secure password entry mode. | v1.1.0 |
| **P1** | Android Client | Sensor Recomposition Isolation | Refactor telemetry UI to use `Modifier.drawWithContent` and decoupled states. | v1.1.0 |
| **P2** | Web Client | iOS Motion Onboarding Flow | Build blocking user gesture modal for `DeviceMotionEvent.requestPermission()`. | v1.1.0 |
| **P2** | DevOps | Automated Fuzz & Chaos CI | Add GitHub Action matrix running packet fuzzing against Go and Python servers. | v1.2.0 |
| **P3** | Packaging | Code Signing & Notarization | Set up Apple Developer Notarization and EV/OV Windows Authenticode signing. | v2.0.0 |

---

## 5. Conclusion & Verdict

AirMouse possesses a robust and highly capable architectural foundation, featuring exceptional low-level OS synthesis capabilities in Go and a feature-rich Jetpack Compose Android client. By addressing the P0 and P1 vulnerabilities outlined in these five audit reports—particularly transitioning high-frequency telemetry to binary framing, hardening cryptographic comparisons, and isolating Compose recompositions—the system will achieve industry-grade commercial reliability and security.
