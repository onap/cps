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

testProfile=$1
echo "Spinning off the CPS and NCMP containers for $testProfile testing..."

if [[ "$testProfile" == "endurance" ]]; then
  docker-compose -f ../docker-compose/docker-compose.yml --profile dmi-stub --project-name "$testProfile" --env-file ../docker-compose/config/endurance.env up --quiet-pull -d
  CONTAINER_IDS=$(docker ps --filter "name=endurance-cps-and-ncmp" --format "{{.ID}}")
else
  docker-compose -f ../docker-compose/docker-compose.yml --profile dmi-stub --project-name "$testProfile" up --quiet-pull -d
  CONTAINER_IDS=$(docker ps --filter "name=kpi-cps-and-ncmp" --format "{{.ID}}")
fi

echo "Waiting for CPS to start..."
READY_MESSAGE="Inventory Model updated successfully"

# Check the logs for each container
for CONTAINER_ID in $CONTAINER_IDS; do
    echo "Checking logs for container: $CONTAINER_ID"
    docker logs "$CONTAINER_ID" -f | grep -m 1 "$READY_MESSAGE" >/dev/null && echo "CPS is ready in container: $CONTAINER_ID" || true
done

# Output build information including git commit info
echo "Build information:"
curl http://localhost:8883/actuator/info
echo
