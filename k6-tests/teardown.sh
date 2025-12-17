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

# The default test profile is kpi, and deployment type is k8sHosts
testProfile=${1:-kpi}
deploymentType=${2:-dockerHosts}

# Function to clean Docker images based on CLEAN_DOCKER_IMAGES environment variable
clean_docker_images_if_needed() {
  if [[ "${CLEAN_DOCKER_IMAGES:-0}" -eq 1 ]]; then
    echo "Also cleaning up all CPS images"
    remove_cps_images
  fi
}

# All CPS docker images:
# nexus3.onap.org:10003/onap/cps-and-ncmp:latest
# nexus3.onap.org:10003/onap/dmi-stub:1.8.0-SNAPSHOT
# nexus3.onap.org:10003/onap/policy-executor-stub:latest
remove_cps_images() {
  local cps_image_names=(cps-and-ncmp policy-executor-stub)
  for cps_image_name in "${cps_image_names[@]}"; do
    local image_path="nexus3.onap.org:10003/onap/$cps_image_name"
    # list all images for all tags except 'latest' since it would be in use
    # result will store in snapshot_tags[0], snapshot_tags[1]
    local snapshot_tags=( $(docker images "$image_path" --format '{{.Tag}}' | grep -v '^latest$') )
    if [ ${#snapshot_tags[@]} -gt 0 ]; then
      echo "Removing snapshot images for tags $image_path: ${snapshot_tags[*]}"
      # remove each snapshot image explicitly
      for tag in "${snapshot_tags[@]}"; do
        docker rmi "$image_path:$tag"
      done
    fi
  done
}

# Function to teardown docker-compose deployment
teardown_docker_deployment() {
  echo '================================== docker info =========================='
  docker ps -a

  # Zip and store logs for the containers
  make_logs "dockerHosts"

  local docker_compose_shutdown_cmd="docker-compose -f ../docker-compose/docker-compose.yml --project-name $testProfile down --volumes"

  # Check env. variable CLEAN_DOCKER_IMAGES=1 to decide removing CPS images
  echo "Stopping, Removing containers and volumes for $testProfile tests..."
  eval "$docker_compose_shutdown_cmd"

  # Clean Docker images if requested
  clean_docker_images_if_needed
}

# Function to teardown kubernetes deployment
teardown_k8s_deployment() {
  echo '================================== k8s info =========================='
  kubectl get all -l app=ncmp

  echo '================================== uninstalling cps... =========================='
  helm uninstall cps

  # Clean Docker images if requested
  clean_docker_images_if_needed
}

# Main logic: determine which deployment type to teardown
case "$deploymentType" in
  "k8sHosts")
    teardown_k8s_deployment
    ;;
  "dockerHosts")
    teardown_docker_deployment
    ;;
  *)
    echo "Unknown deployment type: $deploymentType"
    echo "Supported deployment types: k8sHosts, dockerHosts"
    exit 1
    ;;
esac