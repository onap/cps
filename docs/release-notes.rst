.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _release_notes:



=================
CPS Release Notes
=================

.. contents::
    :depth: 2
..

..      ========================
..      * * *   JAKARTA   * * *
..      ========================

Version: 2.1.0-SNAPSHOT
=======================

This section lists the main changes & fixes merged into master (snapshot) version of CPS-NCMP. This information is here to assist developers that want experiment/test using our latest code bases directly. Stability of this is not guaranteed.

Features
--------
   - `CPS-636 <https://jira.onap.org/browse/CPS-636>`_  Update operation for datastore pass through running
   - `CPS-638 <https://jira.onap.org/browse/CPS-638>`_  Delete operation for datastore pass through running
   - `CPS-741 <https://jira.onap.org/browse/CPS-741>`_  Re sync after removing cm handles

Bug Fixes
---------

   - `CPS-762 <https://jira.onap.org/browse/CPS-762>`_ Query cm handles for module names returns incorrect cm handle identifiers
   - `CPS-788 <https://jira.onap.org/browse/CPS-788>`_ Yang Resource formatting is incorrect

Known Limitations, Issues and Workarounds
-----------------------------------------

*System Limitations*

None

*Known Vulnerabilities*

None

*Workarounds*

None

Security Notes
--------------

*Fixed Security Issues*

None

*Known Security Issues*

None

..      ========================
..      * * *   ISTANBUL   * * *
..      ========================

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
