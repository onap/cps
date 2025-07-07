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
# This script is used to generate K6 performance testing 'Trend' declarations file by:
#   1) Extracting trends from metric metadata JSON (scenario-metadata.json)
#   2) Injects them into a javascript template file (scenario-javascript.tmpl)
#   3) Writes a scenario-javascript file (scenario-javascript.json) with trends declarations.
#

# ─────────────────────────────────────────────────────────────
# Global Variables
# ─────────────────────────────────────────────────────────────

# Path to the JSON file that contains metric definitions (name, unit, threshold, etc.)
# This JSON holds metric names, units, and threshold values.
SCENARIO_METADATA_FILE="./config/scenario-metadata.json"

# Path to the JS template file where the placeholder `#METRICS-TRENDS-PLACEHOLDER#` exists.
# This is where the generated trend declarations will be inserted.
SCENARIO_JAVASCRIPT_TEMPLATE_FILE="./templates/scenario-javascript.tmpl"

# Output JavaScript file where the final result (with inserted trend declarations) will be saved.
SCENARIO_JAVASCRIPT_OUTPUT_FILE="scenario-javascript.js"

# ─────────────────────────────────────────────────────────────────────────────────
# Function: generate_trend_declarations
# Description:
#   Converts metrics from the metadata JSON into JavaScript `Trend` declarations.
#   These declarations are used for K6 performance testing reports.
# ─────────────────────────────────────────────────────────────────────────────────

create_trend_declarations() {
  local scenario_metadata_json_file="$1"  # Accept the path to the metric metadata file as input.

  # Read and assign a JQ script to a here-document variable. (trend_declarations)
  # `-r` makes jq output raw strings and `-d ''` lets us use multiline input.
  read -r -d '' trend_declarations << 'EOF'
  # Define a helper function (toCamelCase) that converts metric names from snake_case to camelCase
  # (for JS variable names for example: cm_handles_deleted → cmHandlesDeleted)
  def toCamelCase:
      split("_") as $parts |                                  # Split string by "_"
      ($parts[0]) +                                           # Keep first part as it is
      ($parts[1:] | map((.[0:1] | ascii_upcase) + .[1:])      # Capitalize rest of each word
      | join(""));                                            # Join all parts into one string

  # Loop through each metric item and generate a JavaScript `Trend` declaration if unit matches.
  .[]                                                                       # Iterate through array
  | select((.unit == "milliseconds") or (.unit | test("/second")))          # Select based on valid units
  | "export let \(.metric | toCamelCase)Trend = new Trend('\(.metric)', \(.unit == "milliseconds"));"
  # Output javascript declaration string: `export let abcTrend = new Trend('abc', true/false);`
EOF
  # Execute the jq script on the metadata file to generate the trend declarations
  jq -r "$trend_declarations" "$scenario_metadata_json_file"
}

# ─────────────────────────────────────────────────────────────
# Function: inject_trends_into_js_template
# Description:
#   Replaces the placeholder line `#METRICS-TRENDS-PLACEHOLDER#` in the template
#   file with actual JS trend declarations.
# Parameters:
#   $1 - JSON string of threshold mappings. Trend declaration strings.
#     for example: export let abcTrend = new Trend('abc', true), from scenario-metadata.json)
#   $2 - Template scenario javascript file path. (scenario-javascript.tmpl)
#   $3 - Output scenario script file path (scenario-javascript.js)
# Returns:
#   Writes the updated JSON to output file
# ─────────────────────────────────────────────────────────────
inject_trends_into_javascript_template() {
  local trend_declarations="$1"
  local scenario_javascript_template_file="$2"
  local scenario_javascript_output_file="$3"

  # Use awk to replace the placeholder line with trend declarations
    awk -v trends="$trend_declarations" '                  # Pass trends into awk variable
      {
        if ($0 ~ /#METRICS-TRENDS-PLACEHOLDER#/) {
          print trends                              # Print the trend declarations instead of the placeholder
        } else {
          print $0                                  # Otherwise, print the original line
        }
      }
    ' "$scenario_javascript_template_file" > "$scenario_javascript_output_file"  # Save the transformed content into the output JS file
}

# ─────────────────────────────────────────────────────────────
# Main Execution Starts Here
# ─────────────────────────────────────────────────────────────

# Display log message to inform that generation has started
echo "Generating trend declarations from [$SCENARIO_METADATA_FILE]..."

# Calling trend generation function
scenario_javascript_trend_declarations=$(create_trend_declarations "$SCENARIO_METADATA_FILE")

# Inject the generated trends into the JavaScript template and write it into scenario output file
inject_trends_into_javascript_template "$scenario_javascript_trend_declarations" "$SCENARIO_JAVASCRIPT_TEMPLATE_FILE" "$SCENARIO_JAVASCRIPT_OUTPUT_FILE"

# Final confirmation message to indicate success
echo "Trend declarations inserted into [$SCENARIO_JAVASCRIPT_OUTPUT_FILE]"