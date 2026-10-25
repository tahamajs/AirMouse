#!/usr/bin/env python3
"""
AirMouse — Single Source of Truth (SSOT) Version Synchronizer & Bumper
======================================================================
Propagates the canonical version from the root `VERSION` file to all platform
targets (Android, Go, Python, Web Client, Release Scripts, and Docs).

Usage:
    python3 scripts/bump_version.py                 # Sync all targets to current VERSION
    python3 scripts/bump_version.py 1.0.2           # Set new version and sync
    python3 scripts/bump_version.py v1.0.2          # Strips leading 'v', sets and syncs
    python3 scripts/bump_version.py --check         # CI verify mode: exit 1 if drift detected
    python3 scripts/bump_version.py --dry-run 1.0.2 # Preview replacements without writing
    python3 scripts/bump_version.py --show          # Print current version from VERSION
"""

import argparse
import re
import sys
from pathlib import Path
from typing import List, Optional, Tuple


ROOT_DIR = Path(__file__).resolve().parent.parent
VERSION_FILE = ROOT_DIR / "VERSION"


def get_current_version() -> str:
    """Reads and returns the current canonical version from root VERSION file."""
    if not VERSION_FILE.exists():
        raise FileNotFoundError(f"Canonical VERSION file not found at: {VERSION_FILE}")
    return VERSION_FILE.read_text(encoding="utf-8").strip()


def validate_version(raw_version: str) -> str:
    """Validates and normalizes semantic version strings (e.g., '1.0.1' or 'v1.0.1')."""
    cleaned = raw_version.strip()
    if cleaned.startswith(("v", "V")):
        cleaned = cleaned[1:]

    pattern = r"^\d+\.\d+\.\d+(-[a-zA-Z0-9_.-]+)?$"
    if not re.match(pattern, cleaned):
        raise ValueError(
            f"Invalid semantic version format: '{raw_version}'. Expected format: X.Y.Z (e.g. 1.0.1)"
        )
    return cleaned


def extract_current_version_code(build_gradle_path: Path) -> int:
    """Extracts existing versionCode from android/app/build.gradle."""
    if not build_gradle_path.exists():
        return 1
    content = build_gradle_path.read_text(encoding="utf-8")
    m = re.search(r"\bversionCode\s+(\d+)", content)
    if m:
        return int(m.group(1))
    return 1


class SyncTarget:
    """Represents a file and its specific version replacement rules."""

    def __init__(self, relative_path: str, description: str):
        self.relative_path = relative_path
        self.description = description
        self.full_path = ROOT_DIR / relative_path

    def update_content(self, content: str, version: str, version_code: int) -> Tuple[str, bool]:
        """Applies transformation rules. Returns (new_content, was_modified)."""
        raise NotImplementedError


class RegexSyncTarget(SyncTarget):
    """Sync target defined by regex replacement tuples: (pattern, replacement_template, flags)."""

    def __init__(self, relative_path: str, description: str, rules: List[Tuple[str, str, int]]):
        super().__init__(relative_path, description)
        self.rules = rules

    def update_content(self, content: str, version: str, version_code: int) -> Tuple[str, bool]:
        new_content = content
        for pattern, template, flags in self.rules:
            # Format template with version and version_code
            repl = template.format(version=version, version_code=version_code)
            new_content = re.sub(pattern, repl, new_content, flags=flags)
        return new_content, new_content != content


class WebAppJsTarget(SyncTarget):
    """Sync target for web-client app.js, ensuring CLIENT_VERSION constant is declared and updated."""

    def update_content(self, content: str, version: str, version_code: int) -> Tuple[str, bool]:
        new_content = content
        # Check if CLIENT_VERSION constant already declared
        if re.search(r"const\s+CLIENT_VERSION\s*=\s*['\"][^'\"]+['\"];", new_content):
            new_content = re.sub(
                r"(const\s+CLIENT_VERSION\s*=\s*['\"])[^'\"]+(['\"];)",
                r"\g<1>" + version + r"\2",
                new_content
            )
        else:
            # Insert after 'use strict';
            pattern = r"(['\"]use strict['\"];)"
            if re.search(pattern, new_content):
                replacement = r"\1\n\n    const CLIENT_VERSION = '" + version + "';"
                new_content = re.sub(pattern, replacement, new_content, count=1)
            else:
                new_content = f"const CLIENT_VERSION = '{version}';\n" + new_content

        return new_content, new_content != content


