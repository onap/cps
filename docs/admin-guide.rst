.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _adminGuide:


CPS Admin Guide
###############

.. toctree::
   :maxdepth: 1

Logging & Diagnostics
=====================

General Guidelines
------------------
CPS-Core logs are sent to `STDOUT` in order to leverage the Kubernetes logging architecture.

These logs are available using the following command:

.. code:: bash

    kubectl logs <cps-core-pod>

The default configuration for CPS logs is the INFO level.

This architecture also makes all logs ready to be sent to an Elastic Search Log Stash and Kibana (ELK) stack or similar.

Enabling tracing for all executed sql statements is done by changing hibernate
loggers log level

Logger configuration is provided as a chart resource :

    +--------------------------------+---------------------------------------------------------------------------------------------------------------------------------+
    | <cps-component-service-name>   | logback.xml location                                                                                                            |
    +================================+=================================================================================================================================+
    | cps-core                       | `logback.xml <https://github.com/onap/oom/blob/master/kubernetes/cps/components/cps-core/resources/config/logback.xml>`_        |
    +--------------------------------+---------------------------------------------------------------------------------------------------------------------------------+
    | cps-temporal                   | `logback.xml <https://github.com/onap/oom/blob/master/kubernetes/cps/components/cps-temporal/resources/config/logback.xml>`_    |
    +--------------------------------+---------------------------------------------------------------------------------------------------------------------------------+
    | ncmp-dmi-plugin                | `logback.xml <https://github.com/onap/oom/tree/master/kubernetes/cps/components/ncmp-dmi-plugin/resources/config/logback.xml>`_ |
    +--------------------------------+---------------------------------------------------------------------------------------------------------------------------------+

Monitoring
==========
Once CPS-Core is deployed, information related to the running instance of the application is available

.. code::

    http://<cps-component-service-name>:8081/manage/info/

Health
------

Cps-Core health status and state can be checked using the following endpoint.
This also includes both the liveliness state and readiness state.

.. code::

    http://<cps-component-service-name>:8081/manage/health/

Metrics
-------

Prometheus Metrics can be checked at the following endpoint

.. code::

    http://<cps-component-service-name>:8081/manage/prometheus

.. note::

    Command to retrieve <cps-component-service-name> can be got from the :ref:`deployment page <deployment>`

