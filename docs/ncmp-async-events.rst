.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2023-2025 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _asyncEvents:


CPS Async Events
################

.. toctree::
   :maxdepth: 1

Introduction
============

Async events are triggered when a valid topic has been detected in a passthrough operation.

:download:`NCMP request response event schema <schemas/ncmp/async-m2m/ncmp-async-request-response-event-schema-v1.json>`

Event header
^^^^^^^^^^^^

.. code-block:: json

    {
        "eventId"               : "001",
        "eventCorrelationId"    : "cps-001",
        "eventTime"             : "2022-09-28T12:24:21.003+0000",
        "eventTarget"           : "test-topic",
        "eventType"             : "org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent",
        "eventSchema"           : "urn:cps:org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent:v1",
        "forwarded-Event"       : { }
    }

Forwarded-Event Payload
^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: json

    "Forwarded-Event": {
        "eventId"               : "002",
        "eventCorrelationId"    : "cps-001",
        "eventTime"             : "2022-09-28T12:24:18.340+0000",
        "eventTarget"           : "test-topic",
        "eventType"             : "org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent",
        "eventSchema"           : "urn:cps:org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent:v1",
        "eventSource"           : "org.onap.cps.ncmp.dmi",
        "response-data-schema"  : "urn:cps:org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent:v1",
        "response-status"       : "OK",
        "response-code"         : "200",
        "response-data"         : { }
    }