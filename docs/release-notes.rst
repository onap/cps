.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021-2025 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _release_notes:

CPS Release Notes
#################

.. contents::
    :depth: 2
..
..      ====================
..      * * *   PARIS   * * *
..      ====================

Version: 3.6.1
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.6.1                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.6.1 Paris                                            |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | Not yet released                                       |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Features
--------

Version: 3.6.0
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.6.0                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.6.0 Paris                                            |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2025 January 29                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-2563 <https://lf-onap.atlassian.net/browse/CPS-2563>`_ Fix for internal error code during duplicated registration.
    - `CPS-2576 <https://lf-onap.atlassian.net/browse/CPS-2576>`_ Fix for cm handle stuck in LOCKED state during registration.

Features
--------
    - `CPS-2249 <https://lf-onap.atlassian.net/browse/CPS-2249>`_ NCMP to support Conflict Handling.
    - `CPS-2540 <https://lf-onap.atlassian.net/browse/CPS-2540>`_ One schemaset per module set tag.


..      ====================
..      * * *   OSLO   * * *
..      ====================

Version: 3.5.5
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.5.5                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.5.5 Oslo                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 November 29                                       |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-2509 <https://lf-onap.atlassian.net/browse/CPS-2509>`_ Fix module endpoints using alternate identifier.
    - `CPS-2517 <https://lf-onap.atlassian.net/browse/CPS-2517>`_ Make Content-Type header default to JSON for CPS APIs.
    - `CPS-2530 <https://lf-onap.atlassian.net/browse/CPS-2530>`_ NCMP Modules API giving empty response on READY CM Handles if two sub systems discovered in parallel.

Features
--------
    - `CPS-2009 <https://lf-onap.atlassian.net/browse/CPS-2009>`_ Update legacy NCMP APIs interfaces to support alternate id.
    - `CPS-2082 <https://lf-onap.atlassian.net/browse/CPS-2082>`_ Support XML content type to data node APIs in cps-core.
    - `CPS-2433 <https://lf-onap.atlassian.net/browse/CPS-2433>`_ Remove traces of unmaintained CPS-TBDMT repository.
    - `CPS-2436 <https://lf-onap.atlassian.net/browse/CPS-2436>`_ CM Avc Event to publish source key to target key while forwarding.
    - `CPS-2445 <https://lf-onap.atlassian.net/browse/CPS-2445>`_ Expose CPS and NCMP version information using git plugin.
    - `CPS-2451 <https://lf-onap.atlassian.net/browse/CPS-2451>`_ Removing oparent from CPS-NCMP and ONAP DMI Plugin repository.
    - `CPS-2478 <https://lf-onap.atlassian.net/browse/CPS-2478>`_ Optimized CM Handle Registration and De-Registration use case.
    - `CPS-2507 <https://lf-onap.atlassian.net/browse/CPS-2507>`_ Upgrade liquibase to 4.30.0 version.

Performance
-----------
The OSLO delivery brought major performance enhancements by streamlining module sync algorithm. It also optimized caching technology, improving instance efficiency and connection management.

Version: 3.5.4
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.5.4                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.5.4 Oslo                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 October 17                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-2403 <https://lf-onap.atlassian.net/browse/CPS-2403>`_ Improve lock handling and queue management during CM-handle Module Sync.

Features
--------
    - `CPS-2408 <https://lf-onap.atlassian.net/browse/CPS-2408>`_ One Hazelcast instance per JVM to manage the distributed data structures.

Version: 3.5.3
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.5.3                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.5.3 Oslo                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 October 04                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-2353 <https://lf-onap.atlassian.net/browse/CPS-2353>`_ Slow cmHandle registration when we use moduleSetTag, alternateId and dataProducerIdentifier
    - `CPS-2395 <https://lf-onap.atlassian.net/browse/CPS-2395>`_ Retry mechanism (with back off algorithm) is removed with more frequent watchdog poll
    - `CPS-2409 <https://lf-onap.atlassian.net/browse/CPS-2409>`_ Return NONE for get effective trust level api if the trust level caches empty (restart case)
    - `CPS-2430 <https://lf-onap.atlassian.net/browse/CPS-2430>`_ Fix memory leak related to using arrays in Hibernate


Features
--------
    - `CPS-2417 <https://lf-onap.atlassian.net/browse/CPS-2417>`_ Remove Hazelcast cache for prefix resolver


Version: 3.5.2
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.5.2                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.5.2 Oslo                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 August 21                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-2306 <https://lf-onap.atlassian.net/browse/CPS-2306>`_ Update response message for data validation failure and make it consistent across APIs
    - `CPS-2319 <https://lf-onap.atlassian.net/browse/CPS-2319>`_ Fix "Create a node" and "Add List Elements" APIs response code
    - `CPS-2372 <https://lf-onap.atlassian.net/browse/CPS-2372>`_ Blank alternate ID overwrites existing one

Features
--------
    - `CPS-1812 <https://lf-onap.atlassian.net/browse/CPS-1812>`_ CM Data Subscriptions ( Create, Delete and Merging ) with positive scenarios
    - `CPS-2326 <https://lf-onap.atlassian.net/browse/CPS-2326>`_ Uplift liquibase-core dependency to 4.28.0
    - `CPS-2353 <https://lf-onap.atlassian.net/browse/CPS-2353>`_ Improve registration performance with moduleSetTag
    - `CPS-2366 <https://lf-onap.atlassian.net/browse/CPS-2366>`_ Improve registration performance with use of alternateID

Version: 3.5.1
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.5.1                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.5.1 Oslo                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 July 15                                           |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-2302 <https://lf-onap.atlassian.net/browse/CPS-2302>`_ Fix handling of special characters in moduleSetTag.

Features
--------
    - `CPS-2121 <https://lf-onap.atlassian.net/browse/CPS-2121>`_ Enabled http client prometheus metrics and manage high cardinality using URL template.
    - `CPS-2289 <https://lf-onap.atlassian.net/browse/CPS-2289>`_ Support for CPS Path Query in NCMP Inventory CM Handle Search.

