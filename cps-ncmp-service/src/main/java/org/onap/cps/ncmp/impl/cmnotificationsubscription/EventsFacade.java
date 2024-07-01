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

package org.onap.cps.ncmp.impl.cmnotificationsubscription;

import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi.DmiInEventProducer;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp.NcmpOutEventProducer;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_client.NcmpOutEvent;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_dmi.DmiInEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventsFacade {
    private final NcmpOutEventProducer ncmpOutEventProducer;
    private final DmiInEventProducer dmiInEventProducer;

    /**
     * Publish the event to the client who requested the subscription with key as subscription id and event is Cloud
     * Event compliant.
     *
     * @param subscriptionId                         Cm Subscription id
     * @param eventType                              Type of event
     * @param ncmpOutEvent Cm Notification Subscription Event for the
     *                                               client
     * @param isScheduledEvent                       Determines if the event is to be scheduled
     *                                               or published now
     */
    public void publishNcmpOutEvent(final String subscriptionId, final String eventType,
                                                              final NcmpOutEvent
                                                                      ncmpOutEvent,
                                                              final boolean isScheduledEvent) {
        ncmpOutEventProducer.publishNcmpOutEvent(subscriptionId,
                eventType, ncmpOutEvent, isScheduledEvent);
    }

    /**
     * Publish the event to the provided dmi plugin with key as subscription id and the event is in Cloud Event format.
     *
     * @param subscriptionId                       Cm Subscription id
     * @param dmiPluginName                        Dmi Plugin Name
     * @param eventType                            Type of event
     * @param dmiInEvent Cm Notification Subscription event for Dmi
     */
    public void publishCmNotificationSubscriptionDmiInEvent(final String subscriptionId, final String dmiPluginName,
                                                            final String eventType,
                                                            final DmiInEvent
                                                                    dmiInEvent) {
        dmiInEventProducer.publishDmiInEvent(subscriptionId,
                dmiPluginName, eventType, dmiInEvent);
    }
}
