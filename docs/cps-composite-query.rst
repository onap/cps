.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2026 Deutsche Telekom AG

.. _compositeQuery:


CPS Composite Query
####################

.. toctree::
   :maxdepth: 1

Introduction
============

The Composite Query feature allows several :ref:`CPS Path<path>` conditions to be combined into a single query
using the logical operators ``and``/``or``. Conditions can be nested to any depth, allowing arbitrarily complex
combinations of criteria to be expressed and evaluated as one query, in a single request.

Each condition in a composite query is itself a self-referential (recursive) structure, so any condition can
have its own nested sub-conditions. This makes it possible to build expressions such as
"(A and B) or (C and D)" where A, B, C and D are themselves independent CPS Path queries.

Structure of a Composite Query
===============================

A composite query is composed of:

  - ``cpsPath``: a CPS Path expression to be evaluated for this node of the query. This is mandatory.
  - ``operator``: the logical operator used to combine this node with its nested conditions. Supported
    values are ``and`` and ``or``. Defaults to ``and`` when omitted.
  - ``conditions``: a collection of nested composite queries, evaluated recursively and combined using
    ``operator``. Defaults to an empty collection when omitted.

**Limitations**
  - Every condition, at every level of nesting, must have a ``cpsPath``.
  - The nesting of conditions may not exceed a depth of 10.
  - Only ``and`` and ``or`` are supported as operators.

Endpoint
========

The composite query is submitted as a JSON request body to:

``POST /v2/dataspaces/{dataspace-name}/anchors/{anchor-name}/nodes/query``

The result is the (deduplicated) set of data nodes that satisfy the composite query, evaluated against the
CPS Path conditions and logical operators supplied.

Example
=======

Using the sample bookstore model and data described in :ref:`CPS Path<path>`, the following composite query
returns all books that are either priced at 5 or tagged with the label "classic", but only within the
"SciFi" category:

.. code-block:: json

    {
      "cpsPath": "/shops/bookstore/categories[@name='SciFi']/books",
      "operator": "and",
      "conditions": [
        {
          "cpsPath": "//book[@price=5]",
          "operator": "or",
          "conditions": [
            {
              "cpsPath": "//book/label[text()='classic']"
            }
          ]
        }
      ]
    }