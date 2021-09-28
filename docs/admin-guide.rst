.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _adminGuide:


CPS Admin Guide
###############

.. warning:: draft

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

This architecture also makes all logs ready to be sent to and ELK stack or similar.

Enabling tracing for all executed sql statements is done by changing hibernate
loggers log level

Logger configuration is provided as a chart resource : `logback.xml <https://github.com/onap/oom/blob/master/kubernetes/cps/components/cps-core/resources/config/logback.xml>`_

+--------------------------------------+--------------------------------------------------------+
| Log Level                            |   Importance                                           |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| Fatal                                | One or more key business functionalities are not       |
|                                      | working. The system is currently not fulfilling        |
|                                      | business functionality.                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| Error                                | One or more functionalities are not working,           |
|                                      | potentially preventing other DMI                       |
|                                      | plugin functionalities from                            |
|                                      | functioning correctly.                                 |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| Warn                                 | Unexpected behaviour has occurred within               |
|                                      | the DMI plugin. The DMI plugin will                    |
|                                      | continue to work and functionalities will operate as   |
|                                      | normal.                                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| Info                                 | To inform the client of an action happening within     |
|                                      | the DMI plugin.                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| Debug                                | Logging event used primarily for software debugging.   |
|                                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| Trace                                | Used to describe step by step events during execution  |
|                                      | of code. This can be ignored during standard operations|
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

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

