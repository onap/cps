.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
.. Modifications Copyright (C) 2021 Bell Canada.

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _deployment:

CPS Deployment
##############

.. contents::
    :depth: 2

Database configuration
======================
CPS uses PostgreSQL database. As per the `PostgreSQL documentation on resource consumption
<https://www.postgresql.org/docs/current/runtime-config-resource.html#GUC-SHARED-BUFFERS>`_, the *shared_buffers*
parameter should be set between 25% and 40% of total memory. It has a default value of 128 megabytes, so this should be
set appropriately. For example, given a database with 2GB of memory, 512MB is a recommended value.

CPS and NCMP Configuration
==========================

CPU and Memory Requirements
---------------------------

The following are minimum requirements for NCMP:

* For 20,000 CM-handles: 2 CPUs and 2 GB RAM per instance, with 70% heap allocation.
* For 50,000 CM-handles: 3 CPUs and 3 GB RAM per instance, with 70% heap allocation.

JVM Memory Allocation
^^^^^^^^^^^^^^^^^^^^^

When running with 2 GB or more memory per instance, allocating 70% of the JVM memory to the heap ensures efficient
memory management. It is not recommended to go above 70%.

.. code-block:: yaml

    JAVA_TOOL_OPTIONS: "-XX:InitialRAMPercentage=70.0 -XX:MaxRAMPercentage=70.0"


Load balancer configuration
===========================

For optimal performance in CPS/NCMP, load balancers should be configured to use a least-requests policy, also known as
least-connected. Use of round-robin load balancing can lead to instability.

CPS OOM Charts
==============
The CPS kubernetes chart is located in the `OOM repository <https://github.com/onap/oom/tree/master/kubernetes/cps>`_.
This chart includes different cps components referred as <cps-component-name> further in the document are listed below:

.. container:: ulist

  - `cps-core <https://github.com/onap/oom/tree/master/kubernetes/cps/components/cps-core>`__
  - `ncmp-dmi-plugin <https://github.com/onap/oom/tree/master/kubernetes/cps/components/ncmp-dmi-plugin>`__

Please refer to the `OOM documentation <https://docs.onap.org/projects/onap-oom/en/latest/sections/guides/user_guides/oom_user_guide.html>`_ on how to install and deploy ONAP.

Installing or Upgrading CPS Components
======================================

The assumption is you have cloned the charts from the OOM repository into a local directory.

**Step 1** Go to the cps charts and edit properties in values.yaml files to make any changes to particular cps component if required.

.. code-block:: bash

  cd oom/kubernetes/cps/components/<cps-component-name>

**Step 2** Build the charts

.. code-block:: bash

  cd oom/kubernetes
  make SKIP_LINT=TRUE cps

.. note::
   SKIP_LINT is only to reduce the "make" time

**Step 3** Undeploying already deployed cps components

After undeploying cps components, keep monitoring the cps pods until they go away.

.. code-block:: bash

  helm del --purge <my-helm-release>-<cps-component-name>
  kubectl get pods -n <namespace> | grep <cps-component-name>

**Step 4** Make sure there is no orphan database persistent volume or claim.

First, find if there is an orphan database PV or PVC with the following commands:

.. note::
   This step does not apply to ncmp-dmi-plugin.

.. code-block:: bash

  kubectl get pvc -n <namespace> | grep <cps-component-name>
  kubectl get pv -n <namespace> | grep <cps-component-name>

If there are any orphan resources, delete them with

.. code-block:: bash

    kubectl delete pvc <orphan-cps-core-pvc-name>
    kubectl delete pv <orphan-cps-core-pv-name>

**Step 5** Delete NFS persisted data for CPS components

Connect to the machine where the file system is persisted and then execute the below command

.. code-block:: bash

  rm -fr /dockerdata-nfs/<my-helm-release>/<cps-component-name>

**Step 6** Re-Deploy cps pods

After deploying cps, keep monitoring the cps pods until they come up.

.. code-block:: bash

  helm deploy <my-helm-release> local/cps --namespace <namespace>
  kubectl get pods -n <namespace> | grep <cps-component-name>

Restarting a faulty component
=============================
Each cps component can be restarted independently by issuing the following command:

.. code-block:: bash

    kubectl delete pod <cps-component-pod-name> -n <namespace>

.. Below Label is used by documentation for other CPS components to link here, do not remove even if it gives a warning
.. _cps_common_credentials_retrieval:

Credentials Retrieval
=====================

When using OOM application and database credentials are kept in Kubernetes secrets. They are defined as external secrets in the
values.yaml file to be used across different components as :

.. container:: ulist

  - `cps-core <https://github.com/onap/oom/blob/master/kubernetes/cps/components/cps-core/values.yaml>`_
  - `ncmp-dmi-plugin <https://github.com/onap/oom/blob/master/kubernetes/cps/components/ncmp-dmi-plugin/values.yaml>`_

Below are the list of secrets for different cps components.

+--------------------------+---------------------------------+---------------------------------------------------+
| Component                | Secret type                     | Secret Name                                       |
+==========================+=================================+===================================================+
| cps-core                 | Database authentication         | <my-helm-release>-cps-core-pg-user-creds          |
+--------------------------+---------------------------------+---------------------------------------------------+
| cps-core                 | Rest API Authentication         | <my-helm-release>-cps-core-app-user-creds         |
+--------------------------+---------------------------------+---------------------------------------------------+
| ncmp-dmi-plugin          | Rest API Authentication         | <my-helm-release>-cps-dmi-plugin-user-creds       |
+--------------------------+---------------------------------+---------------------------------------------------+
| ncmp-dmi-plugin          | SDNC authentication             | <my-helm-release>-ncmp-dmi-plugin-sdnc-creds      |
+--------------------------+---------------------------------+---------------------------------------------------+

