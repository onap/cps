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

# ------------------------
# Constants & Configuration
# ------------------------
readonly LOG_DIR="${WORKSPACE:-.}/logs"
readonly LOG_RETENTION_DAYS=14
readonly TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
readonly DOCKER_SERVICES=("cps-and-ncmp" "ncmp-dmi-plugin-demo-and-csit-stub" "dbpostgresql")
deploymentType=${1:-dockerHosts}

mkdir -p "$LOG_DIR"

# ------------------------
# Helper Functions
# ------------------------

# Fetch logs from a Docker container
fetch_container_logs() {
    local container_id="$1" dest_dir="$2"
    local name
    name=$(docker inspect --format="{{.Name}}" "$container_id" | sed 's/\///g')
    docker logs "$container_id" > "$dest_dir/${name}_logs_$TIMESTAMP.log"
}

# Fetch logs from Kubernetes pods
fetch_pod_logs() {
    local service="$1" dest_dir="$2"
    local pods
    pods=$(kubectl get pods -o custom-columns=":metadata.name" | grep "^$service")

    [ -z "$pods" ] && { echo "No pods found for $service"; return 1; }

    for pod in $pods; do
        echo "  Fetching logs for pod: $pod"
        kubectl logs "$pod" > "$dest_dir/${pod}_logs.log"
    done
}

# Zip collected logs
create_log_archive() {
    local service="$1" src_dir="$2" zip_file="$3"
    if [ -n "$(ls -A "$src_dir")" ]; then
        echo "  Zipping logs to $zip_file"
        zip -r -j "$zip_file" "$src_dir"
        echo "  Logs saved for $service"
    else
        echo "  No logs fetched for $service"
    fi
}

# Archive logs for a service (Docker or K8s)
archive_service_logs() {
    local service="$1" type="$2"
    local temp_dir="$LOG_DIR/temp_${type}_${service}_$TIMESTAMP"
    local zip_file="$LOG_DIR/logs_${type}_${service}_$TIMESTAMP.zip"
    local fetched=false

    echo "Processing $service..."
    mkdir -p "$temp_dir"

    if [ "$type" = "docker" ]; then
        local containers
        containers=$(docker ps --filter "name=$service" --format "{{.ID}}")
        if [ -z "$containers" ]; then
            echo "No Docker containers found for $service"
        else
            for c in $containers; do fetch_container_logs "$c" "$temp_dir"; done
            fetched=true
        fi

    elif [ "$type" = "k8s" ]; then
        fetch_pod_logs "$service" "$temp_dir" && fetched=true
    fi

    $fetched && create_log_archive "$service" "$temp_dir" "$zip_file"
    rm -r "$temp_dir"
}

# Remove old log files
cleanup_old_logs() {
    local pattern="$1"
    echo "Cleaning up logs older than $LOG_RETENTION_DAYS days..."
    find "$LOG_DIR" -name "$pattern" -mtime +$LOG_RETENTION_DAYS -delete
}

# Renumber log files newest first
renumber_logs_latest_first() {
    local pattern="$1" width=3 index=1
    mapfile -t files < <(ls -1t "$LOG_DIR"/$pattern 2>/dev/null) || return
    for f in "${files[@]}"; do
        local base=$(basename "$f" | sed -E 's/^[0-9]+_//')
        printf -v prefix "%0*d" "$width" "$index"
        mv "$f" "$LOG_DIR/${prefix}_${base}"
        ((index++))
    done
}

# ------------------------
# Main Process
# ------------------------
case "$deploymentType" in
    dockerHosts)
        echo "Processing Docker Compose logs..."
        for service in "${DOCKER_SERVICES[@]}"; do
            archive_service_logs "$service" "docker"
        done
        cleanup_old_logs "logs_docker_*.zip"
        renumber_logs_latest_first "logs_docker_*.zip"
        ls -la "$LOG_DIR"/logs_docker_*.zip 2>/dev/null || echo "No Docker logs found."
        ;;

    k8sHosts)
        echo "Processing Kubernetes logs..."
        archive_service_logs "cps-ncmp" "k8s"
        cleanup_old_logs "*logs_*.zip"
        renumber_logs_latest_first "*logs_k8s_*.zip"
        ls -la "$LOG_DIR"/logs_k8s_*.zip 2>/dev/null || echo "No Kubernetes logs found."
        ;;

    *)
        echo "Error: Unknown deployment type '$deploymentType'. Supported: dockerHosts, k8sHosts"
        exit 1
        ;;
esac
