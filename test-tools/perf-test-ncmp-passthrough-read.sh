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

# This script measures the performance of NCMP data passthrough operations:
# NCMP endpoint tested: /ncmp/v1/ch/{cmHandleId}/data/ds/{datastoreName}

set -o errexit  # Exit on most errors
set -o nounset  # Disallow expansion of unset variables
set -o pipefail # Use last non-zero exit code in a pipeline
#set -o xtrace   # Uncomment for debugging

############################
# Configuration parameters #
############################
CPS_HOST=localhost
CPS_PORT=8883
PARALLEL_REQUESTS=12
WARMUP_REQUESTS=600
MEASUREMENT_REQUESTS=240

SCRIPT_DIR=$(dirname -- "${BASH_SOURCE[0]}")
# Read DMI delay from docker-compose.yml
DMI_DATA_DELAY=$(grep 'DATA_FOR_CM_HANDLE_DELAY_MS:' "$SCRIPT_DIR"/../docker-compose/docker-compose.yml | grep -oE '[0-9]+')

function cmHandleExists() {
  local cmHandleId=$1
  curl --silent --fail --output /dev/null "http://$CPS_HOST:$CPS_PORT/ncmp/v1/ch/$cmHandleId"
}

function failIfCmHandlesNotFound() {
  # Just check to see if last needed CM-handle exists
  local MAX_NEEDED_CM_HANDLES=$((WARMUP_REQUESTS > MEASUREMENT_REQUESTS ? WARMUP_REQUESTS : MEASUREMENT_REQUESTS))
  local cmHandleId="ch-$MAX_NEEDED_CM_HANDLES"
  if ! cmHandleExists "$cmHandleId"; then
    echo "ERROR: CM-handles not registered ($cmHandleId not found)" >&2
    echo "Note: this test assumes CM-handles have IDs ch-1, ch-2... ch-$MAX_NEEDED_CM_HANDLES" >&2
    exit 1
  fi
}

function warnIfLessThan20kCmHandlesFound() {
  local cmHandleId='ch-20000'
  if ! cmHandleExists "$cmHandleId"; then
    echo "WARNING: testing with less than 20,000 CM-handles is not recommended ($cmHandleId not found)" >&2
  fi
}

function measureAverageResponseTimeInMillis() {
  local totalRequests=$1
  curl --show-error --fail --fail-early \
    --output /dev/null --write-out '%{time_total}\n' \
    --parallel --parallel-max $PARALLEL_REQUESTS --parallel-immediate \
    --request POST "http://$CPS_HOST:$CPS_PORT/ncmp/v1/ch/ch-[1-$totalRequests]/data/ds/ncmp-datastore%3Apassthrough-operational?resourceIdentifier=x&include-descendants=true" |
    awk '{ sum += $1; n++ } END { if (n > 0) print (sum / n) * 1000; }'
}

# Sanity checks
failIfCmHandlesNotFound
warnIfLessThan20kCmHandlesFound

# Do JVM warmup
echo "Warming up ($WARMUP_REQUESTS requests, ignoring results)"
measureAverageResponseTimeInMillis "$WARMUP_REQUESTS" > /dev/null

# Measure performance
echo "Measuring average time of $MEASUREMENT_REQUESTS total requests, sending $PARALLEL_REQUESTS requests in parallel"
ncmpResponseTime=$(measureAverageResponseTimeInMillis "$MEASUREMENT_REQUESTS")
ncmpOverhead=$(echo "$ncmpResponseTime - $DMI_DATA_DELAY" | bc)

# Report performance
echo "Average response time from NCMP: $ncmpResponseTime ms"
echo "Average response time from DMI: $DMI_DATA_DELAY ms"
echo "NCMP overhead: $ncmpOverhead ms"
