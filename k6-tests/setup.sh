#!/bin/bash
#
# Copyright 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

# Load image check and logging functions
source "$(dirname "$0")/verify-docker-image-digests.sh"

testProfile=$1
ENV_FILE="../docker-compose/env/${testProfile}.env"
COMPOSE_FILE="../docker-compose/cps-base.yml"

# Define images to pre-check (add more if needed)
IMAGES_TO_CHECK=(
  "nexus3.onap.org:10003/onap/dmi-stub:latest"
)

# Run the image checks before anything else
check_images "${IMAGES_TO_CHECK[@]}"

# Define a function to encapsulate docker-compose command
compose() {
  docker-compose \
    --file "$COMPOSE_FILE" \
    --env-file "$ENV_FILE" \
    --project-name "$testProfile" "$@"
}

# Start the containers
echo
echo "Spinning off the following containers for '$testProfile'..."
echo
compose up --quiet-pull --detach --wait || { echo "Failed to start containers."; exit 1; }

# Define port mappings based on the test profile
declare -A CPS_PORTS=( ["kpi"]=8883 ["endurance"]=8884 )
declare -A DMI_DEMO_STUB_PORTS=( ["kpi"]=8784 ["endurance"]=8787 )

CPS_ACTUATOR_PORT="${CPS_PORTS[$testProfile]}"
DMI_DEMO_STUB_ACTUATOR_PORT="${DMI_DEMO_STUB_PORTS[$testProfile]}"

# Function to fetch and display build information
fetch_build_info() {
  local service_name="$1"
  local port="$2"
  local url="http://localhost:${port}/actuator/info"

  echo -e "\n${service_name} Build Information:"
  if curl --silent --show-error "$url"; then
    echo
  else
    echo "Error: Unable to retrieve ${service_name} build information from ${url}"
    exit 1
  fi
}

# Fetch and display build information for CPS and DMI
fetch_build_info "CPS and NCMP" "$CPS_ACTUATOR_PORT"
fetch_build_info "DMI" "$DMI_DEMO_STUB_ACTUATOR_PORT"
echo