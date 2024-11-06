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

if [[ "$testProfile" == "ENDURANCE" ]]; then
  echo 'Stopping, Removing containers and volumes for endurance tests...'
  docker_compose_cmd="docker-compose -f ../docker-compose/docker-compose.yml --profile dmi-stub --project-name endurance down --volumes"
else
  echo 'Stopping, Removing containers and volumes for performance tests...'
  docker_compose_cmd="docker-compose -f ../docker-compose/docker-compose.yml --profile dmi-stub down --volumes"
fi

# Set an environment variable CLEAN_DOCKER_IMAGES=1 to also remove docker images when done (used on jenkins job)
if [ "${CLEAN_DOCKER_IMAGES:-0}" -eq 1 ]; then
  $docker_compose_cmd --rmi all
else
  $docker_compose_cmd
fi
