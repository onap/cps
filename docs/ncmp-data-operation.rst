.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2023 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _cmHandleDataOperation:


CM Handles Data Operation Endpoints
###################################

.. toctree::
   :maxdepth: 1

Introduction
============

For data operation CM Handles we have a Post endpoints:

- /ncmp/v1/data?topic={client-topic-name} forward request to it's dmi plugin service.

- Returns request id (UUID) with http status 200.

Request Body
============

This endpoint executes data operation for given array of operations:

    +--------------------------+-------------+-------------------------------------------------------------------------+
    | Operation attributes     | Mandatory   |  Description                                                            |
    +==========================+=============+=========================================================================+
    | operation                | Yes         | Only read operation is allowed.                                         |
    +--------------------------+-------------+-------------------------------------------------------------------------+
    | operationId              | Yes         | Unique operation id for each operation.                                 |
    +--------------------------+-------------+-------------------------------------------------------------------------+
    | datastore                | Yes         | Supports only ncmp-datastore:passthrough-operational and                |
    |                          |             | ncmp-datastore:passthrough-running.                                     |
    +--------------------------+-------------+-------------------------------------------------------------------------+
    | options                  | No          | It is mandatory to wrap key(s)=value(s) in parenthesis'()'. The format  |
    |                          |             | of options parameter depend on the associated DMI Plugin implementation.|
    +--------------------------+-------------+-------------------------------------------------------------------------+
    | resourceIdentifier       | No          | The format of resource identifier depend on the associated DMI Plugin   |
    |                          |             | implementation. For ONAP DMI Plugin it will be RESTConf paths but it can|
    |                          |             | really be anything.                                                     |
    +--------------------------+-------------+-------------------------------------------------------------------------+
    | targetIds                | Yes         | List of cm handle ids.                                                  |
    +--------------------------+-------------+-------------------------------------------------------------------------+

The status codes used in the events resulting from these operations are defined here:

.. toctree::
   :maxdepth: 1

   cps-ncmp-message-status-codes.rst

Request Body example from client app to NCMP endpoint:

.. code-block:: bash

    curl --location 'http: //{ncmp-host-name}:{ncmp-port}/ncmp/v1/data?topic=my-topic-name' \
    --header 'Content-Type: application/json' \
    --header 'Authorization: Basic Y3BzdXNlcjpjcHNyMGNrcyE=' \
    --data '{
    "operations": [
        {
            "operation": "read",
            "operationId": "operational-12",
            "datastore": "ncmp-datastore:passthrough-operational",
            "options": "some option",
            "resourceIdentifier": "parent/child",
            "targetIds": [
                "836bb62201f34a7aa056a47bd95a81ed",
                "202acb75b4a54e43bb1ff8c0c17a8e08"
            ]
        },
        {
            "operation": "read",
            "operationId": "running-14",
            "datastore": "ncmp-datastore:passthrough-running",
            "targetIds": [
                "ec2e9495679a43c58659c07d87025e72",
                "0df4d39af6514d99b816758148389cfd"
            ]
        }
    ]
    }'


DMI service batch endpoint
--------------------------

DMI Service 1 (POST): `http://{dmi-host-name}:{dmi-port}/dmi/v1/data?topic=my-topic-name&requestId=4753fc1f-7de2-449a-b306-a6204b5370b3`

.. code-block:: json

    [
    {
        "operationType": "read",
        "operationId": "running-14",
        "datastore": "ncmp-datastore:passthrough-running",
        "cmHandles": [
            {
                "id": "ec2e9495679a43c58659c07d87025e72",
                "cmHandleProperties": {
                    "neType": "RadioNode"
                }
            },
            {
                "id": "0df4d39af6514d99b816758148389cfd",
                "cmHandleProperties": {
                    "neType": "RadioNode"
                }
            }
        ]
    }
    ]

DMI Service 2 (POST) : `http://{dmi-host-name}:{dmi-port}/dmi/v1/data?topic=my-topic-name&requestId=4753fc1f-7de2-449a-b306-a6204b5370b3`

.. code-block:: json

    [
    {
        "operationType": "read",
        "operationId": "operational-12",
        "datastore": "ncmp-datastore:passthrough-operational",
        "options": "some option",
        "resourceIdentifier": "parent/child",
        "cmHandles": [
            {
                "id": "836bb62201f34a7aa056a47bd95a81ed",
                "cmHandleProperties": {
                    "neType": "RadioNode"
                }
            },
            {
                "id": "202acb75b4a54e43bb1ff8c0c17a8e08",
                "cmHandleProperties": {
                    "neType": "RadioNode"
                }
            }
        ]
    }
    ]

Above examples are for illustration purpose only please refer link below for latest schema.

:download:`Data operation event schema <schemas/data-operation-event-schema-1.0.0.json>`