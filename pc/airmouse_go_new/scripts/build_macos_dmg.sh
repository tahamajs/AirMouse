#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_NAME="${APP_NAME:-Air Mouse Pro Server}"
BINARY_NAME="${BINARY_NAME:-airmouse-server}"
VERSION="${VERSION:-3.0.0}"
BUILD_DIR="${BUILD_DIR:-$ROOT_DIR/dist/macos}"
APP_BUNDLE_NAME="${APP_BUNDLE_NAME:-Air Mouse Pro Server.app}"
DMG_NAME="${DMG_NAME:-AirMouseProServer}"
VOLUME_NAME="${VOLUME_NAME:-Air Mouse Pro Server}"

mkdir -p "$BUILD_DIR"
rm -rf "$BUILD_DIR/$APP_BUNDLE_NAME" "$BUILD_DIR/$DMG_NAME.dmg" "$BUILD_DIR/dmg-root"

mkdir -p "$BUILD_DIR/$APP_BUNDLE_NAME/Contents/MacOS"
mkdir -p "$BUILD_DIR/$APP_BUNDLE_NAME/Contents/Resources"

cat > "$BUILD_DIR/$APP_BUNDLE_NAME/Contents/Info.plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>CFBundleName</key>
  <string>${APP_NAME}</string>
  <key>CFBundleDisplayName</key>
  <string>${APP_NAME}</string>
  <key>CFBundleIdentifier</key>
  <string>io.airmouse.server</string>
  <key>CFBundleVersion</key>
  <string>${VERSION}</string>
  <key>CFBundleShortVersionString</key>
  <string>${VERSION}</string>
  <key>CFBundleExecutable</key>
  <string>${BINARY_NAME}</string>
  <key>CFBundlePackageType</key>
  <string>APPL</string>
  <key>CFBundleSignature</key>
  <string>AMSR</string>
  <key>LSMinimumSystemVersion</key>
  <string>12.0</string>
  <key>NSHighResolutionCapable</key>
  <true/>
</dict>
</plist>
EOF

cp "$ROOT_DIR/$BINARY_NAME" "$BUILD_DIR/$APP_BUNDLE_NAME/Contents/MacOS/$BINARY_NAME"
chmod +x "$BUILD_DIR/$APP_BUNDLE_NAME/Contents/MacOS/$BINARY_NAME"

if command -v codesign >/dev/null 2>&1; then
  codesign --force --deep --sign - "$BUILD_DIR/$APP_BUNDLE_NAME" || true
fi

mkdir -p "$BUILD_DIR/dmg-root"
cp -R "$BUILD_DIR/$APP_BUNDLE_NAME" "$BUILD_DIR/dmg-root/"
ln -s /Applications "$BUILD_DIR/dmg-root/Applications"

hdiutil create \
  -volname "$VOLUME_NAME" \
  -srcfolder "$BUILD_DIR/dmg-root" \
  -ov \
  -format UDZO \
  "$BUILD_DIR/$DMG_NAME.dmg"

echo "Created DMG: $BUILD_DIR/$DMG_NAME.dmg"
