<!--
  ============LICENSE_START=======================================================
   Copyright (C) 2021 Nordix Foundation.
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

## Continuous System and Integration Testing (CSIT) for CPS

The directory structure:

- **plans/** contains testing plans, each sub-folder represents a separate test plan, contains processed subsequently:
    _startup.sh_ (serves docker containers startup), _testplan.txt_ (lists test-suits), _teardown.sh_ (serves docker containers stopping and images removal)
- **scripts/** contains shell scripts used on tests executions
- **tests/** contains test suits which are processed by folder name (relative to _tests_ folder) taken from _testplan.txt_

Test suits are executed using Robots framework.

### Running on local environment

Prerequisites:
- docker
- python + pip
- virtualenv

```bash
sudo apt install python3 python3-pip virtualenv
```

Add an alias in the ```.bashrc``` file for pip3 to be pip at the end of the file. </br>
This file will be present on the home directory of the Ubuntu system.
```bash
alias pip=pip3
```

Now load the ```.bashrc``` file.
```bash
. .bashrc
```

The Robot framework and required python packages will be installed on first execution.

Navigate to cps project directory
```bash
cd ~/<your_git_repo>/cps
```

Build a docker image (see also [docker-compose readme](../docker-compose/README.md) ) from your cps directory:

```bash
mvn clean install -Dmaven.test.skip=true -Ddocker.repository.push=
```

Execute test from current cps folder:
```bash
./csit/run-project-csit.sh
```

