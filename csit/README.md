## Continuous System and Integration Testing (CSIT) for CPS



The directory structure:

- **plans/** contains testing plans, each sub-folder represents a separate test plan, contains processed subsequently:
    _startup.sh_ (serves docker containers startup), _testplan.txt_ (lists test-suits), _teardown.sh_ (serves docker containers stopping and images removal)
- **scripts/** contains shell scripts used on tests executions
- **tests/** contains test suits which are processed by folder name (relative to _tests_ folder) taken from _testplan.txt_

Test suits are executed using Robots Framework.

### Running on local environment

Prerequisites: 
- docker
- python + pip

```dtd
sudo apt install python3-pip 
```

The Robot framework and required python packages will be installed on first execution.

Build a docker image (see also [docker-compose readme](../docker-compose/README.md) ):

```dtd
mvn clean package -Dmaven.test.skip=true -Dnexus.repository= -Pcps-xnf-docker
```

Execute test from current folder:
```dtd
./run-project-csit.sh
```
 