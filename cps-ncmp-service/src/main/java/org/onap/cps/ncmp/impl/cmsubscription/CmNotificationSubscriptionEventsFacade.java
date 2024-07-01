/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2024 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.cmsubscription;

import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.ncmp_to_dmi.CmNotificationSubscriptionDmiInEvent;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent;
import org.onap.cps.ncmp.impl.cmsubscription.dmi.CmNotificationSubscriptionDmiInEventProducer;
import org.onap.cps.ncmp.impl.cmsubscription.ncmp.CmNotificationSubscriptionNcmpOutEventProducer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CmNotificationSubscriptionEventsFacade {
    private final CmNotificationSubscriptionNcmpOutEventProducer cmNotificationSubscriptionNcmpOutEventProducer;
    private final CmNotificationSubscriptionDmiInEventProducer cmNotificationSubscriptionDmiInEventProducer;

    /**
     * Publish the event to the client who requested the subscription with key as subscription id and event is Cloud
     * Event compliant.
     *
     * @param subscriptionId                         Cm Subscription id
     * @param eventType                              Type of event
     * @param cmNotificationSubscriptionNcmpOutEvent Cm Notification Subscription Event for the
     *                                               client
     * @param isScheduledEvent                       Determines if the event is to be scheduled
     *                                               or published now
     */
    public void publishCmNotificationSubscriptionNcmpOutEvent(final String subscriptionId, final String eventType,
                                                              final CmNotificationSubscriptionNcmpOutEvent
                                                                      cmNotificationSubscriptionNcmpOutEvent,
                                                              final boolean isScheduledEvent) {
        cmNotificationSubscriptionNcmpOutEventProducer.publishCmNotificationSubscriptionNcmpOutEvent(subscriptionId,
                eventType, cmNotificationSubscriptionNcmpOutEvent, isScheduledEvent);
    }

    /**
     * Publish the event to the provided dmi plugin with key as subscription id and the event is in Cloud Event format.
     *
     * @param subscriptionId                       Cm Subscription id
     * @param dmiPluginName                        Dmi Plugin Name
     * @param eventType                            Type of event
     * @param cmNotificationSubscriptionDmiInEvent Cm Notification Subscription event for Dmi
     */
    public void publishCmNotificationSubscriptionDmiInEvent(final String subscriptionId, final String dmiPluginName,
                                                            final String eventType,
                                                            final CmNotificationSubscriptionDmiInEvent
                                                                    cmNotificationSubscriptionDmiInEvent) {
        cmNotificationSubscriptionDmiInEventProducer.publishCmNotificationSubscriptionDmiInEvent(subscriptionId,
                dmiPluginName, eventType, cmNotificationSubscriptionDmiInEvent);
    }
}