Version: 3.5.0
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.5.0                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.5.0 Oslo                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 June 20                                           |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Features
--------
    - `CPS-989 <https://lf-onap.atlassian.net/browse/CPS-989>`_ Replace RestTemplate with WebClient.
    - `CPS-2172 <https://lf-onap.atlassian.net/browse/CPS-2172>`_ Support for OpenTelemetry Tracing.

..      =========================
..      * * *   NEW DELHI   * * *
..      =========================

Version: 3.4.9
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.4.9                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.4.9 New Delhi                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 May 14                                            |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-2211 <https://lf-onap.atlassian.net/browse/CPS-2211>`_ Toggle switch to disable CPS Core change events if not used by application. Set CPS_CHANGE_EVENT_NOTIFICATIONS_ENABLED environment variable for the same.

Features
--------
    - `CPS-1836 <https://lf-onap.atlassian.net/browse/CPS-1836>`_ Delta between anchor and JSON payload.

Version: 3.4.8
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.4.8                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.4.8 New Delhi                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 May 1                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-2186 <https://lf-onap.atlassian.net/browse/CPS-2186>`_ Report async task failures to client topic during data operations request
    - `CPS-2190 <https://lf-onap.atlassian.net/browse/CPS-2190>`_ Improve performance of NCMP module searches
    - `CPS-2194 <https://lf-onap.atlassian.net/browse/CPS-2194>`_ Added defaults for CPS and DMI username and password
    - `CPS-2204 <https://lf-onap.atlassian.net/browse/CPS-2204>`_ Added error handling for yang module upgrade operation

Version: 3.4.7
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.4.7                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.4.7 New Delhi                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 March 29                                          |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-2150 <https://lf-onap.atlassian.net/browse/CPS-2150>`_ Fix for Async task execution failed by TimeoutException.

Features
--------
    - `CPS-2061 <https://lf-onap.atlassian.net/browse/CPS-2061>`_ Liquibase Steps Condensing and Cleanup.
    - `CPS-2101 <https://lf-onap.atlassian.net/browse/CPS-2101>`_ Uplift Spring Boot to 3.2.4 version.

Version: 3.4.6
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.4.6                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.4.6 New Delhi                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 February 29                                       |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-2126 <https://lf-onap.atlassian.net/browse/CPS-2126>`_ Passing HTTP Authorization Bearer Token to DMI Plugins.


Features
--------
    - `CPS-2133 <https://lf-onap.atlassian.net/browse/CPS-2133>`_ Revert Uplift of Spring Boot version from 3.2.2 to 3.1.2

Notes
-----
This release brings improvements to compatibility with Service Mesh and for that below measures are been taken.

Basic authorization provided using Spring security is been removed from CPS-Core and NCMP and hence authorization is no longer enforced.(basic auth header will be ignored, but is still allowed).
NCMP will propagate a bearer token to DMI conditionally.
401 Unauthorized will not be returned. Best effort has been made to ensure backwards compatibility.

Version: 3.4.5
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.4.5                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.4.5 New Delhi                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 February 27                                       |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+


Features
--------
    - `CPS-2101 <https://lf-onap.atlassian.net/browse/CPS-2101>`_ Uplift Spring Boot version to 3.2.2


Version: 3.4.4
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.4.4                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.4.4 New Delhi                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 February 23                                       |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-2027 <https://lf-onap.atlassian.net/browse/CPS-2027>`_ Upgrade Yang modules using module set tag functionalities fix

Features
--------
    - `CPS-2057 <https://lf-onap.atlassian.net/browse/CPS-2057>`_ Leaf lists are sorted by default if Yang model does not specify order.
    - `CPS-2087 <https://lf-onap.atlassian.net/browse/CPS-2087>`_ Performance improvement of CPS Path Queries.


Version: 3.4.3
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.4.3                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.4.3 New Delhi                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 February 07                                       |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-2000 <https://lf-onap.atlassian.net/browse/CPS-2000>`_ Fix for Schema object cache not being distributed.
    - `CPS-2027 <https://lf-onap.atlassian.net/browse/CPS-2027>`_ Fixes for upgrade yang modules using module set tag.
    - `CPS-2070 <https://lf-onap.atlassian.net/browse/CPS-2070>`_ Add retry interval for Kafka consumer.

Features
--------
    - `CPS-1824 <https://lf-onap.atlassian.net/browse/CPS-1824>`_ CPS Delta between 2 anchors.
    - `CPS-2072 <https://lf-onap.atlassian.net/browse/CPS-2072>`_ Add maven classifier to Spring Boot JAR.
    - `CPS-1135 <https://lf-onap.atlassian.net/browse/CPS-1135>`_ Extend CPS Module API to allow retrieval single module definition.

Notes
-----
The maven build of cps-application has been changed so that the JAR produced by spring-boot-maven-plugin has a
*-springboot* classifier (`CPS-2072 <https://lf-onap.atlassian.net/browse/CPS-2072>`_). This means that the filename
of the Spring Boot JAR is *cps-application-3.4.3-springboot.jar*.

Version: 3.4.2
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.4.2                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.4.2 New Delhi                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2024 January 11                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Features
--------
    - `CPS-1638 <https://lf-onap.atlassian.net/browse/CPS-1638>`_ Introduce trust level for CM handle.
    - `CPS-1795 <https://lf-onap.atlassian.net/browse/CPS-1795>`_ Double performance of CPS write operations (via write batching)
    - `CPS-2018 <https://lf-onap.atlassian.net/browse/CPS-2018>`_ Improve performance of CPS update operations.
    - `CPS-2019 <https://lf-onap.atlassian.net/browse/CPS-2019>`_ Improve performance of saving CM handles.

Notes
-----
    - Java API method CpsDataService::saveListElementsBatch has been removed as part of CPS-2019.

Known Limitations, Issues and Workarounds
-----------------------------------------

