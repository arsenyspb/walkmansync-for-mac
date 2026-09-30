#!/bin/bash
set -e

echo "=================================================="
echo "  WalkmanSync for Mac — Automated Installer"
echo "=================================================="
echo ""

APP_DIR="/Applications/WalkmanSync.app"
DMG_URL="https://github.com/arsenyspb/walkmansync-for-mac/releases/latest/download/WalkmanSync.dmg"
TEMP_DMG="/tmp/WalkmanSync_Install.dmg"
MOUNT_DIR="/tmp/WalkmanSync_Mount"

echo "[1/4] Downloading latest WalkmanSync.dmg..."
curl -fsSL -o "$TEMP_DMG" "$DMG_URL"

echo "[2/4] Mounting disk image..."
mkdir -p "$MOUNT_DIR"
hdiutil attach "$TEMP_DMG" -mountpoint "$MOUNT_DIR" -nobrowse -quiet

echo "[3/4] Installing to /Applications..."
rm -rf "$APP_DIR"
cp -R "$MOUNT_DIR/WalkmanSync.app" /Applications/

echo "[4/4] Bypassing macOS Gatekeeper quarantine..."
# Strip com.apple.quarantine so macOS runs it natively without warnings
xattr -cr "$APP_DIR" || true

echo "Cleaning up..."
hdiutil detach "$MOUNT_DIR" -quiet || true
rm -rf "$TEMP_DMG" "$MOUNT_DIR"

echo ""
echo "=================================================="
echo "✓ Installation Complete!"
echo "  WalkmanSync is now installed in /Applications."
echo "  You can open it from Spotlight, Launchpad, or by typing:"
echo "    open /Applications/WalkmanSync.app"
echo "=================================================="
