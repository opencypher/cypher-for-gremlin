.DEFAULT_GOAL := dist

PWD = $(shell pwd)

PROJECT_VERSION ?= 1.0.0
TINKERPOP_VERSION ?= 3.4.0

PROJECT = $(PWD)/../..
SUBPROJECT = $(PROJECT)/tinkerpop/cypher-gremlin-console-plugin
BUILD_JAR = build/libs/cypher-gremlin-console-plugin-$(PROJECT_VERSION)-all.jar

DIST_DIR = $(SUBPROJECT)/build/dist
INSTALL_DIR = $(SUBPROJECT)/build/install
INSTALL_PLUGIN = $(CONSOLE_DIR)/lib/cypher-gremlin-console-plugin-$(PROJECT_VERSION)-all.jar

CONSOLE = apache-tinkerpop-gremlin-console-$(TINKERPOP_VERSION)
CONSOLE_OUT = cypher-gremlin-console-$(TINKERPOP_VERSION)
CONSOLE_DIR = $(INSTALL_DIR)/$(CONSOLE_OUT)
CONSOLE_ZIP = build/tmp/$(CONSOLE).zip
CONSOLE_DIST = $(DIST_DIR)/$(CONSOLE_OUT).zip


$(INSTALL_PLUGIN): $(BUILD_JAR) $(CONSOLE_DIR)
	cp $(BUILD_JAR) $(CONSOLE_DIR)/lib/

$(BUILD_JAR): $(shell find src -type f)
	$(PROJECT)/gradlew -p $(SUBPROJECT) shadowJar

$(CONSOLE_ZIP):
	curl -o $(CONSOLE_ZIP) "http://www-eu.apache.org/dist/tinkerpop/$(TINKERPOP_VERSION)/$(CONSOLE)-bin.zip"

$(CONSOLE_DIR): $(CONSOLE_ZIP)
	mkdir -p $(INSTALL_DIR)/$(CONSOLE)
	unzip -q $(CONSOLE_ZIP) -d $(INSTALL_DIR)
	mv $(INSTALL_DIR)/$(CONSOLE) $(CONSOLE_DIR)
	find $(CONSOLE_DIR) -exec touch {} \;

$(CONSOLE_DIST): $(INSTALL_PLUGIN)
	mkdir -p $(DIST_DIR)
	cd $(INSTALL_DIR) && zip -qr $(CONSOLE_DIST) $(CONSOLE_OUT)

.PHONY: dist
dist: $(CONSOLE_DIST)

.PHONY: install
install: $(INSTALL_PLUGIN)

.PHONY: run
run: $(INSTALL_PLUGIN)
	cd $(CONSOLE_DIR) && ./bin/gremlin.sh

.PHONY: clean
clean:
	rm -rf $(INSTALL_DIR) $(DIST_DIR) $(BUILD_JAR)
