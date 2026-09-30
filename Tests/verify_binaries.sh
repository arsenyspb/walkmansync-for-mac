#!/bin/bash
set -e

echo "=== Verifying Binary Architectures & Deployment Targets ==="

check_binary() {
    local BINARY="$1"
    local NAME="$(basename "$BINARY")"

    if [ ! -f "$BINARY" ]; then
        echo "  [SKIP] $BINARY does not exist yet."
        return 0
    fi

    echo "Checking: $BINARY"

    # 1. Architecture Check: Must contain both arm64 and x86_64
    ARCH_INFO="$(lipo -info "$BINARY" 2>&1)"
    if ! echo "$ARCH_INFO" | grep -q "x86_64" || ! echo "$ARCH_INFO" | grep -q "arm64"; then
        echo "  [FAIL] $NAME is NOT Universal 2! Found: $ARCH_INFO"
        exit 1
    fi
    echo "  ✓ Universal 2 confirmed (arm64 + x86_64)"

    # 2. Dynamic Dependencies: Must not link to /opt/homebrew or /usr/local
    BAD_DYLIBS="$(otool -L "$BINARY" | grep -E "(/opt/homebrew|/usr/local)" || true)"
    if [ -n "$BAD_DYLIBS" ]; then
        echo "  [FAIL] $NAME links against non-system dynamic libraries:"
        echo "$BAD_DYLIBS"
        exit 1
    fi
    echo "  ✓ Zero non-system dynamic library dependencies"

    # 3. Minimum macOS Version: Must be <= 13.0
    MINOS_ENTRIES="$(otool -l "$BINARY" | awk '/cmd LC_BUILD_VERSION/{flag=1} flag && /minos/{print $2; flag=0}')"
    for VER in $MINOS_ENTRIES; do
        MAJOR="$(echo "$VER" | cut -d. -f1)"
        if [ "$MAJOR" -gt 13 ]; then
            echo "  [FAIL] $NAME has LC_BUILD_VERSION minos $VER (> 13.0)!"
            exit 1
        fi
    done
    echo "  ✓ Deployment target <= macOS 13.0 verified (minos: $MINOS_ENTRIES)"
}

check_binary "WalkmanSync/Resources/atracdenc"

if [ -f "WalkmanSync/WalkmanSync.app/Contents/MacOS/WalkmanSync" ]; then
    check_binary "WalkmanSync/WalkmanSync.app/Contents/MacOS/WalkmanSync"
    check_binary "WalkmanSync/WalkmanSync.app/Contents/Resources/atracdenc"
fi

echo "=== All Binary Checks PASSED ==="
