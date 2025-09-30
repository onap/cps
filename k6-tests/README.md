# k6 tests

[k6](https://k6.io/) is used for performance tests.
k6 tests are written in JavaScript.

## k6 installation
Follow the instructions in the [build from source guide](https://github.com/mostafa/xk6-kafka) to get started.

## Running k6 test suites
The CPS k6 tests measure the system capabilities as per requirements.

### Test Profiles
There are two test profiles that can be run with either:
1. kpi — The test profile is to evaluate overall performance.
2. endurance — The test profile to measure long-term stability.

### Deployment Types
1. dockerHosts — A docker-compose based deployment for the services in CPS/NCMP.
2. k8sHosts — A Kubernetes based deployment for the services in CPS/NCMP with Helm Charts.

### Running the k6 test suites on a docker-compose environment
Only docker-compose deployment type supported: dockerHosts
Run the main script.
The script assumes k6 and the relevant docker-compose have been installed.
```shell
./k6-main.sh [kpi|endurance] [dockerHosts]
```

### Running the k6 test suites on a Kubernetes environment
Only kubernetes cluster deployment type supported: k8sHosts

#### Prerequisites for Windows
1. Docker Desktop
2. Enable Kubernetes in Docker Desktop (Settings, Kubernetes). Known issue: it may hang on "starting kubernetes" for a few minutes. Resolution: click "Reset Cluster" then it starts.
3. Install Helm, see [installing helm on windows](https://helm.sh/docs/intro/install/). Recommended approach: install Helm with winget.

#### Prerequisites for Linux
1. k3s from Rancher [installing k3s on linux](https://ranchermanager.docs.rancher.com/how-to-guides/new-user-guides/kubernetes-cluster-setup/k3s-for-rancher)
2. Install Helm, see [installing helm on linux](https://helm.sh/docs/intro/install/)

Run the main script.
By default, kpi profile is supported, and it assumes the kubernetes environment with Helm is already available.
```shell
./k6-main.sh [kpi|endurance] [k8sHosts]
```

## Running k6 tests manually
Before running tests, ensure CPS/NCMP is running:
```shell
docker-compose -f docker-compose/docker-compose.yml up -d
```

To run an individual test from the command line, use
```shell
k6 run ncmp/scenarios-config.js
```
