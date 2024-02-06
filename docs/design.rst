.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021-2023 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _design:


CPS Design
##########

.. toctree::
   :maxdepth: 1

Offered APIs
============

CPS supports the public APIs listed in the following sections.

CPS-Core
--------

CPS-Core functionality.

:download:`CPS Rest OpenApi Specification <api/swagger/cps/openapi.yaml>`

CPS-NCMP
--------

XNF data access and module information.

:download:`CPS NCMP RestOpenApi Specification <api/swagger/ncmp/openapi.yaml>`

CPS-NCMP-Inventory
------------------

DMI-Plugin Inventory.

:download:`CPS NCMP RestOpenApi Inventory Specification <api/swagger/ncmp/openapi-inventory.yaml>`

View Offered APIs
-----------------

The standard for API definition in the RESTful API world is the OpenAPI Specification (OAS).
The OAS 3, which is based on the original "Swagger Specification", is being widely used in API developments.

Specification can be accessed using following URI:

.. code-block:: bash

  http://<hostname>:<port>/v3/api-docs?group=cps-docket

Additionally, the Swagger User Interface can be found at the following URI. The component may be changed between CPS-Core, CPS-NCMP
and CPS-NCMP-Inventory using the drop down table in the top right:

.. code-block:: bash

  http://<hostname>:<port>/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config#/

Consumed APIs
=============

CPS Core uses API's from the following ONAP components

* DMI-Plugin: REST based interface which is used to provide integration
  and allow the DMI registry API's have access to the corresponding NCMP API's within CPS Core.
  More information on the DMI-Plugins offered APIs can be found on the :ref:`DMI-Plugin's Design Page <onap-cps-ncmp-dmi-plugin:design>`.

CPS Path
========

Several CPS APIs use the cps-path (or cpsPath in Java API) parameter.
The CPS Path is described in detail in :doc:`cps-path`.

CPS Delta
=========

CPS Delta feature provides the ability to find the delta/difference between two JSON configurations.
The CPS Delta feature is described in detail in :doc:`cps-delta-feature`.

NCMP CM Handle Querying
=======================

The CM Handle searches endpoints can be used to query for CM Handles or CM Handle IDs.
This endpoint is described in detail in :doc:`ncmp-cmhandle-querying`.

NCMP Inventory CM Handle Querying
=================================

The CM Handle searches ncmp inventory endpoints can be used to query for CM Handles or CM Handle IDs.
This endpoint is described in detail in :doc:`ncmp-inventory-querying`.

Common NCMP Response Codes
==========================

NCMP uses common responses codes in REST responses and events. Also the DMI plugin interface uses these codes which are defined here:

.. toctree::
   :maxdepth: 1

   cps-ncmp-message-status-codes.rst

Contract Testing (stubs)
========================

The CPS team is committed to supporting consumers of our APIs through contract testing.
Obviously we test our own contracts on a continuous basis as part of the build and delivery process.
CPS uses a contract-first approach. That means we write our OpenAPi contracts first and then generate the interface code from that.
This means our interface implementation simply cannot deviate from the OpenApi contracts we deliver.

Another advantage is that we can also generate 'stubs'. Stubs are a basic implementation of the same interface for testing purposes.
These stubs can be used by clients for unit testing but also for more higher level integration-like testing where the real service is replaced by a stub.
This can be useful for faster feedback loops where deployment of a full stack is difficult and not strictly needed for the purpose of the tests.

Stubs for contract testing typically always return the same response which is sufficient for the strict definition of a contract test.
However it is often useful to allow more variation in the responses so different clients or the same client can test different scenarios without having to mock the service.
CPS has implemented what we call 'extended stubs' that allow clients to provide alternate responses.implementation

The available stubs and how to use them are described in :doc:'cps-stubs'.



