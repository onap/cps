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

# ─────────────────────────────────────────────────────────────
# 📁 Navigate to Script Directory
# ─────────────────────────────────────────────────────────────
pushd "$(dirname "$0")" >/dev/null || {
  echo "❌ Failed to access script directory. Exiting."
  exit 1
}

# ─────────────────────────────────────────────────────────────
# 📌 Global Variables
# ─────────────────────────────────────────────────────────────
number_of_failures=0
testProfile=$1
summaryFile="${testProfile}Summary.csv"
KPI_METADATA_FILE="./config/test-kpi-metadata.json"
KPI_CONFIG_FILE="./config/kpi.json"
NCMP_RUNNER_FILE="ncmp-test-runner.js"

echo
echo "📢 Running NCMP K6 performance test for profile: [$testProfile]"
echo

# ───────────────────────────────────────────────────────────────────────────
# 1️⃣ Generate trend declarations and (conditionally) thresholds from metadata
# ───────────────────────────────────────────────────────────────────────────
echo "🔧 Generating trend declarations from [$KPI_METADATA_FILE]..."

# Pass true or false depending on testProfile
kpiTestProfile=false
if [[ "$testProfile" == "kpi" ]]; then
  kpiTestProfile=true
fi

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
' "$NCMP_RUNNER_FILE" > "$TMP_FILE"
mv "$TMP_FILE" "$NCMP_RUNNER_FILE"
echo "✅ Trend declarations inserted into [$NCMP_RUNNER_FILE]"

# If profile is KPI, generate threshold config too
if [ "$kpiTestProfile" = true ]; then
  echo "📌 Writing thresholds to [$KPI_CONFIG_FILE]..."
  # Update thresholds in KPI config
  # Extract thresholds
  thresholds_json=$(echo "$jq_output" | jq '.thresholds')
  TMP_FILE=$(mktemp)
  cp "$KPI_CONFIG_FILE" "$TMP_FILE"
  jq --argjson thresholds "$thresholds_json" '.thresholds = $thresholds' "$TMP_FILE" | jq '.' > "$KPI_CONFIG_FILE"
  rm -f "$TMP_FILE"
  echo "✅ Threshold block has been injected into [$KPI_CONFIG_FILE]"
  echo
fi

# ─────────────────────────────────────────────────────────────
# 2️⃣ Run K6 and Capture Output
# ─────────────────────────────────────────────────────────────
k6 run ncmp-test-runner.js -e TEST_PROFILE="$testProfile" > "$summaryFile"
k6_exit_code=$?

case $k6_exit_code in
  0) echo "✅ K6 executed successfully for profile: [$testProfile]." ;;
  99) echo "⚠️  K6 thresholds failed (exit code 99). Processing failures..." ;;
  *) echo "❌ K6 execution error (exit code $k6_exit_code)."; number_of_failures=$((number_of_failures + 1)) ;;
esac