*System Limitations*

For upgrading, CPS uses Liquibase for database upgrades. In order to enable Hibernate write batching
(`CPS-1795 <https://lf-onap.atlassian.net/browse/CPS-1795>`_), a change to the database entity ID generation is required.
As such, *this release does not fully support In-Service Software Upgrade* - CPS will not store new DataNodes and
NCMP will not register new CM Handles during an upgrade with old and new versions of CPS running concurrently.
Other operations (read, update, delete) are not impacted.


Version: 3.4.1
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.4.1                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.4.1 New Delhi                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 December 20                                       |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-1979 <https://lf-onap.atlassian.net/browse/CPS-1979>`_ Bug fix for Invalid topic name suffix.

Features
--------
    - CPS-Temporal is no longer supported and any related documentation has been removed.
    - `CPS-1733 <https://lf-onap.atlassian.net/browse/CPS-1733>`_ Upgrade YANG schema-set for CM handle without removing and adding it.
    - `CPS-1980 <https://lf-onap.atlassian.net/browse/CPS-1980>`_ Exposing health and cluster metrics for hazelcast.
    - `CPS-1994 <https://lf-onap.atlassian.net/browse/CPS-1994>`_ Use Apache Http Client for DMI REST requests.
    - `CPS-2005 <https://lf-onap.atlassian.net/browse/CPS-2005>`_ Removing notification feature for cps updated events ( exclusively used by cps-temporal )

Known Issues
------------
    - `CPS-2000 <https://lf-onap.atlassian.net/browse/CPS-2000>`_ Schema object cache is not distributed.


Version: 3.4.0
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.4.0                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.4.0 New Delhi                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 November 09                                       |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-1956 <https://lf-onap.atlassian.net/browse/CPS-1956>`_ Bug fix for No yang resources stored during cmhandle discovery.

..      ========================
..      * * *   MONTREAL   * * *
..      ========================

Version: 3.3.9
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.3.9                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.3.9 Montreal                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 November 06                                       |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-1923 <https://lf-onap.atlassian.net/browse/CPS-1923>`_ CPS and NCMP changed management endpoint and port from /manage to /actuator and port same as cps application port.
    - `CPS-1933 <https://lf-onap.atlassian.net/browse/CPS-1933>`_ Setting up the class loader explicitly in hazelcast config.

Version: 3.3.8
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.3.8                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.3.8 Montreal                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 September 29                                      |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Features
--------
    - `CPS-1888 <https://lf-onap.atlassian.net/browse/CPS-1888>`_ Uplift Spring Boot to 3.1.2.

Version: 3.3.7
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.3.7                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.3.7 Montreal                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 September 20                                      |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-1866 <https://lf-onap.atlassian.net/browse/CPS-1866>`_ Fix ClassDefNotFoundError in opendaylight Yang parser

Features
--------
    - `CPS-1789 <https://lf-onap.atlassian.net/browse/CPS-1789>`_ CPS Upgrade to Springboot 3.0.

Note
----
Migrating to Spring Boot 3.0 requires the product be built with Java 17 and at least MVN version 3.8.7.

Version: 3.3.6
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.3.6                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.3.6 Montreal                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 August 23                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-1841 <https://lf-onap.atlassian.net/browse/CPS-1841>`_ Update of top-level data node fails with exception
    - `CPS-1842 <https://lf-onap.atlassian.net/browse/CPS-1842>`_ Replace event-id with correlation-id for data read operation cloud event

Features
--------
    - `CPS-1696 <https://lf-onap.atlassian.net/browse/CPS-1696>`_ Get Data Node to return entire List data node.
    - `CPS-1819 <https://lf-onap.atlassian.net/browse/CPS-1819>`_ Ability to disable sending authorization header.


Version: 3.3.5
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.3.5                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.3.5 Montreal                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 July 21                                           |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Features
--------
    - `CPS-1760 <https://lf-onap.atlassian.net/browse/CPS-1760>`_ Improve handling of special characters in Cps Paths

Version: 3.3.4
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.3.4                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.3.4 Montreal                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 July 19                                           |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Features
--------
    - `CPS-1767 <https://lf-onap.atlassian.net/browse/CPS-1767>`_ Upgrade CPS to java 17

Version: 3.3.3
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.3.3                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.3.3 Montreal                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 June 30                                           |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Features
--------
    - `CPS-1515 <https://lf-onap.atlassian.net/browse/CPS-1515>`_ Support Multiple CM Handles for NCMP Get Operation
    - `CPS-1675 <https://lf-onap.atlassian.net/browse/CPS-1675>`_ Persistence write performance improvement(s)
    - `CPS-1745 <https://lf-onap.atlassian.net/browse/CPS-1745>`_ Upgrade to Openapi 3.0.3

Version: 3.3.2
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.3.2                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.3.2 Montreal                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 June 15                                           |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-1716 <https://lf-onap.atlassian.net/browse/CPS-1716>`_ NCMP: Java Heap OutOfMemory errors and slow registration in case of 20k cmhandles

Features
--------
    - `CPS-1006 <https://lf-onap.atlassian.net/browse/CPS-1006>`_ Extend CPS PATCH API to allow update of leaves for multiple data nodes
    - `CPS-1273 <https://lf-onap.atlassian.net/browse/CPS-1273>`_ Add <,> operators support to cps-path
    - `CPS-1664 <https://lf-onap.atlassian.net/browse/CPS-1664>`_ Use recursive SQL to fetch descendants in CpsPath queries to improve query performance
    - `CPS-1676 <https://lf-onap.atlassian.net/browse/CPS-1676>`_ Entity ID types do not match types in database definition
    - `CPS-1677 <https://lf-onap.atlassian.net/browse/CPS-1677>`_ Remove dataspace_id column from Fragment table

