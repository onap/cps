.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2023 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _xpath:


XPath
########

.. toctree::
   :maxdepth: 1

Introduction
============

In CPS , Xpath is a parameter used in several APIs, which allows us to retrieve JSON and XML data efficiently.
The XPath syntax provides us with the ability to navigate through the hierarchical structure of data used in CPS easily via specification of element names, values ,attributes etc.

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

Nodes
=====

- Xpaths uses 'nodes' to navigate through the data.
    - Nodes typically used in CPS:
        - container
        - list
        - leaf (CPS does not support addressing leaf nodes using XPath syntax)
- JSON data and XML data are node trees
    - the 'root node' contains all the configuration data nodes that exist at the top-level in all modules, serving as their parent node.

Relationships
=============

- Every node has a parent, not every node has a children
- Ancestor: a node's parent, parent's parent , parent's parent's parents …
    - In the given sample model and data, the top data node 'shops' is the ancestor of every other node
- Descendants: a node's children, children's children,  children's children's children
    - In the given sample model and data, the list element 'books' is a descendant of the container element 'books'

Supported XPath syntax in CPS
=============================

Selecting nodes
---------------

  - ``/``: Selects from the root node
    - **Note:** CPS uses absolute location path which means that XPath expression expected in CPS must start with / followed by the elements leading to the node being selected.
  - ``@``: Selects attributes
    - **Note:** Providing the attribute value can be enclosed in quotations i.e. ``[@code=1]`` will provide same result as ``[@code='1']``

.. list-table::
   :header-rows: 1

   * - XPath
     - Description
     - Selected node(s) without descendants
   * - - /
       - /shops
     - Selects the root element/ root node
     - .. code-block:: json

            [
              {
                "book-store:shops": {}
              }
            ]
   * - /shops/bookstore
     - Selects the bookstore container node
     - .. code-block:: json

            [
              {
                "book-store:bookstore": {
                  "name": "Chapters",
                  "bookstore-name": "Chapters"
                }
              }
            ]
   * - /shops/bookstore/categories[@code='1']
     - Selects 'categories' list element in 'bookstore' container that contains the key attribute 'code=1'
     - .. code-block:: json

            [
              {
                "book-store:categories": {
                  "code": 1,
                  "name": "SciFi",
                  "numberOfBooks": 2
                }
              }
            ]
   * - /shops/bookstore/categories[@code='1']/books
     - Selects the 'books' container node in 'categories' list element in 'bookstore' container that contains the key attribute 'code=1'
     - .. code-block:: json

            [
              {
                "book-store:books": {}
              }
            ]

