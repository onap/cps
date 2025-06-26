#!/bin/bash
#
# Copyright 2024-2025 OpenInfra Foundation Europe.
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

# Zip and store logs for the containers
chmod +x make-logs.sh
./make-logs.sh

testProfile=$1
docker_compose_shutdown_cmd="docker-compose -f ../docker-compose/docker-compose.yml --project-name $testProfile down --volumes"

# All CPS docker images:
# nexus3.onap.org:10003/onap/cps-and-ncmp:latest
# nexus3.onap.org:10003/onap/dmi-stub:1.8.0-SNAPSHOT
# nexus3.onap.org:10003/onap/policy-executor-stub:latest
remove_cps_images() {
  local cps_image_names=(cps-and-ncmp dmi-stub policy-executor-stub)
  for cps_image_name in "${cps_image_names[@]}"; do
    local image_path="nexus3.onap.org:10003/onap/$cps_image_name"
    # list all image IDs for this repository (all tags)
    image_tags=$(docker images -q "$image_path")
    if [ -n "$image_tags" ]; then
      echo "Removing images for $image_path..."
      docker rmi -f $image_tags
    fi
  done
}

# Check env. variable CLEAN_DOCKER_IMAGES=1 to decide removing CPS images
echo "Stopping, Removing containers and volumes for $testProfile tests..."
if [[ "${CLEAN_DOCKER_IMAGES:-0}" -eq 1 ]]; then
  # down the compose stack, then purge any remaining CPS images,
  # regardless of any test profile!
  eval "$docker_compose_shutdown_cmd"
  echo "Also cleaning up all CPS images"
  remove_cps_images
else
  # for local test operations
  eval "$docker_compose_shutdown_cmd"
fi
