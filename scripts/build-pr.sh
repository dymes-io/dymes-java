#!/usr/bin/env bash

# SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
#
# SPDX-License-Identifier: Apache-2.0

set -Eeu

echo Building Dymes Java support...
mvn --batch-mode --no-transfer-progress clean install
echo Built Dymes Java support.
