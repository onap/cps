.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021-2025 Nordix Foundation
.. Modifications Copyright (C) 2023 Deutsche Telekom AG

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _path:


CPS Path
########

.. toctree::
   :maxdepth: 1

Introduction
============

Several CPS APIs use the CPS path (or cpsPath in Java API) parameter.
The CPS path parameter is used for querying xpaths. CPS path is inspired by the `XML Path Language (XPath) 3.1. <https://www.w3.org/TR/2017/REC-xpath-31-20170321/>`_

This section describes the functionality currently supported by CPS Path.

Sample Yang Model
=================

.. code-block::

  module stores {
      yang-version 1.1;
      namespace "org:onap:ccsdk:sample";

      prefix book-store;

      revision "2020-09-15" {
          description
            "Sample Model";
      }
      container shops {

          container bookstore {

              leaf bookstore-name {
                  type string;
              }

              leaf name {
                  type string;
              }

              list categories {

                  key "code";

                  leaf code {
                      type uint16;
                  }

                  leaf name {
                      type string;
                  }

                  leaf numberOfBooks {
                      type uint16;
                  }

                  container books {

                      list book {
                          key title;

                          leaf title {
                              type string;
                          }
                          leaf price {
                              type uint16;
                          }
                          leaf-list label {
                              type string;
                          }
                          leaf-list edition {
                              type string;
                          }
                      }
                  }
              }
          }
      }
  }

**Note.** 'categories' is a Yang List and 'code' is its key leaf. All other data nodes are Yang Containers. 'label' and 'edition' are both leaf-lists.

**Note.** CPS accepts only json data. The xml data presented here is for illustration purposes only.

The json and xml below describes some basic data to be used to illustrate the CPS Path functionality.

Sample Data in Json
===================

.. code-block:: json

    {
      "shops": {
        "bookstore": {
          "bookstore-name": "Chapters",
          "name": "Chapters",
          "categories": [
            {
              "code": 1,
              "name": "SciFi",
              "numberOfBooks": 2,
              "books": {
                "book": [
                  {
                    "title": "2001: A Space Odyssey",
                    "price": 5,
                    "label": ["sale", "classic"],
                    "edition": ["1968", "2018"]
                  },
                  {
                    "title": "Dune",
                    "price": 5,
                    "label": ["classic"],
                    "edition": ["1965"]
                  }
                ]
              }
            },
            {
              "code": 2,
              "name": "Kids",
              "numberOfBooks": 1,
              "books": {
                "book": [
                  {
                    "title": "Matilda"
                  }
                ]
              }
            }
          ]
        }
      }
    }

Sample Data in XML
==================

.. code-block:: xml

    <shops>
       <bookstore name="Chapters">
          <bookstore-name>Chapters</bookstore-name>
          <categories code=1 name="SciFi" numberOfBooks="2">
             <books>
                <book title="2001: A Space Odyssey" price="5">
                   <label>sale</label>
                   <label>classic</label>
                   <edition>1968</edition>
                   <edition>2018</edition>
              </book>
                <book title="Dune" price="5">
                   <label>classic</label>
                   <edition>1965</edition>
                </book>
             </books>
          </categories>
          <categories code=2 name="Kids" numberOfBooks="1">
             <books>
                <book title="Matilda" />
             </books>
          </categories>
       </bookstore>
    </shops>

General Notes
=============

- String values must be wrapped in quotation marks ``"`` (U+0022) or apostrophes ``'`` (U+0027).
- Quotations marks and apostrophes can be escaped by doubling them in their respective quotes, for example ``'CPS ''Path'' Query' -> CPS 'Path' Query``
- String comparisons are case sensitive.

Query Syntax
============

``( <absolute-path> | <descendant-path> ) [ <leaf-conditions> ] [ <text()-condition> ] [ <contains()-condition> ] [ <ancestor-axis> ] [ <attribute-axis> ]``

Each CPS path expression need to start with an 'absolute' or 'descendant' xpath.

absolute-path
-------------

**Syntax**: ``'/' <container-name> ( '[' <list-key> ']' )? ( '/' <containerName> ( '[' <list-key> ']' )? )*``

  - ``container name``: Any yang container or list.
  - ``list-key``:  One or more key-value pairs, each preceded by the ``@`` symbol, combined using the ``and`` keyword.
  - The above van repeated any number of times.

**Examples**
  - ``/shops/bookstore``
  - ``/shops/bookstore/categories[@code='1']/books``
  - ``/shops/bookstore/categories[@code='1']/books/book[@title='2001: A Space Odyssey']``

**Limitations**
  - Absolute paths must start with the top element (data node) as per the model tree.
  - Each list reference must include a valid instance reference to the key for that list. Except when it is the last element.

descendant-path
---------------

**Syntax**: ``'//' <container-name> ( '[' <list-key> ']' )? ( '/' <containerName> ( '[' <list-key> ']' )? )*``

  - The syntax of a descendant path is identical to a absolute path except that it is preceded by a double slash ``//``.

**Examples**
  - ``//bookstore``
  - ``//categories[@code='1']/books``
  - ``//bookstore/categories``

**Limitations**
  - Each list reference must include a valid instance reference to the key for that list.  Except when it is the last element.

leaf-conditions
---------------

