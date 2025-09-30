#!/bin/bash
#
# Copyright 2025 OpenInfra Foundation Europe. All rights reserved.
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

# the default test profile is kpi
testProfile=${1:-kpi}

# the default deployment type is k8sHosts
deploymentType=${2:-k8sHosts}

# Cleanup handler:
# 1- capture exit status
# 2- run teardown
# 3- restore directory
# 4- report failures
# 5- exit with original code
on_exit() {
  rc=$?
  chmod +x teardown-k8s.sh
  ./teardown-k8s.sh
  popd
  echo "TEST FAILURES: $rc"
  exit $rc
}

# call on_exit, on script exit (EXIT) or when interrupted (SIGINT, SIGTERM, SIGQUIT) to perform cleanup
trap on_exit EXIT SIGINT SIGTERM SIGQUIT
pushd "$(dirname "$0")" || exit 1

echo "Test profile: $testProfile, and deployment type: $deploymentType provided for k8s cluster"
# deploy cps charts
helm install cps ../../cps-charts
# wait for pods and services until becomes ready
echo "Waiting for cps and ncmp pods to be ready..."
kubectl wait --for=condition=available deploy -l app=cps-and-ncmp --timeout=120s

# run k6 test suite
../ncmp/execute-k6-scenarios.sh "$testProfile" "$deploymentType"
NCMP_RESULT=$?

# Note that the final steps are done in on_exit function after this exit!
exit $NCMP_RESULT