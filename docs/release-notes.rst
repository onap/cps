.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021-2022 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _release_notes:

CPS Release Notes
#################

.. contents::
    :depth: 2
..

..      ====================
..      * * *   KOHN   * * *
..      ====================

Version: 3.1.3
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.1.3                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.1.3 Kohn                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | (yet to release)                                       |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
   - None

Version: 3.1.2
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.1.2                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.1.2 Kohn                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2022 September 28                                      |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
   - None

Version: 3.1.1
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.1.1                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.1.1 Kohn                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2022 September 28                                      |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
   - None

Security Notes
--------------

*Fixed Security Issues*

   - `CPS-1226 <https://jira.onap.org/browse/CPS-1226>`_  Security bug in the logs

Version: 3.1.0
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.1.0                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.1.0 Kohn                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2022 September 14                                      |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Features
--------
   - `CPS-340 <https://jira.onap.org/browse/CPS-340>`_  Patch and update the root data node
   - `CPS-575 <https://jira.onap.org/browse/CPS-575>`_  Write data for cmHandle using ncmp-datastores:passthrough-running (NCMP.)
   - `CPS-731 <https://jira.onap.org/browse/CPS-731>`_  Query based on Public CM Properties
   - `CPS-828 <https://jira.onap.org/browse/CPS-828>`_  Async: NCMP Rest impl. including Request ID generation
   - `CPS-829 <https://jira.onap.org/browse/CPS-829>`_  Async: Internal message topic incl. basic producer & Consumer
   - `CPS-830 <https://jira.onap.org/browse/CPS-830>`_  DMI-NCMP Asynchronously Publish Response Event to Client Topic
   - `CPS-869 <https://jira.onap.org/browse/CPS-869>`_  Apply Standardized logging fields to adhere to ONAP Best practice REQ-1072
   - `CPS-870 <https://jira.onap.org/browse/CPS-870>`_  Align CPS-Core output with SDN-C output (add module name)
   - `CPS-875 <https://jira.onap.org/browse/CPS-875>`_  CM Handle State: Watchdog-process that syncs 'ADVISED' CM Handles
   - `CPS-877 <https://jira.onap.org/browse/CPS-877>`_  CM Handle State: Exclude any CM-Handles from queries/operations that are not in state 'READY'
   - `CPS-899 <https://jira.onap.org/browse/CPS-899>`_  Start and stop sessions on Java API
   - `CPS-909 <https://jira.onap.org/browse/CPS-909>`_  Separate NCMP endpoint for ch/{cm-handle}/properties and ch/{cm-handle}/state
   - `CPS-917 <https://jira.onap.org/browse/CPS-917>`_  Structured Errors response for passthrough use-cases in NCMP
   - `CPS-953 <https://jira.onap.org/browse/CPS-953>`_  Update maven deploy plugin version
   - `CPS-977 <https://jira.onap.org/browse/CPS-977>`_  Query CM Handles using CpsPath
   - `CPS-1000 <https://jira.onap.org/browse/CPS-1000>`_  Create Data Synchronization watchdog
   - `CPS-1016 <https://jira.onap.org/browse/CPS-1016>`_  Merge 2 'query' end points in NCMP
   - `CPS-1034 <https://jira.onap.org/browse/CPS-1034>`_  Publish lifecycle events for ADVISED , READY and LOCKED state transition"
   - `CPS-1064 <https://jira.onap.org/browse/CPS-1064>`_  Support retrieval of YANG module sources for CM handle on the NCMP interface
   - `CPS-1099 <https://jira.onap.org/browse/CPS-1099>`_  Expose simplified 'external' lock reason enum state over REST interface
   - `CPS-1101 <https://jira.onap.org/browse/CPS-1101>`_  Introducing the DELETING and DELETED Cmhandle State
   - `CPS-1102 <https://jira.onap.org/browse/CPS-1102>`_  Register the Cmhandle Sends Advised State notification.
   - `CPS-1133 <https://jira.onap.org/browse/CPS-1133>`_  Enable/Disable Data Sync for Cm Handle
   - `CPS-1136 <https://jira.onap.org/browse/CPS-1136>`_  DMI Audit Support (get all CM Handles for a registered DMI)