Version: 3.3.1
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.3.1                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.3.1 Montreal                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 May 03                                            |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Features
--------
    - `CPS-1272 <https://lf-onap.atlassian.net/browse/CPS-1272>`_ Add Contains operation to CPS Path
    - `CPS-1573 <https://lf-onap.atlassian.net/browse/CPS-1573>`_ Remove 32K limit for DB operations
    - `CPS-1627 <https://lf-onap.atlassian.net/browse/CPS-1627>`_ Dependency versions uplift because of vulnerability issues
    - `CPS-1629 <https://lf-onap.atlassian.net/browse/CPS-1629>`_ Ordering of leaf elements to support combination of AND/OR in cps-path
    - `CPS-1637 <https://lf-onap.atlassian.net/browse/CPS-1637>`_ Extend hazelcast to work on kubernetes

Version: 3.3.0
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.3.0                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.3.0 Montreal                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 April 20                                          |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Features
--------
    - `CPS-1215 <https://lf-onap.atlassian.net/browse/CPS-1215>`_ Add OR operation for CPS Path
    - `CPS-1617 <https://lf-onap.atlassian.net/browse/CPS-1617>`_ Use cascade delete in fragments table

..      ======================
..      * * *   LONDON   * * *
..      ======================

Version: 3.2.6
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.2.6                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.2.6 London                                           |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 March 22                                          |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-1526 <https://lf-onap.atlassian.net/browse/CPS-1526>`_ Fix response message for PATCH operation
    - `CPS-1563 <https://lf-onap.atlassian.net/browse/CPS-1563>`_ Fix 500 response error on id-searches with empty parameters

Features
--------
    - `CPS-1396 <https://lf-onap.atlassian.net/browse/CPS-1396>`_ Query data nodes across all anchors under one dataspace

Version: 3.2.5
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.2.5                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.2.5 London                                           |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 March 10                                          |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
    - `CPS-1537 <https://lf-onap.atlassian.net/browse/CPS-1537>`_ Introduce control switch for model loader functionality.

Version: 3.2.4
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.2.4                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.2.4 London                                           |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 March 09                                          |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
3.2.4
    - `CPS-1533 <https://lf-onap.atlassian.net/browse/CPS-1533>`_ Fix for Temp tables cause Out of shared memory errors in Postgres
    - `CPS-1537 <https://lf-onap.atlassian.net/browse/CPS-1537>`_ NCMP failed to start due to issue in SubscriptionModelLoader

Features
--------
    - None

Version: 3.2.3
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.2.3                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.2.3 London                                           |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 March 07                                          |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
3.2.3
   - `CPS-1494 <https://lf-onap.atlassian.net/browse/CPS-1494>`_ NCMP Inventory Performance Improvements

Features
--------
    - `CPS-1401 <https://lf-onap.atlassian.net/browse/CPS-1401>`_ Added V2 of Get Data Node API,support to retrieve all data nodes under an anchor
    - `CPS-1502 <https://lf-onap.atlassian.net/browse/CPS-1502>`_ Delete Performance Improvements

Version: 3.2.2
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.2.2                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.2.2 London                                           |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 February 08                                       |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
3.2.2
   - `CPS-1173 <https://lf-onap.atlassian.net/browse/CPS-1173>`_  Delete Performance Improvements.

Features
--------
   - None

Version: 3.2.1
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.2.1                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.2.1 London                                           |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2023 January 27                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Features
--------
3.2.1
   - `CPS-341 <https://lf-onap.atlassian.net/browse/CPS-341>`_  Added support for multiple data tree instances under 1 anchor.
   - `CPS-1002 <https://lf-onap.atlassian.net/browse/CPS-1002>`_  Add CPS-E-05 endpoint for 'Query data, NCMP-Operational Datastore' using cpsPaths
   - `CPS-1182 <https://lf-onap.atlassian.net/browse/CPS-1182>`_  Upgrade Opendaylight
   - `CPS-1185 <https://lf-onap.atlassian.net/browse/CPS-1185>`_  Get all dataspaces.
   - `CPS-1186 <https://lf-onap.atlassian.net/browse/CPS-1186>`_  Get single dataspace.
   - `CPS-1187 <https://lf-onap.atlassian.net/browse/CPS-1187>`_  Added API to get all schema sets for a given dataspace.
   - `CPS-1236 <https://lf-onap.atlassian.net/browse/CPS-1236>`_  DMI audit support for NCMP: Filter on any properties of CM Handles
   - `CPS-1257 <https://lf-onap.atlassian.net/browse/CPS-1257>`_  Added support for application/xml Content-Type (write only).
   - `CPS-1381 <https://lf-onap.atlassian.net/browse/CPS-1381>`_  Query large outputs using limit/depth/pagination
   - `CPS-1421 <https://lf-onap.atlassian.net/browse/CPS-1421>`_  Optimized query for large number of hits with descendants.
   - `CPS-1422 <https://lf-onap.atlassian.net/browse/CPS-1422>`_  Fetch CM handles by collection of xpaths (CPS Core)
   - `CPS-1424 <https://lf-onap.atlassian.net/browse/CPS-1424>`_  Updating CmHandleStates using batch operation
   - `CPS-1439 <https://lf-onap.atlassian.net/browse/CPS-1439>`_  Use native query to delete data nodes

