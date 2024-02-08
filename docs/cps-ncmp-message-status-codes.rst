.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2023 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _dataOperationMessageStatusCodes:


CPS-NCMP Message Status Codes
#############################

    +-----------------+------------------------------------------------------+-----------------------------------+
    | Status Code     | Status Message                                       | Feature(s)                        |
    +=================+======================================================+===================================+
    | 0               | Successfully applied changes                         | Data Operation                    |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 1               | successfully applied subscription                    | CM Data Notification Subscription |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 100             | cm handle id(s) is(are) not found                    | Data Operation, Inventory         |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 101             | cm handle(s) not ready                               | Data Operation                    |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 102             | dmi plugin service is not responding                 | Data Operation                    |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 103             | dmi plugin service is not able to read resource data | Data Operation                    |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 104             | partially applied subscription                       | CM Data Notification Subscription |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 105             | subscription not applicable for all cm handles       | CM Data Notification Subscription |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 106             | subscription pending for all cm handles              | CM Data Notification Subscription |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 107             | southbound system is busy                            | Data Operation                    |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 108             | Unknown error                                        | Inventory                         |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 109             | cm-handle already exists                             | Inventory                         |
    +-----------------+------------------------------------------------------+-----------------------------------+
    | 110             | cm-handle has an invalid character(s) in id          | Inventory                         |
    +-----------------+------------------------------------------------------+-----------------------------------+

.. note::

    - Single response format for all scenarios both positive and error, just using optional fields instead.
    - status-code 0-99 is reserved for any success response.
    - status-code from 100 to 199 is reserved for any failed response.



