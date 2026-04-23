<!--
  ============LICENSE_START=======================================================
   Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
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

# Configuration Persistence Service

## General Information
* [CPS Project Wiki](https://lf-onap.atlassian.net/wiki/spaces/DW/pages/16398157/Configuration+Persistence+Service+Project)

## For Developers
* [Developer Wiki](https://lf-onap.atlassian.net/wiki/spaces/DW/pages/16442177/Configuration+Persistence+Service+Developer+s+Landing+Page)
* [Building and running CPS using Docker](docker-compose/README.md)
* [Running CPS locally using Kubernetes](cps-charts/README.md)

## Compile & Build Project

```bash
mvn clean install
```

Use `-DskipTests` to speed up the build:

```bash
mvn clean install -DskipTests
```

# IntelliJ setup for ProvMnS API Dependencies
* [Resolving provmns-api classes in IntelliJ](provmns-api/README.md)

## Local Git Setup for ProvMnS API JAR

The `provmns-api-18.6.0.jar` file is committed to the repository for faster builds, but gets regenerated during `mvn clean install`. To prevent Git from tracking local changes to this file, run this command after cloning:

```bash
git update-index --skip-worktree provmns-api/local-repo/org/onap/cps/provmns-api/18.6.0/provmns-api-18.6.0.jar
```

**Note**: Each team member must run this command on their local machine after cloning the repository.

