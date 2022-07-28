CPS Events
##########

CPS Core
********
..
   Cps core events yet to be written


CPS-NCMP
********

Lifecycle Management (LCM) Event
================================


Overview
--------
Lifecycle management events are published as cm handle state transitions from one state to another.


LCM events and state handler
----------------------------
The LCM events are triggered under the state handler which has the following responsibilities:

#. Updating and persisting cm handle state based on the current state of the cm handle

#. Create and calls to publish the LCM event based on the cm handle state transition that occured

	**3 possible event types:**

	* Create
	* Update
	* Delete



LCM Event Schema
----------------
The published LCM event is based on the following schema:

.. code-block:: json

 {
  "$schema": "https://json-schema.org/draft/2019-09/schema",
  "$id": "urn:cps:org.onap.ncmp.cmhandle.lcm-event:v1",

  "$ref": "#/definitions/LcmEvent",

  "definitions": {

    "Values": {
      "description": "Values that represents the state of a cmHandle",
      "type": "object",
      "properties": {
        "dataSyncEnabled":{
          "description": "Whether data sync enabled",
          "type": "boolean"
        },
        "cmHandleState": {
          "description": "State of cmHandle",
          "type": "string",
          "enum": ["ADVISED", "READY", "LOCKED", "DELETING", "DELETED"]
        },
        "cmHandleProperties": {
          "description": "cmHandle properties",
          "type": "object",
          "default": null,
          "existingJavaType": "java.util.List<java.util.Map<String,String>>",
          "additionalProperties": false
        }
      },
      "additionalProperties": false
    },

    "Event": {
      "description": "The Payload of an event",
      "type": "object",
      "properties": {
        "cmHandleId": {
          "description": "cmHandle id",
          "type": "string"
        },
        "oldValues": {
          "$ref": "#/definitions/Values"
        },
        "newValues": {
          "$ref": "#/definitions/Values"
        }
      },
      "required": [
        "cmHandleId"
      ],
      "additionalProperties": false
    },

    "LcmEvent": {
      "description": "The payload for LCM event",
      "type": "object",
      "javaType" : "org.onap.ncmp.cmhandle.event.lcm.LcmEvent",
      "properties": {
        "eventId": {
          "description": "The unique id identifying the event",
          "type": "string"
        },
        "eventCorrelationId": {
          "description": "The id identifying the event",
          "type": "string"
        },
        "eventTime": {
          "description": "The timestamp when original event occurred",
          "type": "string"
        },
        "eventSource": {
          "description": "The source of the event",
          "type": "string"
        },
        "eventType": {
          "description": "The type of the event",
          "type": "string"
        },
        "eventSchema": {
          "description": "The schema that this event adheres to",
          "type": "string"
        },
        "eventSchemaVersion": {
          "description": "The version of the schema that this event adheres to",
          "type": "string"
        },
        "event": {
          "$ref": "#/definitions/Event"
        }
      },
      "required": [
        "eventId",
        "eventCorrelationId",
        "eventTime",
        "eventSource",
        "eventType",
        "eventSchema",
        "eventSchemaVersion",
        "event"
      ],
      "additionalProperties": false
      }
    }
  }



LCM Event structure
-------------------

Events header
^^^^^^^^^^^^^
*Event header prototype for all event types*

.. code-block:: json

	{
  		"eventId"                : "00001",
  		"eventCorrelationId      : "cmhandle-001",
  		"eventTime"              : "2021-11-16T16:42:25-04:00",
  		"eventSource"            : "org.onap.ncmp",
  		"eventType"              : "org.onap.ncmp.cmhandle-lcm-event.create/update/delete",
  		"eventSchema"            : "org.onap.ncmp:cmhandle-lcm-event",
  		"eventSchemaVersion"	   : "1.0"
  		"event": ....
	}

Events payload
^^^^^^^^^^^^^^
Event payload varies based on the type of event.

**CREATE**

Event payload for this event contains the properties of the new cm handle created

*Create event payload prototype*

.. code-block:: json

  "event": {
         "cmHandleId" : "cmhandle-001",
         "newValues" : {
             "cmHandleState"  : "ADVISED",
             "dataSyncEnabled" : "TRUE",
             "cmhandleProperties" : [
                          "prop1" : "val1",
                          "prop2" : "val2"
                ]
            }
       }
   }


**UPDATE**

Event payload for this event contains the properties of the updated cm handle and its initial properties

*Update event payload prototype*

.. code-block:: json

  "event": {
         "cmHandleId" : "cmhandle-001",
         "oldValues" : {
                 "cmHandleState"  : "ADVISED",
                 "dataSyncEnabled" : "FALSE",
                 "cmhandleProperties" : [
                          "prop1" : "val1",
                          "prop2" : "val2",
              }
          "newValues" : {
             "cmHandleState"  : "READY",
             "dataSyncEnabled" : "TRUE",
             "cmhandleProperties" : [
                          "prop1" : "updatedval1",
                          "prop2" : "updatedval2"
                   ]
            }
       }
   }


**DELETE**

Event payload for this event contains the identifier of the deleted cm handle

*Delete event payload prototype*

.. code-block:: json

  "event": {
         "cmHandleId" : "cmhandle-001",
   }


