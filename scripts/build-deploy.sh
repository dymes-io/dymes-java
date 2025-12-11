#!/usr/bin/env bash

# SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
#
# SPDX-License-Identifier: Apache-2.0

set -Eeu

echo Deploying Dymes Java support...
mvn --batch-mode --no-transfer-progress -DskipTests=true clean deploy
echo Deployed Dymes Java support.
