# k6 tests

[k6](https://k6.io/) is used for performance tests.
k6 tests are written in JavaScript.

## k6 installation
Follow the instructions in the [build from source guide](https://github.com/mostafa/xk6-kafka) to get started.

## Running the k6 test suites
These tests measure the system capabilities as per requirements.
There are two test profiles can be run with either: kpi or endurance.
Simply run the main script. (The script assumes k6 and the relevant docker-compose have been installed).
```shell
./run-k6-tests.sh kpi
```

## Running k6 tests manually
Before running tests, ensure CPS/NCMP is running:
```shell
docker-compose -f docker-compose/docker-compose.yml --profile dmi-stub up
```

To run an individual test from command line, use
```shell
k6 run ncmp/ncmp-kpi.js
```
