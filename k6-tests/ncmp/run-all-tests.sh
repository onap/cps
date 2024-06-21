#!/bin/bash
#
# Copyright 2024 Nordix Foundation.
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

ALL_TEST_SCRIPTS=( \
1-create-cmhandles.js \
2-wait-for-cmhandles-to-be-ready.js \
10-mixed-load-test.js \
11-delete-cmhandles.js \
)

pushd "$(dirname "$0")" || exit 1

printf "Test Case\tCondition\tLimit\tActual\tResult\n" > summary.log

number_of_failures=0
for test_script in "${ALL_TEST_SCRIPTS[@]}"; do
  echo "k6 run $test_script"
  k6 --quiet run -e K6_MODULE_NAME="$test_script" "$test_script" >> summary.log || ((number_of_failures++))
done

echo '##############################################################################################################################'
echo '##                             K 6   P E R F O R M A N C E   T E S T   R E S U L T S                                        ##'
echo '##############################################################################################################################'
awk -F$'\t' '{printf "%-40s%-50s%-20s%-10s%-6s\n", $1, $2, $3, $4, $5}' summary.log

popd || exit 1

echo "NCMP TEST FAILURES: $number_of_failures"
exit $number_of_failures
