.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2023-2025 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _dataOperationEvents:

CPS-NCMP Data Operations Events
###############################

These events are based on the cloud events standard which is a specification for describing event data in common formats to provide interoperability across services, platforms and systems.

Please refer to the `cloud events <https://cloudevents.io/>`_ for more details.

Data operation response events
******************************

:download:`Data operation event schema <schemas/ncmp/async-m2m/data-operation-event-schema-1.0.0.json>`

Event headers example
^^^^^^^^^^^^^^^^^^^^^

.. code-block:: json

    {
        "specversion":      "1.0",
        "id":               "77b8f114-4562-4069-8234-6d059ff742ac",
        "type":             "org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent",
        "source":           "DMI",
        "dataschema":       "urn:cps:org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent:1.0.0",
        "time":             "2020-12-01T00:00:00.000+0000",
        "content-type":     "application/json",
        "data":             "{some-key:some-value}",
        "correlationid":    "6ea5cb30ecfd4a938de36fdc07a5008f",
        "destination":      "client-topic"
    }

Data operation event headers
============================

    +----------------+-----------------+------------------------------------------------------------------------+
    | Field name     | Mandatory       |  Description                                                           |
    +================+=================+========================================================================+
    | specversion    | Yes             | default : 1.0                                                          |
    +----------------+-----------------+------------------------------------------------------------------------+
    | id             | Yes             | UUID                                                                   |
    +----------------+-----------------+------------------------------------------------------------------------+
    | type           | Yes             | org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent                 |
    +----------------+-----------------+------------------------------------------------------------------------+
    | source         | Yes             | NCMP / DMI                                                             |
    +----------------+-----------------+------------------------------------------------------------------------+
    | dataschema     | No              | `urn:cps:org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent:1.0.0` |
    +----------------+-----------------+------------------------------------------------------------------------+
    | time           | No              | ISO_TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"                   |
    +----------------+-----------------+------------------------------------------------------------------------+
    | content-type   | No              | default : application/json                                             |
    +----------------+-----------------+------------------------------------------------------------------------+
    | data           | Yes             | actual event/payload now would be under "data" field.                  |
    +----------------+-----------------+------------------------------------------------------------------------+
    | correlationid  | Yes             | request id                                                             |
    +----------------+-----------------+------------------------------------------------------------------------+
    | destination    | Yes             | client topic                                                           |
    +----------------+-----------------+------------------------------------------------------------------------+

