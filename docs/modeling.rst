.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021 Pantheon.tech
.. _modeling:

.. toctree::
   :maxdepth: 1

CPS Modeling
############

CPS-Core Modeling
=================

Data Model
----------

.. image:: _static/cps-modeling-concepts.png
   :alt: Basic entities relationship

Basic Concepts
--------------

Administrative entities

- **Dataspace** is a primary logical separation of data.

  Any application can define its own dataspace to store the model(s) and data it owns.
  Dataspace is uniquely identified by it's name.

- **Schema Set** describes a data model(s).

  Schema Set holds reference(s) to single or multiple YANG modules. Schema Set belongs to dataspace
  and uniquely identified by its name (within its own dataspace). Same YANG resources (source files) can be
  referenced by multiple schema sets from different dataspaces.

- **Anchor** identifies the unique data set (data record) within a dataspace.

  Anchor always references a schema set within same dataspace which describes a data model of associated data.
  Multiple anchors may reference same schema set. Anchor is uniquely identified by its name (within own dataspace).

Data

- **Data Node** represents a data fragment.

  Each data node can have zero or more descendants and together they form a data instance tree.
  The data node tree belongs to an anchor.

  Data node is representing a data fragment described in a YANG model as a *container* and/or a *list*.
  The data described as a *leaf* and/or a *leaf-list* are stored within a parent data node.

  The data node position within a tree is uniquely identified by the node's unique **xpath** which can be used
  for partial data query.

Querying

- **CPS Path** is used to query data nodes. The CPS Path is described in detail in :doc:`cps-path`.

NCMP Modeling
=============

Data Model
----------

NCMP stores DMI-Plugin and CM Handle relations using a data model described as per this Yang module.

:download:`DMI Yang Module <api/yang/dmiYangResource.yang>`

Basic Concepts
--------------

- **CM-Handle** represents an instance a modeled Network Function(node) in ONAP.

    These are stored as Anchors within CPS-Core.

- **Datastores** represent different views of the cm data.

    Datastores are defined for NCMP to access the CPS running or operational datastores. Currently supported datastores are:

    +--------------------------------+-------------------------------------+-------------------------+
    | Datastore                      | Configurations                      | Data access type        |
    +================================+=====================================+=========================+
    | Passthrough-operational        | config-true, config-false           | read-only               |
    +--------------------------------+-------------------------------------+-------------------------+
    | Passthrough-running            | config-true                         | read-write              |
    +--------------------------------+-------------------------------------+-------------------------+