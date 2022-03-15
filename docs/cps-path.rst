.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021-2022 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _design:


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

      typedef year {
          type uint16 {
              range "1000..9999";
          }
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
                      type string;
                  }

                  leaf name {
                      type string;
                  }

                  leaf numberOfBooks {
                      type string;
                  }

                  container books {

                      list book {
                          key title;

                          leaf title {
                              type string;
                          }
                          leaf price {
                              type string;
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

The xml and json below describes some basic data to be used to illustrate the CPS Path functionality.

Sample Data in XML
==================

.. code-block:: xml

    <shops>
       <bookstore name="Chapters">
          <bookstore-name>Chapters</bookstore-name>
          <categories code="1" name="SciFi" numberOfBooks="2">
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
          <categories code="2" name="Kids" numberOfBooks="1">
             <books>
                <book title="Matilda" />
             </books>
          </categories>
       </bookstore>
    </shops>

**Note.** The CPS accepts only json data and the xml is for illustration purpose only.

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
              "code": "1",
              "name": "SciFi",
              "numberOfBooks": "2",
              "books": {
                "book": [
                  {
                    "title": "2001: A Space Odyssey",
                    "price": "5",
                    "label": ["sale", "classic"],
                    "edition": ["1968", "2018"]
                  },
                  {
                    "title": "Dune",
                    "price": "5",
                    "label": ["classic"],
                    "edition": ["1965"]
                  }
                ]
              }
            },
            {
              "code": "2",
              "name": "Kids",
              "numberOfBooks": "1",
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


**Note.** 'categories' is a Yang List and 'code' is its key leaf. All other data nodes are Yang Containers. 'label' and 'edition' are both leaf-lists.

General Notes
=============

- String values must be wrapped in quotation marks ``"`` (U+0022) or apostrophes ``'`` (U+0027).
- String comparisons are case sensitive.
- List key-fields containing ``\`` or ``@[`` will not be processed correctly when being referenced with such key values in absolute or descendant paths. This means such entries will be omitted from any query result. See `CPS-500 <https://jira.onap.org/browse/CPS-500>`_ Special Character Limitations of cpsPath Queries

Query Syntax
============

``( <absolute-path> | <descendant-path> ) [ <leaf-conditions> ] [ <text()-condition> ] [ <ancestor-axis> ]``

Each CPS path expression need to start with an 'absolute' or 'descendant' xpath.

absolute-path
-------------

**Syntax**: ``'/' <container-name> ( '[' <list-key> ']' )? ( '/' <containerName> ( '[' <list-key> ']' )? )*``

  - ``container name``: Any yang container or list.
  - ``list-key``:  One or more key-value pairs, each preceded by the ``@`` symbol, combined using the ``and`` keyword.
  - The above van repeated any number of times.

**Examples**
  - ``/shops/bookstore``
  - ``/shops/bookstore/categories[@code='1']``
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

**Syntax**: ``<xpath> '[' @<leaf-name1> '=' <leaf-value1> ( ' and ' @<leaf-name> '=' <leaf-value> )* ']'``
  - ``xpath``: Absolute or descendant or xpath to the (list) node which elements will be queried.
  - ``leaf-name``: The name of the leaf which value needs to be compared.
  - ``leaf-value``: The required value of the leaf.

**Examples**
  - ``/shops/bookstore/categories[@numberOfBooks='1']``
  - ``//categories[@name="Kids"]``
  - ``//categories[@name='Kids']``
  - ``//categories[@code='1']/books/book[@title='Dune' and @price='5']``

**Limitations**
  - Only the last list or container can be queried leaf values. Any ancestor list will have to be referenced by its key name-value pair(s).
  - Multiple attributes can only be combined using ``and``. ``or`` and bracketing is not supported.
  - Only leaves can be used, leaf-list are not supported.
  - Only string and integer values are supported, boolean and float values are not supported.

**Notes**
  - For performance reasons it does not make sense to query using key leaf as attribute. If the key value is known it is better to execute a get request with the complete xpath.

text()-condition
----------------

The text()-condition  can be added to any CPS path query.

**Syntax**: ``<cps-path> ( '/' <leaf-name> '[text()=' <string-value> ']' )?``
  - ``cps-path``: Any CPS path query.
  - ``leaf-name``: The name of the leaf or leaf-list which value needs to be compared.
  - ``string-value``: The required value of the leaf or leaf-list element as a string wrapped in quotation marks (U+0022) or apostrophes (U+0027). This wil still match integer values.

**Examples**
  - ``//book/label[text()="classic"]``
  - ``//book/edition[text()="1965"]``

**Limitations**
  - Only the last list or container can be queried for leaf values with a text() condition. Any ancestor list will have to be referenced by its key name-value pair(s).
  - Only one leaf or leaf-list can be tested.
  - Only string and integer values are supported, boolean and float values are not supported.
  - Since CPS cannot return individual leaves it will always return the container with all its leaves. Ancestor-axis can be used to specify a parent higher up the tree.
  - When querying a leaf value (instead of leaf-list) it is better, more performant to use a text value condition use @<leaf-name> as described above.

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
