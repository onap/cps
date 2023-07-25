.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2023 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _dataOperationMessageStatusCodes:


CPS-NCMP Message Status Codes
#############################

    +-----------------+------------------------------------------------------+
    | Status Code     | Status Message                                       |
    +=================+======================================================+
    | 0               | Successfully applied changes                         |
    +-----------------+------------------------------------------------------+
    | 100             | cm handle id(s) is(are) not found                    |
    +-----------------+------------------------------------------------------+
    | 101             | cm handle id(s) is(are) in non ready state           |
    +-----------------+------------------------------------------------------+
    | 102             | dmi plugin service is not responding                 |
    +-----------------+------------------------------------------------------+
    | 103             | dmi plugin service is not able to read resource data |
    +-----------------+------------------------------------------------------+

.. note::

    - 202 is non-committal, meaning that there is no way for the HTTP to later send an asynchronous response indicating the outcome of processing the request. It is intended for cases where another process or server handles the request, or for batch processing.
    - Single response format for all scenarios bot positive and error, just using optional fields instead.
    - status-code 0-99 is reserved for any success response.
    - status-code from 100 to 199 is reserved for any failed response.