class PythonTcpServerTarget(SyncTarget):
    """Sync target for Python tcp_server.py: updates SERVER_VERSION constant & welcome response."""

    def update_content(self, content: str, version: str, version_code: int) -> Tuple[str, bool]:
        new_content = content
        # Ensure SERVER_VERSION constant is present
        if re.search(r"SERVER_VERSION\s*=\s*['\"][^'\"]+['\"]", new_content):
            new_content = re.sub(
                r'(SERVER_VERSION\s*=\s*")[^"]+(")',
                r'\g<1>' + version + r'\2',
                new_content
            )
        else:
            # Place after logger or imports
            if 'logger = logging.getLogger' in new_content:
                new_content = re.sub(
                    r'(logger = logging.getLogger\([^)]+\))',
                    r'\1\nSERVER_VERSION = "' + version + '"',
                    new_content,
                    count=1
                )

        # Update welcome response version string if present
        new_content = re.sub(
            r'("server":\s*"AirMouse Hardened Python Server",\s*"version":\s*")[^"]+(")',
            r'\g<1>' + version + r'\2',
            new_content
        )
        return new_content, new_content != content


def get_all_sync_targets() -> List[SyncTarget]:
    """Builds the comprehensive registry of all platform targets."""
    return [
        # ----------------------------------------------------------------------
        # 1. Android Platform
        # ----------------------------------------------------------------------
        RegexSyncTarget(
            "android/app/build.gradle",
            "Android App Gradle Build Configuration",
            [
                (r'(\bversionName\s+")[^"]+(")', r'\g<1>{version}\2', 0),
                (r'(\bversionCode\s+)\d+', r'\g<1>{version_code}', 0),
            ]
        ),
        RegexSyncTarget(
            "android/app/src/main/java/com/airmouse/AirMouseApplication.kt",
            "Android Application Class Constants",
            [
                (r'(const\s+val\s+APP_VERSION\s*=\s*")[^"]+(")', r'\g<1>{version}\2', 0),
                (r'(const\s+val\s+BUILD_NUMBER\s*=\s*)\d+', r'\g<1>{version_code}', 0),
            ]
        ),
        RegexSyncTarget(
            "android/app/src/main/java/com/airmouse/presentation/ui/about/AboutState.kt",
            "Android About Screen UI State",
            [
                (r'(val\s+versionName\s*:\s*String\s*=\s*")[^"]+(")', r'\g<1>{version}\2', 0),
                (r'(val\s+versionCode\s*:\s*Int\s*=\s*)\d+', r'\g<1>{version_code}', 0),
            ]
        ),
        RegexSyncTarget(
            "android/app/src/main/java/com/airmouse/domain/model/UpdateModels.kt",
            "Android Domain Update Models",
            [
                (r'(val\s+versionName\s*:\s*String\s*=\s*")[^"]+(")', r'\g<1>{version}\2', 0),
                (r'(val\s+versionCode\s*:\s*Int\s*=\s*)\d+', r'\g<1>{version_code}', 0),
            ]
        ),
        RegexSyncTarget(
            "android/app/src/main/java/com/airmouse/presentation/ui/about/AboutViewModel.kt",
            "Android About Screen ViewModel Feedback Toast",
            [
                (r'("AirMouse Pro is up to date \(v)[^)]+(\)")', r'\g<1>{version}\2', 0),
            ]
        ),
        RegexSyncTarget(
            "android/app/src/main/java/com/airmouse/presentation/ui/home/HomeScreen.kt",
            "Android Home Screen Version Label",
            [
                (r'(text\s*=\s*"Air Mouse Pro v)[^"]+(")', r'\g<1>{version}\2', 0),
            ]
        ),
        RegexSyncTarget(
            "android/app/src/main/java/com/airmouse/presentation/ui/logs/ServerLogsViewModel.kt",
            "Android Server Logs Initial System Log",
            [
                (r'("Log viewer initialized",\s*"Version\s+)[^"]+(")', r'\g<1>{version}\2', 0),
            ]
        ),
        RegexSyncTarget(
            "android/app/src/main/java/com/airmouse/presentation/ui/main/MainScreen.kt",
            "Android Modern Drawer Version Badge",
            [
                (r'(text\s*=\s*"Air Mouse Pro"[\s\S]*?text\s*=\s*"v)[^"]+(")', r'\g<1>{version}\2', 0),
            ]
        ),
        RegexSyncTarget(
            "android/app/src/main/java/com/airmouse/presentation/ui/settings/SettingsScreen.kt",
            "Android Settings About Section Version",
            [
                (r'Text\("Version\s+[^ ]+\s+\(Release\s+v[^)]+\)"', 'Text("Version {version} (Release v{version})"', 0),
            ]
        ),

        # ----------------------------------------------------------------------
        # 2. Go Desktop Server Platform
        # ----------------------------------------------------------------------
        RegexSyncTarget(
            "pc/airmouse_go_new/cmd/airmouse-server/main.go",
            "Go Server main.go Build Variables & User-Agent",
            [
                (r'(version\s*=\s*")[^"]+(")', r'\g<1>{version}\2', 0),
                (r'(gitCommit\s*=\s*"v)[^"-]+(-release")', r'\g<1>{version}\2', 0),
                (r'("AirMouse-Go-Server/)[^"]+(")', r'\g<1>{version}\2', 0),
            ]
        ),
        RegexSyncTarget(
            "pc/airmouse_go_new/internal/config/config.go",
            "Go Server Internal Config Default Version",
            [
                (r'(Version:\s*")[^"]+(")', r'\g<1>{version}\2', 0),
            ]
        ),
        RegexSyncTarget(
            "pc/airmouse_go_new/web/client/index.html",
            "Go Client Web Index Badge",
            [
                (r'(<span class="app-badge">v)[^<]+(</span>)', r'\g<1>{version}\2', 0),
            ]
        ),
        WebAppJsTarget(
            "pc/airmouse_go_new/web/client/app.js",
            "Go Client Web App JS Version Constant"
        ),

        # ----------------------------------------------------------------------
        # 3. Python Server & Core Package
        # ----------------------------------------------------------------------
        RegexSyncTarget(
            "pc/airmouse_py/airmouse/__init__.py",
            "Python Package Canonical __version__",
            [
                (r'(__version__\s*=\s*")[^"]+(")', r'\g<1>{version}\2', 0),
            ]
        ),
        PythonTcpServerTarget(
            "pc/airmouse_py/network/tcp_server.py",
            "Python TCP Server Handshake & Version Constant"
        ),
        RegexSyncTarget(
            "pc/airmouse_py/network/ws_server.py",
            "Python WebSocket Server Handshake Version",
            [
                (r'("server":\s*"AirMouse WS Python",\s*"version":\s*")[^"]+(")', r'\g<1>{version}\2', 0),
            ]
        ),
        RegexSyncTarget(
            "pc/airmouse_py/pyproject.toml",
            "Python pyproject.toml Package Metadata",
            [
                (r'(^version\s*=\s*")[^"]+(")', r'\g<1>{version}\2', re.MULTILINE),
            ]
        ),
        RegexSyncTarget(
            "pc/airmouse_py/ui/app.py",
            "Python Desktop GUI Dashboard App Version",
            [
                (r'(^VERSION\s*=\s*")[^"]+(")', r'\g<1>{version}\2', re.MULTILINE),
            ]
        ),
        RegexSyncTarget(
            "pc/airmouse_py/airmouse/web_static/index.html",
            "Python Web Static Index Badge",
            [
                (r'(<span class="app-badge">v)[^<]+(</span>)', r'\g<1>{version}\2', 0),
            ]
        ),

        # ----------------------------------------------------------------------
        # 4. Zero-Install Mobile Web Client
        # ----------------------------------------------------------------------
        RegexSyncTarget(
            "web-client/index.html",
            "Web Client Index Badge",
            [
                (r'(<span class="app-badge">v)[^<]+(</span>)', r'\g<1>{version}\2', 0),
            ]
        ),
        WebAppJsTarget(
            "web-client/app.js",
            "Web Client App JS Version Constant"
        ),

        # ----------------------------------------------------------------------
        # 5. Release Scripts & Batches
        # ----------------------------------------------------------------------
        RegexSyncTarget(
            "scripts/build_all_releases.sh",
            "Universal Release Builder Script",
            [
                (r'(^VERSION=")[^"]+(")', r'\g<1>{version}\2', re.MULTILINE),
            ]
        ),
        RegexSyncTarget(
            "scripts/publish_github_release.py",
            "GitHub Release Publisher Script",
            [
                (r'(^TAG\s*=\s*"v)[^"]+(")', r'\g<1>{version}\2', re.MULTILINE),
                (r'(^RELEASE_TITLE\s*=\s*"AirMouse Pro v)[^ ]+(\s+—)', r'\g<1>{version}\2', re.MULTILINE),
                (r'(# 🖱️ AirMouse Pro v)[^ ]+(\s+\(Canonical Production Release\))', r'\g<1>{version}\2', 0),
                (r'(AirMouse-(?:Server-)?v)[0-9]+\.[0-9]+\.[0-9]+(-[a-z0-9_.-]+|\.apk)', r'\g<1>{version}\2', 0),
            ]
        ),
        RegexSyncTarget(
            "start-window.bat",
            "Windows 1-Click Batch Launcher",
            [
                (r'(AirMouse-Server-v)[0-9]+\.[0-9]+\.[0-9]+(-windows-x64\.exe)', r'\g<1>{version}\2', 0),
            ]
        ),

        # ----------------------------------------------------------------------
        # 6. Documentation & Release Notes
        # ----------------------------------------------------------------------
        RegexSyncTarget(
            "README.md",
            "Root Repository README Downloads Matrix",
            [
                (r'(# 🖱️ AirMouse Pro \(v)[0-9]+\.[0-9]+\.[0-9]+(\))', r'\g<1>{version}\2', 0),
                (r'(AirMouse-(?:Server-)?v)[0-9]+\.[0-9]+\.[0-9]+(-[a-z0-9_.-]+|\.apk)', r'\g<1>{version}\2', 0),
            ]
        ),
        RegexSyncTarget(
            "DOCUMENTATION.md",
            "Master Technical Documentation Header",
            [
                (r'(\*\*Version:\*\* .*?Release v)[0-9]+\.[0-9]+\.[0-9]+', r'\g<1>{version}', 0),
            ]
        ),
        RegexSyncTarget(
            "releases/README.md",
            "Releases Matrix Documentation",
            [
                (r'(# AirMouse Pro v)[^ ]+(\s+—\s+Official Multi-Platform Releases)', r'\g<1>{version}\2', 0),
                (r'(AirMouse-(?:Server-)?v)[0-9]+\.[0-9]+\.[0-9]+(-[a-z0-9_.-]+|\.apk)', r'\g<1>{version}\2', 0),
            ]
        ),
    ]


