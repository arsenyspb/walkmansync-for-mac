APP_NAME = WalkmanSync
BUNDLE_ID = com.arsenyspb.walkmansync
SWIFT_FILES = $(wildcard Sources/*.swift)
APP_DIR = $(APP_NAME).app
MACOS_DIR = $(APP_DIR)/Contents/MacOS
RESOURCES_DIR = $(APP_DIR)/Contents/Resources

all: $(APP_DIR)

$(APP_DIR): $(SWIFT_FILES) Info.plist
	@echo "Building $(APP_NAME)..."
	@mkdir -p $(MACOS_DIR)
	@mkdir -p $(RESOURCES_DIR)
	@cp Info.plist $(APP_DIR)/Contents/
	@swiftc -parse-as-library -O -whole-module-optimization $(SWIFT_FILES) -o $(MACOS_DIR)/$(APP_NAME)
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
	@echo '    <key>CFBundleVersion</key>' >> Info.plist
	@echo '    <string>1.0</string>' >> Info.plist
	@echo '    <key>CFBundleShortVersionString</key>' >> Info.plist
	@echo '    <string>1.0</string>' >> Info.plist
	@echo '    <key>LSMinimumSystemVersion</key>' >> Info.plist
	@echo '    <string>10.15</string>' >> Info.plist
	@echo '    <key>NSPrincipalClass</key>' >> Info.plist
	@echo '    <string>NSApplication</string>' >> Info.plist
	@echo '</dict>' >> Info.plist
	@echo '</plist>' >> Info.plist

clean:
	@rm -rf $(APP_DIR) Info.plist

run: $(APP_DIR)
	@open $(APP_DIR)
