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
		WalkmanSync/Sources/WalkmanKeyManager.swift \
		WalkmanSync/Sources/OMAContainerBuilder.swift \
		WalkmanSync/Sources/WalkmanDBGenerator.swift \
		Tests/TestRunner.swift \
		-o bin/test_runner
	@./bin/test_runner

run:
	$(MAKE) -C WalkmanSync run

.PHONY: all build clean test run
