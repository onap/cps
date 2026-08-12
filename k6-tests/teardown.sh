#!/bin/bash
#
# Copyright 2024-2026 OpenInfra Foundation Europe.
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

# The default test profile is kpi
testProfile=${1:-kpi}

# Use test profile as namespace for k8s deployments
K8S_NAMESPACE="${K8S_NAMESPACE:-$testProfile}"

echo "=========================================="
echo "TEARDOWN FOR PROFILE: $testProfile"
echo "NAMESPACE:            $K8S_NAMESPACE"
echo "=========================================="

# Function to create and store logs
make_logs() {
  echo "Creating logs for namespace: $K8S_NAMESPACE ..."
  chmod +x archive-logs.sh
  ./archive-logs.sh
}

# Force-remove a stuck namespace by clearing its finalizers
force_remove_namespace() {
  echo "Forcing removal of namespace '$K8S_NAMESPACE' by clearing finalizers..."
  kubectl get namespace "$K8S_NAMESPACE" -o json 2>/dev/null | \
    sed 's/"finalizers": \[[^]]*\]/"finalizers": []/' | \
    kubectl replace --raw "/api/v1/namespaces/$K8S_NAMESPACE/finalize" -f - 2>/dev/null || true
}

# Function to teardown kubernetes deployment
teardown_k8s_deployment() {

  # Check if namespace exists at all
  if ! kubectl get namespace "$K8S_NAMESPACE" &>/dev/null; then
    echo "Namespace '$K8S_NAMESPACE' does not exist. Nothing to tear down."
    return 0
  fi

  echo "================================== k8s info [namespace: $K8S_NAMESPACE] =========================="
  kubectl get all --namespace "$K8S_NAMESPACE" -l app=ncmp || true

  echo "================================== uninstalling helm release 'cps' [namespace: $K8S_NAMESPACE] =========================="
  helm uninstall cps --namespace "$K8S_NAMESPACE" --no-hooks 2>/dev/null || echo "Helm release 'cps' not found or already removed in namespace '$K8S_NAMESPACE'."

  echo "================================== cleaning up resources [namespace: $K8S_NAMESPACE] =========================="
  kubectl delete pods --all --namespace "$K8S_NAMESPACE" --grace-period=0 --force 2>/dev/null || true
  kubectl delete all --all --namespace "$K8S_NAMESPACE" --grace-period=0 --force 2>/dev/null || true
  kubectl delete configmaps,secrets,serviceaccounts,roles,rolebindings --all --namespace "$K8S_NAMESPACE" 2>/dev/null || true

  echo "================================== deleting namespace: $K8S_NAMESPACE =========================="
  kubectl delete namespace "$K8S_NAMESPACE" --ignore-not-found --wait=false

  # Force-remove finalizers immediately to prevent the namespace from getting stuck
  sleep 5
  if kubectl get namespace "$K8S_NAMESPACE" &>/dev/null; then
    force_remove_namespace
  fi

  echo "Namespace '$K8S_NAMESPACE' deletion initiated."
}

# Main logic: archive logs and teardown
make_logs
teardown_k8s_deployment

echo "=========================================="
echo "TEARDOWN COMPLETE FOR: $testProfile (namespace: $K8S_NAMESPACE)"
echo "=========================================="
