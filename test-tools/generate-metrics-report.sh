#!/bin/bash
#
# Copyright 2023-2025 Nordix Foundation.
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

function script_usage() {
  cat <<EOF
Usage:
     -h|--help              Displays this help
     -u|--metrics-url=URL   URL to Prometheus metrics
                            Default: http://localhost:8883/actuator/prometheus
     -o|--output=FILE       Path to output file
                            Default: metrics-reports/metrics-[timestamp].tsv
EOF
}

function parse_params() {
  # Set parameter defaults
  METRICS_URL="http://localhost:8883/actuator/prometheus"
  OUTFILE="metrics-reports/metrics-$(date --iso-8601=seconds).tsv"
  TEMP_DIR="/tmp/cps-metrics"

  # Parse parameters
  local param
  while [[ $# -gt 0 ]]; do
    param="$1"
    shift
    case $param in
    -h | --help)
      script_usage
      exit 0
      ;;
    -u | --metrics-url)
      METRICS_URL=$1
      shift
      ;;
    -o | --output)
      OUTFILE=$1
      shift
      ;;
    *)
      echo "Invalid parameter was provided: $param" >&2
      script_usage
      exit 1
      ;;
    esac
  done
}

function generate_report() {
  # Create needed directories.
  mkdir -p $TEMP_DIR "$(dirname "$OUTFILE")"

  # Scrape raw metrics (suppress progress meter).
  curl --fail --silent --show-error --output $TEMP_DIR/metrics-raw.txt "$METRICS_URL"

  # Remove comments, sort by name, and separate by tabs.
  grep --invert-match "^#" $TEMP_DIR/metrics-raw.txt | sort | sed 's/,[}]/}\t/' >$TEMP_DIR/metrics-all.txt

  # Extract useful metrics.
  grep -E "^cps_|^spring_data_|^http_server_|^http_client_|^tasks_scheduled_execution_|^spring_kafka_template_|^spring_kafka_listener_" $TEMP_DIR/metrics-all.txt >$TEMP_DIR/metrics-cps.txt

  # Extract into columns.
  grep "_count" $TEMP_DIR/metrics-cps.txt | sed 's/_count//' | cut -d ' ' -f 1 >$TEMP_DIR/column1.txt
  grep "_count" $TEMP_DIR/metrics-cps.txt | cut -d ' ' -f 2 >$TEMP_DIR/column2.txt
  grep "_sum"   $TEMP_DIR/metrics-cps.txt | cut -d ' ' -f 2 >$TEMP_DIR/column3.txt
  grep "_max"   $TEMP_DIR/metrics-cps.txt | cut -d ' ' -f 2 >$TEMP_DIR/column4.txt

  # Combine columns into report.
  paste $TEMP_DIR/column{1,2,3,4}.txt >$TEMP_DIR/report.txt

  # Sort by Sum (column 3), descending.
  sort --general-numeric-sort --reverse --field-separator=$'\t' --key=3 $TEMP_DIR/report.txt >$TEMP_DIR/report-sorted.txt

  # Compile final report, with column headers.
  echo -e "Method\tCount\tSum\tMax" >"$OUTFILE"
  cat $TEMP_DIR/report-sorted.txt >>"$OUTFILE"

  # Output path to generated file
  echo "$OUTFILE"
}

function cleanup() {
  rm -f $TEMP_DIR/* && rmdir $TEMP_DIR 2>/dev/null
}
# Set up the cleanup function to be triggered upon script exit
trap cleanup EXIT

# Main script logic
parse_params "$@"
generate_report
exit 0
