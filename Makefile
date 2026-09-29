.DEFAULT_GOAL := all

all:
	$(MAKE) -C WalkmanSync

build:
	$(MAKE) -C WalkmanSync

clean:
	$(MAKE) -C WalkmanSync clean

run:
	$(MAKE) -C WalkmanSync run

.PHONY: all build clean run
