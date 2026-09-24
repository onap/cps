#!/bin/bash
#
# Copyright 2026 OpenInfra Foundation Europe. All rights reserved.
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

pushd "$(dirname "$0")" >/dev/null || {
  echo "❌ Failed to access script directory. Exiting."
  exit 1
}

testProfile=${1:-functional}
echo "Running ONAP DMI stack $testProfile tests..."

# Forward endpoint overrides to k6 when set, so the caller can retarget the tests
# without editing the config (e.g. the minikube NodePort host). Unset variables
# fall back to the values in config/<profile>.json.
k6EnvArgs=()
for overridableVariable in DMI_BASE_URL NCMP_BASE_URL DMI_PLUGIN_IN_CLUSTER_URL \
                           DMI_USERNAME DMI_PASSWORD CM_HANDLE; do
  if [[ -n "${!overridableVariable:-}" ]]; then
    k6EnvArgs+=(-e "${overridableVariable}=${!overridableVariable}")
  fi
done

k6 run "./onap-dmi-stack-test-runner.js" --quiet --no-usage-report --address "" \
  -e TEST_PROFILE="$testProfile" "${k6EnvArgs[@]}"
k6_exit_code=$?

case $k6_exit_code in
  0) echo "✅ ONAP DMI stack tests passed for profile: [$testProfile]" ;;
  99) echo "❌ ONAP DMI stack test checks failed (exit code 99)" ;;
  *) echo "❌ K6 execution error (exit code $k6_exit_code)" ;;
esac

popd >/dev/null || exit 1

exit $k6_exit_code
