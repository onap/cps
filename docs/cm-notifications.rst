.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _cmNotifications:


CM Notifications
################

.. toctree::
   :maxdepth: 1

Overview
========

CM Notifications are triggered by any change in the network. They are processed by NCMP and forwarded to clients on a dedicated Kafka topic.

**Note.** Currently, CM Notifications are sent regardless of CM Subscriptions. Notifications controlled by CM Subscription have not yet been delivered.

NCMP uses the event key from the source topic to forward notifications, ensuring that the order of notifications within a topic partition is maintained. If the key from the source topic is null, ordering is not guaranteed.
Additionally, if a CM handle has ``data-sync-enabled`` set to ``true``, NCMP will apply the changes from the notification to its local data cache.

Event Schema
============

The CM Notification event follows the RFC 8641 YANG push format, wrapped as a CloudEvent:

:download:`CM Notification Event Schema <schemas/dmi/cm-events/avc-event-schema-1.0.0.json>`

Each event contains a ``push-change-update`` with YANG patch edits. Supported operations: ``create``, ``update``, ``patch``, ``delete``.

Kafka Topics
============

+---------------------------------------+-------------------+---------------------------------------------------+
| Property                              | Default           | Description                                       |
+=======================================+===================+===================================================+
| ``app.dmi.cm-events.topic``           | ``dmi-cm-events`` | Source topic where DMI plugins publish events     |
+---------------------------------------+-------------------+---------------------------------------------------+
| ``app.ncmp.avc.cm-events-topic``      | ``cm-events``     | Target topic where NCMP forwards events to clients|
+---------------------------------------+-------------------+---------------------------------------------------+

Consumer Configuration
======================

The notification consumer is enabled by default (``notification.enabled: true``). It uses the global consumer group (``spring.kafka.consumer.group-id``, default: ``ncmp-group``).

Two consumer modes are available, controlled by the ``batch-enabled`` property.

Single-Record Mode (Default)
-----------------------------

.. code-block:: yaml

   ncmp:
     notifications:
       avc-event-consumer:
         batch-enabled: false

- Processes one event at a time
- No Kafka transactions
- Suitable for low-throughput deployments or when strict ordering is critical

Batch Mode with Exactly-Once Semantics
---------------------------------------

.. code-block:: yaml

   ncmp:
     notifications:
       avc-event-consumer:
         batch-enabled: true
         concurrency: 2
         max-poll-records: 500
       avc-event-producer:
         transaction-id-prefix: tx-

When enabled, NCMP processes events in batches within Kafka transactions, providing exactly-once delivery guarantees:

- Events are only read after they are committed (``read_committed`` isolation)
- Forwarded events are produced idempotently with ``acks=all``
- The entire consume-and-forward cycle is wrapped in a single transaction
- On failure, retries use exponential backoff (1s initial, 2x multiplier, 30s max) for ``KafkaException`` and its subclasses only

``max-poll-records`` controls how many events are processed per transaction. Higher values increase throughput but also increase the number of events reprocessed on failure. The default of ``500`` is a reasonable starting point; tune based on your event size and processing latency.

.. warning::

   When ``concurrency > 1``, message ordering within a partition is **not** guaranteed.
   Use ``concurrency: 1`` if strict ordering is required.

Kafka Broker Prerequisites
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Exactly-once semantics requires the Kafka broker's transaction state log to be properly configured:

.. code-block:: yaml

   KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: "1"
   KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: "1"

These control the internal ``__transaction_state`` topic that Kafka uses to coordinate transactions.

.. warning::

   In production, both values **must** match your cluster's replication factor (typically 3). The values above (``1``) are suitable only for single-broker development/test environments. Without these settings, transactional producers will fail to initialize.

Configuration Reference
========================

+-----------------------------------------------------------------+-----------+------------------------------------------------+
| Property                                                        | Default   | Description                                    |
+=================================================================+===========+================================================+
| ``notification.enabled``                                        | ``true``  | Enables/disables the notification consumer     |
+-----------------------------------------------------------------+-----------+------------------------------------------------+
| ``ncmp.notifications.avc-event-consumer.batch-enabled``         | ``false`` | Enables batch mode with exactly-once semantics |
+-----------------------------------------------------------------+-----------+------------------------------------------------+
| ``ncmp.notifications.avc-event-consumer.concurrency``           | ``2``     | Number of parallel consumer threads            |
+-----------------------------------------------------------------+-----------+------------------------------------------------+
| ``ncmp.notifications.avc-event-consumer.max-poll-records``      | ``500``   | Max records per poll (batch mode only)         |
+-----------------------------------------------------------------+-----------+------------------------------------------------+
| ``ncmp.notifications.avc-event-producer.transaction-id-prefix`` | ``tx-``   | Prefix for Kafka transaction IDs               |
+-----------------------------------------------------------------+-----------+------------------------------------------------+

Metrics
=======

- ``cps.ncmp.cm.avc.events.forwarded`` — Total number of events forwarded to the client topic.
- ``cps.ncmp.cm.notifications.consume.and.forward`` — Time taken to process and forward a batch.
