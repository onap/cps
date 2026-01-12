#!/bin/bash
#
# Docker Environment K6 Test Execution Script
# Copyright 2024-2025 OpenInfra Foundation Europe. All rights reserved.
#

pushd "$(dirname "$0")" >/dev/null || {
  echo "❌ Failed to access script directory. Exiting."
  exit 1
}

threshold_failures=0
testProfile=$1
deploymentType="dockerHosts"
summaryFile="${testProfile}Summary.csv"
echo "Running $testProfile performance tests on Docker environment..."

# Run K6 and Capture Output
k6 run ../../ncmp/ncmp-test-runner.js --quiet -e TEST_PROFILE="$testProfile" -e DEPLOYMENT_TYPE="$deploymentType" > "$summaryFile"
k6_exit_code=$?

case $k6_exit_code in
  0) echo "✅ K6 executed successfully for profile: [$testProfile]" ;;
  99) echo "⚠️  K6 thresholds failed (exit code 99)" ;;
  *) echo "❌ K6 execution error (exit code $k6_exit_code)";;
esac

# Add result column processing (same as original)
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

  local newFails
  newFails=$(grep -c '❌' "$summaryFile")
  threshold_failures=$(( threshold_failures + newFails ))
}

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

# Final summary
if [[ "$testProfile" == "kpi" ]]; then
  if (( threshold_failures > 0 )); then
    echo "❌ Summary: [$threshold_failures] test(s) failed FS requirements."
    exit $threshold_failures
  else
    echo "✅ All tests passed FS requirements."
  fi
else
  echo "🔍 Skipping KPI evaluation for profile [$testProfile]"
  echo "📌 Please use Grafana dashboards to investigate performance"
  exit 0
fi