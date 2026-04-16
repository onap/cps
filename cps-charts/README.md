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
# Running CPS locally using Kubernetes

## CPS and NCMP Helm Chart
This Helm chart deploys the **CPS** and **NCMP** ecosystem along with PostgreSQL, Kafka, Zookeeper, and dual DMI Stub services.
---
#### Prerequisites for Windows
1. Docker Desktop
2. Enable Kubernetes in Docker Desktop (Settings, Kubernetes, kubeadm). Known issue: it may hang on "starting kubernetes" for a few minutes. Resolution: click "Reset Cluster" then it starts.
3. Helm 3.x  (see [installing helm on windows](https://helm.sh/docs/intro/install/). Recommended approach: install Helm with winget)


#### Prerequisites for Linux
1. Kubernetes cluster (tested on K8s 1.24+)
   - Minikube (see [minikube start](https://minikube.sigs.k8s.io/docs/start))
2. Helm 3.x (see [installing helm on linux](https://helm.sh/docs/intro/install/))

---
## Installation
To install the chart into the **default namespace**:
(assuming you are in the root directory of this repository)
```bash
helm install cps ./cps-charts
```

To install with policy executor checking enabled
```bash
helm install cps ./cps-charts --set cps.env.POLICY_SERVICE_ENABLED=true
```
---

## check deployment status
You can verify the deployment using:
```bash
kubectl get pods
```
Wait until all pods show `Running` and `READY` before proceeding.

---

## Accessing CPS

> **Note:** Execute all URLS below using your preferred tool in your environment e.g. curl, a web browser or Postman.
FYI CPS Repo includes a comprehensive Postman collection for testing CPS and NCMP.

The CPS/NCMP service is exposed on **NodePort 30080**.

Sample request — get all dataspaces:
```bash
http://localhost:30080/cps/api/v2/admin/dataspaces
```
Expected output:
```json
[
  { "name": "NCMP-Admin" },
  { "name": "NFP-Operational" },
  { "name": "CPS-Admin" }
]
```
> **Note (Linux/Minikube users):** Enable port forwarding first (keep running in a separate terminal)::
> ```bash
> kubectl port-forward service/cps-ncmp-service 30080:8080
> ```
---

## Liveness and Readiness Checks
CPS exposes Spring Boot Actuator health endpoints. You can check them from **outside the pod** using port forwarding, or from **within the pod** using `wget`.

**From outside (recommended)** — ensure port forwarding is running, then from your local terminal:
```bash
http://localhost:30080/actuator/health/liveness
http://localhost:30080/actuator/health/readiness
``

Expected response when healthy:
```json
{"status":"UP"}
```

**From within the pod** — useful when port forwarding is not running. First find a running CPS pod:
```bash
kubectl get pods -l component=cps
```
Then exec into it and check the health endpoints:
> **Note:** `curl` is not available inside the CPS container image. Using `wget -qO-` instead.

```bash
kubectl exec -it <cps-pod-name> -- wget -qO- http://localhost:8080/actuator/health/liveness
kubectl exec -it <cps-pod-name> -- wget -qO- http://localhost:8080/actuator/health/readiness
```

You can also check the DMI Stub instances the same way:
```bash
kubectl get pods -l component=dmi-stub
kubectl exec -it <dmi-stub-pod-name> -- wget -qO- http://localhost:8092/actuator/health/liveness
kubectl exec -it <dmi-stub-pod-name> -- wget -qO- http://localhost:8092/actuator/health/readiness
```

---

## Viewing and Filtering Logs

View logs for a specific pod:
```bash
kubectl logs <pod-name>
```

Follow (tail) logs in real time:
```bash
kubectl logs -f <pod-name>
```

### Filter Logs Using `grep` on **Linux**:

```bash
kubectl logs <pod-name> --tail=-1 | grep ERROR
kubectl logs <pod-name> --tail=-1 | grep -i "exception\|error\|warn"
```

View logs from **all CPS pods at once** using a label selector:

```bash
kubectl logs -l component=cps --prefix --tail=-1 | grep ERROR
```

View previous (crashed/restarted) container logs:
```bash
kubectl logs <pod-name> --previous
```
---

### Filter Logs Using a Docker Desktop Extension on **Windows**

Install an extension like [Maltus Docker Log Viewer](https://hub.docker.com/r/maltus/docker-logs-viewer)

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
