.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2024 TechMahindra Ltd.
.. _cpsDeltaEndpoints:

.. toctree::
   :maxdepth: 1

CPS Delta Endpoints
###################

The CPS Delta feature provides two endpoints:

1. GET /v2/dataspaces/{dataspace-name}/anchors/{source-anchor-name}/delta
2. POST /v2/dataspaces/{dataspace-name}/anchors/{source-anchor-name}/delta

Common Path Parameters
~~~~~~~~~~~~~~~~~~~~~~
Both the endpoints takes 2 path parameters as input:
    - **dataspace-name:** name of dataspace where the anchor(s) to be used for delta generation are stored.
    - **source-anchor-name:** the source anchor name, the data under this anchor will be the reference data for delta report generation

Common Query Parameter
~~~~~~~~~~~~~~~~~~~~~~
Both the endpoint need the following query parameters as input:
    - **xpath:** the xpath to a particular data node, Example: /bookstore/categories[@code='1']

Description of API 1: Delta between 2 Anchors
---------------------------------------------
The first API performs a GET operation, which allows the user to find the delta between configurations stored under two anchors within the same dataspace. The API has following input parameters:

Query Parameters
~~~~~~~~~~~~~~~~
The endpoint takes following query parameters as input:
    - **target-anchor-name:** the data retrieved from target anchor is compared against the data retrieved from source anchor
    - **descendants:** specifies the number of descendants to query.

Description of API 2: Delta between Anchor and JSON payload
-----------------------------------------------------------
The second API performs a POST operation, which allows the user to find the delta between configuration stored under an anchors and a JSON payload provided as part of the request. The API has following input parameters:

Request Body for Endpoint 2
~~~~~~~~~~~~~~~~~~~~~~~~~~~
The endpoint accepts a **multipart/form-data** input as part of request body. This allows the user to provide the following inputs as part of request body:
    - **JSON payload:** this field acceps a valid JSON string as an input. The data provided as part of this JSON will be parsed using the schema, the schema is either retrieved using the anchor name or it can be provided as part of request body using the optional parameter of request body defined below. Once the JSON is parsed and validated, it is compared to the data fetched from the source anchor and the delta report is generated.
    - **schema-context:** this is an optional parameter and allows the user to provide the schema of the JSON payload, as a yang or zip file, and this schema can be used to parse the JSON string in case the schema of JSON differs from the schema associated with source anchor. If the schema of JSON payload is similar to the schema associated with the anchor then this parameter can be left empty.

Sample Delta Report
-------------------
Both the APIs have the same format for the delta report. The format is as follows:

.. code-block:: json

    [
      {
        "action": "create",
        "xpath": "/bookstore/categories/[@code=3]",
        "target-data": {
          "code": "3,",
          "name": "kidz"
        }
      },
      {
        "action": "remove",
        "xpath": "/bookstore/categories/[@code=1]",
        "source-data": {
          "code": "1,",
          "name": "Fiction"
        }
      },
      {
        "action": "replace",
        "xpath": "/bookstore/categories/[@code=2]",
        "source-data": {
          "name": "Funny"
        },
        "target-data": {
          "name": "Comic"
        }
      }
    ]