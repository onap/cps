.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _design:


CPS Design
##########

.. toctree::
   :maxdepth: 1

Offered APIs
============

CPS supports the public APIs listed in the link below:

:download:`OpenApi Specification <api/swagger/openapi.yml>`

Exposed API
-----------

The standard for API definition in the RESTful API world is the OpenAPI Specification (OAS).
The OAS 3, which is based on the original "Swagger Specification", is being widely used in API developments.

Specification can be accessed using following URI:

.. code-block:: bash

  “http://<hostname>:<port>/v3/api-docs?group=cps-docket”

CPS Path
========

Several CPS APIs use the cps-path (or cpsPath in Java API) parameter.
The CPS Path is described in detail in :doc:`cps-path`.
