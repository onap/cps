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
# This script is used to generate K6 performance testing 'thresholds' declarations file by:
#   1) Extracting thresholds from metric metadata JSON (scenario-metadata.json)
#   2) Injects them into a scenario execution template file (scenario-execution.tmpl)
#   2) Writes a scenario-execution config file (kpi-scenario-execution.json) with thresholds declarations.
#

# ─────────────────────────────────────────────────────────────
# Global Variables
# ─────────────────────────────────────────────────────────────

# Path to the JSON file containing metric metadata.
# This JSON holds metric names, units, and threshold values.
SCENARIO_METADATA_FILE="./config/scenario-metadata.json"

# Scenario JSON template file for scenario execution configuration.
# Contains placeholder (#THRESHOLDS-PLACEHOLDER#) to be replaced with actual threshold values.
SCENARIO_CONFIG_TEMPLATE_FILE="./templates/scenario-execution.tmpl"

# Final json scenario execution configuration file with thresholds injected.
SCENARIO_CONFIG_OUTPUT_FILE="./config/kpi-scenario-execution.json"

# ─────────────────────────────────────────────────────────────
# Function: create_threshold_block
# Description:
#   Prepares threshold expressions for each metric.
# Parameters:
#   $1 - Path to the metric metadata JSON file. (scenario-metadata.json)
# Returns:
#   JSON object containing thresholds for each metric. (kpi-scenario-execution.json)
# ─────────────────────────────────────────────────────────────
create_thresholds() {
  local scenario_metadata_json_file="$1"

  # Define the jq script to build the thresholds JSON object
  read -r -d '' thresholds_per_metric_as_json << 'EOF'
  # Set threshold expression based on metric type.
  reduce .[] as $metric (
    {};
    .[$metric.metric] = (
      if $metric.metric == "http_req_failed" then
        ["rate <= \($metric.kpiThreshold)"]              # For failure rate metric, threshold is rate <= value
      elif ($metric.unit | test("/second")) then
        ["avg >= \($metric.kpiThreshold)"]               # For per-second metrics, expect average >= threshold
      else
        ["avg <= \($metric.kpiThreshold)"]               # Otherwise, average <= threshold
      end
    )
  )
EOF

  # This returns a JSON object with:
  # - 'thresholds': array of JS declaration strings
  jq -r "$thresholds_per_metric_as_json" "$scenario_metadata_json_file"
}

# ─────────────────────────────────────────────────────────────
# Function: inject_thresholds_into_scenario-execution
# Description:
#   Injects the extracted threshold JSON object into the scenario
#   configuration template by replacing the `.thresholds` named property.
# Parameters:
#   $1 - JSON string of threshold mappings. (scenario-metadata.json)
#   $2 - Template scenario config file path. (scenario-execution.tmpl)
#   $3 - Output scenario config file path (kpi-scenario-execution.json)
# Returns:
#   Writes the updated JSON to output file
# ─────────────────────────────────────────────────────────────
inject_thresholds_into_scenario_execution_config() {
  local thresholds_json="$1"
  local scenario_execution_template_file="$2"
  local scenario_execution_output_file="$3"

  # Use jq to overwrite the `.thresholds` property in the template with the generated thresholds JSON
  jq --argjson thresholds "$thresholds_json" '.thresholds = $thresholds' "$scenario_execution_template_file" | jq '.' > "$scenario_execution_output_file"
}

# ─────────────────────────────────────────────────────────────
# Main script execution starts here
# ─────────────────────────────────────────────────────────────

# Inform user script is starting threshold generation
echo "Generating thresholds from [$SCENARIO_METADATA_FILE]..."

# Calling function to extract threshold JSON object from metric metadata JSON file
scenario_execution_thresholds_json=$(create_thresholds "$SCENARIO_METADATA_FILE")

# Inject the extracted thresholds json block into the scenario config template and write into output file
inject_thresholds_into_scenario_execution_config "$scenario_execution_thresholds_json" "$SCENARIO_CONFIG_TEMPLATE_FILE" "$SCENARIO_CONFIG_OUTPUT_FILE"

# Final confirmation message on successful injection
echo "Threshold block has been injected into [$SCENARIO_CONFIG_OUTPUT_FILE]"