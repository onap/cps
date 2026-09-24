#!/bin/bash
#
# Copyright 2026 Deutsche Telekom AG
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

# ══════════════════════════════════════════════════════════════
# Navigate to Script Directory
# ══════════════════════════════════════════════════════════════
pushd "$(dirname "$0")" >/dev/null || {
  echo "❌ Failed to access script directory. Exiting."
  exit 1
}

# ══════════════════════════════════════════════════════════════
# Global Variables
# ══════════════════════════════════════════════════════════════
threshold_failures=0
testProfile=$1
summaryFile="${testProfile}Summary.csv"
echo "Running cps-core $testProfile performance tests..."

# ══════════════════════════════════════════════════════════════
# Run K6 Performance Tests
# ══════════════════════════════════════════════════════════════
k6 run "./cps-core-test-runner.js" --quiet --no-usage-report --address "" -e TEST_PROFILE="$testProfile" > "$summaryFile"
k6_exit_code=$?

# ══════════════════════════════════════════════════════════════
# K6 Exit Code Summary
# ══════════════════════════════════════════════════════════════
case $k6_exit_code in
  0) echo "✅ K6 executed successfully for profile: [$testProfile]" ;;
  99) echo "⚠️  K6 thresholds failed (exit code 99)" ;;
  *) echo "❌ K6 execution error (exit code $k6_exit_code)";;
esac

# ══════════════════════════════════════════════════════════════
# Add Result Column to Summary
# ══════════════════════════════════════════════════════════════
# Adds a ✅/❌ Result column based on pass/fail criteria:
#   • Throughput tests (0,1,2,7): PASS if Actual ≥ Requirement
#   • Duration tests: PASS if Actual ≤ Requirement
addResultColumn() {
  local summaryFile="$1"
  local tmp
  tmp=$(mktemp)

awk -F',' -v OFS=',' '
    function initRowVariables() {
        titleRow      = $0
        testNumber    = $1
        fsRequirement = $4 + 0
        actual        = $6 + 0
    }

    NR == 1 { # block for header
        titleRow      = $0
        print titleRow, "Result"
        next
    }

    { # block for every data row
        initRowVariables()
        isThroughput = (testNumber=="0" || testNumber=="1" || \
                        testNumber=="2" || testNumber=="7")

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

# ══════════════════════════════════════════════════════════════
# Generate and Display Results
# ══════════════════════════════════════════════════════════════
if [ -f "$summaryFile" ]; then
  echo "-- BEGIN CSV REPORT"
  cat "$summaryFile"
  echo "-- END CSV REPORT"
  echo

  echo "####################################################################################################"
  if [ "$testProfile" = "kpi" ]; then
    echo "##          K 6     C P S - C O R E   K P I   P E R F O R M A N C E   T E S T   R E S U L T S      ##"
  fi
  echo "####################################################################################################"
  addResultColumn "$summaryFile"
  column -t -s, "$summaryFile"
  echo

  rm -f "$summaryFile"
else
  echo "Error: Failed to generate $summaryFile" >&2
fi

popd >/dev/null || exit 1

# ══════════════════════════════════════════════════════════════
# Final Summary and Exit
# ══════════════════════════════════════════════════════════════
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
  echo
  echo "🔍 Skipping KPI evaluation for profile [$testProfile]"
  echo
  echo "📌 Please use the following tools and dashboards to investigate performance:"
  echo
  echo "  • 📈 Grafana Dashboards:"
  echo "     - Use your Prometheus/Grafana instance to visualize memory and latency trends."
  echo "     - Especially useful for endurance/stability runs."
  echo "     - Recommended dashboards include:"
  echo "         ▪ CPS-Core API latency trends over time."
  echo "         ▪ Memory usage patterns (cps-core container)"
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
