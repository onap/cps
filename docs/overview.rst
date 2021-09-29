.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021 Pantheon.tech, Nordix Foundation
.. _overview:

CPS Overview
============

.. warning:: draft

The Configuration Persistence Service (CPS) is a platform component that is designed to serve as a
data repository for runtime data that needs persistence.

Types of data that is stored:

- **Configuration Parameters**

  These are configuration parameters that are used by xNFs during installation & commissioning. Configuration
  parameters are typically used before the xNF has been brought up or is operational. For example, a 5G Network
  configuration parameter for a PNFs that sets the mechanical tilt which is a configuration setting upon
  installation.

- **Operational Parameters**

  This operational information could be either an actual state or configuration of a network service or device.
  These are parameters that are derived, discovered, computed that are used by xNFs during run time AFTER the
  xNF becomes operational i.e. AFTER it has "booted up", been installed or configured. For example, in 5G Network,
  5G PNFs may need to adjust a tower electrical antenna tilt. These operational parameters are Exo-inventory
  information, meaning it is information that doesn't belong in A&AI. In principle, some parameters might be both
  configuration and operational parameters depending on how they are used.

NCMP
-----------

The Network Configuration Management Proxy (NCMP) provides access to network configuration data.
NCMP accesses all network Data-Model-Inventory (DMI) information via the NCMP-DMI-Plugin.

NCMP-DMI-Plugin
---------------

The Data-Model-Inventory (DMI) Plugin is used to allow the DMI API's access to the corresponding NCMP API's.

Temporal
-----------

This service is responsible to provide a time oriented perspective for
operational network data. It provides features to store and retrieve sequences
of configurations or states along with the associated times when they occurred
or have been observed.

CPS Project
-----------

Wiki: `Configuration Persistence Service Project <https://wiki.onap.org/display/DW/Configuration+Persistence+Service+Project>`_

Contact Information
-------------------

onap-discuss@lists.onap.org

Meeting details `Join  <https://zoom.us/j/836561560?pwd=TTZNcFhXTWYxMmZ4SlgzcVZZQXluUT09>`_
`Agenda <https://wiki.onap.org/pages/viewpage.action?pageId=111117075>`_
