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

import io.cloudevents.CloudEvent;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.events.EventsPublisher;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionOutcomeType;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus;
import org.onap.cps.ncmp.api.impl.utils.DataNodeHelper;
import org.onap.cps.ncmp.api.impl.utils.SubscriptionOutcomeCloudMapper;
import org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.SubscriptionEventResponse;
import org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_client.SubscriptionEventOutcome;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionEventResponseOutcome {

    private final SubscriptionPersistence subscriptionPersistence;

    private final EventsPublisher<CloudEvent> outcomeEventsPublisher;

    private final SubscriptionOutcomeMapper subscriptionOutcomeMapper;

    @Value("${app.ncmp.avc.subscription-outcome-topic:cm-avc-subscription-response}")
    private String subscriptionOutcomeEventTopic;

    /**
     * This is for construction of outcome message to be published for client apps.
     *
     * @param subscriptionEventResponse event produced by Dmi Plugin
     */
    public void sendResponse(final SubscriptionEventResponse subscriptionEventResponse) {
        final SubscriptionEventOutcome subscriptionEventOutcome =
                formSubscriptionOutcomeMessage(subscriptionEventResponse);
        final String subscriptionClientId = subscriptionEventResponse.getData().getClientId();
        final String subscriptionName = subscriptionEventResponse.getData().getSubscriptionName();
        final String subscriptionEventId = subscriptionClientId + subscriptionName;
        outcomeEventsPublisher.publishCloudEvent(subscriptionOutcomeEventTopic,
                subscriptionEventId, SubscriptionOutcomeCloudMapper.toCloudEvent(subscriptionEventOutcome));
    }

    private SubscriptionEventOutcome formSubscriptionOutcomeMessage(
            final SubscriptionEventResponse subscriptionEventResponse) {
        final Map<String, Map<String, String>> cmHandleIdToStatusAndDetailsAsMapFromDataNode =
                DataNodeHelper.cmHandleIdToStatusAndDetailsAsMapFromDataNode(
                subscriptionPersistence.getCmHandlesForSubscriptionEvent(
                        subscriptionEventResponse.getData().getClientId(),
                        subscriptionEventResponse.getData().getSubscriptionName()));
        final List<org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.SubscriptionStatus>
                subscriptionStatusList = mapCmHandleIdStatusDetailsMapToSubscriptionStatusList(
                        cmHandleIdToStatusAndDetailsAsMapFromDataNode);
        subscriptionEventResponse.getData().setSubscriptionStatus(subscriptionStatusList);
        return fromSubscriptionEventResponse(subscriptionEventResponse,
                isFullOutcomeResponse(cmHandleIdToStatusAndDetailsAsMapFromDataNode));
    }

    private static List<org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.SubscriptionStatus>
        mapCmHandleIdStatusDetailsMapToSubscriptionStatusList(
            final Map<String, Map<String, String>> cmHandleIdToStatusAndDetailsAsMapFromDataNode) {
        return cmHandleIdToStatusAndDetailsAsMapFromDataNode.entrySet()
                .stream().map(entryset -> {
                    final org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.SubscriptionStatus
                            subscriptionStatus = new org.onap.cps.ncmp.events.avcsubscription1_0_0
                            .dmi_to_ncmp.SubscriptionStatus();
                    final String cmHandleId = entryset.getKey();
                    final Map<String, String> statusAndDetailsMap = entryset.getValue();
                    final String status = statusAndDetailsMap.get("status");
                    final String details = statusAndDetailsMap.get("details");
                    subscriptionStatus.setId(cmHandleId);
                    subscriptionStatus.setStatus(
                            org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp
                                    .SubscriptionStatus.Status.fromValue(status));
                    subscriptionStatus.setDetails(details);
                    return subscriptionStatus;
                }).collect(Collectors.toList());
    }

    private boolean isFullOutcomeResponse(final Map<String, Map<String, String>>
                                                  cmHandleIdToStatusAndDetailsAsMapFromDataNode) {
        return cmHandleIdToStatusAndDetailsAsMapFromDataNode.values().stream()
                .allMatch(Predicate.not(entryset -> entryset.containsValue(SubscriptionStatus.PENDING.toString())));
    }

    private SubscriptionEventOutcome fromSubscriptionEventResponse(
            final SubscriptionEventResponse subscriptionEventResponse,
            final boolean isFullOutcomeResponse) {

        final SubscriptionEventOutcome subscriptionEventOutcome =
                subscriptionOutcomeMapper.toSubscriptionEventOutcome(subscriptionEventResponse);

        if (isFullOutcomeResponse) {
            subscriptionEventOutcome.getData().setStatusCode(SubscriptionOutcomeType.SUCCESS.outcomeCode());
            subscriptionEventOutcome.getData().setStatusMessage("Fully applied subscription");
        } else {
            subscriptionEventOutcome.getData().setStatusCode(SubscriptionOutcomeType.PARTIAL_SUCCESS.outcomeCode());
            subscriptionEventOutcome.getData().setStatusMessage("Partially Applied Subscription");
        }

        return subscriptionEventOutcome;
    }
}
