package org.onap.cps.integration.functional

import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING;
import org.onap.cps.integration.base.CpsIntegrationSpecBase;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.service.CmNotificationSubscriptionPersistenceService;
import org.springframework.beans.factory.annotation.Autowired;

class NcmpCmNotificationSubscriptionSpec extends CpsIntegrationSpecBase {

    @Autowired
    CmNotificationSubscriptionPersistenceService cmNotificationSubscriptionPersistenceService;

    def 'Adding a new cm notification subscription'() {
        given: 'there is no ongoing cm subscription for the following'
            def datastoreType = PASSTHROUGH_RUNNING
            def cmHandleId = 'ch-1'
            def xpath = '/x/y'
            assert cmNotificationSubscriptionPersistenceService.
                getOngoingCmNotificationSubscriptionIds(datastoreType,cmHandleId,xpath).size() == 0
        when: 'we add a new cm notification subscription'
            cmNotificationSubscriptionPersistenceService.addOrUpdateCmNotificationSubscription(datastoreType,cmHandleId,xpath,
                'subId-1')
        then: 'there is an ongoing cm subscription for that CM handle and xpath'
            assert cmNotificationSubscriptionPersistenceService.isOngoingCmNotificationSubscription(datastoreType,cmHandleId,xpath)
        and: 'only one subscription id is related to now ongoing cm subscription'
            assert cmNotificationSubscriptionPersistenceService.
                getOngoingCmNotificationSubscriptionIds(datastoreType,cmHandleId,xpath).size() == 1
    }

    def 'Adding a cm notification subscription to an already existing'() {
        given: 'an ongoing cm subscription'
            def datastoreType = PASSTHROUGH_RUNNING
            def cmHandleId = 'ch-1'
            def xpath = '/x/y'
            cmNotificationSubscriptionPersistenceService.addOrUpdateCmNotificationSubscription(datastoreType,cmHandleId,xpath,
                'subId-1')
        when: 'a new cm notification subscription is made for the SAME CM handle and xpath'
            cmNotificationSubscriptionPersistenceService.addOrUpdateCmNotificationSubscription(datastoreType,cmHandleId,xpath,
                'subId-2')
        then: 'it is added to the ongoing list of subscription ids'
            def subscriptionIds = cmNotificationSubscriptionPersistenceService.getOngoingCmNotificationSubscriptionIds(datastoreType,cmHandleId,xpath)
            assert subscriptionIds.size() == 2
        and: 'both subscription ids exists for the CM handle and xpath'
            assert subscriptionIds.contains("subId-1") && subscriptionIds.contains("subId-2")
    }
}