Bug Fixes
---------
   - `CPS-896 <https://jira.onap.org/browse/CPS-896>`_  CM Handle Registration Process only partially completes when exception is thrown
   - `CPS-957 <https://jira.onap.org/browse/CPS-957>`_  NCMP: fix getResourceDataForPassthroughOperational endpoint
   - `CPS-1020 <https://jira.onap.org/browse/CPS-1020>`_  DuplicatedYangResourceException error at parallel cmHandle registration
   - `CPS-1056 <https://jira.onap.org/browse/CPS-1056>`_  Wrong error response format in case of Dmi plugin error
   - `CPS-1067 <https://jira.onap.org/browse/CPS-1067>`_  NCMP returns 500 error on searches endpoint when No DMi Handles registered
   - `CPS-1085 <https://jira.onap.org/browse/CPS-1085>`_  Performance degradation on ncmp/v1/ch/searches endpoint
   - `CPS-1088 <https://jira.onap.org/browse/CPS-1088>`_  Kafka consumer can not be turned off
   - `CPS-1097 <https://jira.onap.org/browse/CPS-1097>`_  Unable to change state from LOCKED to ADVISED
   - `CPS-1126 <https://jira.onap.org/browse/CPS-1126>`_  CmHandle creation performance degradation
   - `CPS-1175 <https://jira.onap.org/browse/CPS-1175>`_  Incorrect response when empty body executed for cmhandle id-searches
   - `CPS-1179 <https://jira.onap.org/browse/CPS-1179>`_  Node API - GET method returns invalid response when identifier contains '/'
   - `CPS-1212 <https://jira.onap.org/browse/CPS-1212>`_  Additional Properties for CM Handles not included when send to DMI Plugin
   - `CPS-1217 <https://jira.onap.org/browse/CPS-1217>`_  Searches endpoint gives back empty list however there are already available cmhandles
   - `CPS-1218 <https://jira.onap.org/browse/CPS-1218>`_  NCMP logs are flooded with SyncUtils logs

Known Limitations, Issues and Workarounds
-----------------------------------------

*System Limitations*

Having '[' token in any index in any list will have a negative impact on the search functions leaf-conditions and text()-condition.
Example of an xpath that would cause problems while using cps-path queries : /parent/child[@id='id[with]braces']

*Known Vulnerabilities*

None

*Workarounds*

None

Security Notes
--------------

*Fixed Security Issues*

   - `CPS-963 <https://jira.onap.org/browse/CPS-963>`_  Liquibase has got serious vulnerability, upgrade required

*Known Security Issues*

None

..      ========================
..      * * *   JAKARTA   * * *
..      ========================

Version: 3.0.1
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.0.1                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.0.1 Jakarta                                          |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2022 April 28                                          |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
   - `CPS-961 <https://jira.onap.org/browse/CPS-961>`_  Updated ANTLR compiler version to 4.9.2 to be compatible with runtime version

Version: 3.0.0
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.0.0                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.0.0 Jakarta                                          |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2022 March 15                                          |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Features
--------
   - `CPS-559 <https://jira.onap.org/browse/CPS-559>`_  Define response objects (schemas) in cps-ncmp
   - `CPS-636 <https://jira.onap.org/browse/CPS-636>`_  Update operation for datastore pass through running
   - `CPS-638 <https://jira.onap.org/browse/CPS-638>`_  Delete operation for datastore pass through running
   - `CPS-677 <https://jira.onap.org/browse/CPS-677>`_  Support 'public' Cm Handle Properties
   - `CPS-741 <https://jira.onap.org/browse/CPS-741>`_  Re sync after removing cm handles
   - `CPS-777 <https://jira.onap.org/browse/CPS-777>`_  Ensure all DMI operations use POST method
   - `CPS-780 <https://jira.onap.org/browse/CPS-780>`_  Add examples for parameters, request and response in openapi yaml for cps-core
   - `CPS-789 <https://jira.onap.org/browse/CPS-789>`_ CPS Data Updated Event Schema V2 to support delete operation
   - `CPS-791 <https://jira.onap.org/browse/CPS-791>`_ CPS-Core sends delete notification event
   - `CPS-817 <https://jira.onap.org/browse/CPS-817>`_  Create Endpoint For Get Cm Handles (incl. public properties) By Name
   - `CPS-837 <https://jira.onap.org/browse/CPS-837>`_  Add Remove and Update properties (DMI and Public) as part of CM Handle Registration update

