.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _design:


CPS Path
########

.. toctree::
   :maxdepth: 1

Introduction
============

Several CPS APIs use the cps-path (or cpsPath in Java API) parameter.
The CPS Path parameter is used for querying xpaths. CPS Path is insprired by the `XML Path Language (XPath) 3.1. <https://www.w3.org/TR/2017/REC-xpath-31-20170321/>`_

This section describes the functionality currently supported by CPS Path.

Sample Data
===========

The xml below describes some basic data to be used to illustrate the CPS Path functionality.

.. code-block:: xml

    <shops>
       <bookstore name="Chapters">
         <bookstore-name>Chapters</bookstore-name>
         <categories code="1" name="SciFi" numberOfBooks="2">
           <books>
             <book name="Space Odyssee"/>
             <book name="Dune"/>
           </books>
        </categories>
        <categories code="2" name="Kids" numberOfBooks="1">
           <books>
             <book name="Matilda"/>
           </books>
        </categories>
       </bookstore>
    </shops>

**Note.** 'categories' is a Yang List and 'code' is its key leaf. All other data nodes are Yang Containers

General Notes
=============

- String values must be wrapped in quotation marks (U+0022) or apostrophes (U+0027).
- String comparisons are case sensitive.

Supported Queries
=================

Get List Elements by Any Attribute Value
----------------------------------------

**Syntax**: ``<xpath>/<target-node>[@<leaf-name>=<leaf-value>]``
  - ``xpath``: The xpath to the parent of the target node including all ancestors.
  - ``target-node``: The name of the (list) node which elements will queried.
  - ``leaf-name``: The name of the leaf which value needs to be compared.
  - ``leaf-value``: The required value of the leaf.

**Examples**
  - ``/shops/bookstore/categories[@numberOfBooks=1]``
  - ``/shops/bookstore/categories[@name="Kids"]``
  - ``/shops/bookstore/categories[@name='Kids']``

**Limitations**
  - Only one list (last descendant) can be queried for a non-key value. Any ancestor list will have to be referenced by its key name-value pair(s).
  - Only one attribute can be queried.
  - Only string and integer values are supported (boolean and float values are not supported).

**Notes**
  - For performance reasons it does not make sense to query using key leaf as attribute. If the key value is known it is better to execute a get request with the complete xpath.

Get Any Descendant
------------------

**Syntax**: ``//<direct-ancestors><target-node>``
  - ``direct-ancestors``: Optional path to direct ancestors of the target node. This can contain zero to many ancestor nodes separated by a /.
  - ``target-node``: The name of the (list) node from which element will be selected. If the target node is a Yang List he element needs to be specified using the key as normal e.g. ``categories[@code=1]``.

**Examples**
  - ``//book``
  - ``//books/book``
  - ``//categories[@code=1]``
  - ``//categories[@code=1]/books``

**Limitations**
  - List elements can only be addressed using the list key leaf.

Get Any Descendant by Any Attribute Value
-----------------------------------------

**Syntax**: ``//<direct-ancestors><target-node>[@<leaf-name>=<leaf-value>]``
  - ``direct-ancestors``: Optional path to direct ancestors of the target node. This can contain zero to many ancestor nodes separated by a /.
  - ``target-node``: The name of the (list) node which elements will queried.
  - ``leaf1-name .. leafN-name:``: One or more leaves whose value needs to be compared.
  - ``leaf1-value .. leafN-value:``: One or more required leaf values (multiple condition can be combined using the 'and' keyword).

**Examples**
  - ``//categories[@name='Kids']``
  - ``//categories[@name='Kids' and @numberOfBooks=1]``

**Limitations**
  - Only string and integer values are supported (boolean and float values are not supported).
  - Multiple attributes can only be combined using 'and'. 'or' and bracketing is not supported.

Query Extensions
================

Get Any Descendant by Any Ancestor Value
----------------------------------------

The ancestor extension can be added to any CPS path query.

**Syntax**: ``//<cps-path>/ancestor::<ancestor-path>``
  - ``cps-path``: Any CPS path query.
  - ``ancestor-path``:  Partial path to ancestors of the target node. This can contain one or more ancestor nodes separated by a /.

**Examples**
  - ``//book/ancestor::categories``
  - ``//categories[@genre="SciFi"]/book/ancestor::bookstore``
  - ``book/ancestor::categories[@code=1]/books``

**Limitations**
  - Ancestor list elements can only be addressed using the list key leaf.
  - List elements with multiple attributes are not supported.