The credential values from these secrets are configured in running container as environment variables. Eg:
`cps core deployment.yaml <https://github.com/onap/oom/blob/master/kubernetes/cps/components/cps-core/templates/deployment.yaml>`_

If no specific passwords are provided to the chart as override values for deployment, then passwords are automatically
generated when deploying the Helm release. Below command can be used to retrieve application property credentials

.. code::

  kubectl get secret <my-helm-release>-<secret-name> -n <namespace> -o json | jq '.data | map_values(@base64d)'

.. note::
   base64d works only with jq version 1.6 or above.

CPS Core Pods
=============
To get a listing of the cps-core Pods, run the following command:

.. code-block:: bash

  kubectl get pods -n <namespace> | grep cps-core

  dev-cps-core-ccd4cc956-r98pv                          1/1     Running            0          24h
  dev-cps-core-postgres-primary-f7766d46c-s9d5b         1/1     Running            0          24h
  dev-cps-core-postgres-replica-84659d68f9-6qnt4        1/1     Running            0          24h

.. note::
    The CPS Service will have to be restarted each time a change is made to a configurable property.

.. _configuration-properties:

Configuration Properties
========================
The following tables list properties that can be configured in the deployment. This list is not exhaustive.

.. csv-table:: 3PP Properties
   :file: csv/3pp_properties.csv
   :widths: 20, 50, 30
   :header-rows: 1

.. note::
    - The default datasource is defined as ``jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${POSTGRES_DB:cpsdb}``. You can configure the database connection by setting the environment variables ``DB_HOST`` for the hostname, ``DB_PORT`` for the port number, and ``POSTGRES_DB`` for the database name.
    - The kafka bootstrap-servers can also be overridden with the environment variable ``KAFKA_BOOTSTRAP_SERVER``.

.. csv-table:: Common CPS-NCMP Custom Properties
   :file: csv/common_custom_properties.csv
   :widths: 20, 50, 30
   :header-rows: 1

.. csv-table:: NCMP Custom Properties
   :file: csv/ncmp_custom_properties.csv
   :widths: 20, 50, 30
   :header-rows: 1

.. note::
    - [app]:  can be ``policy-executor`` or ``dmi``.
    - [services]: ``all-services`` for 'policy-executor'.
    - [services]: ``data-services`` and 'model-services' for 'dmi'.
    - All ncmp.policy-executor properties can also be overridden using environment variables: ``POLICY_SERVICE_ENABLED``, ``POLICY_SERVICE_DEFAULT_DECISION``, ``POLICY_SERVICE_URL``, ``POLICY_SERVICE_PORT``

CPS-Core Docker Installation
============================

CPS-Core can also be installed in a docker environment. Latest `docker-compose <https://github.com/onap/cps/blob/master/docker-compose/cps-base.yml>`_ is included in the repo to start all the relevant services.
The latest instructions are covered in the `README <https://github.com/onap/cps/blob/master/docker-compose/README.md>`_.

.. Below Label is used by documentation for other CPS components to link here, do not remove even if it gives a warning
.. _cps_common_distributed_datastructures:

NCMP Distributed Data Structures
================================

NCMP utilizes embedded distributed data structures to replicate state across various instances, ensuring low latency and high performance. Each JVM runs a Hazelcast instance to manage these data structures. To facilitate member visibility and cluster formation, an additional port (defaulting to 5701) must be available.

Below are the list of distributed datastructures that we have.

+--------------+------------------------------------+-----------------------------------------------------------+
| Component    | Data Structure Name                |                 Use                                       |
+==============+====================================+===========================================================+
| cps-ncmp     | moduleSyncStartedOnCmHandles       | Watchdog process to register CM Handles.                  |
+--------------+------------------------------------+-----------------------------------------------------------+
| cps-ncmp     | dataSyncSemaphores                 | Watchdog process to sync data from the nodes.             |
+--------------+------------------------------------+-----------------------------------------------------------+
| cps-ncmp     | moduleSyncWorkQueue                | Queue used internally for workers to pick the task.       |
+--------------+------------------------------------+-----------------------------------------------------------+
| cps-ncmp     | trustLevelPerCmHandle              | Stores the trust level per CM Handle id                   |
+--------------+------------------------------------+-----------------------------------------------------------+
| cps-ncmp     | trustLevelPerDmiPlugin             | Stores the trust level for the dmi-plugins.               |
+--------------+------------------------------------+-----------------------------------------------------------+
| cps-ncmp     | cmNotificationSubscriptionCache    | Stores and tracks cm notification subscription requests.  |
+--------------+------------------------------------+-----------------------------------------------------------+
| cps-ncmp     | cpsAndNcmpLock                     | Cps and NCMP distributed lock for various use cases.      |
+--------------+------------------------------------+-----------------------------------------------------------+
| cps-ncmp     | cmHandleIdPerAlternateId           | Stores cm handle ids per alternate ids.                   |
+--------------+------------------------------------+-----------------------------------------------------------+

Total number of caches : 8
