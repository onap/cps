<!--
  ============LICENSE_START=======================================================
   Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
  ================================================================================
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

  SPDX-License-Identifier: Apache-2.0
  ============LICENSE_END=========================================================
-->


# CPS and NCMP Helm Chart
This Helm chart deploys the **CPS** and **NCMP** ecosystem along with PostgreSQL, Kafka, Zookeeper, and dual DMI Stub services.
---
## Prerequisites
- Kubernetes cluster (tested on K8s 1.24+)
- Helm 3.x
- Access to the necessary Docker image registry (e.g., `nexus3.onap.org`)
---
## Installation
To install the chart into the **default namespace**:
```bash
helm install cps ./<chart-directory>
```
Replace <chart-directory> with the path to this Helm chart.

To install with policy executor stub enabled
```bash
helm install cps ./<chart-directory> --set cps.env.POLICY_SERVICE_ENABLED=true
```
---

## check deployment status
You can verify the deployment using:
```bash
kubectl get pods
```
---

# Port forwarding
Required for local access to cluster services
```bash
kubectl port-forward service/cps-ncmp-service 8080:8080
```
---

## Uninstallation
To uninstall the chart and delete all related resources:
```bash
helm uninstall cps
```
---

## Configuration
This chart includes default settings suitable for local development and testing. You can customize values using a custom values.yaml file or by passing --set parameters at install time.
Example:
```bash
helm install cps ./<chart-directory> --set cps.replicas=1
```
---
## Chart Components
This Helm chart deploys the following components:
- postgresql: CPS database
- cps-and-ncmp: CPS and NCMP backend services
- kafka: Kafka message broker
- zookeeper: Zookeeper coordination service for Kafka
- dmi-stub: Stub service for NCMP device interactions
- policy-executor-stub: Stub service for Policy Executor interactions
---