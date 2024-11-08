.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2024 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _cmNotificationSubscriptions:


CM Data Subscriptions and Notifications
#######################################

.. toctree::
   :maxdepth: 1

CM Data Subscriptions
=====================
CM Subscriptions are created to subscribe to notifications for CM related changes that happened in the network based on predicates.
Predicates can be used to filter on CM Handle (id), Datastore and Xpath.

The CM Subscription flow is event driven and adheres to the CNCF Cloud Events Specifications.

Event to create and delete a subscription.

:download:`CM Subscription Event Schema <schemas/ncmp-in-event-schema-1.0.0.json>`

Event to receive status of participants in a subscription.

:download:`CM Subscription Response Event Schema <schemas/ncmp-out-event-schema-1.0.0.json>`

CM Subscriptions Creation
-------------------------
To create a subscription, a client sends an event to a configured topic to register its interest with NCMP allowing the client to receive notifications based on the subscription.

CM Subscriptions Deletion
-------------------------
If a client no longer wishes to receive notifications based on a registered subscription, the client can delete the subscription by providing the subscription id.

CM Subscriptions Response
-------------------------
The response for the involved subscription participants for the Create and Delete flow can be as follows based on how the DMI Plugin responds back to NCMP.
    - **ACCEPTED:** DMI Plugin successfully applied the subscription.
    - **REJECTED:** DMI Plugin failed to apply the subscription.
    - **PENDING:** DMI Plugin failed to respond within a configured time.

**Note.** The Cm Subscription feature relies on the DMI Plugin support for applying the subscriptions. This support is currently not implemented in the ONAP DMI Plugin.

CM Data Notifications
=====================
CM Notifications are triggered by any change in the network, provided the client has already set up a CM Subscription to receive such notifications. Once the events are generated, they are processed by NCMP and forwarded to the client in the same format.

**Note.** Currently, CM Notifications are sent regardless of the CM Subscriptions. Notifications controlled by CM Subscription have not yet been delivered.

The CM Notification Event follows the structure outlined in the schema below:

:download:`CM Data Notification Event Schema <schemas/dmidataavc/avc-event-schema-1.0.0.json>`

**Note.** NCMP uses the CM Notification event key from the source topic to forward notifications to the client, ensuring that the order of notifications within a topic partition is maintained during forwarding.
**Note.** If the notification key from the source topic is null, NCMP cannot guarantee the order of events within a topic partition when forwarding.