Bug Fixes
---------

   - `CPS-762 <https://jira.onap.org/browse/CPS-762>`_ Query cm handles for module names returns incorrect cm handle identifiers
   - `CPS-788 <https://jira.onap.org/browse/CPS-788>`_ Yang Resource formatting is incorrect
   - `CPS-783 <https://jira.onap.org/browse/CPS-783>`_ Remove cm handle does not completely remove all cm handle information
   - `CPS-841 <https://jira.onap.org/browse/CPS-841>`_ Upgrade log4j to 2.17.1 as recommended by ONAP SECCOM
   - `CPS-856 <https://jira.onap.org/browse/CPS-856>`_ Retry mechanism not working for concurrent CmHandle registration
   - `CPS-867 <https://jira.onap.org/browse/CPS-867>`_ Database port made configurable through env variable DB_PORT
   - `CPS-886 <https://jira.onap.org/browse/CPS-886>`_ Fragment handling decreasing performance for large number of cmHandles
   - `CPS-887 <https://jira.onap.org/browse/CPS-887>`_ Increase performance of cmHandle registration for large number of schema sets in DB
   - `CPS-892 <https://jira.onap.org/browse/CPS-892>`_ Fixed the response code during CM-Handle Registration from 201 CREATED to 204 NO_CONTENT
   - `CPS-893 <https://jira.onap.org/browse/CPS-893>`_ NCMP Java API depends on NCM-Rest-API (cyclic) through json properties on Java API

Known Limitations, Issues and Workarounds
-----------------------------------------

*System Limitations*

Null can no longer be passed within the dmi plugin service names when registering a cm handle, as part of
`CPS-837 <https://jira.onap.org/browse/CPS-837>`_ null is now used to indicate if a property should be removed as part
of cm handle registration.

The Absolute path to list with integer key will not work. Please refer `CPS-961 <https://jira.onap.org/browse/CPS-961>`_
for more information.

*Known Vulnerabilities*

None

*Workarounds*

Instead of passing null as a value within the dmi plugin service names, remove them from the request completely, or
pass an empty string as the value if you do not want to include names for these values.

Security Notes
--------------

*Fixed Security Issues*

None

*Known Security Issues*

None

..      ========================
..      * * *   ISTANBUL   * * *
..      ========================

Version: 2.0.4
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:2.0.4                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 2.0.4 Istanbul                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2022 Feb 09                                            |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------

   - `CPS-879 <https://jira.onap.org/browse/CPS-879>`_  Fix docker compose for csit test.
   - `CPS-873 <https://jira.onap.org/browse/CPS-873>`_  Fix intermittent circular dependency error when the application starts.

Version: 2.0.3
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:2.0.3                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 2.0.3 Istanbul                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2022-07-01                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------

   - `CPS-841 <https://jira.onap.org/browse/CPS-841>`_  Update log4j version to 2.17.1 due to security vulnerability

Version: 2.0.2
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:2.0.2                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 2.0.2 Istanbul                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2021-16-12                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------

   - `CPS-820 <https://jira.onap.org/browse/CPS-820>`_  Update log4j version due to security vulnerability

