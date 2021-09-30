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

CPS supports the public APIs listed in the link below:

:download:`CPS Rest OpenApi Specification <api/swagger/cps/openapi.yaml>`

:download:`CPS NCMP RestOpenApi Specification <api/swagger/ncmp/openapi.yaml>`

Exposed APIs
------------

The standard for API definition in the RESTful API world is the OpenAPI Specification (OAS).
The OAS 3, which is based on the original "Swagger Specification", is being widely used in API developments.

Specification can be accessed using following URI:

.. code-block:: bash

  http://<hostname>:<port>/v3/api-docs?group=cps-docket

Consumed APIs
-------------

CPS Core uses API's from the following ONAP components

* DMI-Plugin: REST based interface which is used to provide integration
  and allow the DMI registry API's have access to the corresponding NCMP API's within CPS Core.
  More information on the DMI Plugins offered APIs can be found on the `DMI Plugin's Design Page <https://docs.onap.org/projects/onap-cps-ncmp-dmi-plugin/en/latest/design.html>`_.

CPS Path
========

Several CPS APIs use the cps-path (or cpsPath in Java API) parameter.
The CPS Path is described in detail in :doc:`cps-path`.
