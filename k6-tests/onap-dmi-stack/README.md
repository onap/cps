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

# ONAP DMI stack k6 tests

Functional tests for the **DMI → NCMP → SDNC** cm-handle onboarding flow,
exercised through DMI's own API.

These replace the CSIT Robot suite that used to live under
`csit/tests/dmi-integration/` (`dmi-ncmp.robot`, `dmi-sdnc.robot`).

## What is covered

| Step | Check | Endpoint |
|---|---|---|
| 1 | DMI plugin liveness is UP | `GET /actuator/health/liveness` |
| 2 | DMI plugin readiness is UP | `GET /actuator/health/readiness` |
| 3 | CPS-NCMP liveness is UP | `GET /actuator/health/liveness` |
| 4 | CPS-NCMP readiness is UP | `GET /actuator/health/readiness` |
| 5 | register cm handle status is 201 | `POST /dmi/v1/inventory/cmHandles` |
| 6 | CM handle reaches READY | polls `GET /ncmp/v1/ch/{cmHandle}/state` |
| 7 | get all modules status is 200 | `POST /dmi/v1/ch/{cmHandle}/modules` |
| 8 | number of modules is 22 | (same response) |

Health checks assert the actuator reports `"status":"UP"`, not merely a 200 — the
endpoint can answer 200 while a component is degraded.

Step 6 polls with a bounded timeout (`cmHandleReadyTimeoutInSeconds`, default
180s). Module sync against a real SDNC-backed device takes up to a minute, and
retrieving modules before READY returns nothing useful.

Both `checks: rate==1` and `http_req_failed: rate==0` are enforced as thresholds,
so any failed assertion fails the run.

### Two details worth knowing

**Registration returns 201, not 200.** The DMI OpenAPI spec defines
`201 Created` for `POST /dmi/v1/inventory/cmHandles`, and the deployed plugin
returns 201.

**22 modules, 44 entries.** DMI returns each of pnfsim's modules twice — the
entries are byte-identical, same name, revision and namespace. The test counts
distinct module names, so `expectedNumberOfModules` is 22 rather than the raw
array length.

## Target environment

The CM stack deployed by `cps-charts`: DMI + cps-ncmp + SDNC + pnfsim, with the
simulator mounted into SDNC as a cm handle.

```shell
helm install cps ./cps-charts --set onapDmiStack.enabled=true --timeout 20m
```

See [../../cps-charts/README_DMI_STACK.md](../../cps-charts/README_DMI_STACK.md)
for full deployment detail.

## Running

Via the shared entry point, using the `functional` profile. It deploys the stack
with `onapDmiStack.enabled=true`, waits for the DMI plugin, and resolves the
NodePort host automatically (the minikube node IP where applicable, otherwise
localhost):

```shell
./k6-main.sh functional
```

Or against an already-deployed stack:

```shell
cd onap-dmi-stack
k6 run onap-dmi-stack-test-runner.js -e TEST_PROFILE=functional
```

## Configuration

`config/functional.json` holds the endpoints, credentials, cm-handle id, expected
module count and the READY timeout. Any value can be overridden by environment
variable:

| Variable | Default |
|---|---|
| `DMI_BASE_URL` | `http://localhost:30097` |
| `NCMP_BASE_URL` | `http://localhost:30080` |
| `DMI_PLUGIN_IN_CLUSTER_URL` | `http://cps-ncmp-ncmp-dmi-plugin-service:8080` |
| `CM_HANDLE` | `ietfYang-PNFDemo` |
| `EXPECTED_NUMBER_OF_MODULES` | `22` |
| `DMI_USERNAME` / `DMI_PASSWORD` | `cpsuser` / `cpsr0cks!` |

Note `DMI_PLUGIN_IN_CLUSTER_URL` is a cluster-internal service DNS name, **not** a
NodePort URL: it goes into the de-registration body, and NCMP resolves it from
inside the cluster.

On Linux/minikube, NodePorts bind to the node IP rather than localhost. Override
when running k6 directly:

```shell
k6 run onap-dmi-stack-test-runner.js \
  -e DMI_BASE_URL="http://$(minikube ip):30097" \
  -e NCMP_BASE_URL="http://$(minikube ip):30080"
```

## Repeatability

`setup()` de-registers the cm handle before the run. Without this, a leftover
cm-handle makes NCMP return `errorCode 109 "cm-handle already exists"`, which DMI
surfaces as a 500 — the failure that caused the original CSIT tests to be
disabled. That cleanup call is excluded from `http_req_failed`, since a non-2xx is
the expected outcome when there is nothing to remove.
