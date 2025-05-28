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
SERVICE_NAMES=("cps-and-ncmp" "dbpostgresql")
TIMESTAMP=$(date +"%Y%m%d%H%M%S")
LOG_DIR="${WORKSPACE:-.}/logs"
mkdir -p "$LOG_DIR"
# Store logs for each service's containers and zip them individually
for SERVICE_NAME in "${SERVICE_NAMES[@]}"; do
    TEMP_DIR="$LOG_DIR/temp_${SERVICE_NAME}_$TIMESTAMP"
    ZIP_FILE="$LOG_DIR/logs_${SERVICE_NAME}_$TIMESTAMP.zip"
    mkdir -p "$TEMP_DIR"
    CONTAINER_IDS=$(docker ps --filter "name=$SERVICE_NAME" --format "{{.ID}}")
    for CONTAINER_ID in $CONTAINER_IDS; do
        CONTAINER_NAME=$(docker inspect --format="{{.Name}}" "$CONTAINER_ID" | sed 's/\///g')
        LOG_FILE="$TEMP_DIR/${CONTAINER_NAME}_logs_$TIMESTAMP.log"
        docker logs "$CONTAINER_ID" > "$LOG_FILE"
    done
    # Zip the logs for the current service
    zip -r "$ZIP_FILE" "$TEMP_DIR"
    echo "Logs for service $SERVICE_NAME saved to $ZIP_FILE"
    # Clean temp files for the current service
    rm -r "$TEMP_DIR"
done
# Delete logs older than 2 weeks
find "$LOG_DIR" -name "logs_*.zip" -mtime +14 -delete