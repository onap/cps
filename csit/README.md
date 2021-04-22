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
mvn clean install -Dmaven.test.skip=true -Dnexus.repository=
```

Execute test from current cps folder:
```bash
./csit/run-project-csit.sh
```