Bug Fixes
---------
3.2.1
   - `CPS-1171 <https://lf-onap.atlassian.net/browse/CPS-1171>`_  Optimized retrieval of data nodes with many descendants.
   - `CPS-1288 <https://lf-onap.atlassian.net/browse/CPS-1288>`_  Hazelcast TTL for IMap is not working
   - `CPS-1289 <https://lf-onap.atlassian.net/browse/CPS-1289>`_  Getting wrong error code for create node api
   - `CPS-1326 <https://lf-onap.atlassian.net/browse/CPS-1326>`_  Creation of DataNodeBuilder with module name prefix is very slow
   - `CPS-1344 <https://lf-onap.atlassian.net/browse/CPS-1344>`_  Top level container (prefix) is not always the first module
   - `CPS-1350 <https://lf-onap.atlassian.net/browse/CPS-1350>`_  Add Basic Authentication to CPS/NCMP OpenAPI Definitions.
   - `CPS-1352 <https://lf-onap.atlassian.net/browse/CPS-1352>`_  Handle YangChoiceNode in right format.
   - `CPS-1409 <https://lf-onap.atlassian.net/browse/CPS-1409>`_  Fix Delete uses case with '/' in path.
   - `CPS-1433 <https://lf-onap.atlassian.net/browse/CPS-1433>`_  Fix to allow posting data with '/' key fields.
   - `CPS-1442 <https://lf-onap.atlassian.net/browse/CPS-1442>`_  CPS PATCH operation does not merge existing data
   - `CPS-1446 <https://lf-onap.atlassian.net/browse/CPS-1446>`_  Locked cmhandles and ready to locked state transitions causing long cmHandle discovery
   - `CPS-1457 <https://lf-onap.atlassian.net/browse/CPS-1457>`_  CpsDataPersistenceService#getDataNodes uses non-normalized xpaths
   - `CPS-1458 <https://lf-onap.atlassian.net/browse/CPS-1458>`_  CpsDataPersistenceService#getDataNodes does not handle root xpath
   - `CPS-1460 <https://lf-onap.atlassian.net/browse/CPS-1460>`_  CPS Path Processing Performance Test duration is too low

3.2.0
   - `CPS-1312 <https://lf-onap.atlassian.net/browse/CPS-1312>`_  CPS(/NCMP) does not have version control.

Known Limitations, Issues and Workarounds
-----------------------------------------

*System Limitations*

For upgrading, CPS uses Liquibase for database upgrades. CPS/NCMP currently only supports upgrading from Liquibase changelog 11 to Liquibase changelog 16.
This is from commit CPS-506: List all known modules and revision to CPS-1312: Default CMHandles to READY during upgrade or from ONAP release Honolulu to Kohn.

CPS core Patch operation currently supports updating data of one top level data node. When performing Patch on multiple top level data nodes at once
a 400 Bad Request is sent as response. This is part of commit CPS-1526.

..      ====================
..      * * *   KOHN   * * *
..      ====================

Version: 3.1.4
==============

Release Data
------------

+--------------------------------------+--------------------------------------------------------+
| **CPS Project**                      |                                                        |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Docker images**                    | onap/cps-and-ncmp:3.1.4                                |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release designation**              | 3.1.4 Kohn                                             |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+
| **Release date**                     | 2022 October 5                                         |
|                                      |                                                        |
+--------------------------------------+--------------------------------------------------------+

Bug Fixes
---------
   - `CPS-1265 <https://lf-onap.atlassian.net/browse/CPS-1265>`_  Revision field should not be required (NotNull) on cps-ri YangResourceEntity
   - `CPS-1294 <https://lf-onap.atlassian.net/browse/CPS-1294>`_  Kafka communication fault caused cmHandle registration error

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
| **Release date**                     | 2022 September 29                                      |
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

   - `CPS-1226 <https://lf-onap.atlassian.net/browse/CPS-1226>`_  Security bug in the logs

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
   - `CPS-340 <https://lf-onap.atlassian.net/browse/CPS-340>`_  Patch and update the root data node
   - `CPS-575 <https://lf-onap.atlassian.net/browse/CPS-575>`_  Write data for cmHandle using ncmp-datastores:passthrough-running (NCMP.)
   - `CPS-731 <https://lf-onap.atlassian.net/browse/CPS-731>`_  Query based on Public CM Properties
   - `CPS-828 <https://lf-onap.atlassian.net/browse/CPS-828>`_  Async: NCMP Rest impl. including Request ID generation
   - `CPS-829 <https://lf-onap.atlassian.net/browse/CPS-829>`_  Async: Internal message topic incl. basic producer & Consumer
   - `CPS-830 <https://lf-onap.atlassian.net/browse/CPS-830>`_  DMI-NCMP Asynchronously Publish Response Event to Client Topic
   - `CPS-869 <https://lf-onap.atlassian.net/browse/CPS-869>`_  Apply Standardized logging fields to adhere to ONAP Best practice REQ-1072
   - `CPS-870 <https://lf-onap.atlassian.net/browse/CPS-870>`_  Align CPS-Core output with SDN-C output (add module name)
   - `CPS-875 <https://lf-onap.atlassian.net/browse/CPS-875>`_  CM Handle State: Watchdog-process that syncs 'ADVISED' CM Handles
   - `CPS-877 <https://lf-onap.atlassian.net/browse/CPS-877>`_  CM Handle State: Exclude any CM Handles from queries/operations that are not in state 'READY'
   - `CPS-899 <https://lf-onap.atlassian.net/browse/CPS-899>`_  Start and stop sessions on Java API
   - `CPS-909 <https://lf-onap.atlassian.net/browse/CPS-909>`_  Separate NCMP endpoint for ch/{cm-handle}/properties and ch/{cm-handle}/state
   - `CPS-917 <https://lf-onap.atlassian.net/browse/CPS-917>`_  Structured Errors response for passthrough use-cases in NCMP
   - `CPS-953 <https://lf-onap.atlassian.net/browse/CPS-953>`_  Update maven deploy plugin version
   - `CPS-977 <https://lf-onap.atlassian.net/browse/CPS-977>`_  Query CM Handles using CpsPath
   - `CPS-1000 <https://lf-onap.atlassian.net/browse/CPS-1000>`_  Create Data Synchronization watchdog
   - `CPS-1016 <https://lf-onap.atlassian.net/browse/CPS-1016>`_  Merge 2 'query' end points in NCMP
   - `CPS-1034 <https://lf-onap.atlassian.net/browse/CPS-1034>`_  Publish lifecycle events for ADVISED , READY and LOCKED state transition"
   - `CPS-1064 <https://lf-onap.atlassian.net/browse/CPS-1064>`_  Support retrieval of YANG module sources for CM handle on the NCMP interface
   - `CPS-1099 <https://lf-onap.atlassian.net/browse/CPS-1099>`_  Expose simplified 'external' lock reason enum state over REST interface
   - `CPS-1101 <https://lf-onap.atlassian.net/browse/CPS-1101>`_  Introducing the DELETING and DELETED Cmhandle State
   - `CPS-1102 <https://lf-onap.atlassian.net/browse/CPS-1102>`_  Register the Cmhandle Sends Advised State notification.
   - `CPS-1133 <https://lf-onap.atlassian.net/browse/CPS-1133>`_  Enable/Disable Data Sync for CM Handle
   - `CPS-1136 <https://lf-onap.atlassian.net/browse/CPS-1136>`_  DMI Audit Support (get all CM Handles for a registered DMI)


