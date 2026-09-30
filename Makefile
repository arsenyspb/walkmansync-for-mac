.DEFAULT_GOAL := all

all:
	$(MAKE) -C WalkmanSync

build:
	$(MAKE) -C WalkmanSync

clean:
	$(MAKE) -C WalkmanSync clean
	@rm -rf bin/

test:
	@mkdir -p bin
	@swiftc -parse-as-library \
		WalkmanSync/Sources/WalkmanLogger.swift \
		WalkmanSync/Sources/WalkmanKeyManager.swift \
		WalkmanSync/Sources/OMAContainerBuilder.swift \
		WalkmanSync/Sources/WalkmanDBGenerator.swift \
		Tests/TestRunner.swift \
		-o bin/test_runner
	@./bin/test_runner

run:
	$(MAKE) -C WalkmanSync run

cli: build
	@mkdir -p bin
	@ln -sf ../WalkmanSync/WalkmanSync.app/Contents/MacOS/WalkmanSync bin/walkmansync
	@echo "CLI symlinked to bin/walkmansync"

install-cli: build
	@mkdir -p $(HOME)/.local/bin
	@ln -sf $(PWD)/WalkmanSync/WalkmanSync.app/Contents/MacOS/WalkmanSync $(HOME)/.local/bin/walkmansync
	@echo "Installed to $(HOME)/.local/bin/walkmansync"

dmg:
	$(MAKE) -C WalkmanSync dmg
	@cp WalkmanSync/WalkmanSync.dmg ./WalkmanSync.dmg 2>/dev/null || true

.PHONY: all build clean test run cli install-cli dmg
