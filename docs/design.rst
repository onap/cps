.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021 Nordix Foundation

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
