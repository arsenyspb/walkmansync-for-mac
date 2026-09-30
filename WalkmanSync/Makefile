APP_NAME = WalkmanSync
BUNDLE_ID = com.arsenyspb.walkmansync
SWIFT_FILES = $(wildcard Sources/*.swift)
APP_DIR = $(APP_NAME).app
MACOS_DIR = $(APP_DIR)/Contents/MacOS
RESOURCES_DIR = $(APP_DIR)/Contents/Resources
VERSION = 0.3.1

DMG_NAME = $(APP_NAME)-$(VERSION).dmg
DMG_STAGING = $(APP_NAME)_DMG

all: $(APP_DIR)

dmg: $(APP_DIR)
	@echo "Creating standard Drag-and-Drop installer DMG..."
	@rm -rf $(DMG_STAGING) $(DMG_NAME) $(APP_NAME).dmg
	@mkdir -p $(DMG_STAGING)
	@cp -R $(APP_DIR) $(DMG_STAGING)/
	@ln -s /Applications $(DMG_STAGING)/Applications
	@hdiutil create -volname "$(APP_NAME)" -srcfolder $(DMG_STAGING) -ov -format UDZO $(DMG_NAME)
	@ln -sf $(DMG_NAME) $(APP_NAME).dmg
	@rm -rf $(DMG_STAGING)
	@echo "DMG Created successfully: $(DMG_NAME) (also symlinked as $(APP_NAME).dmg)"

TARGET_MACOS = 13.0

$(APP_DIR): $(SWIFT_FILES) Info.plist
	@echo "Building Universal 2 binary for $(APP_NAME) (Apple Silicon & Intel, macOS $(TARGET_MACOS)+)..."
	@mkdir -p $(MACOS_DIR)
	@mkdir -p $(RESOURCES_DIR)
	@cp Info.plist $(APP_DIR)/Contents/
	@cp -R Resources/* $(RESOURCES_DIR)/ 2>/dev/null || true
	@echo "  -> Compiling arm64 slice..."
	@swiftc -parse-as-library -O -whole-module-optimization -target arm64-apple-macos$(TARGET_MACOS) $(SWIFT_FILES) -o /tmp/$(APP_NAME)_arm64
	@echo "  -> Compiling x86_64 slice..."
	@swiftc -parse-as-library -O -whole-module-optimization -target x86_64-apple-macos$(TARGET_MACOS) $(SWIFT_FILES) -o /tmp/$(APP_NAME)_x86_64
	@echo "  -> Creating Universal 2 binary..."
	@lipo -create /tmp/$(APP_NAME)_arm64 /tmp/$(APP_NAME)_x86_64 -output $(MACOS_DIR)/$(APP_NAME)
	@rm -f /tmp/$(APP_NAME)_arm64 /tmp/$(APP_NAME)_x86_64
	@codesign --force --deep --sign - $(APP_DIR)
	@echo "Build complete. Output: $(APP_DIR)"

Info.plist:
	@echo '<?xml version="1.0" encoding="UTF-8"?>' > Info.plist
	@echo '<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">' >> Info.plist
	@echo '<plist version="1.0">' >> Info.plist
	@echo '<dict>' >> Info.plist
	@echo '    <key>CFBundleExecutable</key>' >> Info.plist
	@echo '    <string>$(APP_NAME)</string>' >> Info.plist
	@echo '    <key>CFBundleIdentifier</key>' >> Info.plist
	@echo '    <string>$(BUNDLE_ID)</string>' >> Info.plist
	@echo '    <key>CFBundleName</key>' >> Info.plist
	@echo '    <string>$(APP_NAME)</string>' >> Info.plist
	@echo '    <key>CFBundleIconFile</key>' >> Info.plist
	@echo '    <string>AppIcon</string>' >> Info.plist
	@echo '    <key>CFBundleVersion</key>' >> Info.plist
	@echo '    <string>$(VERSION)</string>' >> Info.plist
	@echo '    <key>CFBundleShortVersionString</key>' >> Info.plist
	@echo '    <string>$(VERSION)</string>' >> Info.plist
	@echo '    <key>LSMinimumSystemVersion</key>' >> Info.plist
	@echo '    <string>$(TARGET_MACOS)</string>' >> Info.plist
	@echo '    <key>NSPrincipalClass</key>' >> Info.plist
	@echo '    <string>NSApplication</string>' >> Info.plist
	@echo '</dict>' >> Info.plist
	@echo '</plist>' >> Info.plist

clean:
	@rm -rf $(APP_DIR) Info.plist $(DMG_STAGING) *.dmg

run: $(APP_DIR)
	@open $(APP_DIR)
