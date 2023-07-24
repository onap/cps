.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2023 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _cmhandledataoperation:


CM Handles Data Operation Endpoints
#########################

.. toctree::
   :maxdepth: 1

Introduction
============

For data operation CM Handles we have a Post endpoints:

- /ncmp/v1/data?topic={client-topic-name} forward request to it's dmi plugin service.

returns request id (UUID) with http status 202.

Request Body
============

Currently this endpoint executes data operation for given array of operations:

- *operation* (mandatory) only read operation is allowed.

- *operationId* (mandatory) unique operation id for each operation.

- *datastore* (mandatory) supports only ncmp-datastore:passthrough-operational and ncmp-datastore:passthrough-running.

- *options* (optional) options parameter in query, it is mandatory to wrap key(s)=value(s) in parenthesis'()'. The format of options parameter depend on the associated DMI Plugin implementation..

- *resourceIdentifier* (optional) The format of resource identifier depend on the associated DMI Plugin implementation. For ONAP DMI Plugin it will be RESTConf paths but it can really be anything.

- *targetIds* (mandatory) list of cm handle ids.


NCMP error codes and Http status:
----------------------------------------------------

.. note::
- 202 is non-committal, meaning that there is no way for the HTTP to later send an asynchronous response indicating the outcome of processing the request. It is intended for cases where another process or server handles the request, or for batch processing.
- Single response format for all scenarios bot positive and error, just using optional fields instead.
- status-code 0-99 is reserved for any success response
- status-code from 100 to 199 is reserved for any failed response.


Request Body example from client app to NCMP endpoint:

.. code-block:: json

    curl --location 'http: //localhost:8080/ncmp/v1/data?topic=my-topic-name' \
--header 'Content-Type: application/json' \
--header 'Authorization: Basic Y3BzdXNlcjpjcHNyMGNrcyE=' \
--data '{
    "operations": [
        {
            "operation": "read",
            "operationId": "12",
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
            "operationId": "14",
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

DMI Service 1 (POST): http://172.26.202.25:8783/dmi/v1/data?topic=my-topic-name&requestId=4753fc1f-7de2-449a-b306-a6204b5370b3

.. code-block:: json

    [
    {
        "operationType": "read",
        "operationId": "14",
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

DMI Service 2 (POST) : http://172.26.202.26:8783/dmi/v1/data?topic=my-topic-name&requestId=4753fc1f-7de2-449a-b306-a6204b5370b3

.. code-block:: json

    [
    {
        "operationType": "read",
        "operationId": "12",
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

Common NCMP response status code & message
------------------------------------------

    +-----------------+------------------------------------------------------+
    | Status Code     | Status Message                                       |                                                                                  |
    +=================+======================================================+
    | 0               | Successfully applied changes                         |
    +-----------------+------------------------------------------------------+
    | 100             | cm handle id(s) is(are) not found                    |
    +-----------------+------------------------------------------------------+
    | 101             | cm handle id(s) is(are) in non ready state           |                                                                                                 |
    +-----------------+------------------------------------------------------+
    | 102             | dmi plugin service is not responding                 |                                                                                        |
    +-----------------+------------------------------------------------------+
    | 103             | dmi plugin service is not able to read resource data |                                                                                                         |
    +-----------------+------------------------------------------------------+
