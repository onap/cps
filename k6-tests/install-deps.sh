#!/bin/bash
#
# Copyright 2024-2026 OpenInfra Foundation Europe. All rights reserved.
# Modifications Copyright (C) 2026 Deutsche Telekom AG
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

echo "---> install-deps.sh"
echo "Installing dependencies"

# Create directory for downloaded binaries.
mkdir -p bin
touch bin/.gitignore

# Add it to the PATH, so downloaded versions will be used.
export PATH="$(pwd)/bin:$PATH"

# The default suite is ncmp; cps-core has no Kafka dependency so it doesn't need xk6-kafka.
suite=${1:-ncmp}

if [ "$suite" = "cps-core" ]; then
  # cps-core doesn't use Kafka, so skip the xk6-kafka download and use a plain k6 already on PATH.
  if ! command -v k6 >/dev/null 2>&1 && [ ! -x bin/k6 ]; then
    echo " Error: k6 not found. Install k6 (e.g. via winget/choco/apt) or place it on PATH before running cps-core tests." >&2
    exit 1
  fi
  echo " Skipping xk6-kafka installation (suite: cps-core, no Kafka dependency)"
  echo " Checking k6 Version:"
  k6 --version
else
  # Download k6 with kafka extension
  if [ ! -x bin/k6 ]; then
    echo " Installing k6 1.0.0 with kafka extension"
    curl -s -L https://github.com/mostafa/xk6-kafka/releases/download/v1.0.0/xk6-kafka_v1.0.0_linux_amd64.tar.gz | tar -xz
    mv dist/xk6-kafka_v1.0.0_linux_amd64 bin/k6 && rmdir dist
    chmod +x bin/k6
  else
    echo " k6 already installed"
  fi
  echo " Checking k6 Version:"
  k6 --version

  # Only shell out to apt (and prompt for sudo) when something is actually missing,
  # so repeat local runs don't block on a password prompt.
  missingPackages=""
  command -v kafkacat >/dev/null 2>&1 || missingPackages="$missingPackages kafkacat"
  command -v jq >/dev/null 2>&1 || missingPackages="$missingPackages jq"
  if [ -n "$missingPackages" ]; then
    echo " Installing:$missingPackages"
    sudo apt-get install -y $missingPackages
  else
    echo " kafkacat and jq already installed"
  fi
fi