if [ "$kpiTestProfile" = true ]; then
# ─────────────────────────────────────────────────────────────
# 3️⃣ Extract and Filter Summary Data
# ─────────────────────────────────────────────────────────────
if [ -f "$summaryFile" ]; then
  echo "🔍 Extracting expected test names from metadata..."
  expected_tests=()
  while IFS= read -r test_name; do
    [[ -n "$test_name" ]] && expected_tests+=("$test_name")
  done < <(jq -r '.[].name' "$KPI_METADATA_FILE")

  if [[ ${#expected_tests[@]} -eq 0 ]]; then
    echo "❌ No test names found in metadata. Aborting."
    exit 1
  fi

  filtered_summary=$(mktemp)

  # Extract the CSV header line starting with '#'
  grep -m 1 "^#" "$summaryFile" > "$filtered_summary"

  # Match each expected test name with summary rows
  for test_name in "${expected_tests[@]}"; do
    trimmedTestName="$(echo "$test_name" | xargs)"
    matched_line=$(grep -F "$trimmedTestName" "$summaryFile")
    [[ -n "$matched_line" ]] && echo "$matched_line" >> "$filtered_summary" || echo "⚠️ Result not found for [$trimmedTestName]"
  done

  # Output raw CSV for plotting job
  echo -e "\n📊 -- BEGIN CSV REPORT --"
  cat "$filtered_summary"
  echo -e "📊 -- -- END CSV REPORT --\n"

  # ─────────────────────────────────────────────────────────────
  # 4️⃣ Evaluate FS Thresholds
  # ─────────────────────────────────────────────────────────────

  # Evaluate FS pass/fail thresholds
  annotated_summary=$(mktemp)
  threshold_failures=0

  # Append header with new column "Pass FS"
  head -n 1 "$filtered_summary" | awk '{print $0",Pass FS"}' > "$annotated_summary"
  tail -n +2 "$filtered_summary" > tmp_input

  # Exit early if no valid test results were found in the filtered summary
  if [[ ! -s tmp_input ]]; then
    echo "⚠️ No valid test results found in [$summaryFile]. Skipping FS evaluation."
    echo "❌ Summary: No tests were executed or matched expected names."
    ((number_of_failures++))
    rm -f tmp_input "$summaryFile" "$filtered_summary"
    popd >/dev/null || true
    exit $number_of_failures
  fi

  # Process each test case (skip header and check values) append pass/fail to annotated_summary
  while IFS=, read -r id test_name unit fs_requirement current_expected actual_value; do
    [[ -z "$test_name" ]] && continue

    # Trim whitespace from fs_requirement and actual
    fs_req=$(echo "$fs_requirement" | xargs)
    actual_val=$(echo "$actual_value" | xargs)
    fs_pass_status="✅"

    # Special case: zero actual is valid, assign ✅ without warning
    if [[ "$test_name" == "HTTP request failures for all tests" ]]; then
      if [[ "$actual_val" != "0" && "$actual_val" != "0.000" ]]; then
        fs_condition_met=$(awk -v a="$actual_val" -v r="$fs_req" 'BEGIN { print (a <= r) ? 1 : 0 }')
        [[ "$fs_condition_met" -ne 1 ]] && fs_pass_status="❌" && ((threshold_failures++))
      fi
    else

      # For all other tests: if actual is 0 or 0.000, mark as ❌ failure
      if [[ "$actual_val" == "0" || "$actual_val" == "0.000" ]]; then
        fs_pass_status="❌"
        echo "❌ Error: Actual value for metric '$test_name' is 0. This may indicate an error or missing data."
        ((threshold_failures++))
      else
        if [[ "$unit" == *"millisecond"* || "$unit" == *"rate of failed requests"* ]]; then
          fs_condition_met=$(awk -v a="$actual_val" -v r="$fs_req" 'BEGIN { print (a <= r) ? 1 : 0 }')
        else
          fs_condition_met=$(awk -v a="$actual_val" -v r="$fs_req" 'BEGIN { print (a >= r) ? 1 : 0 }')
        fi
        [[ "$fs_condition_met" -ne 1 ]] && fs_pass_status="❌" && ((threshold_failures++))
      fi
    fi

    echo "$id,$test_name,$unit,$fs_requirement,$current_expected,$actual_value,$fs_pass_status" >> "$annotated_summary"
  done < tmp_input
  rm -f tmp_input

  # ─────────────────────────────────────────────────────────────
  # 5️⃣ Print Human-Readable Report
  # ─────────────────────────────────────────────────────────────
  table_preview=$(column -t -s, "$annotated_summary")

  # Compute table width safely
  table_width=$(echo "$table_preview" | awk '{ if (length > max) max = length } END { print max }')

  # Fallback if table_width is empty or not a valid number
  if ! [[ "$table_width" =~ ^[0-9]+$ ]]; then
    table_width=80
  fi

  # Now safely create the border line
  border_line=$(printf '#%.0s' $(seq 1 "$table_width"))

  format_title_spaced() {
    local input="$1"
    local result=""
    for word in $input; do
      for ((i=0; i<${#word}; i++)); do
        result+="${word:$i:1} "
      done
      result+="  "
    done
    echo "$result"
  }

  # Pad title string to center it in the table width
  raw_title="K6 ${kpiTestProfile^^} PERFORMANCE TEST RESULTS"

  # Dynamically center title within the line
  title="$(format_title_spaced "$raw_title")"
  title_line=$(printf "## %*s %*s##" \
    $(( (table_width - 6 + ${#title}) / 2 )) "$title" \
    $(( (table_width - 6 - ${#title}) / 2 )) "")

  # Print header
  echo "$border_line"
  echo "$title_line"
  echo "$border_line"

  # Then print the table
  echo "$table_preview"

  # Print closing border after the table
  echo "$border_line"
  echo

  # 🎯 Final FS Summary of threshold result
  if (( threshold_failures > 0 )); then
    echo "❌ Summary: [$threshold_failures] test(s) failed FS requirements."
    echo
      echo "⚠️ Performance tests completed with issues for profile: [$testProfile]."
      echo "❗ Number of failures or threshold breaches: $threshold_failures"
      echo "Please check the summary reports and logs above for details."
      echo "Investigate any failing metrics and consider re-running the tests after fixes."
      echo
    ((number_of_failures++))
  else
    echo "✅ All tests passed FS requirements."
    echo "✅ No threshold violations or execution errors detected."
    echo "You can review detailed results in the generated summary."
  fi

  # Cleanup temp files related to reporting
  rm -f "$filtered_summary" "$annotated_summary"

else  # no summary file
  echo "❌ Error: Summary file [$summaryFile] was not generated. Possible K6 failure."
  ((number_of_failures++))
fi
else
# ─────────────────────────────────────────────────────────────
# Non-KPI Profile: Investigative Guidance
# ─────────────────────────────────────────────────────────────
    echo
    echo "🔍 Skipping KPI evaluation for profile [$testProfile]"
    echo
    echo "📌 Please use the following tools and dashboards to investigate performance:"
    echo
    echo "  • 📈 Grafana Dashboards:"
    echo "     - Nordix Prometheus/Grafana can visualize memory and latency trends."
    echo "     - Especially useful for endurance/stability runs."
    echo "     - 🌐 https://monitoring.nordix.org/login"
    echo "     - Dashboards include:"
    echo "         ▪ Check CM Handle operation latency trends over time."
    echo "         ▪ Focus on 'Pass-through Read/Write', 'Search', or 'Kafka Batch' graphs."
    echo "         ▪ Memory usage patterns (cps/ncmp containers)"
    echo "         ▪ Kafka lag and consumer trends (if applicable)"
    echo
    echo "  • 📊 GnuPlot:"
    echo "     - Optional local alternative to visualize memory trends."
    echo "     - Requires exporting memory data (CSV/JSON) and plotting manually."
    echo
    echo "  • 🔎 Important Metrics to Watch:"
    echo "     - HTTP duration (avg, p95, max)"
    echo "     - VU concurrency and iteration rates"
    echo "     - Error rates and failed checks"
    echo "     - Container memory growth over time (especially in endurance tests)"
    echo
    echo "  • 📄 Logs and Summary:"
    echo "     - Check '$summaryFile' for raw execution summary."
    echo "     - Inspect logs for timeout/retries/exception patterns."
    echo
    echo "ℹ️  Reminder: For KPI validation with FS thresholds, re-run with profile: 'kpi'"
    echo
fi

# Cleanup global temp file
rm -f "$summaryFile"

# ─────────────────────────────────────────────────────────────
# 🔚 Final Exit
# ─────────────────────────────────────────────────────────────
popd >/dev/null || true
exit $number_of_failures