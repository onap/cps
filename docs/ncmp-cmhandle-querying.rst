.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2022-2024 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _cmhandlequerying:


CM Handle Query Endpoints
#########################

.. toctree::
   :maxdepth: 1

Introduction
============

For querying CM Handles there are four Post endpoints:

+-------------------------------------+------------------+----------------------+
| Endpoint                            | Target           | Output               |
+-------------------------------------+------------------+----------------------+
| ncmp/v1/ch/searches                 | northbound: rApp | cm-handle references |
+-------------------------------------+------------------+----------------------+
| ncmp/v1/ch/id-searches              | northbound: rApp | cm-handle objects    |
+-------------------------------------+------------------+----------------------+
| ncmpInventory/v1/ch/searches        | southbound: dmi  | cm-handle references |
+-------------------------------------+------------------+----------------------+
| ncmpInventory/v1/ch/searchCmHandles | southbound: dmi  | cm-handle objects    |
+-------------------------------------+------------------+----------------------+

All these endpoints use the request body structure to define to search conditions and similar parameters described in the next sections.
If no matching CM Handles are found an empty array is returned.


Parameters
==========

+------------------------------------+-------------------+--------------------------------------+------------------------------------+
| Parameter                          | Value             | Behavior                             | Scope                              |
+------------------------------------+-------------------+--------------------------------------+------------------------------------+
| outputAlternateId                  | true              | return Alternate Ids                 | cm-handle reference searches       |
+------------------------------------+-------------------+--------------------------------------+------------------------------------+
| outputAlternateId                  | false / undefined | return CM Handle Ids                 | cm-handle reference searches       |
+------------------------------------+-------------------+--------------------------------------+------------------------------------+
| includeAdditionalPropertiesInQuery | true              | include additional properties        | southbound cm-handle object search |
+------------------------------------+-------------------+--------------------------------------+------------------------------------+
| includeAdditionalPropertiesInQuery | false / undefined | do not include additional properties | southbound cm-handle object search |
+------------------------------------+-------------------+--------------------------------------+------------------------------------+


Request Body
============

Supported search criteria (conditionName in request body)

+----------------------------+---------------------------------------------------------------+---------------------------|
| Condition Name             | Description                                                   | Scope                     |
+----------------------------+---------------------------------------------------------------+---------------------------|
| hasAllModules              | find cm handles that have all the given modules               | both interfaces           |
+----------------------------+---------------------------------------------------------------+---------------------------|
| hasAllProperties           | find cm handles that have all the given public properties     | both interfaces           |
+----------------------------+---------------------------------------------------------------+---------------------------|
| hasAllAdditionalProperties | find cm handles that have all the given additional properties | southbound interface only |
+----------------------------+---------------------------------------------------------------+---------------------------|
| cmHandleWithCpsPath        | find cm handles that match the CPS Path provided              | both interfaces           |
+----------------------------+---------------------------------------------------------------+---------------------------|
| cmHandleWithDmiPlugin      | find cm handles registered by the given dm plugin             | southbound interface only |
+----------------------------+---------------------------------------------------------------+---------------------------|
| cmHandleWithTrustLevel     | find cm handles with the given trust level                    | both interfaces           |
+----------------------------+---------------------------------------------------------------+---------------------------|


Casing conventions: 'camelCasing' and 'kebab-casing'
----------------------------------------------------

.. note::
    By convention REST JSON return bodies use 'camelCasing'. By convention field names in yang modelled data use 'kebab-casing'. Therefore some inconsistencies can be seen in the JSON use in CPS REST interfaces. For CM Handle related endpoints we return data in 'camelCasing'. But for *cmHandleWithCpsPath*, the query is accessing yang modelled field names and as such needs to use 'kebab-casing'. Therefore the dmi-registry field names should be referenced when using the *cmHandleWithCpsPath* condition: :doc:`modeling`

Examples
========

Has all Modules
---------------

With the *hasAllModules* condition, we can provide a list of module names. The CM Handles returned will have these module names. The parameter names must be as below with the key of each of the module names being "moduleName" where "my-module-X" is to be replaced with the name of the module to query with. The returned CM Handle must have all supplied modules. For the example request, a CM Handle will be returned if it has "my-module-1", "my-module-2" and "my-module-3".

.. code-block:: json

    {
      "cmHandleQueryParameters": [
        {
          "conditionName": "hasAllModules",
          "conditionParameters": [
            {
              "moduleName": "my-module-1"
            },
            {
              "moduleName": "my-module-2"
            },
            {
              "moduleName": "my-module-3"
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


CM Handle with CPS Path
-----------------------

The *cmHandleWithCpsPath* condition allows any data of the CM Handle to be queried as long as it is accessible by CPS path. CPS path is described in detail in :doc:`cps-path`. For this endpoint, the ancestor of CM Handles is appended automatically so that a CM Handle is always returned. For example ``//state[@cm-handle-state='LOCKED']`` will become ``//state[@cm-handle-state='LOCKED']/ancestor::cm-handles``. The yang model for the dmi-registry can be found in :doc:`modeling` under the NCMP Modeling Section. Please note that although CM Handle additional-properties are shown in the dmi-registry yang model, these are considered private properties and cannot be used to query CM Handles. Any attempt to use the additional-properties to query will return an empty array.

.. code-block:: json

    {
      "cmHandleQueryParameters": [
        {
          "conditionName": "cmHandleWithCpsPath",
          "conditionParameters": [
            {
              "cpsPath": "//state[@cm-handle-state='LOCKED']"
            }
          ]
        }
      ]
    }

With DMI Plugin
---------------

With the *cmHandleWithDmiPlugin* condition, we can provide a dmiPluginName. The CM Handles returned will have this DMI Plugin Name value in at least one of the following registered service names: dmi-service-name, dmi-data-service-name and dmi-model-service-name. For the example request a CM Handle will be returned if it has my-dmi-plugin as a value in at least one of the aforementioned leaves.

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


CM Handle with Trust Level
--------------------------

With the *cmHandleWithTrustLevel* condition, we can provide just one trust level. The CM handles returned will have this trust level. Providing more than one parameter causes unexpected results. Condition parameter name is not being validated.

.. code-block:: json

    {
      "cmHandleQueryParameters": [
        {
          "conditionName": "cmHandleWithTrustLevel",
          "conditionParameters": [
            {
              "trustLevel": "COMPLETE"
            }
          ]
        }
      ]
    }

Combining Conditions
--------------------

Request Body example using several conditions. This query would return all CM Handles which have the specified modules my-module-(1-3), have the specified properties of Color yellow, Shape circle, Size small and are in a sync state of ADVISED:

.. code-block:: json

    {
      "cmHandleQueryParameters": [
        {
          "conditionName": "hasAllModules",
          "conditionParameters": [
            {
              "moduleName": "my-module-1"
            },
            {
              "moduleName": "my-module-2"
            },
            {
              "moduleName": "my-module-3"
            }
          ]
        },
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
          "conditionName": "cmHandleWithCpsPath",
          "conditionParameters": [
            {
              "cpsPath": "//state[@cm-handle-state='ADVISED']"
            }
          ]
        },
        {
          "conditionName": "cmHandleWithTrustLevel",
          "conditionParameters": [
            {
              "trustLevel": "COMPLETE"
            }
          ]
        }
      ]
    }
