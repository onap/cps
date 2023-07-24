.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2023 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _dataoperationEvents:

CPS Data Operation Events
#########################

CPS-NCMP-Data-operation
***********************

Data-operation cloud events is a specification for describing event data in common formats to provide interoperability across services, platforms and systems..

:download:`NCMP request response event schema <schemas/data-operation-event-schema-1.0.0.json>`

Event header
^^^^^^^^^^^^^

.. code-block:: json

    {
        "specversion":      "1.0",
        "id":               "77b8f114-4562-4069-8234-6d059ff742ac",
        "type":             "org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent",
        "source":           "urn:cps:org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent:1.0.0",
        "dataschema":       "org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent",
        "time":             "2020-12-01T00:00:00.000+0000",
        "content-type":     "application/json",
        "data":             "{some-key:some-value}",
        "correlationid":    "6ea5cb30ecfd4a938de36fdc07a5008f",
        "destination":      "client-topic"
    }

Mandatory data operation event header
=====================================

    +--------------------+-----------------+
    | Status Code        | Is Mandatory ?  |
    +====================+=================+
    | (ce_)specversion   | Yes             |
    +--------------------+-----------------+
    | (ce_)id            | Yes             |
    +--------------------+-----------------+
    | (ce_)type          | Yes             |
    +--------------------+-----------------+
    | (ce_)source        | Yes             |
    +--------------------+-----------------+
    | (ce_)dataschema    | Optional        |
    +--------------------+-----------------+
    | (ce_)time          | Optional        |
    +--------------------+-----------------+
    | content-type       | Optional        |
    +--------------------+-----------------+
    | (ce_)data          | Yes             |
    +--------------------+-----------------+
    | (ce_)correlationid | Yes             |
    +--------------------+-----------------+
    | (ce_)destination   | Yes             |
    +--------------------+-----------------+

