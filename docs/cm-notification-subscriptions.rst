.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2024 Nordix Foundation

.. DO NOT CHANGE THIS LABEL FOR RELEASE NOTES - EVEN THOUGH IT GIVES A WARNING
.. _cmNotificationSubscriptions:


CM Data Subscriptions
#####################

.. toctree::
   :maxdepth: 1

Introduction
============
CM Subscriptions are created to filter any CM related changes that happened in the network based on predicates.
Predicates have granular details about CmHandle, Datastore and Xpath.

CM Subscription flow is designed to be event driven and adheres to the CNCF Cloud Events Specifications.

Event to create and delete a subscription.

:download:`CM Subscription Event Schema <schemas/ncmp-in-event-schema-1.0.0.json>`

Event to receive status of participants in a subscription.

:download:`CM Subscription Response Event Schema <schemas/ncmp-out-event-schema-1.0.0.json>`

CM Subscriptions Creation
-------------------------
When a client wants to create a subscription , an event is sent to a configured topic inorder to register its interest with NCMP and further receive notifications based on it.

CM Subscriptions Deletion
-------------------------
When a client is no longer interested in receiving the notifications based on the subscription registered with NCMP , there is a provision to delete the subscription when the subscription id is provided.

CM Subscriptions Response
-------------------------
The response for the involved subscription participants for the Create and Delete flow can be in PENDING , ACCEPTED or REJECTED state based on how the DMI Plugin responds back to NCMP.





