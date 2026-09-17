<!--
  ============LICENSE_START=======================================================
     Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

# Importing and running the CPS postman collections

## Importing the CPS collections
To import the CPS collections and environments:
1. Create a "Workspace" (or if you have one already you can use that)
2. Click "Import" (or click "File" then "Import")
3. Drag and drop the "postman-collection" folder into the import pop-up
4. The collection and environments should now be imported
5. Select the environment that matches your deployment (see "Choosing an environment" below)

## Choosing an environment
Two environments are provided. Which one you need depends on whether the DMI side
of your deployment is the lightweight dmi-stub or the real ncmp-dmi-plugin.

| Environment file | Environment name | Use for |
|---|---|---|
| `Env_k8s.json` | CPS Environment k8s (linux and windows) | Default deployment, using dmi-stub |
| `Env_onap_dmi_stack.json` | CPS Environment ONAP DMI Stack (k8s) | Optional `onapDmiStack` deployment, using the real ncmp-dmi-plugin and SDNC |

The two are kept separate because their variables do not overlap: the dmi-stub
endpoints are not deployed when `onapDmiStack` is enabled, and the
ncmp-dmi-plugin/SDNC endpoints do not exist otherwise.

Folders in the CPS collection map to the environments as follows:

- `CPS-Core` — neither (only needs `CPS_HOST`/`CPS_PORT`, present in both)
- `CPS-NCMP`, `NCMP-DMI-Stub` — `Env_k8s.json`
- `ONAP-DMI-Stack` — `Env_onap_dmi_stack.json`
- `Liveness & Readiness` — spans both; its `DMI-Stub` subfolder needs
  `Env_k8s.json`, its `ONAP-DMI-Stack` subfolder needs `Env_onap_dmi_stack.json`

### Environment Setup
Configure the correct host and ports based on your local setup:

**Linux (minikube):**
```bash
# Get minikube IP and NodePorts
minikube service list

# Update environment variables with:
# CPS_HOST = <minikube-ip>  (e.g., 192.168.49.2)
# CPS_PORT = <nodeport>     (e.g., 30080)
#
# Env_k8s.json (dmi-stub):
# DMI_HOST_1 = <minikube-ip>
# DMI_PORT_1 = <nodeport>   (e.g., 30092)
# DMI_HOST_2 = <minikube-ip>
# DMI_PORT_2 = <nodeport>   (e.g., 30094)
#
# Env_onap_dmi_stack.json (ONAP DMI stack):
# DMI_PLUGIN_NODEPORT = <nodeport>  (e.g., 30097)
# SDNC_NODEPORT = <nodeport>        (e.g., 30096)
```

**Windows (Docker Desktop / minikube):**
```powershell
# For Docker Desktop Kubernetes:
# CPS_HOST = localhost
# Ports = NodePort values from: kubectl get svc

# For minikube:
minikube service list
# Use the URLs shown (host:port)
```

**Verify your services:**
```bash
kubectl get svc | grep -E "cps|dmi"
# Note the NodePort values (e.g., 8080:30080/TCP)
# Use the second port (30080) in your environment
```

### ONAP DMI stack setup
The `ONAP-DMI-Stack` requests target the real ncmp-dmi-plugin and SDNC, which are
only deployed when the optional stack is enabled:

```bash
helm install cps ./cps-charts --set onapDmiStack.enabled=true --timeout 20m
```

Select the "CPS Environment ONAP DMI Stack (k8s)" environment
(`Env_onap_dmi_stack.json`) for these requests.

On Linux/minikube, NodePorts bind to the minikube IP rather than localhost.
Either set `CPS_HOST` to the output of `minikube ip`, or port-forward:

```bash
kubectl port-forward service/cps-ncmp-service 30080:8080
kubectl port-forward service/cps-ncmp-ncmp-dmi-plugin-service 30097:8080
kubectl port-forward service/cps-ncmp-sdnc-service 30096:8181
```

## Environment Variables

Shared by both environments:
- `CPS_HOST` and `CPS_PORT` - CPS service endpoint
- `basic_auth_171s` - basic auth header for CPS NCMP requests

`Env_k8s.json` (dmi-stub) adds:
- `DMI_HOST_1` and `DMI_PORT_1` - DMI-Stub-1 endpoint
- `DMI_HOST_2` and `DMI_PORT_2` - DMI-Stub-2 endpoint

`Env_onap_dmi_stack.json` (ONAP DMI stack) adds:
- `DMI_PLUGIN_HOST` and `DMI_PLUGIN_PORT` - in-cluster ncmp-dmi-plugin service,
  used in the `dmiPlugin` field of the cm-handle registration body
- `DMI_PLUGIN_NODEPORT` - ncmp-dmi-plugin NodePort, for health checks from
  outside the cluster
- `SDNC_NODEPORT` and `SDNC_AUTH` - SDNC endpoint and basic auth header
- `CM_HANDLE` - cm-handle id matching the netconf node mounted in SDNC

Note: the `NCMP-DMI-Stub` folder also references `DMI_STUB_1_PORT`, which is not
defined in either environment file. Set it manually (or add it to
`Env_k8s.json`) before running those requests.

## Using collections in Postman
A how-to guide is provided in the CPS collection. To access this guide click on the parent CPS folder icon at the top of the collection and follow the provided instructions.

## Running the collections
To run the requests in the collections in CPS-CORE simply select the request and click send. "Create Schema Set" in "CPS-CORE" requires a file to send the request. Example files are provided: "bookstore.yang" and "bookstore-types.yang" (these files must be zipped before adding them to the request)

## Liveness & Readiness Checks
The collection includes health check endpoints for all services:
- **CPS**: Liveness and Readiness checks
- **DMI-Stub-1**: Liveness and Readiness checks
- **DMI-Stub-2**: Liveness and Readiness checks
- **ONAP-DMI-Stack**: DMI plugin health check and SDNC mounted node check
  (requires `onapDmiStack.enabled=true` and the ONAP DMI stack environment)

These checks work with k8s deployments by using environment variables.

## ONAP DMI Stack demo flow
The `ONAP-DMI-Stack` folder exercises the DMI -> NCMP -> SDNC integration path
against the real ncmp-dmi-plugin. Run the requests in order:

1. Register the cm-handle
2. Get CM handle state - module sync takes up to a minute; wait for
   `"cmHandleState":"READY"` before step 3
3. Get module references - returns the YANG modules read from the simulated
   device via SDNC
4. De-register the cm-handle - cleans up after the demo

## Notes
When exporting postman collections tabs are used for spacing, so replacing all the tabs is necessary
