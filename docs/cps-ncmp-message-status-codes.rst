.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2023-2024 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _dataOperationMessageStatusCodes:


CPS-NCMP Message Status Codes
#############################

    +-----------------+------------------------------------------------------+-----------------------------------+
    | Status Code     | Status Message                                       | Feature(s)                        |
    +=================+======================================================+===================================+
    | 0               | Successfully applied changes                         | Data Operation                    |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 1               | ACCEPTED                                             | CM Data Notification Subscription |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 100             | CM Handle id(s) is(are) not found                    | All features                      |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 101             | CM Handle(s) not ready                               | Data Operation                    |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 102             | dmi plugin service is not responding                 | Data Operation                    |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 103             | dmi plugin service is not able to read resource data | Data Operation                    |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 104             | REJECTED                                             | CM Data Notification Subscription |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 107             | southbound system is busy                            | Data Operation                    |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 108             | Unknown error                                        | All features                      |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 109             | CM Handle already exists                             | Inventory                         |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 110             | CM Handle has an invalid character(s) in id          | Inventory                         |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 111             | alternate id already associated                      | Inventory                         |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 112             | message too large                                    | Data Operation                    |
    +-----------------+------------------------------------------------------+-----------------------------------+

.. note::

    - Single response format for all scenarios both positive and error, just using optional fields instead.
    - status-code 0-99 is reserved for any success response.
    - status-code from 100 to 199 is reserved for any failed response.



