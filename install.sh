#!/bin/bash
set -e

echo "=================================================="
echo "  WalkmanSync for Mac — Automated Installer"
echo "=================================================="
echo ""

APP_DIR="/Applications/WalkmanSync.app"
TEMP_DMG="/tmp/WalkmanSync_Install.dmg"
MOUNT_DIR="/tmp/WalkmanSync_Mount"

echo "[1/4] Resolving latest WalkmanSync release..."
LATEST_TAG=$(curl -fsSL https://api.github.com/repos/arsenyspb/walkmansync-for-mac/releases/latest 2>/dev/null | grep '"tag_name":' | head -n 1 | cut -d '"' -f 4 || true)
if [ -z "$LATEST_TAG" ]; then
    LATEST_TAG="v0.3.1"
fi
echo "      Target release: $LATEST_TAG"

echo "      Downloading WalkmanSync.dmg..."
DOWNLOAD_SUCCESS=0
for URL in \
    "https://github.com/arsenyspb/walkmansync-for-mac/releases/download/${LATEST_TAG}/WalkmanSync.dmg" \
    "https://github.com/arsenyspb/walkmansync-for-mac/releases/download/v0.3.1/WalkmanSync.dmg" \
    "https://github.com/arsenyspb/walkmansync-for-mac/releases/latest/download/WalkmanSync.dmg"; do
    if curl -fsSL -o "$TEMP_DMG" "$URL"; then
        DOWNLOAD_SUCCESS=1
        break
    fi
done

if [ "$DOWNLOAD_SUCCESS" -ne 1 ]; then
    echo "Error: Failed to download WalkmanSync.dmg from GitHub releases."
    exit 1
fi

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
