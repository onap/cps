<!--
  ============LICENSE_START=======================================================
   Copyright (C) 2020 Pantheon.tech
   Modifications Copyright (C) 2020-2025 OpenInfra Foundation Europe. All rights reserved.
   Modifications Copyright (C) 2021 Bell Canada.
   Modifications Copyright (C) 2022 Deutsche Telekom AG
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

# Building and Running CPS Locally

## Building Java Archive only

Following command builds all Java components to `cps-application/target/cps-application-x.y.z-SNAPSHOT.jar` JAR file
without generating any docker images:

```bash
mvn clean install -Pcps-docker -Pncmp-docker -Pcps-ncmp-docker -Djib.skip
```

## Building Java Archive and Docker Images

* Following command builds the JAR file and also generates the Docker image for all CPS components:

```bash
mvn clean install -Pcps-docker -Pncmp-docker -Pcps-ncmp-docker
```

* Following command builds the JAR file and generates the Docker image for specified CPS component:
  (with `<docker-profile>` being one of `cps-docker`, `ncmp-docker` or `cps-ncmp-docker`):

```bash
mvn clean install -P<docker-profile>
```

## Running Docker Containers

`docker-compose/docker-compose.yml` contains the base services required to run CPS and NCMP.
This deployment can also be used for KPI test. Please run the following command from `docker-compose` folder:
```bash
docker-compose up -d
or
docker-compose --profile tracing up -d
```

`docker-compose/dmi-services.yml` contains the DMI services including SDNC and PNF Simulator.
This deployment is required for the CSIT test.
```bash
docker-compose -f docker-compose.yml -f dmi-services.yml up -d
```

To deploy services that are required for Endurance test, please use the following command:
```bash
docker-compose --project-name endurance --env-file env/endurance.env up -d
```

To stop any deployment, please replace `up -d` flag with `down` in the above commands.

### Running from Jar Archive

Following command starts the application using JAR file:

```bash
DB_HOST=localhost DB_USERNAME=cps DB_PASSWORD=cps \
  DMI_USERNAME=cpsuser DMI_PASSWORD=cpsr0cks! \
  java -jar cps-application/target/cps-application-x.y.z-SNAPSHOT.jar
```

### Running from IntelliJ IDE

Here are the steps to run or debug the application from Intellij:

1. Enable the desired maven profile form Maven Tool Window
2. Run a configuration from `Run -> Edit configurations` with following settings:
   * `Environment variables`: `DB_HOST=localhost;DB_USERNAME=cps;DB_PASSWORD=cps
                                DMI_USERNAME=cpsuser DMI_PASSWORD=cpsr0cks!`

## Accessing services

Swagger UI and Open API specifications are available to discover service endpoints and send requests.

* `http://localhost:<port-number>/swagger-ui.html`
* `http://localhost:<port-number>/api-docs/cps-core/openapi.yaml`
* `http://localhost:<port-number>/api-docs/cps-ncmp/openapi.yaml`
* `http://localhost:<port-number>/api-docs/cps-ncmp/openapi-inventory.yaml`

with <port-number> is being either `8080` if running the plain Java build or retrieved using following command
if running from `docker-compose`:

```bash
docker inspect \
  --format='{{range $p, $conf := .NetworkSettings.Ports}} {{$p}} -> {{(index $conf 0).HostPort}} {{end}}' \
  <cps-docker-container>
```

Enjoy CPS!
