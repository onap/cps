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
readonly K8S_SERVICES_TO_BE_LOGGED=("cps-cps-and-ncmp-cps" "cps-cps-and-ncmp-dmi-stub" "cps-cps-and-ncmp-postgresql")
readonly K8S_APP_LABEL="app=cps-and-ncmp"


# Ensure log directory exists
mkdir -p "$LOG_DIR"

# Main process
for service_name in "${K8S_SERVICES_TO_BE_LOGGED[@]}"; do
  echo "Processing service: $service_name"

  # A temporary directory for the logs of the current service
  temp_dir="$LOG_DIR/temp_k8s_${service_name}_$TIMESTAMP"
  mkdir -p "$temp_dir"

  # Get pod names for the current service, filtering by the app label and then grepping by service name.
  pod_names=$(kubectl get pods -l "$K8S_APP_LABEL" --no-headers -o custom-columns=":metadata.name" | grep "^${service_name}" || echo "")

  if [ -z "$pod_names" ]; then
      echo "No running pods found for service: $service_name"
      rm -r "$temp_dir"
      continue
  fi

  for pod_name in $pod_names; do
      echo "  Fetching logs for pod: $pod_name"
      log_file="$temp_dir/${pod_name}_logs.log"
      kubectl logs "$pod_name" > "$log_file"
  done

  # Only create a zip file if logs were collected
  if [ -n "$(ls -A "$temp_dir")" ]; then
    zip_file="$LOG_DIR/logs_k8s_${service_name}_$TIMESTAMP.zip"
    echo "  Zipping logs to $zip_file"
    # The -j option in zip will junk the directory structure. Logs will be at the root of the zip.
    zip -r -j "$zip_file" "$temp_dir"
    echo "  Logs for service '$service_name' saved to $zip_file"
  else
    echo "  No logs were fetched for service '$service_name'"
  fi

  # Clean up the temporary directory
  rm -r "$temp_dir"
done

# Clean up old logs
echo "Cleaning up logs older than $LOG_RETENTION_DAYS days..."
find "$LOG_DIR" -name "logs_k8s_*.zip" -mtime +$LOG_RETENTION_DAYS -delete

ls -la "$LOG_DIR"/logs_*.zip 2>/dev/null || echo "No log zip files found."