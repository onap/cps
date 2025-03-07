#!/bin/bash
#
# Copyright 2025 Nordix Foundation.
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

SERVICE_NAME="cps-and-ncmp"
TIMESTAMP=$(date +"%Y%m%d%H%M%S")
LOG_DIR="logs"
TEMP_DIR="$LOG_DIR/temp_$TIMESTAMP"
ZIP_FILE="$LOG_DIR/${SERVICE_NAME}_logs_$TIMESTAMP.zip"

mkdir -p "$LOG_DIR"
mkdir -p "$TEMP_DIR"

# Store logs for cps-and-ncmp containers to temp directory
CONTAINER_IDS=$(docker ps --filter "name=$SERVICE_NAME" --format "{{.ID}}")
for CONTAINER_ID in $CONTAINER_IDS; do
    CONTAINER_NAME=$(docker inspect --format="{{.Name}}" "$CONTAINER_ID" | sed 's/\///g')
    LOG_FILE="$TEMP_DIR/${CONTAINER_NAME}_logs_$TIMESTAMP.log"
    docker logs "$CONTAINER_ID" > "$LOG_FILE"
done

# Zip the logs
zip -r "$ZIP_FILE" "$TEMP_DIR"
echo "Logs saved to $ZIP_FILE inside workspace"

# Clean temp files
rm -r "$TEMP_DIR"

# Delete logs older than 1 week
find "$LOG_DIR" -name "${SERVICE_NAME}_logs_*.zip" -mtime +7 -delete
