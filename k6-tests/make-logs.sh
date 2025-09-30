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

# Docker services
readonly DOCKER_SERVICES_TO_BE_LOGGED=("cps-and-ncmp" "ncmp-dmi-plugin-demo-and-csit-stub" "dbpostgresql")

# Kubernetes services
readonly K8S_SERVICES_TO_BE_LOGGED=("cps-cps-and-ncmp-cps" "cps-cps-and-ncmp-dmi-stub" "cps-cps-and-ncmp-postgresql")
readonly K8S_APP_LABEL="app=cps-and-ncmp"

# The default deployment type is dockerHosts
deploymentType=${2:-dockerHosts}

# Ensure log directory exists
mkdir -p "$LOG_DIR"

# Function to fetch logs from a Docker container
fetch_container_logs() {
  local container_id="$1"
  local container_name
  container_name=$(docker inspect --format="{{.Name}}" "$container_id" | sed 's/\///g')
  local log_file="$2/${container_name}_logs_$TIMESTAMP.log"
  docker logs "$container_id" > "$log_file"
}

# Function to fetch logs from Kubernetes pods
fetch_pod_logs() {
  local service_name="$1"
  local temp_dir="$2"

  # Get pod names for the current service, filtering by the app label and then grepping by service name.
  local pod_names
  pod_names=$(kubectl get pods -l "$K8S_APP_LABEL" --no-headers -o custom-columns=":metadata.name" | grep "^${service_name}" || echo "")

  if [ -z "$pod_names" ]; then
      echo "No running pods found for service: $service_name"
      return 1
  fi

  for pod_name in $pod_names; do
      echo "  Fetching logs for pod: $pod_name"
      local log_file="$temp_dir/${pod_name}_logs.log"
      kubectl logs "$pod_name" > "$log_file"
  done

  return 0
}

# Generic function to create zip archive from collected logs
create_log_archive() {
  local service_name="$1"
  local temp_dir="$2"
  local zip_file="$3"

  # Only create a zip file if logs were collected
  if [ -n "$(ls -A "$temp_dir")" ]; then
    echo "  Zipping logs to $zip_file"
    # The -j option flattens the directory structure. Logs will be at the root of the zip.
    zip -r -j "$zip_file" "$temp_dir"
    echo "  Logs for service '$service_name' saved to $zip_file"
  else
    echo "  No logs were fetched for service '$service_name'"
  fi
}

# Generic function to archive logs for a service (works for both Docker and Kubernetes)
archive_service_logs() {
  local service_name="$1"
  local deployment_type="$2"
  local temp_dir="$LOG_DIR/temp_${deployment_type}_${service_name}_$TIMESTAMP"
  local zip_file="$LOG_DIR/logs_${deployment_type}_${service_name}_$TIMESTAMP.zip"
  local logs_fetched=false

  echo "Processing service: $service_name"
  mkdir -p "$temp_dir"

  case "$deployment_type" in
    "docker")
      local container_ids
      container_ids=$(docker ps --filter "name=$service_name" --format "{{.ID}}")

      if [ -z "$container_ids" ]; then
        echo "No running containers found for service: $service_name"
      else
        for container_id in $container_ids; do
          fetch_container_logs "$container_id" "$temp_dir"
        done
        logs_fetched=true
      fi
      ;;
    "k8s")
      if fetch_pod_logs "$service_name" "$temp_dir"; then
        logs_fetched=true
      fi
      ;;
  esac

  if [ "$logs_fetched" = true ]; then
    create_log_archive "$service_name" "$temp_dir" "$zip_file"
  fi

  # Clean up the temporary directory
  rm -r "$temp_dir"
}

# Function to clean up old logs
cleanup_old_logs() {
  local pattern="$1"
  echo "Cleaning up logs older than $LOG_RETENTION_DAYS days..."
  find "$LOG_DIR" -name "$pattern" -mtime +$LOG_RETENTION_DAYS -delete
}

# Main process - handle different deployment types
case "$deploymentType" in
  "dockerHosts")
    echo "Processing Docker Compose deployment logs..."
    for service_name in "${DOCKER_SERVICES_TO_BE_LOGGED[@]}"; do
      archive_service_logs "$service_name" "docker"
    done
    cleanup_old_logs "logs_docker_*.zip"
    ls -la "$LOG_DIR"/logs_docker_*.zip 2>/dev/null || echo "No Docker log zip files found."
    ;;
  "k8sHosts")
    echo "Processing Kubernetes deployment logs..."
    for service_name in "${K8S_SERVICES_TO_BE_LOGGED[@]}"; do
      archive_service_logs "$service_name" "k8s"
    done
    cleanup_old_logs "logs_k8s_*.zip"
    ls -la "$LOG_DIR"/logs_k8s_*.zip 2>/dev/null || echo "No Kubernetes log zip files found."
    ;;
  *)
    echo "Error: Unknown deployment type '$deploymentType'. Supported types: 'dockerHosts', 'k8sHosts'"
    exit 1
    ;;
esac