# k6 tests

[k6](https://k6.io/) is used for performance tests.
k6 tests are written in JavaScript.

## k6 installation
Follow the instructions in the [k6 installation guide](https://grafana.com/docs/k6/latest/set-up/install-k6/)
to get started.

## k6 installation with Apache Kafka extension
To install k6 including Apache Kafka support, it should be built from source as mentioned in the
[build from source guide](https://github.com/mostafa/xk6-kafka) to run the test cases that integrate with Apache
Kafka.

## Running the k6 test suites
Simply run the main script. (The script assumes k6 and docker-compose have been installed).
```shell
./run-k6-tests.sh
```

## Running k6 tests manually
Before running tests, ensure CPS/NCMP is running:
```shell
docker-compose -f docker-compose/docker-compose.yml --profile dmi-stub --profile monitoring up
```

To run an individual test from command line, use
```shell
k6 run ncmp/ncmp-kpi.js
```
