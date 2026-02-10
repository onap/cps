<!--
  ============LICENSE_START=======================================================
     Copyright (C) 2024 Nordix Foundation.
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
To import the CPS collections and environment:
1. Create a "Workspace" (or if you have one already you can use that)
2. Click "Import" (or click "File" then "Import")
3. Drag and drop the "postman-collection" folder into the import pop-up
4. The collections and environment should now be imported
5. Set the current environment based on your deployment:
   - For **Docker**: Select "CPS Environment Docker" (CPS_PORT=8883)
   - For **Kubernetes**: Select "CPS Environment k8s-linux-minikube" (CPS_PORT=30080)

## Switching Between Docker and Kubernetes
The collection now supports both docker-compose and k8s deployments using environment variables:
- Simply change the active environment in Postman (top right dropdown)
- All requests will automatically use the correct ports and hosts
- No need for separate folders or duplicate tests

### Kubernetes Environment Setup
For Kubernetes deployments, you need to configure the correct host and ports based on your local setup:

**Linux (minikube):**
```bash
# Get minikube IP and NodePorts
minikube service list

# Update environment variables with:
# CPS_HOST = <minikube-ip>  (e.g., 192.168.49.2)
# CPS_PORT = <nodeport>     (e.g., 30080)
# DMI_HOST = <minikube-ip>
# DMI_PORT = <nodeport>     (e.g., 30092)
# DMI_HOST_2 = <minikube-ip>
# DMI_PORT_2 = <nodeport>   (e.g., 30094)
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

## Environment Variables
Both environments support:
- `CPS_HOST` and `CPS_PORT` - CPS service endpoint
- `DMI_HOST` and `DMI_PORT` - DMI-Stub-1 endpoint
- `DMI_HOST_2` and `DMI_PORT_2` - DMI-Stub-2 endpoint

## Using collections in Postman
A how-to guide is provided in the CPS collection. To access this guide click on the parent CPS folder icon at the top of the collection and follow the provided instructions.

## Running the collections
To run the requests in the collections in CPS-CORE simply select the request and click send. "Create Schema Set" in "CPS-CORE" requires a file to send the request. Example files are provided: "bookstore.yang" and "bookstore-types.yang" (these files must be zipped before adding them to the request)

## Liveness & Readiness Checks
The collection includes health check endpoints for all services:
- **CPS**: Liveness and Readiness checks
- **DMI-Stub-1**: Liveness and Readiness checks
- **DMI-Stub-2**: Liveness and Readiness checks

These checks work with both docker and k8s deployments by using environment variables.

## Notes
When exporting postman collections tabs are used for spacing, so replacing all the tabs is necessary
