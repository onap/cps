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

For querying the NCMP Inventory we have one Post endpoint:

- ncmpInventory/v1/ch/searches Returns all CM Handles which match the query properties provided. Gives a JSON payload of the **details** of all matching CM Handles.

If no matching CM Handles are found an empty array is returned.

Request Body
============

Currently this endpoint allows three criteria to be query on:

- *hasAllProperties* returns CM Handles which have the public properties provided.

- *hasAllAdditionalProperties* returns CM Handles which have the private or additional properties (key and value) provided.

- *cmHandleWithDmiPlugin* returns CM Handles which match the CPS Dmi Plugin provided.

Not all request body combinations have been validated and as such request bodies which do not conform to the structure as documented here can produce results in which all CM Handles are returned.

Casing conventions: 'camelCasing' and 'kebab-casing'
----------------------------------------------------

.. note::
    By convention REST JSON return bodies use 'camelCasing'. By convention field names in yang modelled data use 'kebab-casing'. Therefore some inconsistencies can be seen in the JSON use in CPS REST interfaces. For CM Handle related endpoints we return data in 'camelCasing'. But for *cmHandleWithCpsPath*, the query is accessing yang modelled field names and as such needs to use 'kebab-casing'. Therefore the dmi-registry field names should be referenced when using the *cmHandleWithCpsPath* condition: :doc:`modeling`

Request Body example using all available query criteria. This query would return all CM Handles which have the specified public properties of Color yellow, Shape circle, Size small, have the specified private/additional properties of Price 5, Year 2022 and Quantity 12 and have related to DMI plugin dmiPlugin1:

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
              "Price": "5"
            },
            {
              "Year": "2022"
            },
            {
              "Quantity": "12"
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

With the *hasAllProperties* condition, we can provide a list of property keys and values. The CM Handles returned will have these public properties. The parameter names must be as below with key and value for each property. The returned CM Handle must have all supplied properties. For the example request, a CM Handle will be returned if it has properties where there is a key of "Color" with value "yellow", a key of "Shape" with value "circle" and a key of "Size" with value "small".

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

With the *hasAllAdditionalProperties* condition, we can provide a list of property keys and values. The CM Handles returned will have these additional properties. The parameter names must be as below with key and value for each property. The returned CM Handle must have all supplied properties. For the example request, a CM Handle will be returned if it has properties where there is a key of "Price" with value "5", a key of "Year" with value "2022" and a key of "Quantity" with value "12".

.. code-block:: json

    {
      "cmHandleQueryParameters": [
        {
          "conditionName": "hasAllAdditionalProperties",
          "conditionParameters": [
            {
              "Price": "5"
            },
            {
              "Year": "2022"
            },
            {
              "Quantity": "12"
            }
          ]
        }
      ]
    }

Has all DMI Plugins
-------------------

With the *cmHandleWithDmiPlugin* condition, we can provide a dmiPluginName. The CM Handles returned will have this DMI Plugin Name value in at least one of the following related leaves: dmi-service-name, dmi-data-service-name and dmi-model-service-name. For the example request a CM Handle will be returned if it has my-dmi-plugin as a value in at least one of the aforementioned leaves.

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

CM Handle search with CPS Path
------------------------------

The *cmHandleWithCpsPath* condition allows any data of the CM Handle to be queried as long as it is accessible by CPS path. CPS path is described in detail in :doc:`cps-path`. For this endpoint, the ancestor axis for CM Handles is appended automatically so that a CM Handle is always returned. For example ``/dmi-registry/cm-handles[@module-set-tag='']`` will become ``/dmi-registry/cm-handles[@module-set-tag='']/ancestor::cm-handles``.

.. code-block:: json

    {
      "cmHandleQueryParameters": [
        {
          "conditionName": "cmHandleWithCpsPath",
          "conditionParameters": [
            {
              "cpsPath": "/dmi-registry/cm-handles[@module-set-tag='some-value or empty']"
            }
          ]
        }
      ]
    }