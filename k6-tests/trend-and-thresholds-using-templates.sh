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

# ─────────────────────────────────────────────────────────────
# 📌 Global Variables
# ─────────────────────────────────────────────────────────────
KPI_METADATA_FILE="./config/test-kpi-metadata.json"
KPI_CONFIG_FILE_TEMPLATE="./templates/kpi.tmpl"
KPI_CONFIG_FILE="./config/kpi.json"
SCENARIOS_CONFIG_SCRIPT_TEMPLATE="./templates/scenarios-config.tmpl"
SCENARIOS_CONFIG_SCRIPT="scenarios-config.js"

# ───────────────────────────────────────────────────────────────────────────
# 1️⃣ Generate trend declarations and (conditionally) thresholds from metadata
# ───────────────────────────────────────────────────────────────────────────
echo "🔧 Generating trend declarations from [$KPI_METADATA_FILE]..."

read -r -d '' jq_script << 'EOF'
def toCamelCase:
  split("_") as $parts |
  ($parts[0]) + ($parts[1:] | map((.[0:1] | ascii_upcase) + .[1:]) | join(""));

reduce .[] as $item (
  { trends: [], thresholds: {} };
  if ($item.unit == "milliseconds") or ($item.unit | test("/second")) then
    .trends += [
      "export let \($item.metric | toCamelCase)Trend = new Trend('\($item.metric)', \($item.unit == "milliseconds"));"
    ]
  else
    .
  end
  |
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

# Execute jq script
jq_output=$(jq -r "$jq_script" "$KPI_METADATA_FILE")

# Extract trends
trend_declarations=$(echo "$jq_output" | jq -r '.trends[]')

# Replace placeholder in runner with generated trends
TMP_FILE=$(mktemp)
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
' "$SCENARIOS_CONFIG_SCRIPT_TEMPLATE" > "$TMP_FILE"
mv "$TMP_FILE" "$SCENARIOS_CONFIG_SCRIPT"
echo "✅ Trend declarations inserted into [$SCENARIOS_CONFIG_SCRIPT]"

# If profile is KPI, generate threshold config too
if [[ "$testProfile" == "kpi" ]]; then
  echo "📌 Writing thresholds to [$KPI_CONFIG_FILE]..."
  # Update thresholds in KPI config
  # Extract thresholds
  thresholds_json=$(echo "$jq_output" | jq '.thresholds')
  TMP_FILE=$(mktemp)
  cp "$KPI_CONFIG_FILE_TEMPLATE" "$TMP_FILE"
  jq --argjson thresholds "$thresholds_json" '.thresholds = $thresholds' "$TMP_FILE" | jq '.' > "$KPI_CONFIG_FILE"
  rm -f "$TMP_FILE"
  echo "✅ Threshold block has been injected into [$KPI_CONFIG_FILE]"
  echo
fi