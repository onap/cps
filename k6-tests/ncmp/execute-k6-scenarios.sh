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
deploymentType=$2
summaryFile="${testProfile}Summary.csv"
echo "Running $testProfile performance tests..."

# ══════════════════════════════════════════════════════════════
# Run K6 Performance Tests
# ══════════════════════════════════════════════════════════════
k6 run "./ncmp-test-runner.js" --quiet -e TEST_PROFILE="$testProfile" -e DEPLOYMENT_TYPE="$deploymentType" > "$summaryFile"
k6_exit_code=$?

# ══════════════════════════════════════════════════════════════
# Kafka Message Verification (Test #10)
# ══════════════════════════════════════════════════════════════
BROKER="localhost:30093"
SOURCE_TOPIC="dmi-cm-events"
TARGET_TOPIC="cm-events"
PARTITION="0"
TIMEOUT=60
CHECK_INTERVAL=5

# Calculate expected messages from source topic
latest_source=$(kafkacat -Q -b $BROKER -t $SOURCE_TOPIC:$PARTITION:-1 | awk '{print $NF}')
earliest_source=$(kafkacat -Q -b $BROKER -t $SOURCE_TOPIC:$PARTITION:-2 | awk '{print $NF}')
expected_messages=$((latest_source - earliest_source))
target_threshold=$((expected_messages * 98 / 100))

# Poll target topic until threshold met or timeout
elapsed=0
while [ $elapsed -lt $TIMEOUT ]; do
    latest=$(kafkacat -Q -b $BROKER -t $TARGET_TOPIC:$PARTITION:-1 | awk '{print $NF}')
    earliest=$(kafkacat -Q -b $BROKER -t $TARGET_TOPIC:$PARTITION:-2 | awk '{print $NF}')
    message_count=$((latest - earliest))
    
    if [ "$message_count" -ge "$target_threshold" ]; then
        break
    fi
    
    if [ $((elapsed + CHECK_INTERVAL)) -ge $TIMEOUT ]; then
        break
    fi
    
    sleep $CHECK_INTERVAL
    elapsed=$((elapsed + CHECK_INTERVAL))
done

# Calculate percentage of messages collected
if [ "$expected_messages" -gt 0 ]; then
    actual_percentage=$((message_count * 100 / expected_messages))
else
    actual_percentage=0
fi

# Append Kafka verification result to summary with percentages
# Format: TestNumber,TestName,Unit,FSRequirement,ExpectedValue,ActualValue
echo "12,Kafka Message Verification,%,98,100,$actual_percentage" >> "$summaryFile"

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
# Adds ✅/❌ based on pass/fail criteria:
#   • Throughput tests (0,1,2,7): PASS if Actual ≥ Requirement
#   • Duration tests: PASS if Actual ≤ Requirement
#   • Kafka verification (12): PASS if Actual ≥ 98% of Requirement
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
        isKafkaVerification = (testNumber=="12")

        if (isKafkaVerification)
            pass = (actual >= fsRequirement)
        else if (actual == 0 && testNumber != "0")
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
    echo "##            K 6     K P I       P E R F O R M A N C E   T E S T   R E S U L T S                  ##"
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
