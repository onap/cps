# k6 tests

[k6](https://k6.io/) is used for performance tests.
k6 tests are written in JavaScript.

## k6 installation
Follow the instructions in the [build from source guide](https://github.com/mostafa/xk6-kafka) to get started.

## Running k6 test suites
The CPS k6 tests measure the system capabilities as per requirements.

### Test Profiles
There are two test profiles that can be run:
1. kpi — The test profile is to evaluate overall performance.
2. endurance — The test profile to measure long-term stability.

### Deployment
Tests run on a Kubernetes cluster using Helm Charts. Each test profile deploys into its own namespace
(e.g., `kpi` namespace, `endurance` namespace), allowing profiles to run in parallel without conflicts.

### Prerequisites
See [Prerequisites for Windows](../cps-charts/README.md#prerequisites-for-windows) or [Prerequisites for Linux](../cps-charts/README.md#prerequisites-for-linux) in the CPS Charts README.

### Running the k6 test suites
Run the main script. It assumes a Kubernetes environment with Helm is already available.
```shell
./k6-main.sh [kpi|endurance]
```

### Parallel runs
KPI and endurance can run simultaneously on the same cluster. They use separate namespaces and
non-conflicting NodePorts:

| Profile   | CPS NodePort | Kafka NodePort |
|-----------|-------------|----------------|
| kpi       | 30080       | 30093          |
| endurance | 30180       | 30193          |

The endurance profile uses a Helm values override file (`cps-charts/values-endurance.yaml`) to
configure its unique NodePorts.

## Running k6 tests manually
Before running tests, ensure CPS/NCMP is deployed via Helm:
```shell
helm install cps ../cps-charts --namespace kpi --create-namespace
```

To run an individual test from the command line, use:
```shell
k6 run ncmp/scenarios-config.js -e TEST_PROFILE=kpi
```
