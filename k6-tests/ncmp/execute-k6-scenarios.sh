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
# Navigate to Script Directory
# ─────────────────────────────────────────────────────────────
pushd "$(dirname "$0")" >/dev/null || {
  echo "❌ Failed to access script directory. Exiting."
  exit 1
}

# ─────────────────────────────────────────────────────────────
# 📌 Global Variables
# ─────────────────────────────────────────────────────────────
threshold_failures=0
testProfile=$1
summaryFile="${testProfile}Summary.csv"
echo "Running $testProfile performance tests..."

# ─────────────────────────────────────────────────────────────
# Run K6 and Capture Output
# '$?' is immediately captures the exit code after k6 finishes,
#   and assign it to k6_exit_code.
# ─────────────────────────────────────────────────────────────
k6 run ncmp-test-runner.js --quiet -e TEST_PROFILE="$testProfile"  > "$summaryFile"
k6_exit_code=$?

case $k6_exit_code in
  0) echo "✅ K6 executed successfully for profile: [$testProfile]" ;;
  99) echo "⚠️  K6 thresholds failed (exit code 99)" ;;
  *) echo "❌ K6 execution error (exit code $k6_exit_code)";;
esac

###############################################################################
# Adds a “Result” column with ✅ / ❌ to the given summary file
#   • Increments global variable `threshold_failures` for each ❌ row
# NR == 1 catches the header only once, appending “Result”
# PASS rules
#   • Throughput Tests #1, #2, #7: PASS when Actual ≥ Fs Requirement
#   • All others (Duration Tests): PASS when Actual ≤ Fs Requirement
###############################################################################
addResultColumn() {
  local summaryFile="$1"
  local tmp
  tmp=$(mktemp)

  awk -F',' -v OFS=',' '
      NR == 1 {                               # ─── header row ───
          titleRow = $0                       # keep full header if you need it later
          print titleRow, "Result"            # append new column: result
          next
      }

      {                                       # ─── data rows ───
          titleRow = $0                       # save the complete current row
          testNumber    = $1
          fsRequirement = $4 + 0
          actual        = $6 + 0
          isThroughput  = (testNumber=="0" || testNumber=="1" || testNumber=="2" || testNumber=="7")

          if (actual == 0 && testNumber != "0")
              pass = 0
          else if (isThroughput)
              pass = (actual >= fsRequirement)
          else
              pass = (actual <= fsRequirement)

          print titleRow, (pass ? "✅" : "❌")
      }
  ' "$summaryFile" > "$tmp"

  mv "$tmp" "$summaryFile"

  # how many failures (❌) occurred?
  local newFails
  newFails=$(grep -c '❌' "$summaryFile")
  threshold_failures=$(( threshold_failures + newFails ))
}

if [ -f "$summaryFile" ]; then

  # Output raw CSV for plotting job
  echo "-- BEGIN CSV REPORT"
  cat "$summaryFile"
  echo "-- END CSV REPORT"
  echo

  # Output human-readable report
  echo "####################################################################################################"
  if [ "$testProfile" = "kpi" ]; then
    echo "##            K 6     K P I       P E R F O R M A N C E   T E S T   R E S U L T S                  ##"
  fi
  echo "####################################################################################################"
  addResultColumn "$summaryFile"
  column -t -s, "$summaryFile"
  echo

  # Clean up
  rm -f "$summaryFile"

else
  echo "Error: Failed to generate $summaryFile" >&2
fi

# Change the directory back where it was
popd >/dev/null || exit 1

# 🎯 Final FS Summary of threshold result and exit if needed
if [[ "$testProfile" == "kpi" ]]; then
  if (( threshold_failures > 0 )); then
    echo "❌ Summary: [$threshold_failures] test(s) failed FS requirements."
    echo
      echo "⚠️ Performance tests completed with issues for profile: [$testProfile]."
      echo "❗ Number of failures or threshold breaches: $threshold_failures"
      echo "Please check the summary reports and logs above for details."
      echo "Investigate any failing metrics and consider re-running the tests after fixes."
      exit $threshold_failures
  else
    echo "✅ All tests passed FS requirements."
    echo "✅ No threshold violations or execution errors detected."
    echo "You can review detailed results in the generated summary."
  fi
else
# ─────────────────────────────────────────────────────────────
# Endurance Profile: Investigative Guidance
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
    echo "  • 📄 Logs:"
    echo "     - Inspect logs for timeout/retries/exception patterns."
    echo
    echo "ℹ️  Reminder: For KPI validation with FS thresholds, re-run with profile: 'kpi'"
    exit 0
fi