Bug Fixes
---------
   - `CPS-896 <https://lf-onap.atlassian.net/browse/CPS-896>`_  CM Handle Registration Process only partially completes when exception is thrown
   - `CPS-957 <https://lf-onap.atlassian.net/browse/CPS-957>`_  NCMP: fix getResourceDataForPassthroughOperational endpoint
   - `CPS-1020 <https://lf-onap.atlassian.net/browse/CPS-1020>`_  DuplicatedYangResourceException error at parallel cmHandle registration
   - `CPS-1056 <https://lf-onap.atlassian.net/browse/CPS-1056>`_  Wrong error response format in case of Dmi plugin error
   - `CPS-1067 <https://lf-onap.atlassian.net/browse/CPS-1067>`_  NCMP returns 500 error on searches endpoint when No DMI Handles registered
   - `CPS-1085 <https://lf-onap.atlassian.net/browse/CPS-1085>`_  Performance degradation on ncmp/v1/ch/searches endpoint
   - `CPS-1088 <https://lf-onap.atlassian.net/browse/CPS-1088>`_  Kafka consumer can not be turned off
   - `CPS-1097 <https://lf-onap.atlassian.net/browse/CPS-1097>`_  Unable to change state from LOCKED to ADVISED
   - `CPS-1126 <https://lf-onap.atlassian.net/browse/CPS-1126>`_  CmHandle creation performance degradation
   - `CPS-1175 <https://lf-onap.atlassian.net/browse/CPS-1175>`_  Incorrect response when empty body executed for cmhandle id-searches
   - `CPS-1179 <https://lf-onap.atlassian.net/browse/CPS-1179>`_  Node API - GET method returns invalid response when identifier contains '/'
   - `CPS-1212 <https://lf-onap.atlassian.net/browse/CPS-1212>`_  Additional Properties for CM Handles not included when send to DMI Plugin
   - `CPS-1217 <https://lf-onap.atlassian.net/browse/CPS-1217>`_  Searches endpoint gives back empty list however there are already available cmhandles
   - `CPS-1218 <https://lf-onap.atlassian.net/browse/CPS-1218>`_  NCMP logs are flooded with SyncUtils logs

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

   - `CPS-963 <https://lf-onap.atlassian.net/browse/CPS-963>`_  Liquibase has got serious vulnerability, upgrade required

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
   - `CPS-961 <https://lf-onap.atlassian.net/browse/CPS-961>`_  Updated ANTLR compiler version to 4.9.2 to be compatible with runtime version

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
   - `CPS-559 <https://lf-onap.atlassian.net/browse/CPS-559>`_  Define response objects (schemas) in cps-ncmp
   - `CPS-636 <https://lf-onap.atlassian.net/browse/CPS-636>`_  Update operation for datastore pass through running
   - `CPS-638 <https://lf-onap.atlassian.net/browse/CPS-638>`_  Delete operation for datastore pass through running
   - `CPS-677 <https://lf-onap.atlassian.net/browse/CPS-677>`_  Support 'public' CM Handle Properties
   - `CPS-741 <https://lf-onap.atlassian.net/browse/CPS-741>`_  Re sync after removing CM Handles
   - `CPS-777 <https://lf-onap.atlassian.net/browse/CPS-777>`_  Ensure all DMI operations use POST method
   - `CPS-780 <https://lf-onap.atlassian.net/browse/CPS-780>`_  Add examples for parameters, request and response in openapi yaml for cps-core
   - `CPS-789 <https://lf-onap.atlassian.net/browse/CPS-789>`_ CPS Data Updated Event Schema V2 to support delete operation
   - `CPS-791 <https://lf-onap.atlassian.net/browse/CPS-791>`_ CPS-Core sends delete notification event
   - `CPS-817 <https://lf-onap.atlassian.net/browse/CPS-817>`_  Create Endpoint For Get CM Handles (incl. public properties) By Name
   - `CPS-837 <https://lf-onap.atlassian.net/browse/CPS-837>`_  Add Remove and Update properties (DMI and Public) as part of CM Handle Registration update

Bug Fixes
---------

   - `CPS-762 <https://lf-onap.atlassian.net/browse/CPS-762>`_ Query CM Handles for module names returns incorrect CM Handle identifiers
   - `CPS-788 <https://lf-onap.atlassian.net/browse/CPS-788>`_ Yang Resource formatting is incorrect
   - `CPS-783 <https://lf-onap.atlassian.net/browse/CPS-783>`_ Remove CM Handle does not completely remove all CM Handle information
   - `CPS-841 <https://lf-onap.atlassian.net/browse/CPS-841>`_ Upgrade log4j to 2.17.1 as recommended by ONAP SECCOM
   - `CPS-856 <https://lf-onap.atlassian.net/browse/CPS-856>`_ Retry mechanism not working for concurrent CmHandle registration
   - `CPS-867 <https://lf-onap.atlassian.net/browse/CPS-867>`_ Database port made configurable through env variable DB_PORT
   - `CPS-886 <https://lf-onap.atlassian.net/browse/CPS-886>`_ Fragment handling decreasing performance for large number of cmHandles
   - `CPS-887 <https://lf-onap.atlassian.net/browse/CPS-887>`_ Increase performance of cmHandle registration for large number of schema sets in DB
   - `CPS-892 <https://lf-onap.atlassian.net/browse/CPS-892>`_ Fixed the response code during CM Handle Registration from 201 CREATED to 204 NO_CONTENT
   - `CPS-893 <https://lf-onap.atlassian.net/browse/CPS-893>`_ NCMP Java API depends on NCMP-Rest-API (cyclic) through json properties on Java API

