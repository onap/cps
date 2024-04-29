# k6 tests

[k6](https://k6.io/) is used for performance tests.
k6 tests are written in JavaScript.

## k6 installation
Follow the instructions in the [k6 installation guide](https://grafana.com/docs/k6/latest/set-up/install-k6/)
to get started.

## Running k6 tests
Before running tests, ensure CPS/NCMP is running:
```shell
docker-compose -f docker-compose/docker-compose.yml --profile dmi-stub up
```

To run an individual test from command line, use
```shell
k6 run ncmp/1-create-20k.js
```
To run all k6 tests, you may use the test runner shell script provided:
```shell
./ncmp/run-all-tests.sh
```
