#!/bin/bash
#
# Copyright 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

# Default test profile is kpi.
testProfile=${1:-kpi}

# The default deployment type is dockerCompose
deploymentType=${2:-dockerHosts}

# Cleanup handler: capture exit status, run teardown,
# and restore directory, report failures, and exit with original code.
on_exit() {
  rc=$?
  popd
  echo "TEST FAILURES: $rc"
  exit $rc
}

# Call on_exit, on script exit (EXIT) or when interrupted (SIGINT, SIGTERM, SIGQUIT) to perform cleanup
trap on_exit EXIT SIGINT SIGTERM SIGQUIT

pushd "$(dirname "$0")" || exit 1

# Install needed dependencies for any deployment type
source ./install-deps.sh "$deploymentType"

# Handle deployment type specific setup
if [[ "$deploymentType" == "dockerHosts" ]]; then
    echo "Test profile: $testProfile, and deployment type: $deploymentType provided for docker-compose cluster"

    # Run setup for docker-compose environment
    ./setup.sh "$testProfile"

elif [[ "$deploymentType" == "k8sHosts" ]]; then
    echo "Test profile: $testProfile, and deployment type: $deploymentType provided for k8s cluster"

    # Set default values for local development if not provided by Jenkins
    IMAGE_TAG="${IMAGE_TAG:-latest}"
    DMI_STUB_VERSION="${DMI_STUB_VERSION:-1.8.1-SNAPSHOT}"
    POLICY_EXECUTOR_STUB_VERSION="${POLICY_EXECUTOR_STUB_VERSION:-latest}"
    IMAGE_PULL_POLICY="${IMAGE_PULL_POLICY:-IfNotPresent}"

    # Display image configuration for verification
    cat << EOF
==========================================
IMAGE CONFIGURATION FOR K6 TESTS:
==========================================
CPS Image Tag:                ${IMAGE_TAG}
DMI Stub Version:             ${DMI_STUB_VERSION}
Policy Executor Stub Version: ${POLICY_EXECUTOR_STUB_VERSION}
Image Pull Policy:            ${IMAGE_PULL_POLICY}
==========================================
EOF

    # Deploy cps charts for k8s
    helm install cps ../cps-charts \
      --set cps.image.tag="${IMAGE_TAG}" \
      --set cps.image.pullPolicy="${IMAGE_PULL_POLICY}" \
      --set dmiStub.image.tag="${DMI_STUB_VERSION}" \
      --set policyExecutorStub.image.tag="${POLICY_EXECUTOR_STUB_VERSION}" \
      --set policyExecutorStub.image.pullPolicy="${IMAGE_PULL_POLICY}"

    # Wait for pods and services until becomes ready
    echo "Waiting for cps and ncmp pods to be ready..."
    kubectl wait --for=condition=available deploy -l app=ncmp --timeout=300s

    # Verify actual images running in pods
    cat << EOF
==========================================
VERIFYING ACTUAL IMAGES IN RUNNING PODS:
==========================================
EOF
    kubectl get pods -l app=ncmp -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[*].image}{"\n"}{end}' | while read -r pod_name images; do
      echo "Pod: $pod_name"
      echo "  Images: $images"
    done
    echo "=========================================="

else
    echo "Error: Unsupported deployment type: $deploymentType"
    echo "Supported deployment types: dockerHosts, k8sHosts"
    exit 1
fi

# Run k6 test suite for both deployment types
./ncmp/execute-k6-scenarios.sh "$testProfile" "$deploymentType"
NCMP_RESULT=$?

# Note that the final steps are done in on_exit function after this exit!
exit $NCMP_RESULT
