/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.events.avcsubscription;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.events.EventsPublisher;
import org.onap.cps.ncmp.api.impl.utils.DmiServiceNameOrganizer;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.event.model.SubscriptionEvent;
import org.onap.cps.spi.exceptions.OperationNotYetSupportedException;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionEventForwarder {

    private final InventoryPersistence inventoryPersistence;
    private final EventsPublisher<SubscriptionEvent> eventsPublisher;
    private static final String DMI_AVC_SUBSCRIPTION_TOPIC_PREFIX = "ncmp-dmi-cm-avc-subscription-";

    /**
     * Forward subscription event.
     *
     * @param subscriptionEvent the event to be forwarded
     */
    public void forwardCreateSubscriptionEvent(final SubscriptionEvent subscriptionEvent) {
        final List<Object> cmHandleTargets = subscriptionEvent.getEvent().getPredicates().getTargets();
        if (cmHandleTargets == null || cmHandleTargets.isEmpty()
                || cmHandleTargets.stream().anyMatch(id -> ((String) id).contains("*"))) {
            throw new OperationNotYetSupportedException(
                    "CMHandle targets are required. \"Wildcard\" operations are not yet supported");
        }
        final List<String> cmHandleTargetsAsStrings = cmHandleTargets.stream().map(
                Objects::toString).collect(Collectors.toList());
        final Collection<YangModelCmHandle> yangModelCmHandles =
                inventoryPersistence.getYangModelCmHandles(cmHandleTargetsAsStrings);

        final Map<String, Map<String, Map<String, String>>> dmiPropertiesPerCmHandleIdPerServiceName
                = DmiServiceNameOrganizer.getDmiPropertiesPerCmHandleIdPerServiceName(yangModelCmHandles);
        dmiPropertiesPerCmHandleIdPerServiceName.forEach((dmiServiceName, cmHandlePropertiesMap) -> {
            subscriptionEvent.getEvent().getPredicates().setTargets(Collections
                    .singletonList(cmHandlePropertiesMap));
            final String eventKey = createEventKey(subscriptionEvent, dmiServiceName);
            eventsPublisher.publishEvent(DMI_AVC_SUBSCRIPTION_TOPIC_PREFIX + dmiServiceName, eventKey,
                    subscriptionEvent);
        });
    }

    private String createEventKey(final SubscriptionEvent subscriptionEvent, final String dmiName) {
        return subscriptionEvent.getEvent().getSubscription().getClientID()
            + "-"
            + subscriptionEvent.getEvent().getSubscription().getName()
            + "-"
            + dmiName;
    }

}
