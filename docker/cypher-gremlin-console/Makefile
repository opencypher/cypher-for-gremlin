.DEFAULT_GOAL := build

RESOURCES_DIR = libs
CWD ?= $(shell pwd)
CONTAINER ?= $(notdir $(abspath .))
IMAGE_NAME ?= cypher-for-gremlin/$(CONTAINER)

PROJECT = $(CWD)/../..
SUBPROJECT = tinkerpop/cypher-gremlin-console-plugin
BUILD_OUTPUT = build/libs

build: copy-jars copy-config
	docker build --force-rm -t $(IMAGE_NAME) .
	rm -rf $(RESOURCES_DIR)
.PHONY: build

copy-jars: gradle
	mkdir -p $(RESOURCES_DIR)
	cp $(PROJECT)/$(SUBPROJECT)/$(BUILD_OUTPUT)/*-all.jar $(RESOURCES_DIR)
.PHONY: copy-jars

copy-config:
	cp $(CWD)/conf/* $(RESOURCES_DIR)/
	cp $(CWD)/../../config/* $(RESOURCES_DIR)/ || true
.PHONY: copy-config

gradle:
	$(PROJECT)/gradlew -p $(PROJECT)/$(SUBPROJECT) shadowJar
.PHONY: gradle

run:
	docker run --rm -it -e JAVA_OPTIONS="-Dplugins=v3d3" --net="host" --name=$(CONTAINER) $(IMAGE_NAME)
.PHONY: run

stop:
	docker stop $(CONTAINER)
.PHONY: stop

sh:
	docker run -it --rm --entrypoint="sh" $(IMAGE_NAME)
.PHONY: sh
