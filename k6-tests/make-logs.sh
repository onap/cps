#!/bin/bash
#
# Copyright 2025 OpenInfra Foundation Europe. All rights reserved.
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

set -euo pipefail

# Constants
readonly LOG_DIR="${WORKSPACE:-.}/logs"
readonly LOG_RETENTION_DAYS=14
readonly TIMESTAMP=$(date +"%Y%m%d%H%M%S")
readonly SERVICES_TO_BE_LOGGED=("cps-and-ncmp" "ncmp-dmi-plugin-demo-and-csit-stub" "dbpostgresql")

# Ensure log directory exists
mkdir -p "$LOG_DIR"

# Function to fetch logs from a container
fetch_container_logs() {
  local container_id="$1"
  local container_name
  container_name=$(docker inspect --format="{{.Name}}" "$container_id" | sed 's/\///g')
  local log_file="$2/${container_name}_logs_$TIMESTAMP.log"
  docker logs "$container_id" > "$log_file"
}

# Function to archive logs for a service
archive_service_logs() {
  local service_name="$1"
  local temp_dir="$2"
  local zip_file="$3"

  mkdir -p "$temp_dir"

  local container_ids
  container_ids=$(docker ps --filter "name=$service_name" --format "{{.ID}}")

  for container_id in $container_ids; do
    fetch_container_logs "$container_id" "$temp_dir"
  done

  zip -r "$zip_file" "$temp_dir"
  echo "Logs for service '$service_name' saved to $zip_file"

  rm -r "$temp_dir"
}

# Main process
for service_name in "${SERVICES_TO_BE_LOGGED[@]}"; do
  temp_dir="$LOG_DIR/temp_${service_name}_$TIMESTAMP"
  zip_file="$LOG_DIR/logs_${service_name}_$TIMESTAMP.zip"

  archive_service_logs "$service_name" "$temp_dir" "$zip_file"
done

# Clean up old logs
find "$LOG_DIR" -name "logs_*.zip" -mtime +$LOG_RETENTION_DAYS -delete
