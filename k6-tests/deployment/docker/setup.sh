#!/bin/bash
#
# Docker Environment Setup Script
# Copyright 2024-2025 OpenInfra Foundation Europe. All rights reserved.
#

set -e

testProfile=$1

# Load image check and logging functions
source "$(dirname "$0")/../../verify-docker-image-digests.sh"

# Set ENV and COMPOSE file paths
ENV_FILE="../../docker-compose/env/${testProfile}.env"

# Load environment variables from the selected .env file
set -o allexport
source "$ENV_FILE"
set +o allexport

# Define images to pre-check
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
    --file "../../docker-compose/docker-compose.yml" \
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

echo -e "\n✅ Docker setup complete for test profile: $testProfile"