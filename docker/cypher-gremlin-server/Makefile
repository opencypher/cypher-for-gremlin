.DEFAULT_GOAL := build

CWD ?= $(shell pwd)
CONTAINER ?= $(notdir $(abspath .))
IMAGE_NAME ?= cypher-for-gremlin/$(CONTAINER)

PROJECT = $(CWD)/../..
SUBPROJECT = tinkerpop/cypher-gremlin-server-plugin
BUILD_OUTPUT = build/libs
JARS_DIR = libs

build: copy-jars
	docker build --force-rm -t $(IMAGE_NAME) .
	rm -rf $(JARS_DIR)
.PHONY: build

run:
	docker run -it --rm -p 8182:8182 --name=$(CONTAINER) $(IMAGE_NAME)
.PHONY: run

stop:
	docker stop $(CONTAINER)
.PHONY: stop

sh:
	docker run -it --rm --entrypoint="sh" $(IMAGE_NAME)
.PHONY: sh

copy-jars: gradle
	mkdir -p $(JARS_DIR)
	cp $(PROJECT)/$(SUBPROJECT)/$(BUILD_OUTPUT)/*-all.jar $(JARS_DIR)
.PHONY: copy-jars

gradle:
	$(PROJECT)/gradlew -p $(PROJECT)/$(SUBPROJECT) shadowJar -x test
.PHONY: gradle
