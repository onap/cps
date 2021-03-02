.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021 Pantheon.tech
.. _overview:

CPS Overview
============

The Configuration Persistence Service (CPS) is a platform component that is designed to serve as a 
data repository for runtime data that needs to be persistent.

The types of data that is stored in the Run-Time data storage repository for:

- **Configuration Parameters**

  These are configuration parameters that are used by xNFs during installation & commissioning. Configuration
  parameters are typically used before the xNF has been brought up or is operational. For example, a 5G Network
  configuration parameter for a PNFs that sets the mechanical tilt which is a configuration setting upon
  installation.

- **Operational Parameters**

  These are parameters that are derived, discovered, computed that are used by xNFs during run time AFTER the
  xNF becomes operational i.e. AFTER it has "booted up", been installed, configure. For example, in 5G Network,
  5G PNFs may need to adjust a tower electrical antenna tilt. These operational parameters are Exo-inventory
  information, meaning it is information that doesn't belong in A&AI. In principle, some parameters might be both
  configuration and operational parameters depending on how they are used.