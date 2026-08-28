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

This stack replicates the environment defined in `docker-compose/dmi-services.yml` as native Kubernetes resources, reproducing the environment used by the (now disabled) CSIT `dmi-integration` tests:

- `ncmp-dmi-plugin/csit/tests/dmi-integration/dmi-ncmp.robot`
- `ncmp-dmi-plugin/csit/tests/dmi-integration/dmi-sdnc.robot`

It can be deployed alongside the rest of the CPS/NCMP stack, and used as the target for the K6 tests replacing those CSIT tests.

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

The netconf node mount happens automatically via a Helm post-install/post-upgrade hook — no manual `curl` step needed, unlike the old CSIT `setup.sh` script.

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

Or enable port forwarding (keep running in separate terminals):

```bash
kubectl port-forward service/cps-ncmp-service 30080:8080
kubectl port-forward service/cps-ncmp-ncmp-dmi-plugin-service 30097:8080
kubectl port-forward service/cps-ncmp-sdnc-service 30096:8181
```

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

`cps-ncmp-sdnc-mount-node-...` shows `0/1 Completed` while it exists, then disappears — it is a run-once hook Job that Helm deletes on success.

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

This reproduces the two disabled CSIT `dmi-integration` tests. Substitute `localhost` for `$(minikube ip)` on Windows.

### 1. Register the cm-handle

Equivalent to CSIT's "Register cm handle to test integration between DMI and NCMP".

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

Equivalent to CSIT's "Get all modules for given cm-handle to test integration between DMI and SDNC".

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

> **Note:** The Postman collection (`postman-collections/CPS.postman_collection.json`) has **no requests for the real
> `ncmp-dmi-plugin`** — its DMI requests target `dmi-stub` only. `Env_k8s.json` also points `DMI_HOST_1`/`DMI_HOST_2` at
> in-cluster service names, which do not resolve from your host. Use the `curl` commands above, or add the requests and
> host/NodePort variables to the collection first.

---

## NodePort Allocation

| Service | Default (KPI) |
|---------|---------------|
| CPS | 30080 |
| ncmp-dmi-plugin | 30097 |
| SDNC | 30096 |

`sdnc-db` and `pnfsim` are ClusterIP only — they are reached in-cluster by SDNC and the mount Job, not from the host.

---

## Cleanup

```bash
helm uninstall cps
```

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `helm install` reports failure but pods look fine | SDNC/Karaf boot exceeded Helm's 5-minute default while the mount hook waited | Re-run with `--timeout 20m` |
| `ECONNREFUSED 127.0.0.1:30080` on Linux | NodePorts bind to the minikube IP, not localhost | Use `$(minikube ip)` or `kubectl port-forward` |
| SDNC in `CrashLoopBackOff` with `OutOfMemoryError` | Karaf's OSGi feature resolver needs a large heap; `3Gi` is not enough | Chart sets `6Gi` limit / `3Gi` request in `onapDmiStack.sdnc.resources` — increase further if needed |
| SDNC cannot reach its database | Startup scripts hardcode `dbhost`, `sdnctldb01`, `sdnctldb02` from docker-compose `links:`, which has no K8s equivalent | Chart appends these aliases to `/etc/hosts` at startup — see `templates/onap-dmi-stack-sdnc-deployment.yaml` |
| Mount Job fails `BackoffLimitExceeded` while SDNC and pnfsim are healthy | Original script ran `apk add curl`, and cluster DNS may not resolve `dl-cdn.alpinelinux.org` | Chart runs the script in `curlimages/curl:8.10.1`, which ships `curl` pre-built |
| `402 Invalid license` pulling `ncmp-dmi-plugin` | Sonatype Nexus licensing fault on `nexus3.onap.org:10003` | Resolved upstream; if it recurs, use Docker Hub's `onap/ncmp-dmi-plugin` |
| `MANIFEST_UNKNOWN` for `ncmp-dmi-plugin:1.8.0` | Nexus publishes no `1.8.0` release tag, only `<version>-SNAPSHOT-<timestamp>` and `latest` | Chart pins the newest snapshot instead of tracking floating `latest` — bump the tag when a newer build is needed |
| cm-handle stuck in `ADVISED`/`LOCKED` | Module sync has not run, or the plugin cannot reach SDNC | Check `kubectl logs -l component=ncmp-dmi-plugin` for SDNC connection errors |

---

## Reference

- Original docker-compose equivalent: `docker-compose/dmi-services.yml`
- Original CSIT equivalent: `ncmp-dmi-plugin/csit/plans/dmi/` and `ncmp-dmi-plugin/csit/tests/dmi-integration/`
- SDNC mount script: `docker-compose/config/sdnc/check_sdnc_mount_node.sh` (ported into the chart as a ConfigMap + Job)
- General chart usage: `cps-charts/README.md`
- Monitoring and tracing: `cps-charts/MONITORING-AND-TRACING.md`
