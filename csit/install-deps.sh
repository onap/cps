#!/bin/bash
#
# Copyright 2024 Nordix Foundation.
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

# Download docker-compose.
if [ ! -x bin/docker-compose ]; then
  echo " Downloading docker-compose"
  curl -s -L https://github.com/docker/compose/releases/download/v2.29.2/docker-compose-linux-x86_64 > bin/docker-compose
  chmod +x bin/docker-compose
else
  echo " docker-compose already installed"
fi
docker-compose version
