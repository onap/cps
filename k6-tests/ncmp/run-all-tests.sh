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
3-passthrough-read.js \
4-id-search-no-filter.js \
5-search-no-filter.js \
6-id-search-public-property.js \
7-search-public-property.js \
8-id-search-module.js \
9-search-module.js \
10-mixed-load-test.js \
11-delete-cmhandles.js \
)

pushd "$(dirname "$0")" || exit 1

number_of_failures=0
for test_script in "${ALL_TEST_SCRIPTS[@]}"; do
  echo "k6 run $test_script"
  k6 --quiet run "$test_script" || ((number_of_failures++))
  echo
done

popd || exit 1

echo "NCMP TEST FAILURES: $number_of_failures"
exit $number_of_failures
