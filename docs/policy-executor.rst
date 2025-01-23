.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2024-2025 Nordix Foundation

.. DO NOT CHANGE THIS LABEL - EVEN THOUGH IT GIVES A WARNING
.. _policy_executor:

Policy Executor
###############

.. toctree::
   :maxdepth: 1

Introduction
============

The Policy Executor feature can be used to connect an external system to make decisions on CM write operation.
When the feature is enabled NCMP will first call the configured external system and depending on the response return an error or continue.
The details of the interface can be found in the ':ref:`policy_executor_consumed_apis`' section below.

This feature is available on 'legacy data interface' for operation on a single cm handle: "/v1/ch/{cm-handle}/data/ds/{datastore-name}" and only applies to "ncmp-datastore:passthrough-running".

By default the feature is not enabled. This is controlled by 'config.additional.ncmp.policy-executor.enabled' and other deployment parameters in the same group to enable it. See :ref:`additional-cps-ncmp-customizations`

.. DO NOT CHANGE THIS LABEL - EVEN THOUGH IT GIVES A WARNING
.. _policy_executor_consumed_apis:

Consumed APIs
-------------

:download:`Policy Executor OpenApi Specification <api/swagger/policy-executor/openapi.yaml>`
