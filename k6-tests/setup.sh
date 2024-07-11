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

docker-compose -f ../docker-compose/docker-compose.yml --profile dmi-stub up -d

echo "Waiting for CPS to start..."
READY_MESSAGE="Processing module sync fetched 0 advised cm handles from DB"

# Get the container IDs of the cps-and-ncmp replicas
CONTAINER_IDS=$(docker ps --filter "name=cps-and-ncmp" --format "{{.ID}}")

# Check the logs for each container
for CONTAINER_ID in $CONTAINER_IDS; do
    echo "Checking logs for container: $CONTAINER_ID"
    docker logs "$CONTAINER_ID" -f | grep -m 1 "$READY_MESSAGE" >/dev/null && echo "CPS is ready in container: $CONTAINER_ID" || true
done
