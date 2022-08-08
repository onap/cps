.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2022 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. cpsProcesses:


CPS Processes
#############

.. toctree::
   :maxdepth: 1

Introduction
============

The following section is a list of the current scheduled processes running within the CPS system.

Module Sync
-----------
The module sync is a user configurable timed process, which is set to search for CM-Handles within CPS with an *'ADVISED'* state.
Once the CM-Handle(s) in this state is picked up with an *'ADVISED'* state by the module sync,
a process will occur which will then set the CM-Handle state to *'READY'* if the process completes successfully.
If for any reason the module sync fails, the CM-Handle state will then be set to *'LOCKED'*,
and the reason for the lock will also be stored within CPS.

Data Sync
---------
The data sync is a user configurable timed process, which is set to search for CM-Handles with a sync state of *'UNSYNCHRONIZED'*.
Once the CM-Handle(s) in an *'UNSYNCHRONIZED'* are picked up by the data sync,
a process will occur which will then set the CM-Handle sync state to *'SYNCHRONIZED'*, once the process completes successfully.
If the data sync fails, the CM-Handle sync state will remain as *'UNSYNCHRONIZED'*, and will be re-attempted.


Retry Mechanism
---------------
The retry mechanism is a user configurable timed process,
which is used to search for CM-Handles which are currently in a *'LOCKED'* state.
Once the CM-Handle(s) in a *'LOCKED'* state are picked up by the retry mechanism,
a process will occur which will set the CM-Handle state to *'ADVISED'* if the CM-Handle is ready to be retried.
This is dependent on the number of attempts to sync the CM-Handle,
and the last update time of the CM-Handle state, with each attempt,
the time until the CM-Handle can next be retried is doubled.
