.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _design:


CPS Design
##########

.. toctree::
   :maxdepth: 1

Offered APIs
============

CPS supports the public APIs listed in the link below:

:download:`OpenApi Specification <api/swagger/openapi.yml>`

Exposed API
-----------

The standard for API definition in the RESTful API world is the OpenAPI Specification (OAS).
The OAS 3, which is based on the original "Swagger Specification", is being widely used in API developments.

Specification can be accessed using following URI:

.. code-block:: bash

  “http://<hostname>:<port>/v3/api-docs?group=cps-docket”

CPS Path
========

Several CPS APIs use the cps-path (or cpsPath in Java API) parameter.
The CPS Path parameter is used for querying xpaths. CPS Path is a limited version of the `XML Path Language (XPath) 3.1. <https://www.w3.org/TR/2017/REC-xpath-31-20170321/>`_

This section describes the functionality currently supported by CPS Path.

The xml below describes some basic data to be used to illustrate the CPS Path functionality.

.. code-block:: xml
    <shops>
       <bookstore name="Chapters">
         <bookstore-name>Chapters</bookstore-name>
         <categories code="1" name="SciFi">
           <books>
             <book name="Space Odyssee"/>
             <book name="Dune"/>
           </books>
        </categories>
        <categories code="2" name="Kids">
           <books>
             <book name="Matilda"/>
           </books>
        </categories>
       </bookstore>
    </shops>

**Note.** 'categories' is a Yang List all other data nodes are Yang Containers

General Notes
-------------
- String values must be wrapped in quotation marks (U+0022) or apostrophes (U+0027).
- String comparisons are case sensitive.


Get List Elements by Any Attribute Value
----------------------------------------

**Syntax**: ``<xpath>/targetNode/[@<leaf-name>=<leaf-value>]``
  - ``xpath``: The xpath to the parent of the target node including all ancestors.
  - ``targetNode``: The name of the (list) node from which element will be selected.
  - ``leafName``: The name of the leaf who's value needs to be compared.
  - ``leafValue``: The required value of the leaf.

**Examples**
  - ``/shops/bookstore/categories[@code=1]``
  - ``/shops/bookstore/categories[@name="Kids"]``
  - ``/shops/bookstore/categories[@name='Kids']``

**Limitations**
  - Only one list (last descendant) can be queried for a non-key value. Any ancestor list will have to be referenced by its key name-value pair(s).
  - Only one attribute can be queried.
  - Only string and integer values are supported (boolean and float values are not supported).

**Notes**
  - For performance reasons it does not make sense to query the list key attribute. If the key value is known it is beter to use a get request with the complete xpath.

Get Any Descendant
------------------
TBC