#!/bin/bash
#
# Copyright 2023 Nordix Foundation.
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

set -o errexit  # Exit on most errors
set -o nounset  # Disallow expansion of unset variables
set -o pipefail # Use last non-zero exit code in a pipeline
#set -o xtrace   # Uncomment for debugging

GRAB_METRICS=true
DOCKER_COMPOSE_FILE=../docker-compose/docker-compose.yml
CREATE_REQUEST=/tmp/cmhandles-create-req.txt
REMOVE_REQUEST=/tmp/cmhandles-remove-req.txt
REPORT_FILE=./metrics-reports/deregister-summary-$(date --iso-8601=seconds).tsv
SCRIPT_DIR=$(dirname -- "${BASH_SOURCE[0]}")

stop_docker() {
    docker-compose -f $DOCKER_COMPOSE_FILE --profile dmi-stub --profile monitoring down >/dev/null
    docker container prune -f >/dev/null
    docker volume prune -f >/dev/null
}

restart_docker() {
    stop_docker
    docker-compose -f $DOCKER_COMPOSE_FILE --profile dmi-stub --profile monitoring up -d >/dev/null
}

wait_for_cps_to_start() {
    docker logs cps-and-ncmp -f | grep -m 1 'Started Application' >/dev/null || true
}

get_number_of_handles_ready() {
    PGPASSWORD=cps psql -h localhost -p 5432 cpsdb cps -c \
        "SELECT count(*) FROM public.fragment where attributes @> '{\"cm-handle-state\": \"READY\"}';" \
        | sed '3!d' | sed 's/ *//'
}

wait_for_handles_to_be_ready() {
    local TOTAL_HANDLES=$1
    local HANDLES_READY=0
    while [ "$HANDLES_READY" -lt "$TOTAL_HANDLES" ]; do
        sleep 10
        HANDLES_READY=$(get_number_of_handles_ready)
        echo "There are $HANDLES_READY CM handles in READY state."
    done
}

create_handles() {
    curl --fail --silent --show-error \
        --location 'http://localhost:8883/ncmpInventory/v1/ch' \
        --header 'Authorization: Basic Y3BzdXNlcjpjcHNyMGNrcyE=' \
        --header 'Content-Type: application/json' \
        --data @$CREATE_REQUEST
}

remove_handles_and_record_time() {
    curl --fail --silent --show-error --output /dev/null --write-out '%{time_total}\n' \
        --location 'http://localhost:8883/ncmpInventory/v1/ch' \
        --header 'Authorization: Basic Y3BzdXNlcjpjcHNyMGNrcyE=' \
        --header 'Content-Type: application/json' \
        --data @$REMOVE_REQUEST >> "$REPORT_FILE"
}

create_request_bodies() {
    local CREATE_SIZE=$1
    local REMOVE_SIZE=$2
    echo -n '{"dmiPlugin": "http://ncmp-dmi-plugin-demo-and-csit-stub:8092","createdCmHandles":[' > $CREATE_REQUEST
    echo -n '{"dmiPlugin": "http://ncmp-dmi-plugin-demo-and-csit-stub:8092","removedCmHandles":[' > $REMOVE_REQUEST
    for i in $(seq 1 "$CREATE_SIZE"); do
        local CMHANDLE
        CMHANDLE=$(uuidgen | tr -d '-')
        echo -n "{\"cmHandle\": \"$CMHANDLE\",\"cmHandleProperties\":{\"neType\":\"RadioNode\"}}" \
            >> $CREATE_REQUEST
        if [ "$i" -lt "$CREATE_SIZE" ]; then
            echo -n "," >> $CREATE_REQUEST
        fi
        if [ "$i" -le "$REMOVE_SIZE" ]; then
            echo -n "\"$CMHANDLE\"" >> $REMOVE_REQUEST
        fi
        if [ "$i" -lt "$REMOVE_SIZE" ]; then
            echo -n "," >> $REMOVE_REQUEST
        fi
    done
    echo ']}' >> $CREATE_REQUEST
    echo ']}' >> $REMOVE_REQUEST
}

test_deregistration() {
    local REMOVE_SIZE=$1
    local CREATE_SIZE=$2

    echo "Testing deregistration of $REMOVE_SIZE out of $CREATE_SIZE CM handles"

    echo "Restarting docker"
    restart_docker
    echo "Waiting for CPS to start"
    wait_for_cps_to_start

    echo "Creating request bodies"
    create_request_bodies "$CREATE_SIZE" "$REMOVE_SIZE"

    echo "[$(date --iso-8601=seconds)] Creating CM handles"
    create_handles
    echo "Waiting for CM handles to be in READY state"
    wait_for_handles_to_be_ready "$CREATE_SIZE"

    if [ "$GRAB_METRICS" = "true" ]; then
        echo "Grabbing metrics before deregistration"
        METRICS_BEFORE=$(./generate-metrics-report.sh)
    fi

    echo "[$(date --iso-8601=seconds)] Removing CM handles"
    echo -e -n "$REMOVE_SIZE\t$CREATE_SIZE\t" >> "$REPORT_FILE"
    remove_handles_and_record_time
    echo "There are $(get_number_of_handles_ready) CM handles still in READY state."

    if [ "$GRAB_METRICS" = "true" ]; then
        echo "Grabbing metrics after deregistration"
        METRICS_AFTER=$(./generate-metrics-report.sh)
        echo "Generating metrics report"
        ./subtract-metrics-reports.py -a "$METRICS_AFTER" -b "$METRICS_BEFORE" \
            -o "metrics-reports/deregister-$(date --iso-8601=seconds)-$REMOVE_SIZE-$CREATE_SIZE.tsv"
        rm "$METRICS_BEFORE" "$METRICS_AFTER"
    fi

    echo
}

cleanup() {
    rm -f "$CREATE_REQUEST" "$REMOVE_REQUEST"
    stop_docker
    cat "$REPORT_FILE"
    popd
}
trap cleanup EXIT

pushd -- "$SCRIPT_DIR"

mkdir -p "$(dirname "$REPORT_FILE")"
echo -e "Removed\tTotal\tTime" > "$REPORT_FILE"

for number_to_delete in 100 500 1000 5000 10000 20000; do
    test_deregistration $number_to_delete $number_to_delete
done
