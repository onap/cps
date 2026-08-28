<!--
  ============LICENSE_START=======================================================
   Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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
# ONAP DMI Stack — Local Developer Guide

This guide explains how to deploy and use the full **DMI → NCMP → SDNC** integration path with the CPS/NCMP Helm chart on a local Kubernetes cluster.

---

## Overview

The Helm chart includes an optional DMI stack, **disabled by default**:

| Component | Purpose | Default |
|-----------|---------|---------|
| `sdnc-db` | MariaDB — SDNC's backing database (separate from CPS's PostgreSQL) | Disabled |
| `sdnc` | ONAP SDNC (`onap/sdnc-image`) — the config controller DMI talks to | Disabled |
| `pnfsim` | Netconf device simulator (`netconf-pnp-simulator`) — a fake network device | Disabled |
| `sdnc-mount-node` (Job) | Mounts the simulated device into SDNC as a netconf node | Disabled |
| `ncmp-dmi-plugin` | The real DMI plugin service | Disabled |

The `ncmp-dmi-plugin` is distinct from the existing `dmi-stub`, which does not implement SDNC-backed module discovery.
The two are mutually exclusive: enabling this stack excludes `dmi-stub` from the deployment, so NCMP has exactly one
DMI to talk to. This applies regardless of `dmiStub.enabled`.

The stack is defined entirely as native Kubernetes resources. It can be deployed alongside the rest of the
CPS/NCMP stack, and provides the environment the `dmi-integration` tests and K6 tests run against:

- `ncmp-dmi-plugin/csit/tests/dmi-integration/dmi-ncmp.robot`
- `ncmp-dmi-plugin/csit/tests/dmi-integration/dmi-sdnc.robot`

---

## Quick Start

> **Important:** SDNC/Karaf takes 5-10+ minutes to boot, and the mount-node hook waits for it.
> Always pass a generous `--timeout` or Helm will report failure on a stack that would have come up fine.

### Enable the DMI stack

```bash
helm install cps ./cps-charts --set onapDmiStack.enabled=true --timeout 20m
```

### Enable on an existing install

```bash
helm upgrade cps ./cps-charts --set onapDmiStack.enabled=true --timeout 20m
```

### CPS only (no DMI stack)

```bash
helm install cps ./cps-charts
```

The netconf node mount happens automatically via a Helm post-install/post-upgrade hook, so no manual `curl` step is needed.

---

## Accessing the Services

### Windows (Docker Desktop K8s)

NodePorts are accessible directly on localhost:

| Service | URL |
|---------|-----|
| CPS/NCMP | http://localhost:30080 |
| ncmp-dmi-plugin | http://localhost:30097 |
| SDNC | http://localhost:30096 |

SDNC default login: **admin / Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U**

### Linux (Minikube)

NodePorts bind to the minikube container's IP, **not** localhost, so `localhost:30080` gives `ECONNREFUSED`. Either use the minikube IP directly:

```bash
curl "http://$(minikube ip):30080/actuator/health"
```

> **Note (Linux/Minikube users):** Or enable port forwarding first (keep running in separate terminals):
> ```bash
> kubectl port-forward service/cps-ncmp-service 30080:8080
> kubectl port-forward service/cps-ncmp-ncmp-dmi-plugin-service 30097:8080
> kubectl port-forward service/cps-ncmp-sdnc-service 30096:8181
> ```

---

## Verifying the Setup

### Check pods

```bash
kubectl get pods
```

Wait until the DMI stack pods are running:
```
cps-ncmp-sdnc-...              1/1  Running
cps-ncmp-sdnc-db-...           1/1  Running
cps-ncmp-pnfsim-...            1/1  Running
cps-ncmp-ncmp-dmi-plugin-...   1/1  Running
```

`cps-ncmp-sdnc-mount-node-...` shows `0/1 Completed`. It is a run-once Helm hook Job, so it is not part of the
release manifest and `helm uninstall` does not remove it. It stays in the cluster until the next install of the
stack replaces it, or you delete it yourself:

```bash
kubectl delete job cps-ncmp-sdnc-mount-node
```

### Watch SDNC boot

```bash
kubectl logs -l component=sdnc -f
```

Once healthy, the simulated device connects:
```
NetconfDevice ... RemoteDeviceId[name=ietfYang-PNFDemo, ...]: Netconf connector initialized successfully
```

### Check the DMI plugin is healthy

```bash
curl "http://$(minikube ip):30097/actuator/health"
```

### Check SDNC has the node mounted

```bash
curl -u admin:Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U \
  "http://$(minikube ip):30096/rests/data/network-topology:network-topology/topology=topology-netconf?content=nonconfig"
```

Look for `"node-id":"ietfYang-PNFDemo"` with `"connection-status":"connected"`.

---

## Demo Flow

This walks through the cm-handle onboarding flow end to end. Substitute `localhost` for `$(minikube ip)` on Windows.

### 1. Register the cm-handle

```bash
curl -X POST "http://$(minikube ip):30080/ncmpInventory/v1/ch" \
  -H "Content-Type: application/json" -u cpsuser:cpsr0cks! \
  -d '{"dmiPlugin":"http://cps-ncmp-ncmp-dmi-plugin-service:8080",
       "createdCmHandles":[{"cmHandle":"ietfYang-PNFDemo","cmHandleProperties":{}}]}'
```

### 2. Wait for the cm-handle to reach READY

```bash
curl -u cpsuser:cpsr0cks! "http://$(minikube ip):30080/ncmp/v1/ch/ietfYang-PNFDemo/state"
```

Module sync takes up to a minute. Expect `"cmHandleState":"READY"`.

### 3. Get all modules for the cm-handle

```bash
curl -u cpsuser:cpsr0cks! "http://$(minikube ip):30080/ncmp/v1/ch/ietfYang-PNFDemo/modules"
```

Returns the YANG modules read from the simulated device, e.g. `ietf-netconf`, `ietf-keystore`, `ietf-system`. To confirm they came via SDNC rather than a stub, check the plugin called SDNC's restconf mount point:

```bash
kubectl logs -l component=ncmp-dmi-plugin --tail=200 | grep sdncRestconfUri
```

### 4. Clean up the cm-handle

```bash
curl -X POST "http://$(minikube ip):30080/ncmpInventory/v1/ch" \
  -H "Content-Type: application/json" -u cpsuser:cpsr0cks! \
  -d '{"dmiPlugin":"http://cps-ncmp-ncmp-dmi-plugin-service:8080",
       "removedCmHandles":["ietfYang-PNFDemo"]}'
```

> **Note:** The same flow is available in Postman. Import
> `postman-collections/CPS_DMI_Stack.postman_collection.json` with the
> `postman-collections/Env_k8s.json` environment. On Linux/Minikube, set `CPS_HOST` to the output of `minikube ip`,
> or use the port-forward commands above. The main `CPS.postman_collection.json` targets `dmi-stub` only, which is
> not deployed when this stack is enabled.

---

## NodePort Allocation

| Service | NodePort |
|---------|----------|
| CPS | 30080 |
| ncmp-dmi-plugin | 30097 |
| SDNC | 30096 |

`sdnc-db` and `pnfsim` are ClusterIP only — they are reached in-cluster by SDNC and the mount Job, not from the host.

---

## Uninstallation
To uninstall the chart and delete all related resources:
```bash
helm uninstall cps
```

The `sdnc-mount-node` hook Job is not removed by `helm uninstall`. Delete it separately if you want a clean cluster:
```bash
kubectl delete job cps-ncmp-sdnc-mount-node
```
---