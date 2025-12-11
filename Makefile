# SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
#
# SPDX-License-Identifier: Apache-2.0

.POSIX:

DYMES_DIR := ./dymes

.PHONY: all
all: $(DYMES_DIR)/Dockerfile build-dymes-image install

.PHONY: clean
clean:
	mvn --batch-mode --no-transfer-progress clean

.PHONY: install
install: build-pr

.PHONY: tilted
tilted: build-qnd

.PHONY: clone-submodules
clone-submodules:
	@git submodule update --force --recursive --init --remote

$(DYMES_DIR)/Dockerfile: clone-submodules

.PHONY: build-dymes-image
build-dymes-image:
	@make -C dymes DYMES_IMAGE_NAME="dymes-io/dymes" images

.PHONY: build-pr
build-pr:
	@scripts/build-pr.sh

.PHONY: build-qnd
build-qnd:
	@scripts/build-qnd.sh

.PHONY: build-deploy
build-deploy:
	@scripts/build-deploy.sh

