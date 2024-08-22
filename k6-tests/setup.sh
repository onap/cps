#!/bin/bash
#
# Copyright 2024-2025 Nordix Foundation.
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

testProfile=$1
echo "Spinning off the CPS and NCMP containers for $testProfile testing..."

ENV_FILE="../docker-compose/env/${testProfile}.env"
docker-compose \
  --file "../docker-compose/docker-compose.yml" \
  --env-file "$ENV_FILE" \
  --project-name "$testProfile" \
  --profile dmi-stub \
  up --quiet-pull --detach --wait || exit 1

echo "Build information:"
CPS_CORE_PORT=$(awk -F= '/^CPS_CORE_PORT=/ {print $2}' "$ENV_FILE")
curl --silent "http://localhost:$CPS_CORE_PORT/actuator/info"
echo
