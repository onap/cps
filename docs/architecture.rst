.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021 Nordix Foundation, Pantheon.tech
.. _architecture:

CPS Architecture
################

.. toctree::
   :maxdepth: 1


High Level Component Definition and Architectural Relationships
===============================================================

The Configuration Persistence Service (CPS) provides storage for run-time configuration and operational
parameters that need to be used by ONAP.

In this release CPS is no longer a stand alone component and is released along with Cps-Temporal and the NCMP-DMI-Plugin.

Project page describing eventual scope and ambition is here:
`Configuration Persistence Service Project <https://wiki.onap.org/display/DW/Configuration+Persistence+Service+Project>`_

This page reflects the state for Istanbul-R9 release.

.. image:: _static/star.png
    :class: float-left

**Note:** SDC and AAI interfaces have not yet been implemented.

.. image:: _static/cps-r9-arch-diagram.png

API definitions
===============

Configuration Persistence Service provides the following interfaces.

.. list-table::
   :header-rows: 1

   * - Interface name
     - Interface definition
     - Interface capabilities
     - Protocol
   * - CPS-E-01
     - Administrative Data Management
     - - create dataspace
       - create/delete schema set
       - create/delete anchor
     - REST
   * - CPS-E-02
     - Generic Data Access
     - - create data node
       - read data node by xpath with or without descendants
       - update data node by xpath with or without descendants
     - REST
   * - CPS-E-03
     - Generic Data Search
     - - query data nodes by xpath prefix and attribute value
     - REST
   * - CPS-E-04
     - Change Notification
     - - Kafka is used as the event messaging system
       - running instance is supplied independently from any Kafka instance deployed from ONAP
       - published events contain Timestamp, Dataspace, Schema set, Anchor and JSON Data Payload
     - Kafka
   * - CPS-E-05
     - xNF Data Access
     - - read xNF data
       - query xNF data
     - REST
   * - CPS-E-06
     - Temporal Data Access
     - - data storage and access
     - REST
   * - CPS-E-07
     - Admin
     - - logging levels and configuration
       - monitoring
       - health including liveliness state and readiness state
       - metrics through Prometheus
     - Various

The CPS Basic Concepts are described in :doc:`modeling`.
