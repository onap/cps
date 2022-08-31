.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2022 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING

.. _cpsScheduledProcesses:


CPS Scheduled Processes
#######################

.. toctree::
   :maxdepth: 1

Introduction
============

The following section is a list of the current scheduled processes running within the CPS system.

Module Sync
-----------
The module sync is a user :ref:`configurable timed process<additional-cps-ncmp-customizations>`,
which is set to search for CM-Handles within CPS with an *'ADVISED'* state.
Once the CM-Handle(s) is processed by the module sync, the CM-Handle state is then set to *'READY'*, if the process completes successfully.
If for any reason the module sync fails, the CM-Handle state will then be set to *'LOCKED'*,
and the reason for the lock will also be stored within CPS.

Data Sync
---------
The data sync is a user :ref:`configurable timed process<additional-cps-ncmp-customizations>`,
which is set to search for CM-Handles with a sync state of *'UNSYNCHRONIZED'*.
Once the CM-Handle(s) with a sync state of *'UNSYNCHRONIZED'* is processed by the data sync,
the CM-Handle sync state is then set to *'SYNCHRONIZED'*, if the process completes successfully.
If the data sync fails, the CM-Handle sync state will remain as *'UNSYNCHRONIZED'*, and will be re-attempted.

Retry Mechanism
---------------
The retry mechanism is a user :ref:`configurable timed process<additional-cps-ncmp-customizations>`,
which is used to search for CM-Handles which are currently in a *'LOCKED'* state.
If the CM-Handle is ready to be retried then, the CM-Handle(s) in a *'LOCKED'* state is processed by the retry mechanism,
the CM-Handle state is then set to *'ADVISED'*.
Whether the CM-Handle is ready to be retried is dependent on both the number of attempts to sync the CM-Handle,
and the last update time of the CM-Handle state.
With each new attempt to unlock the CM-Handle, the time until the CM-Handle can next be retried is doubled.