**Syntax**: ``<xpath> '[' @<leaf-name1> '(=|>|<|>=|<=)' <leaf-value1> ( ' <and|or> ' @<leaf-name> '(=|>|<|>=|<=)' <leaf-value> )* ']'``
  - ``xpath``: Absolute or descendant or xpath to the (list) node which elements will be queried.
  - ``leaf-name``: The name of the leaf which value needs to be compared.
  - ``leaf-value``: The required value of the leaf.

**Examples**
  - ``/shops/bookstore/categories[@numberOfBooks=1]``
  - ``//categories[@name="Kids"]``
  - ``//categories[@name='Kids']``
  - ``//categories[@code='1']/books/book[@title='Dune' and @price=5]``
  - ``//categories[@code='1']/books/book[@title='xyz' or @price=15]``
  - ``//categories[@code='1']/books/book[@title='xyz' or @price>20]``
  - ``//categories[@code='1']/books/book[@title='Dune' and @price<=5]``
  - ``//categories[@code=1]``
**Limitations**
  - Only the last list or container can be queried leaf values. Any ancestor list will have to be referenced by its key name-value pair(s).
  - When mixing ``and/or`` operators, ``and`` has precedence over ``or`` . So ``and`` operators get evaluated first.
  - Bracketing is not supported.
  - Leaf names are not validated so ``or`` operations with invalid leaf names will silently be ignored.
  - Only leaves can be used, leaf-list are not supported.
  - Only string and integer values are supported, boolean and float values are not supported.
  - Using comparative operators with string values will lead to an error at runtime. This error can't be validated earlier as the datatype is unknown until the execution phase.
  - The key should be supplied with correct data type for it to be queried from DB. In the last example above the attribute code is of type
    Integer so the cps query will not work if the value is passed as string.
    e.g.: ``//categories[@code="1"]`` or ``//categories[@code='1']`` will not work because the key attribute code is treated a string.

**Notes**
  - For performance reasons it does not make sense to query using key leaf as attribute. If the key value is known it is better to execute a get request with the complete xpath.

text()-condition
----------------

The text()-condition  can be added to any CPS path query.

**Syntax**: ``<cps-path> ( '/' <leaf-name> '[text()=' <string-value> ']' )?``
  - ``cps-path``: Any CPS path query.
  - ``leaf-name``: The name of the leaf or leaf-list which value needs to be compared.
  - ``string-value``: The required value of the leaf or leaf-list element as a string wrapped in quotation marks (U+0022) or apostrophes (U+0027). This will still match integer values.

**Examples**
  - ``//book/label[text()="classic"]``
  - ``//book/edition[text()="1965"]``

**Limitations**
  - Only the last list or container can be queried for leaf values with a text() condition. Any ancestor list will have to be referenced by its key name-value pair(s).
  - Only one leaf or leaf-list can be tested.
  - Only string and integer values are supported, boolean and float values are not supported.
  - Since CPS cannot return individual leaves it will always return the container with all its leaves. Ancestor-axis can be used to specify a parent higher up the tree.
  - When querying a leaf value (instead of leaf-list) it is better, more performant to use a text value condition use @<leaf-name> as described above.

contains()-condition
--------------------

**Syntax**: ``<cps-path> '[' 'contains' '(' '<leaf-name>','<string-value>' ')' ']'?``
  - ``cps-path``: Any CPS path query.
  - ``leaf-name``: The name of the leaf which value needs to be compared.
  - ``string-value``: The required value of the leaf element as a string wrapped in quotation marks (U+0022) or apostrophes (U+0027). This will still match integer values.

**Examples**
  - ``//categories[contains(@name,'Sci')]``
  - ``//books[contains(@title,'Space')]``

**Limitations**
  - Only leaves can be used, leaf-list are not supported.
  - Leaf names are not validated so ``contains() condition`` with invalid leaf names will silently be ignored.

**Notes**
  - contains condition is case sensitive.

ancestor-axis
-------------

The ancestor axis can be added to any CPS path query but has to be the last part.

**Syntax**: ``<cps-path> ( '/ancestor::' <ancestor-path> )?``
  - ``cps-path``: Any CPS path query.
  - ``ancestor-path``: Partial path to ancestors of the target node. This can contain one or more ancestor nodes separated by a ``/``.

**Examples**
  - ``//book/ancestor::categories``
  - ``//categories[@code='2']/books/ancestor::bookstore``
  - ``//book/ancestor::categories[@code='1']/books``
  - ``//book/label[text()="classic"]/ancestor::shops``

**Limitations**
  - Ancestor list elements can only be addressed using the list key leaf.
  - List elements with compound keys are not supported.

attribute-axis
--------------

The attribute axis can be added to a CPS path query at the end. It will return only distinct values of a specified leaf.

**Syntax**: ``<cps-path> ( '/@' <leaf-name> )?``
  - ``cps-path``: Any CPS path query.
  - ``leaf-name``: The name of the leaf (attribute) for which values should be returned.

**Examples**
  - ``//categories/@name``
  - ``//categories[@code='1']/books/@price``
  - ``//books/ancestor::bookstore/@bookstore-name``

**Notes**
  - The output is a list of attribute-value pairs. For example, ``[{"name":"Kids"},{"name":"SciFi"}]``
  - Only unique values will be returned. For example, if 3 books have a price of 5, then 5 will be returned only once.