Version: 2.0.1
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:2.0.1                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 2.0.1 Istanbul                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2021-14-10                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------

   - `CPS-594 <https://jira.onap.org/browse/CPS-594>`_ SQL ConstraintViolationException when updating the list node element using PATCH List node API
   - `CPS-653 <https://jira.onap.org/browse/CPS-653>`_ cmHandleProperties not supported by dmi in fetch modules
   - `CPS-673 <https://jira.onap.org/browse/CPS-673>`_ Improvement and cleanup for CPS Core charts
   - `CPS-691 <https://jira.onap.org/browse/CPS-691>`_ NCMP no master index label on index documentation page

Known Limitations, Issues and Workarounds
-----------------------------------------

*System Limitations*

Limitations to the amount of child nodes that can be added to the fix above. The current limit is 3.

*Known Vulnerabilities*

   - `CPS-725 <https://jira.onap.org/browse/CPS-725>`_ fix sample docker compose of cps/ncmp and onap dmi plugin

*Workarounds*

Add recursive method to save list node data to loop through all corresponding child nodes.

Security Notes
--------------

*Fixed Security Issues*

   - `CPS-581 <https://jira.onap.org/browse/CPS-581>`_ Remove security vulnerabilities

*Known Security Issues*

None

Version: 2.0.0
==============

Abstract
--------

This document provides the release notes for Istanbul release.

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:2.0.0                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 2.0.0 Istanbul                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2021-14-09                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Features
--------
* Register DMI-Plugins with NCMP for CM Handle registrations.
* Update, Create and Remove CM Handles.
* Add support for retrieving and writing CM Handle data through NCMP datastores.
* Automatic retrieval and caching of model information for CM Handles within NCMP.

Deliverables
------------

Software Deliverables

.. csv-table::
   :header: "Repository", "SubModules", "Version & Docker Image (if applicable)"
   :widths: auto

   "cps", "", "onap/cps-and-ncmp-proxy:2.0.0"

Bug Fixes
---------

   - `CPS-310 <https://jira.onap.org/browse/CPS-310>`_ Data schema migration from Honolulu to Istanbul is failing
   - `CPS-316 <https://jira.onap.org/browse/CPS-316>`_ Xpath cannot be created for augmentation data node
   - `CPS-336 <https://jira.onap.org/browse/CPS-336>`_ Ends-with functionality in cpsPath does not conform with standard xPath behavior
   - `CPS-345 <https://jira.onap.org/browse/CPS-345>`_ Leaf String value comparison matches mix of single and double quotes
   - `CPS-357 <https://jira.onap.org/browse/CPS-357>`_ cps-review-verification-maven-master Jenkins job is failing when running csit test
   - `CPS-367 <https://jira.onap.org/browse/CPS-367>`_ Get descendent does not support xpaths that end in list values
   - `CPS-377 <https://jira.onap.org/browse/CPS-377>`_ Init ran model validation is failing error details are not provided
   - `CPS-422 <https://jira.onap.org/browse/CPS-422>`_ REST 404 response returned instead of 400 for POST/PUT/PATCH request types
   - `CPS-450 <https://jira.onap.org/browse/CPS-450>`_ Datanode query using full path to node causes NPE
   - `CPS-451 <https://jira.onap.org/browse/CPS-451>`_ cps-ran-schema-model@2021-01-28.yang missing root container
   - `CPS-464 <https://jira.onap.org/browse/CPS-464>`_ Request to update node leaves (patch) responds with Internal Server Error
   - `CPS-465 <https://jira.onap.org/browse/CPS-465>`_ Request to update node leaves (patch) responds with json parsing failure
   - `CPS-466 <https://jira.onap.org/browse/CPS-466>`_ Concurrent requests to create schema sets for the same yang model are not supported
   - `CPS-479 <https://jira.onap.org/browse/CPS-479>`_ Get Nodes API does not always return the object from the root
   - `CPS-500 <https://jira.onap.org/browse/CPS-500>`_ Special Character Limitations of cpsPath Queries
   - `CPS-501 <https://jira.onap.org/browse/CPS-501>`_ Put DataNode API has missing transaction and error handling for concurrency issues
   - `CPS-524 <https://jira.onap.org/browse/CPS-524>`_ Issue with CPSData API to add an item to an existing list node
   - `CPS-560 <https://jira.onap.org/browse/CPS-560>`_ Response from cps query using text() contains escape characters
   - `CPS-566 <https://jira.onap.org/browse/CPS-566>`_ Can't access grandparent node through ancestor axis
   - `CPS-573 <https://jira.onap.org/browse/CPS-573>`_ /v1/ch/PNFDemo1/modules returning 401 unauthorised.
   - `CPS-587 <https://jira.onap.org/browse/CPS-587>`_ cps-ncmp-service NullpointerException when DmiPluginRegistration has no additionProperties
   - `CPS-591 <https://jira.onap.org/browse/CPS-591>`_ CPS-Core Leaf stored as integer is being returned from DB as float
   - `CPS-601 <https://jira.onap.org/browse/CPS-601>`_ CPS swagger-ui does not show NCMP endpoints
   - `CPS-616 <https://jira.onap.org/browse/CPS-616>`_ NCMP base path does not conform to agreed API URL
   - `CPS-630 <https://jira.onap.org/browse/CPS-630>`_ Incorrect information sent when same anchor is updated faster than notification service processes
   - `CPS-635 <https://jira.onap.org/browse/CPS-635>`_ Module Resource call does not include body

