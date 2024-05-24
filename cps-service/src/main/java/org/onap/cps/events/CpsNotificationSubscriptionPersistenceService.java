package org.onap.cps.events;

public interface CpsNotificationSubscriptionPersistenceService {

    String CPS_DATASPACE_NAME = "CPS-Admin";

    String SUBSCRIPTIONS_ANCHOR_NAME = "cps-notification-subscriptions";

    void addCpsNotificationSubscription(final String cmHandleId,
                                        final String xpath, final String newSubscriptionId);

    void removeCpsNotificationSubscription(final String cmHandleId,
                                           final String xpath, final String subscriptionId);

}