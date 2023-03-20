.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2023 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _inventoryquerying:


Inventory Query Endpoints
#########################

.. toctree::
   :maxdepth: 1

Introduction
============

For querying the NCMP Inventory we have two Post endpoints:

- ncmpInventory/v1/ch/searches Returns all CM Handles which match the query properties provided. Gives a JSON payload of the **details** of all matching CM Handles.

If no matching CM Handles are found an empty array is returned.

Request Body
============

Currently this endpoint allows three criteria to be query on:

- *hasAllModules* returns CM Handles which have the module names provided.

- *hasAllProperties* returns CM Handles which have the properties (key and value) provided.

- *cmHandleWithCpsPath* returns CM Handles which match the CPS Path provided.

Not all request body combinations have been validated and as such request bodies which do not conform to the structure as documented here can produce results in which all CM Handles are returned.

Casing conventions: 'camelCasing' and 'kebab-casing'
----------------------------------------------------

.. note::
    By convention REST JSON return bodies use 'camelCasing'. By convention field names in yang modelled data use 'kebab-casing'. Therefore some inconsistencies can be seen in the JSON use in CPS REST interfaces. For CM Handle related endpoints we return data in 'camelCasing'. But for *cmHandleWithCpsPath*, the query is accessing yang modelled field names and as such needs to use 'kebab-casing'. Therefore the dmi-registry field names should be referenced when using the *cmHandleWithCpsPath* condition: :doc:`modeling`

Request Body example using all available query criteria. This query would return all CM Handles which have the specified modules my-module-(1-3), have the specified properties of Color yellow, Shape circle, Size small and are in a sync state of ADVISED:

.. code-block:: json

    {
      "cmHandleQueryParameters": [
        {
          "conditionName": "hasAllProperties",
          "conditionParameters": [
            {
              "Color": "yellow"
            },
            {
              "Shape": "circle"
            },
            {
              "Size": "small"
            }
          ]
        },
        {
          "conditionName": "hasAllAdditionalProperties",
          "conditionParameters": [
            {
              "Color": "yellow"
            },
            {
              "Shape": "circle"
            },
            {
              "Size": "small"
            }
          ]
        },
        {
          "conditionName": "cmHandleWithDmiPlugin",
          "conditionParameters": [
            {
              "dmiPluginName": "dmiPlugin1"
            }
          ]
        }
      ]
    }

Has all Properties
------------------

With the *hasAllProperties* condition, we can provide a list of property keys and values. The CM Handles returned will have these properties. The parameter names must be as below with key and value for each property. The returned CM Handle must have all supplied properties. For the example request, a CM Handle will be returned if it has properties where there is a key of "Color" with value "yellow", a key of "Shape" with value "circle" and a key of "Size" with value "small".

.. code-block:: json

    {
      "cmHandleQueryParameters": [
        {
          "conditionName": "hasAllProperties",
          "conditionParameters": [
            {
              "Color": "yellow"
            },
            {
              "Shape": "circle"
            },
            {
              "Size": "small"
            }
          ]
        }
      ]
    }

Has all additional Properties
-----------------------------

With the *hasAllAdditionalProperties* condition, we can provide a list of property keys and values. The CM Handles returned will have these properties. The parameter names must be as below with key and value for each property. The returned CM Handle must have all supplied properties. For the example request, a CM Handle will be returned if it has properties where there is a key of "Color" with value "yellow", a key of "Shape" with value "circle" and a key of "Size" with value "small".

.. code-block:: json

    {
      "cmHandleQueryParameters": [
        {
          "conditionName": "hasAllAdditionalProperties",
          "conditionParameters": [
            {
              "Color": "yellow"
            },
            {
              "Shape": "circle"
            },
            {
              "Size": "small"
            }
          ]
        }
      ]
    }

Has all DMI Plugins
-------------------

With the *hasAll* condition, we can provide a list of property keys and values. The CM Handles returned will have these properties. The parameter names must be as below with key and value for each property. The returned CM Handle must have all supplied properties. For the example request, a CM Handle will be returned if it has properties where there is a key of "Color" with value "yellow", a key of "Shape" with value "circle" and a key of "Size" with value "small".

.. code-block:: json

    {
      "cmHandleQueryParameters": [
        {
          "conditionName": "cmHandleWithDmiPlugin",
          "conditionParameters": [
            {
              "dmi-service-name": "my-dmi-plugin"
            }
          ]
        }
      ]
    }