This document provides the release notes for Istanbul release.

Summary
-------

Following CPS components are available with default ONAP/CPS installation.


    * Platform components

        - CPS (Helm charts)

    * Service components

        - CPS-NCMP
        - CPS-Temporal
        - DMI-Plugin

    * Additional resources that CPS utilizes deployed using ONAP common charts

        - Postgres Database


Below service components (mS) are available to be deployed on-demand.
    - CPS-TBDMT


Under OOM (Kubernetes) all CPS component containers are deployed as Kubernetes Pods/Deployments/Services into Kubernetes cluster.

Known Limitations, Issues and Workarounds
-----------------------------------------

*System Limitations*

Limitations to the amount of child nodes that can be added to the fix above. The current limit is 3.

*Known Vulnerabilities*

   - `CPS-594 <https://jira.onap.org/browse/CPS-594>`_ SQL ConstraintViolationException when updating the list node element using PATCH List node API
   - `CPS-653 <https://jira.onap.org/browse/CPS-653>`_ cmHandleProperties not supported by dmi in fetch modules
   - `CPS-673 <https://jira.onap.org/browse/CPS-673>`_ Improvement and cleanup for CPS Core charts

*Workarounds*

Add recursive method to save list node data to loop through all corresponding child nodes.

Security Notes
--------------

*Fixed Security Issues*

   - `CPS-249 <https://jira.onap.org/browse/CPS-249>`_ Exception stack trace is exposed

*Known Security Issues*

   - `CPS-581 <https://jira.onap.org/browse/CPS-581>`_ Remove security vulnerabilities

Test Results
------------
    * `Integration tests`

..      ========================
..      * * *   HONOLULU   * * *
..      ========================

Version: 1.0.1
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-nf-proxy:1.0.1                            |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 1.0.1 Honolulu                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2021-04-09                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------

   - `CPS-706 <https://jira.onap.org/browse/CPS-706>`_ Get moduleschema/yangresouce endpoint not working
   - `CPS-276 <https://jira.onap.org/browse/CPS-276>`_ Improve error reporting for invalid cpsPath on Queries
   - `CPS-288 <https://jira.onap.org/browse/CPS-288>`_ Move security configuration to the application module
   - `CPS-290 <https://jira.onap.org/browse/CPS-290>`_ Internal Server Error when creating the same data node twice
   - `CPS-292 <https://jira.onap.org/browse/CPS-292>`_ Detailed information is missing to explain why data is not compliant with the specified YANG model
   - `CPS-300 <https://jira.onap.org/browse/CPS-304>`_ Not able to create data instances for 2 different anchors using the same model
   - `CPS-304 <https://jira.onap.org/browse/CPS-304>`_ Use ONAP recommended base Java Docker image
   - `CPS-308 <https://jira.onap.org/browse/CPS-308>`_ Not able to upload yang models files greater than 1MB

