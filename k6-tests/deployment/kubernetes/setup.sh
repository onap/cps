#!/bin/bash
#
# Kubernetes Environment Setup Script
# Copyright 2024-2025 OpenInfra Foundation Europe. All rights reserved.
#

set -e

testProfile=$1

echo "Setting up Kubernetes environment for profile: $testProfile"

# Deploy cps charts for k8s
helm install cps ../../cps-charts

# Wait for pods and services until becomes ready
echo "Waiting for cps and ncmp pods to be ready..."
kubectl wait --for=condition=available deploy -l app=ncmp --timeout=300s

echo "✅ Kubernetes setup complete for test profile: $testProfile"