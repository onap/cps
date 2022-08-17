.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2022 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _cpsEvents:

CPS Events
##########

CPS-NCMP
********

Lifecycle Management (LCM) Event
================================


Overview
--------
Lifecycle management events are published as cm handle state transitions from one state to another.


LCM events and state handler
----------------------------
LCM events are triggered by a change in cm handle state. It has the following responsibilities:

#. Updating and persisting cm handle state based on the target state of the cm handle

#. Create and calls to publish the LCM event based on the cm handle state transition that occurred

	**3 possible event types:**

	* Create
	* Update
	* Delete



LCM Event Schema
----------------
The current published LCM event is based on the following schema:

:download:`Life cycle management event schema <schemas/lcm-event-schema-v1.json>`

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
  		"eventType"              : "org.onap.ncmp.cmhandle-lcm-event.create",
  		"eventSchema"            : "org.onap.ncmp:cmhandle-lcm-event",
  		"eventSchemaVersion"	   : "1.0"
  		"event": ....
	}

Events payload
^^^^^^^^^^^^^^
Event payload varies based on the type of event.

**CREATE**

Event payload for this event contains the properties of the new cm handle created.

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

Event payload for this event contains the difference in state and properties of the cm handle.

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

Event payload for this event contains the identifier of the deleted cm handle.

*Delete event payload prototype*

.. code-block:: json

  "event": {
         "cmHandleId" : "cmhandle-001",
   }


