# k6 tests

[k6](https://k6.io/) is used for performance tests.
k6 tests are written in JavaScript.

## k6 installation
Follow the instructions in the [build from source guide](https://github.com/mostafa/xk6-kafka) to get started.

## Running the k6 test suites
These tests measure the system capabilities as per requirements.
There are two test profiles that can be run with either: kpi or endurance.
Run the main script.
(The script assumes k6 and the relevant docker-compose have been installed).
```shell
./k6-main.sh kpi
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
