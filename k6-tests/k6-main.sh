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

# Use test profile as namespace to allow parallel runs (e.g., kpi and endurance)
export K8S_NAMESPACE="${testProfile}"

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

# Install needed dependencies
source ./install-deps.sh

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
Namespace:                    ${K8S_NAMESPACE}
==========================================
EOF

# Wait for namespace to be fully gone if it's still terminating from a previous run
if kubectl get namespace "$K8S_NAMESPACE" &>/dev/null; then
    namespace_phase=$(kubectl get namespace "$K8S_NAMESPACE" -o jsonpath='{.status.phase}' 2>/dev/null)
    if [ "$namespace_phase" = "Terminating" ]; then
        echo "Namespace '$K8S_NAMESPACE' is still terminating from a previous run. Waiting..."
        local_timeout_sec=120
        local_elapsed_sec=0
        while kubectl get namespace "$K8S_NAMESPACE" &>/dev/null; do
            if [ $local_elapsed_sec -ge $local_timeout_sec ]; then
                echo "ERROR: Namespace '$K8S_NAMESPACE' stuck in Terminating state after ${local_timeout_sec}s."
                echo "Attempting to force-remove finalizers..."
                kubectl get namespace "$K8S_NAMESPACE" -o json 2>/dev/null | \
                  sed 's/"finalizers": \[[^]]*\]/"finalizers": []/' | \
                  kubectl replace --raw "/api/v1/namespaces/$K8S_NAMESPACE/finalize" -f - 2>/dev/null || true
                sleep 5
                if kubectl get namespace "$K8S_NAMESPACE" &>/dev/null; then
                    echo "FATAL: Cannot remove namespace '$K8S_NAMESPACE'. Manual intervention required."
                    exit 1
                fi
                break
            fi
            sleep 5
            local_elapsed_sec=$((local_elapsed_sec + 5))
            echo "  ... waiting for termination (${local_elapsed_sec}s/${local_timeout_sec}s)"
        done
        echo "Namespace '$K8S_NAMESPACE' is now gone."
    fi
fi

# Create namespace for this test profile
kubectl create namespace "$K8S_NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

# Determine values override file for the test profile
VALUES_OVERRIDE_FILE="../cps-charts/values-${testProfile}.yaml"
HELM_VALUES_FLAG=""
if [[ -f "$VALUES_OVERRIDE_FILE" ]]; then
    echo "Using values override: $VALUES_OVERRIDE_FILE"
    HELM_VALUES_FLAG="--values $VALUES_OVERRIDE_FILE"
fi

# Deploy cps charts for k8s in profile-specific namespace
helm install cps ../cps-charts \
  --namespace "$K8S_NAMESPACE" \
  $HELM_VALUES_FLAG \
  --set cps.image.tag="${IMAGE_TAG}" \
  --set cps.image.pullPolicy="${IMAGE_PULL_POLICY}" \
  --set dmiStub.image.tag="${DMI_STUB_VERSION}" \
  --set policyExecutorStub.image.tag="${POLICY_EXECUTOR_STUB_VERSION}" \
  --set policyExecutorStub.image.pullPolicy="${IMAGE_PULL_POLICY}"

# Wait for pods and services until becomes ready
echo "Waiting for cps and ncmp pods to be ready..."
kubectl wait --namespace "$K8S_NAMESPACE" --for=condition=available deploy -l app=ncmp --timeout=300s

# Verify actual images running in pods
cat << EOF
==========================================
VERIFYING ACTUAL IMAGES IN RUNNING PODS:
==========================================
EOF
kubectl get pods --namespace "$K8S_NAMESPACE" -l app=ncmp -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[*].image}{"\n"}{end}' | while read -r pod_name images; do
  echo "Pod: $pod_name"
  echo "  Images: $images"
done
echo "=========================================="

# Run k6 test suite
./ncmp/execute-k6-scenarios.sh "$testProfile"
NCMP_RESULT=$?

# Note that the final steps are done in on_exit function after this exit!
exit $NCMP_RESULT
