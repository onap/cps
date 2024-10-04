.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2024 TechMahindra Ltd.
.. _cpsDelta:

.. toctree::
   :maxdepth: 1

CPS Delta Feature
#################

- The concept of CPS Delta Feature is to have the ability to find the delta or difference between two configurations in CPS.

- The Delta feature provides two new endpoints, providing the following functionalities:

    - Ability to find the delta between the configurations stored in two anchors within the same dataspace.
    - Ability to find the delta between the configurations stored under an anchor and a JSON payload provided as part of REST request.

The difference found are compiled into a Delta Report returned by the server. This report can be used in several different ways, one such use case is sending it over the Kafka Notification Service to the user.

Delta Report Format
-------------------

The Delta Report is based on the RFC 6902 and RFC 9144, which define a set of parameters to be included in the Delta Report. In CPS only the relevant parameters defined in the RFCs are used. These include:
    - **action:** This parameter defines how the data nodes changed between two configurations. If data nodes are added, deleted or modified then the 'action' parameter in the delta report will be set to one of the following pre-defined operation values, i.e., 'create', 'remove' or 'replace' respectively for each data node.
    - **xpath:** This parameter will provide the xpath of each data node that has been either added, deleted or modified.
    - **source-data:** this parameter is added to the delta report when a data node is removed or updated, this represents the source/reference data against which the delta is being generated. In case of newly added data node this field is not included in the delta report.
    - **target-data:** this parameter is added to the delta report when a data node is added or updated, this represents the data values that are being compared to the source data. In case of a data node being removed this field is not included in the delta report.

**Note.** In case of an existing data node being modified, both the source-data and target-data fields are present in the delta report.
**Additional Information.** `Analysis of RFC 9144 and RFC 6902 for Delta Report generation <https://lf-onap.atlassian.net/wiki/spaces/DW/pages/16523520/Analysis+of+RFC+9144+and+RFC6902+for+Delta+Report+generation>`_

Mechanism for Delta generation
------------------------------

.. image:: _static/cps-delta-mechanism.png
   :alt: Mechanism for Delta generation

Endpoints provided as part of Delta Feature
-------------------------------------------

.. toctree::
   :maxdepth: 1

   cps-delta-endpoints.rst