Security Notes
--------------

*Fixed Security Issues*

   - `CPS-249 <https://jira.onap.org/browse/CPS-249>`_ Exception stack trace is exposed

*Known Security Issues*

   - `Security Waiver <https://wiki.onap.org/display/DW/Honolulu+Exception+Request+for+CPS>`_ Security - Expose external endpoints with https

Version: 1.0.0
==============


Abstract
--------

This document provides the release notes for Honolulu release.

Summary
-------

Following CPS components are available with default ONAP/CPS installation.


    * Platform components

        - CPS (Helm charts)

    * Service components

        - CPS Core

    * Additional resources that CPS utilizes deployed using ONAP common charts

        - Postgres Database


Below service components (mS) are available to be deployed on-demand.
    - CPS-TBDMT


Under OOM (Kubernetes) all CPS component containers are deployed as Kubernetes Pods/Deployments/Services into Kubernetes cluster.


Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | Refer :any:`Deliverable <honolulu_deliverable>`        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 1.0.0 Honolulu                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2021-03-11                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+


Features
--------
Configuration Persistence Service is a model driven persistence solution for data described by YANG models.
CPS has been driven by the needs of the E2E Networking Slicing use case.
It currently supports basic (rw) persistence and simple queries.
It also provides MVP support for network data access using simulated data.

.. _honolulu_deliverable:

Deliverables
------------

Software Deliverables

.. csv-table::
   :header: "Repository", "SubModules", "Version & Docker Image (if applicable)"
   :widths: auto

   "cps", "", "onap/cps-and-nf-proxy:1.0.0"


Known Limitations, Issues and Workarounds
-----------------------------------------

   - `CPS-249 <https://jira.onap.org/browse/CPS-249>`_ Exception stack trace is exposed
   - `CPS-264 <https://jira.onap.org/browse/CPS-264>`_ Unique timestamp is missing when tagging docker images.
   - Methods exposed on API which are yet not implemented : deleteAnchor, getNodesByDataspace & deleteDataspace.
   - `CPS-465 <https://jira.onap.org/browse/CPS-465>`_ & `CPS-464 <https://jira.onap.org/browse/CPS-464>`_ Update data node leaves API does not support updating a list element with compound keys.

*System Limitations*

None

*Known Vulnerabilities*

None

*Workarounds*

Documented under corresponding jira if applicable.

Security Notes
--------------

*Fixed Security Issues*

* `CPS-167 <https://jira.onap.org/browse/CPS-167>`_ -Update CPS dependencies as Required for Honolulu release
    - Upgrade org.onap.oparent to 3.2.0
    - Upgrade spring.boot to 2.3.8.RELEASE
    - Upgrade yangtools to 5.0.7

*Known Security Issues*

    * Weak Crytography using md5
    * Risk seen in Zip file expansion

*Known Vulnerabilities in Used Modules*

    None

CPS code has been formally scanned during build time using NexusIQ and all Critical vulnerabilities have been addressed, items that remain open have been assessed for risk and determined to be false positive.

Test Results
------------
    * `Integration tests <https://wiki.onap.org/display/DW/CPS+Integration+Test+Cases>`_

References
----------

For more information on the ONAP Honolulu release, please see:

#. `ONAP Home Page`_
#. `ONAP Documentation`_
#. `ONAP Release Downloads`_
#. `ONAP Wiki Page`_


.. _`ONAP Home Page`: https://www.onap.org
.. _`ONAP Wiki Page`: https://wiki.onap.org
.. _`ONAP Documentation`: https://docs.onap.org
.. _`ONAP Release Downloads`: https://git.onap.org

Quick Links:

        - `CPS project page <https://wiki.onap.org/pages/viewpage.action?pageId=71834216>`_
        - `Passing Badge information for CPS <https://bestpractices.coreinfrastructure.org/en/projects/4398>`_
