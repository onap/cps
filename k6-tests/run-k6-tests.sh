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

set -o errexit  # Exit on most errors
set -o nounset  # Disallow expansion of unset variables
set -o pipefail # Use last non-zero exit code in a pipeline
#set -o xtrace   # Uncomment for debugging

# default is empty string, which means performance tests
testProfile=${1:-kpi}

on_exit() {
  rc=$?
  ./teardown.sh "$testProfile"
  popd
  echo "TEST FAILURES: $rc"
  exit $rc
}
trap on_exit EXIT

pushd "$(dirname "$0")" || exit 1

# Install needed dependencies.
source install-deps.sh

echo "Test profile provided: $testProfile"

# Run k6 test suite.
./setup.sh "$testProfile"
./ncmp/run-all-tests.sh "$testProfile"
NCMP_RESULT=$?

# Note that the final steps are done in on_exit function after this exit!
exit $NCMP_RESULT