Known Limitations, Issues and Workarounds
-----------------------------------------

*System Limitations*

Null can no longer be passed within the dmi plugin service names when registering a CM Handle, as part of
`CPS-837 <https://lf-onap.atlassian.net/browse/CPS-837>`_ null is now used to indicate if a property should be removed as part
of CM Handle registration.

The Absolute path to list with integer key will not work. Please refer `CPS-961 <https://lf-onap.atlassian.net/browse/CPS-961>`_
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

   - `CPS-879 <https://lf-onap.atlassian.net/browse/CPS-879>`_  Fix docker compose for csit test.
   - `CPS-873 <https://lf-onap.atlassian.net/browse/CPS-873>`_  Fix intermittent circular dependency error when the application starts.

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

   - `CPS-841 <https://lf-onap.atlassian.net/browse/CPS-841>`_  Update log4j version to 2.17.1 due to security vulnerability

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

   - `CPS-820 <https://lf-onap.atlassian.net/browse/CPS-820>`_  Update log4j version due to security vulnerability

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

   - `CPS-594 <https://lf-onap.atlassian.net/browse/CPS-594>`_ SQL ConstraintViolationException when updating the list node element using PATCH List node API
   - `CPS-653 <https://lf-onap.atlassian.net/browse/CPS-653>`_ cmHandleProperties not supported by dmi in fetch modules
   - `CPS-673 <https://lf-onap.atlassian.net/browse/CPS-673>`_ Improvement and cleanup for CPS Core charts
   - `CPS-691 <https://lf-onap.atlassian.net/browse/CPS-691>`_ NCMP no master index label on index documentation page

Known Limitations, Issues and Workarounds
-----------------------------------------

*System Limitations*

Limitations to the amount of child nodes that can be added to the fix above. The current limit is 3.

*Known Vulnerabilities*

   - `CPS-725 <https://lf-onap.atlassian.net/browse/CPS-725>`_ fix sample docker compose of cps/ncmp and onap dmi plugin

*Workarounds*

Add recursive method to save list node data to loop through all corresponding child nodes.

Security Notes
--------------

*Fixed Security Issues*

   - `CPS-581 <https://lf-onap.atlassian.net/browse/CPS-581>`_ Remove security vulnerabilities

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

   - `CPS-310 <https://lf-onap.atlassian.net/browse/CPS-310>`_ Data schema migration from Honolulu to Istanbul is failing
   - `CPS-316 <https://lf-onap.atlassian.net/browse/CPS-316>`_ Xpath cannot be created for augmentation data node
   - `CPS-336 <https://lf-onap.atlassian.net/browse/CPS-336>`_ Ends-with functionality in cpsPath does not conform with standard xPath behavior
   - `CPS-345 <https://lf-onap.atlassian.net/browse/CPS-345>`_ Leaf String value comparison matches mix of single and double quotes
   - `CPS-357 <https://lf-onap.atlassian.net/browse/CPS-357>`_ cps-review-verification-maven-master Jenkins job is failing when running csit test
   - `CPS-367 <https://lf-onap.atlassian.net/browse/CPS-367>`_ Get descendent does not support xpaths that end in list values
   - `CPS-377 <https://lf-onap.atlassian.net/browse/CPS-377>`_ Init ran model validation is failing error details are not provided
   - `CPS-422 <https://lf-onap.atlassian.net/browse/CPS-422>`_ REST 404 response returned instead of 400 for POST/PUT/PATCH request types
   - `CPS-450 <https://lf-onap.atlassian.net/browse/CPS-450>`_ Datanode query using full path to node causes NPE
   - `CPS-451 <https://lf-onap.atlassian.net/browse/CPS-451>`_ cps-ran-schema-model@2021-01-28.yang missing root container
   - `CPS-464 <https://lf-onap.atlassian.net/browse/CPS-464>`_ Request to update node leaves (patch) responds with Internal Server Error
   - `CPS-465 <https://lf-onap.atlassian.net/browse/CPS-465>`_ Request to update node leaves (patch) responds with json parsing failure
   - `CPS-466 <https://lf-onap.atlassian.net/browse/CPS-466>`_ Concurrent requests to create schema sets for the same yang model are not supported
   - `CPS-479 <https://lf-onap.atlassian.net/browse/CPS-479>`_ Get Nodes API does not always return the object from the root
   - `CPS-500 <https://lf-onap.atlassian.net/browse/CPS-500>`_ Special Character Limitations of cpsPath Queries
   - `CPS-501 <https://lf-onap.atlassian.net/browse/CPS-501>`_ Put DataNode API has missing transaction and error handling for concurrency issues
   - `CPS-524 <https://lf-onap.atlassian.net/browse/CPS-524>`_ Issue with CPSData API to add an item to an existing list node
   - `CPS-560 <https://lf-onap.atlassian.net/browse/CPS-560>`_ Response from cps query using text() contains escape characters
   - `CPS-566 <https://lf-onap.atlassian.net/browse/CPS-566>`_ Can't access grandparent node through ancestor axis
   - `CPS-573 <https://lf-onap.atlassian.net/browse/CPS-573>`_ /v1/ch/PNFDemo1/modules returning 401 unauthorised.
   - `CPS-587 <https://lf-onap.atlassian.net/browse/CPS-587>`_ cps-ncmp-service NullpointerException when DmiPluginRegistration has no additionProperties
   - `CPS-591 <https://lf-onap.atlassian.net/browse/CPS-591>`_ CPS-Core Leaf stored as integer is being returned from DB as float
   - `CPS-601 <https://lf-onap.atlassian.net/browse/CPS-601>`_ CPS swagger-ui does not show NCMP endpoints
   - `CPS-616 <https://lf-onap.atlassian.net/browse/CPS-616>`_ NCMP base path does not conform to agreed API URL
   - `CPS-630 <https://lf-onap.atlassian.net/browse/CPS-630>`_ Incorrect information sent when same anchor is updated faster than notification service processes
   - `CPS-635 <https://lf-onap.atlassian.net/browse/CPS-635>`_ Module Resource call does not include body

