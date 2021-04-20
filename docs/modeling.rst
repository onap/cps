.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021 Pantheon.tech
.. _modeling:

CPS Modeling
############

.. toctree::
   :maxdepth: 1

Basic Concepts
==============

.. image:: _static/cps-modeling-concepts.png
   :alt: Basic entities relationship

Administrative entities

- **Dataspace** is a primary logical separation of data.

  Any application can define its own dataspace to store the model(s) and data it owns.
  Dataspace is uniquely identified by it's name.

- **Schema Set** describes a data model(s).

  Schema Set holds reference(s) to single or multiple YANG modules. Schema Set belongs to dataspace
  and uniquely identified by its name (within its own dataspace). Same YANG resources (source files) can be
  referenced by multiple schema sets from different dataspaces.

- **Anchor** identifies the unique data set (data record) within a dataspace

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
