# k6 tests

[k6](https://k6.io/) is used for performance tests.
k6 tests are written in JavaScript.

## k6 installation
Follow the instructions in the [build from source guide](https://github.com/mostafa/xk6-kafka) to get started.

## Running k6 test suites
The CPS k6 tests measure the system capabilities as per requirements.

### Test Suites
There are two test suites, each with its own scenarios/config, kept as separate k6 invocations so results aren't
contaminated by shared CPS container/DB contention:
1. ncmp — the test scenarios specific to NCMP (default).
2. cps-core — CPS-Core scenarios (scaffold only for now; no scenarios executed yet).

### Test Profiles
There are three test profiles that can be run:
1. kpi — The test profile is to evaluate overall performance.
2. endurance — The test profile to measure long-term stability.
3. onapDmiStack — The test profile that exercises the DMI API against the ONAP DMI stack
   (DMI + cps-ncmp + SDNC + pnfsim) instead of `dmi-stub`. See
   [onap-dmi-stack/README.md](onap-dmi-stack/README.md).

### Deployment
Tests run on a Kubernetes cluster using Helm Charts. Each test profile deploys into its own namespace
(e.g., `kpi` namespace, `endurance` namespace), allowing profiles to run in parallel without conflicts.
The namespace is the profile name lowercased, because a namespace must be a valid RFC 1123 DNS label
(so the `onapDmiStack` profile deploys into the `onapdmistack` namespace).

### Prerequisites
See [Prerequisites for Windows](../cps-charts/README.md#prerequisites-for-windows) or [Prerequisites for Linux](../cps-charts/README.md#prerequisites-for-linux) in the CPS Charts README.

### Running the k6 test suites
Run the main script. It assumes a Kubernetes environment with Helm is already available.
```shell
./k6-main.sh [kpi|endurance|onapDmiStack] [ncmp|cps-core]
```

### Parallel runs
KPI and endurance can run simultaneously on the same cluster. They use separate namespaces and
non-conflicting NodePorts:

| Profile   | CPS NodePort | Kafka UI NodePort | Kafka NodePort |
|-----------|--------------|-------------------|----------------|
| kpi       | 30080        | 30089             | 30093          |
| endurance | 30180        | 30189             | 30193          |

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

or, for cps-core:
```shell
cd cps-core && k6 run cps-core-test-runner.js -e TEST_PROFILE=kpi
```
