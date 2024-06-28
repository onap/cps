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

pushd "$(dirname "$0")" || exit 1

number_of_failures=0
k6 --quiet run ncmp-kpi.js > summary.csv || ((number_of_failures++))

# Output raw CSV for plotting job
echo '-- BEGIN CSV REPORT'
cat summary.csv
echo '-- END CSV REPORT'
echo

# Output human-readable report
echo '####################################################################################################'
echo '##                  K 6   P E R F O R M A N C E   T E S T   R E S U L T S                         ##'
echo '####################################################################################################'
column -t -s, summary.csv
echo

rm -f summary.csv

popd || exit 1

echo "NCMP TEST FAILURES: $number_of_failures"
exit $number_of_failures
