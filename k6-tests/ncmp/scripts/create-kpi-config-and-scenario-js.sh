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

#
# This script is used to generate K6 performance testing configuration files by:
# 1) Extracting trend declarations from metric metadata JSON (scenario-metadata.json)
# 2) Injecting these trend declarations into a JavaScript (scenarios-javascript.js)
# 3) Conditionally generating and injecting threshold configs for KPI tests (scenario-metadata.json)
#

# ─────────────────────────────────────────────────────────────
# 📌 Global Variables
# ─────────────────────────────────────────────────────────────
# Path to the JSON file containing metric metadata.
# This JSON holds metric names, units, and KPI threshold values.
SCENARIO_METADATA_FILE="../config/scenario-metadata.json"

# Scenario JSON template file for scenario execution configuration.
# Contains placeholder (#THRESHOLDS-PLACEHOLDER#) to be replaced with actual threshold values.
SCENARIO_CONFIG_TEMPLATE_FILE="../templates/scenario-execution.tmpl"

# Final json scenario execution configuration file with thresholds injected.
SCENARIO_CONFIG_OUTPUT_FILE="../config/scenario-execution.json"

# JavaScript template file which contains a placeholder (#METRICS-TRENDS-PLACE-HOLDER#) for trend declarations.
SCENARIO_JAVASCRIPT_TEMPLATE_FILE="../templates/scenario-javascript.tmpl"

# Final JavaScript file where generated trend declarations will be inserted.
SCENARIO_JAVASCRIPT_OUTPUT_FILE="scenarios-javascript.js"

# ───────────────────────────────────────────────────────────────────────────
# 1️⃣ Generate trend declarations and (conditionally) thresholds from metadata
# ───────────────────────────────────────────────────────────────────────────
echo "🔧 Generating trend declarations from [$SCENARIO_METADATA_FILE]..."

# It performs three main tasks:
# - toCamelCase: Converts metric names from snake_case to camelCase (for JS variable names for example: cm_handles_deleted → cmHandlesDeleted)
# - Generates JS 'Trend' declarations based on metric name and unit.
# - Prepares threshold expressions for each metric
read -r -d '' jq_script << 'EOF'
# Function to convert strings from snake_case to camelCase
def toCamelCase:
  split("_") as $parts |
  ($parts[0]) + ($parts[1:] | map((.[0:1] | ascii_upcase) + .[1:]) | join(""));

# Reduce over all metric items to build an object containing:
# - 'trends': array of JS trend declaration strings
# - 'thresholds': object mapping metric names to threshold expressions
reduce .[] as $item (
  { trends: [], thresholds: {} };
   # If metric unit is 'milliseconds' or contains '/second', generate a Trend declaration
  if ($item.unit == "milliseconds") or ($item.unit | test("/second")) then
    .trends += [
      # JS declaration example:
      # export let httpReqDurationTrend = new Trend('http_req_duration', true);
      "export let \($item.metric | toCamelCase)Trend = new Trend('\($item.metric)', \($item.unit == "milliseconds"));"
    ]
  end
  |
  # Set threshold expression based on metric type:
  # - For 'http_req_failed', threshold is rate <= kpiThreshold
  # - For metrics per second, threshold is avg >= kpiThreshold
  # - Otherwise, avg <= kpiThreshold
  .thresholds[$item.metric] = (
    if $item.metric == "http_req_failed" then
      ["rate <= \($item.kpiThreshold)"]
    elif ($item.unit | test("/second")) then
      ["avg >= \($item.kpiThreshold)"]
    else
      ["avg <= \($item.kpiThreshold)"]
    end
  )
)
EOF

# Run the above jq script on the metric metadata JSON file (scenario-metadata.json).
# This returns a JSON object with two fields:
# - 'trends': array of JS declaration strings
# - 'thresholds': mapping of metric names to threshold arrays
trends_and_thresholds_as_json=$(jq -r "$jq_script" "$SCENARIO_METADATA_FILE")

# Extract just the trend declarations from the JSON output.
# This results in plain JavaScript code lines like:
# export let httpReqDurationTrend = new Trend('http_req_duration', true);
trend_declarations=$(echo "$trends_and_thresholds_as_json" | jq -r '.trends[]')

# Use awk to replace the placeholder '#METRICS-TRENDS-PLACE-HOLDER#'
# in the JS template file (scenario-javascript.tmpl) with the actual trend declarations.
# We use a flag 'replaced' to ensure only the first occurrence is replaced.
awk -v trends="$trend_declarations" '
  BEGIN { replaced=0 }
  {
    if ($0 ~ /#METRICS-TRENDS-PLACE-HOLDER#/ && replaced == 0) {
      print trends
      replaced=1
    } else {
      print $0
    }
  }
' "$SCENARIO_JAVASCRIPT_TEMPLATE_FILE" > "$SCENARIO_JAVASCRIPT_OUTPUT_FILE"
echo "✅ Trend declarations inserted into [$SCENARIO_JAVASCRIPT_OUTPUT_FILE]"

# Only proceed if the environment variable 'testProfile' is set to 'kpi'.
# This controls whether the threshold block needs to be injected.
if [[ "$testProfile" == "kpi" ]]; then
  echo "📌 Writing thresholds to [$SCENARIO_CONFIG_OUTPUT_FILE]..."
  # Extract the thresholds object from the combined JSON output.
  # This contains a mapping from metric names to their threshold expressions.
  thresholds_json=$(echo "$trends_and_thresholds_as_json" | jq '.thresholds')
  # Use jq to update the thresholds field inside the scenario config template JSON.
  # The updated JSON is then saved as the final scenario execution config file.
  jq --argjson thresholds "$thresholds_json" '.thresholds = $thresholds' "$SCENARIO_CONFIG_TEMPLATE_FILE" | jq '.' > "$SCENARIO_CONFIG_OUTPUT_FILE"
  echo "✅ Threshold block has been injected into [$SCENARIO_CONFIG_OUTPUT_FILE]"
  echo
fi