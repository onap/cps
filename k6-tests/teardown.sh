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

echo '================================== docker info =========================='
docker ps -a

testProfile=$1
docker_compose_shutdown_cmd="docker-compose -f ../docker-compose/docker-compose.yml --profile dmi-stub --project-name $testProfile down --volumes"

# Set an environment variable CLEAN_DOCKER_IMAGES=1 to also remove docker images when done (used on jenkins job)
echo "Stopping, Removing containers and volumes for $testProfile tests..."
if [ "${CLEAN_DOCKER_IMAGES:-0}" -eq 1 ]; then
  $docker_compose_shutdown_cmd --rmi all
else
  $docker_compose_shutdown_cmd
fi
