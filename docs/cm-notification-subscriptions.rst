.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _cmNotificationSubscriptions:


CM Data Notifications
#####################

.. toctree::
   :maxdepth: 1

CM Data Notifications
=====================
CM Notifications are triggered by any change in the network, provided the client has already set up a CM Subscription to receive such notifications. Once the events are generated, they are processed by NCMP and forwarded to the client in the same format.

**Note.** Currently, CM Notifications are sent regardless of the CM Subscriptions. Notifications controlled by CM Subscription have not yet been delivered.

The CM Notification Event follows the structure outlined in the schema below:

:download:`CM Data Notification Event Schema <schemas/dmi/cm-events/avc-event-schema-1.0.0.json>`

**Note.** NCMP uses the CM Notification Event key from the source topic to forward notifications to the client, ensuring that the order of notifications within a topic partition is maintained during forwarding.

**Note.** If the notification key from the source topic is null, NCMP cannot guarantee the order of events within a topic partition when forwarding.

