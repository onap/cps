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
docker_compose_shutdown_cmd="docker-compose --project-name $testProfile down --volumes"
remove_onap_docker_images_cmd="docker images | grep "onap" | awk '{print $3}' | xargs docker rmi"

# Check for remaining ONAP images and log it
check_remaining_onap_images() {
  no_of_onap_docker_images=$(docker images | grep "onap" | wc -l)
  if [[ "$no_of_onap_docker_images" -eq 0 ]]; then
    echo "Successfully removed ONAP docker images!"
  else
    echo "Removing ONAP docker images failed."
  fi
}

# Remove all ONAP docker images
# /onap/cps-and-ncmp
# /onap/dmi-stub
# /onap/policy-executor-stub
remove_all_onap_docker_images() {
  no_of_onap_docker_images=$(docker images | grep "onap" | wc -l)
  if [[ "$no_of_onap_docker_images" -ne 0 ]]; then
    echo "Removing all ONAP docker images..."
    eval "$remove_onap_docker_images_cmd"
  fi
}

# Check env. variable CLEAN_DOCKER_IMAGES=1 to decide removing images
echo "Stopping, Removing containers and volumes for $testProfile tests..."
if [[ "${CLEAN_DOCKER_IMAGES:-0}" -eq 1 ]]; then
  # down the compose stack, then purge any remaining ONAP images,
  # regardless of any test profile!
  echo "Also cleaning up all images"
  eval "$docker_compose_shutdown_cmd --rmi all"
  remove_all_onap_docker_images
  check_remaining_onap_images
else
  # for local test operations
  eval "$docker_compose_shutdown_cmd"
fi
