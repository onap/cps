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
number_of_failures=0
testProfile=$1
summaryFile="${testProfile}Summary.csv"
echo "Running $testProfile performance tests..."

# ─────────────────────────────────────────────────────────────
# Run K6 and Capture Output
# ─────────────────────────────────────────────────────────────
k6 run ncmp-test-runner.js --quiet -e TEST_PROFILE="$testProfile"  > "$summaryFile" || ((number_of_failures++))

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
  column -t -s, "$summaryFile"
  echo

  # Clean up
  rm -f "$summaryFile"

else
  echo "Error: Failed to generate $summaryFile" >&2
  ((number_of_failures++))
fi

popd >/dev/null || exit 1

echo "NCMP TEST FAILURES: $number_of_failures"
exit $number_of_failures