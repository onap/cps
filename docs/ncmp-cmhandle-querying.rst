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

For querying CM Handles we have two Post endpoints:

- ncmp/v1/ch/searches Returns all CM Handles which match the query properties provided. Gives a JSON payload of the **details** of all matching CM Handles.

- ncmp/v1/ch/id-searches Returns all CM Handles IDs or Alternate IDs which match the query properties provided. Gives a JSON payload of the **ids** of all matching CM Handles.

/searches returns whole CM Handle object (data) whereas /id-searches returns only CM Handle IDs or Alternate IDs. Otherwise these endpoints are intended to be functionally identical so both can be queried with the same request body. If no matching CM Handles are found an empty array is returned.

Parameters
==========

/id-searches can return either CM Handle IDs or Alternate IDs. This is controlled with an optional parameter outputAlternateId.

- *outputAlternateId=true* returns Alternate IDs

- *outputAlternateId=false* returns CM Handle IDs

Note: Null values will default to false so /id-searches & /id-searches?outputAlternateId will both return CM Handle IDs

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