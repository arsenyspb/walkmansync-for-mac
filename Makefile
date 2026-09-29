.PHONY: build test clean run

# Maven wrapper or default mvn
MVN ?= mvn

build:
	$(MVN) clean compile assembly:single

test:
	$(MVN) test

clean:
	$(MVN) clean

run:
	java -jar target/jsymphonic-*-jar-with-dependencies.jar
