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

set -e

# Load image check and logging functions
source "$(dirname "$0")/verify-docker-image-digests.sh"

# Accept test profile (e.g., 'kpi', 'endurance') as first argument
testProfile=$1

# Set ENV and COMPOSE file paths
ENV_FILE="../docker-compose/env/${testProfile}.env"
COMPOSE_FILE="../docker-compose/docker-compose.yml"

# Load environment variables from the selected .env file
set -o allexport
source "$ENV_FILE"
set +o allexport

# Define images to pre-check (add more if needed)
IMAGES_TO_CHECK=(
  "nexus3.onap.org:10003/onap/dmi-stub:${DMI_DEMO_STUB_VERSION}"
)

echo -e "\n📦 Docker images to check:"
for img in "${IMAGES_TO_CHECK[@]}"; do
  echo "   - $img"
done

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
echo -e "\n🚀 Spinning off containers for profile: '$testProfile'...\n"
compose up --quiet-pull --detach --wait || {
  echo "❌ Failed to start containers."
  exit 1
}

# Define port mappings from env file based on the test profile
CPS_ACTUATOR_PORT=$CPS_CORE_PORT
DMI_DEMO_STUB_ACTUATOR_PORT=$DMI_DEMO_STUB_PORT

# Fetch build info from actuators
fetch_build_info() {
  local service_name="$1"
  local port="$2"
  local url="http://localhost:${port}/actuator/info"

  echo -e "\n🔍 ${service_name} Build Information:"
  if curl --silent --show-error "$url"; then
    echo
  else
    echo "⚠️  Error: Unable to retrieve ${service_name} build information from ${url}"
    exit 1
  fi
}

# Fetch and display build information for CPS and DMI
fetch_build_info "CPS and NCMP" "$CPS_ACTUATOR_PORT"
fetch_build_info "DMI" "$DMI_DEMO_STUB_ACTUATOR_PORT"

echo -e "\n✅ Setup complete for test profile: $testProfile"
