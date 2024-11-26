<!--
  ============LICENSE_START=======================================================
   Copyright (C) 2020 Pantheon.tech
   Modifications Copyright (C) 2020-2024 Nordix Foundation.
   Modifications Copyright (C) 2021 Bell Canada.
   Modifications Copyright (C) 2022 TechMahindra Ltd.
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

# Building and running CPS locally

## Building Java Archive only

Following command builds all Java components to `cps-application/target/cps-application-x.y.z-SNAPSHOT.jar` JAR file
without generating any docker images:

```bash
mvn clean install -Pcps-docker -Pncmp-docker -Pcps-ncmp-docker -Djib.skip
```

## Building Java Archive and Docker images

* Following command builds the JAR file and also generates the Docker image for all CPS components:

```bash
mvn clean install -Pcps-docker -Pncmp-docker -Pcps-ncmp-docker
```

* Following command builds the JAR file and generates the Docker image for specified CPS component:
  (with `<docker-profile>` being one of `cps-docker`, `ncmp-docker` or `cps-ncmp-docker`):

```bash
mvn clean install -P<docker-profile>
```

## Running Docker containers

`docker-compose/docker-compose.yml` file is provided to be run with `docker-compose` tool and images previously built.
It starts both Postgres database and CPS services.

1. Edit `docker-compose.yml`
   1. uncomment desired service to be deployed, by default `cps-and-ncmp` is enabled. You can comment it and uncomment `cps-standalone` or `ncmp-standalone`.
   2. To send data-updated events to kafka,
      * uncomment the `zookeeper` and `kafka` services.
      * uncomment environment variables
        * `KAFKA_BOOTSTRAP_SERVER: kafka:9092`
2. Execute following command from `docker-compose` folder:

Use one of the below version type that has been generated in the local system's docker image list after the build.
```bash
VERSION=latest DB_USERNAME=cps DB_PASSWORD=cps docker-compose up -d
or
VERSION=<version> DB_USERNAME=cps DB_PASSWORD=cps docker-compose up -d
```

## Running Docker containers with profile: monitoring

Run docker-compose with profile, monitoring, then it will start monitoring services:
* prometheus
* grafana
* kafka-ui

```bash
docker-compose --profile monitoring up -d
```

### prometheus service
It collects and stores metrics as time series data, recording information with a timestamp.

The environment variable, PROMETHEUS_RETENTION_TIME, is used to set the retention time for the metrics
in the prometheus database. The default value is 15d, but can be changed to any value.

To be able to use the historical data, the prometheus container should not be removed.
Instead, it can be stopped and started using the following commands:

```bash
docker-compose start prometheus
```

```bash
docker-compose stop prometheus
```

## Running or debugging Java built code

Before running CPS, a Postgres database instance needs to be started. This can be done with following
command:

```bash
docker run --name postgres -p 5432:5432 -d \
  -e POSTGRES_DB=cpsdb -e POSTGRES_USER=cps -e POSTGRES_PASSWORD=cps \
  postgres:12.4-alpine
```

Then CPS can be started either using a Java Archive previously built or directly from Intellij IDE.

### Running from Jar Archive

Following command starts the application using JAR file:

```bash
DB_HOST=localhost DB_USERNAME=cps DB_PASSWORD=cps CPS_USERNAME=cpsuser CPS_PASSWORD=cpsr0cks! \
  DMI_USERNAME=cpsuser DMI_PASSWORD=cpsr0cks! \
  java -jar cps-application/target/cps-application-x.y.z-SNAPSHOT.jar
```

### Running from IntelliJ IDE

Here are the steps to run or debug the application from Intellij:

1. Enable the desired maven profile form Maven Tool Window
2. Run a configuration from `Run -> Edit configurations` with following settings:
   * `Environment variables`: `DB_HOST=localhost;DB_USERNAME=cps;DB_PASSWORD=cps
                                CPS_USERNAME=cpsuser CPS_PASSWORD=cpsr0cks!
                                DMI_USERNAME=cpsuser DMI_PASSWORD=cpsr0cks!`

## Accessing services

Swagger UI and Open API specifications are available to discover service endpoints and send requests.

* `http://localhost:<port-number>/swagger-ui.html`
* `http://localhost:<port-number>/api-docs/cps-core/openapi.yaml`
* `http://localhost:<port-number>/api-docs/cps-ncmp/openapi.yaml`
* `http://localhost:<port-number>/api-docs/cps-ncmp/openapi-inventory.yaml`

with <port-number> being either `8080` if running the plain Java build or retrieved using following command
if running from `docker-compose`:

```bash
docker inspect \
  --format='{{range $p, $conf := .NetworkSettings.Ports}} {{$p}} -> {{(index $conf 0).HostPort}} {{end}}' \
  <cps-docker-container>
```

Enjoy CPS !