def run_sync(
    new_version: Optional[str] = None,
    explicit_version_code: Optional[int] = None,
    dry_run: bool = False,
    check_only: bool = False,
) -> bool:
    """Core synchronization routine."""
    current_version = get_current_version() if VERSION_FILE.exists() else "1.0.0"

    target_version = validate_version(new_version) if new_version else current_version
    version_changed = (target_version != current_version)

    build_gradle_path = ROOT_DIR / "android" / "app" / "build.gradle"
    current_code = extract_current_version_code(build_gradle_path)

    if explicit_version_code is not None:
        target_code = explicit_version_code
    elif version_changed:
        target_code = current_code + 1
    else:
        target_code = current_code

    print("==============================================================================")
    print("  🚀  AirMouse Single Source of Truth (SSOT) Version Synchronizer")
    print("==============================================================================")
    print(f"  Current Canonical Version:  {current_version}")
    print(f"  Target Version:             {target_version}")
    print(f"  Target Android VersionCode: {target_code}")
    print(f"  Mode:                       {'CI Check' if check_only else ('Dry-Run' if dry_run else 'Live Sync')}")
    print("==============================================================================\n")

    targets = get_all_sync_targets()
    discrepancies: List[str] = []
    modified_files: List[str] = []

    # Check / Update root VERSION file
    if VERSION_FILE.exists():
        v_content = VERSION_FILE.read_text(encoding="utf-8").strip()
        if v_content != target_version:
            discrepancies.append(f"VERSION file ({v_content} != {target_version})")
            if not dry_run and not check_only:
                VERSION_FILE.write_text(f"{target_version}\n", encoding="utf-8")
                modified_files.append("VERSION")
                print(f"  ✏️  Updated VERSION -> {target_version}")
    else:
        discrepancies.append("VERSION file (missing)")
        if not dry_run and not check_only:
            VERSION_FILE.write_text(f"{target_version}\n", encoding="utf-8")
            modified_files.append("VERSION")
            print(f"  ✨ Created VERSION -> {target_version}")

    # Process all registered targets
    for target in targets:
        if not target.full_path.exists():
            print(f"  ⚠️  Skipping missing target: {target.relative_path}")
            continue

        try:
            content = target.full_path.read_text(encoding="utf-8")
            new_content, modified = target.update_content(content, target_version, target_code)

            if modified:
                discrepancies.append(target.relative_path)
                if not dry_run and not check_only:
                    target.full_path.write_text(new_content, encoding="utf-8")
                    modified_files.append(target.relative_path)
                    print(f"  ✅  Updated: {target.relative_path} ({target.description})")
                elif dry_run:
                    print(f"  🔍  Would update: {target.relative_path} ({target.description})")
            else:
                if not check_only:
                    print(f"  ✓   Already in sync: {target.relative_path}")
        except Exception as e:
            print(f"  ❌  Error processing {target.relative_path}: {e}")
            discrepancies.append(f"{target.relative_path} (error: {e})")

    print("\n------------------------------------------------------------------------------")
    if check_only:
        if discrepancies:
            print(f"❌ CI Guardrail Check Failed: {len(discrepancies)} target(s) out of sync with VERSION ({target_version}):")
            for d in discrepancies:
                print(f"   - {d}")
            return False
        else:
            print(f"✅ All {len(targets)} targets are perfectly synchronized with VERSION ({target_version})!")
            return True

    if dry_run:
        print(f"🔍 Dry Run Complete: {len(discrepancies)} target(s) would be updated to v{target_version} (code {target_code}).")
        return True

    print(f"🎉 Successfully synchronized {len(modified_files)} file(s) to v{target_version} (Android versionCode: {target_code})!")
    return True


def main():
    parser = argparse.ArgumentParser(
        description="AirMouse Single Source of Truth (SSOT) Version Synchronizer & Bumper"
    )
    parser.add_argument(
        "version",
        nargs="?",
        default=None,
        help="Optional new version to set (e.g. 1.0.1 or v1.0.1). If omitted, syncs to current VERSION."
    )
    parser.add_argument(
        "--version-code",
        type=int,
        default=None,
        help="Explicit Android versionCode integer override."
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Run in CI verification mode: returns non-zero exit code if any target has drifted from VERSION."
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Simulate version propagation without modifying any files."
    )
    parser.add_argument(
        "--show",
        action="store_true",
        help="Print the current canonical version from the root VERSION file and exit."
    )

    args = parser.parse_args()

    if args.show:
        try:
            print(get_current_version())
            sys.exit(0)
        except Exception as e:
            print(f"Error: {e}", file=sys.stderr)
            sys.exit(1)

    success = run_sync(
        new_version=args.version,
        explicit_version_code=args.version_code,
        dry_run=args.dry_run,
        check_only=args.check,
    )

    if not success:
        sys.exit(1)


if __name__ == "__main__":
    main()