This document provides the release notes for Istanbul release.

Summary
-------

Following CPS components are available with default ONAP/CPS installation.


    * Platform components

        - CPS (Helm charts)

    * Service components

        - CPS-NCMP
        - DMI-Plugin

    * Additional resources that CPS utilizes deployed using ONAP common charts

        - Postgres Database


Under OOM (Kubernetes) all CPS component containers are deployed as Kubernetes Pods/Deployments/Services into Kubernetes cluster.

Known Limitations, Issues and Workarounds
-----------------------------------------

*System Limitations*

Limitations to the amount of child nodes that can be added to the fix above. The current limit is 3.

*Known Vulnerabilities*

   - `CPS-594 <https://lf-onap.atlassian.net/browse/CPS-594>`_ SQL ConstraintViolationException when updating the list node element using PATCH List node API
   - `CPS-653 <https://lf-onap.atlassian.net/browse/CPS-653>`_ cmHandleProperties not supported by dmi in fetch modules
   - `CPS-673 <https://lf-onap.atlassian.net/browse/CPS-673>`_ Improvement and cleanup for CPS Core charts

*Workarounds*

Add recursive method to save list node data to loop through all corresponding child nodes.

Security Notes
--------------

*Fixed Security Issues*

   - `CPS-249 <https://lf-onap.atlassian.net/browse/CPS-249>`_ Exception stack trace is exposed

*Known Security Issues*

   - `CPS-581 <https://lf-onap.atlassian.net/browse/CPS-581>`_ Remove security vulnerabilities

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

   - `CPS-706 <https://lf-onap.atlassian.net/browse/CPS-706>`_ Get moduleschema/yangresouce endpoint not working
   - `CPS-276 <https://lf-onap.atlassian.net/browse/CPS-276>`_ Improve error reporting for invalid cpsPath on Queries
   - `CPS-288 <https://lf-onap.atlassian.net/browse/CPS-288>`_ Move security configuration to the application module
   - `CPS-290 <https://lf-onap.atlassian.net/browse/CPS-290>`_ Internal Server Error when creating the same data node twice
   - `CPS-292 <https://lf-onap.atlassian.net/browse/CPS-292>`_ Detailed information is missing to explain why data is not compliant with the specified YANG model
   - `CPS-300 <https://lf-onap.atlassian.net/browse/CPS-304>`_ Not able to create data instances for 2 different anchors using the same model
   - `CPS-304 <https://lf-onap.atlassian.net/browse/CPS-304>`_ Use ONAP recommended base Java Docker image
   - `CPS-308 <https://lf-onap.atlassian.net/browse/CPS-308>`_ Not able to upload yang models files greater than 1MB

Security Notes
--------------

*Fixed Security Issues*

   - `CPS-249 <https://lf-onap.atlassian.net/browse/CPS-249>`_ Exception stack trace is exposed

*Known Security Issues*

   - `Security Waiver <https://lf-onap.atlassian.net/wiki/spaces/DW/pages/16467851/Honolulu+Exception+Request+for+CPS>`_ Security - Expose external endpoints with https

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

   - `CPS-249 <https://lf-onap.atlassian.net/browse/CPS-249>`_ Exception stack trace is exposed
   - `CPS-264 <https://lf-onap.atlassian.net/browse/CPS-264>`_ Unique timestamp is missing when tagging docker images.
   - Methods exposed on API which are yet not implemented : deleteAnchor, getNodesByDataspace & deleteDataspace.
   - `CPS-465 <https://lf-onap.atlassian.net/browse/CPS-465>`_ & `CPS-464 <https://lf-onap.atlassian.net/browse/CPS-464>`_ Update data node leaves API does not support updating a list element with compound keys.

*System Limitations*

None

*Known Vulnerabilities*

None

*Workarounds*

Documented under corresponding jira if applicable.

Security Notes
--------------

*Fixed Security Issues*

* `CPS-167 <https://lf-onap.atlassian.net/browse/CPS-167>`_ -Update CPS dependencies as Required for Honolulu release
    - Upgrade org.onap.oparent to 3.2.0
    - Upgrade spring.boot to 2.3.8.RELEASE
    - Upgrade yangtools to 5.0.7

*Known Security Issues*

    * Weak Cryptography using md5
    * Risk seen in Zip file expansion

*Known Vulnerabilities in Used Modules*

    None

CPS code has been formally scanned during build time using NexusIQ and all Critical vulnerabilities have been addressed, items that remain open have been assessed for risk and determined to be false positive.

Test Results
------------
    * `Integration tests <https://lf-onap.atlassian.net/wiki/spaces/DW/pages/16462943/CPS+Integration+Test+Cases>`_

References
----------

For more information on the latest ONAP release, please see:

#. `ONAP Home Page`_
#. `ONAP Wiki Page`_
#. `ONAP Documentation`_
#. `ONAP CPS Documentation`_
#. `ONAP Release Downloads`_


.. _`ONAP Home Page`: https://www.onap.org
.. _`ONAP Wiki Page`: https://lf-onap.atlassian.net
.. _`ONAP Documentation`: https://docs.onap.org
.. _`ONAP CPS Documentation`: https://docs.onap.org/projects/onap-cps
.. _`ONAP Release Downloads`: https://git.onap.org

Quick Links:

        - `CPS project page <https://lf-onap.atlassian.net/wiki/spaces/DW/overview>`_
        - `Passing Badge information for CPS <https://bestpractices.coreinfrastructure.org/en/projects/4398>`_
