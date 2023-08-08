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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NcmpEventResponseCode;
import org.onap.cps.ncmp.api.impl.events.EventsPublisher;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus;
import org.onap.cps.ncmp.api.impl.utils.DataNodeHelper;
import org.onap.cps.ncmp.api.impl.utils.SubscriptionOutcomeCloudMapper;
import org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.CmSubscriptionDmiOutEvent;
import org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_client.CmSubscriptionNcmpOutEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CmSubscriptionNcmpOutEventPublisher {

    private final SubscriptionPersistence subscriptionPersistence;

    private final EventsPublisher<CloudEvent> outcomeEventsPublisher;

    private final CmSubscriptionDmiOutEventToCmSubscriptionNcmpOutEventMapper
            cmSubscriptionDmiOutEventToCmSubscriptionNcmpOutEventMapper;

    private final SubscriptionOutcomeCloudMapper subscriptionOutcomeCloudMapper;

    @Value("${app.ncmp.avc.subscription-outcome-topic:subscription-response}")
    private String subscriptionOutcomeEventTopic;

    /**
     * This is for construction of outcome message to be published for client apps.
     *
     * @param cmSubscriptionDmiOutEvent event produced by Dmi Plugin
     */
    public void sendResponse(final CmSubscriptionDmiOutEvent cmSubscriptionDmiOutEvent, final String eventKey) {
        final CmSubscriptionNcmpOutEvent cmSubscriptionNcmpOutEvent =
                formCmSubscriptionNcmpOutEvent(cmSubscriptionDmiOutEvent);
        final String subscriptionClientId = cmSubscriptionDmiOutEvent.getData().getClientId();
        final String subscriptionName = cmSubscriptionDmiOutEvent.getData().getSubscriptionName();
        final String subscriptionEventId = subscriptionClientId + subscriptionName;
        final CloudEvent subscriptionOutcomeCloudEvent =
                subscriptionOutcomeCloudMapper.toCloudEvent(cmSubscriptionNcmpOutEvent,
                subscriptionEventId, eventKey);
        outcomeEventsPublisher.publishCloudEvent(subscriptionOutcomeEventTopic,
                subscriptionEventId, subscriptionOutcomeCloudEvent);
    }

    private CmSubscriptionNcmpOutEvent formCmSubscriptionNcmpOutEvent(
            final CmSubscriptionDmiOutEvent cmSubscriptionDmiOutEvent) {
        final Map<String, Map<String, String>> cmHandleIdToStatusAndDetailsAsMap =
                DataNodeHelper.cmHandleIdToStatusAndDetailsAsMapFromDataNode(
                        subscriptionPersistence.getCmHandlesForSubscriptionEvent(
                                cmSubscriptionDmiOutEvent.getData().getClientId(),
                                cmSubscriptionDmiOutEvent.getData().getSubscriptionName()));
        final List<org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.SubscriptionStatus>
                subscriptionStatusList =
                mapCmHandleIdStatusDetailsMapToSubscriptionStatusList(cmHandleIdToStatusAndDetailsAsMap);
        cmSubscriptionDmiOutEvent.getData().setSubscriptionStatus(subscriptionStatusList);
        return fromSubscriptionEventResponse(cmSubscriptionDmiOutEvent,
                decideOnNcmpEventResponseCodeForSubscription(cmHandleIdToStatusAndDetailsAsMap));
    }

    private static List<org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.SubscriptionStatus>
        mapCmHandleIdStatusDetailsMapToSubscriptionStatusList(
            final Map<String, Map<String, String>> cmHandleIdToStatusAndDetailsAsMap) {
        return cmHandleIdToStatusAndDetailsAsMap.entrySet()
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

    private NcmpEventResponseCode decideOnNcmpEventResponseCodeForSubscription(
            final Map<String, Map<String, String>> cmHandleIdToStatusAndDetailsAsMap) {

        final boolean isAllTargetsPending = isAllTargetCmHandleStatusMatch(cmHandleIdToStatusAndDetailsAsMap,
                SubscriptionStatus.PENDING);

        final boolean isAllTargetsRejected = isAllTargetCmHandleStatusMatch(cmHandleIdToStatusAndDetailsAsMap,
                SubscriptionStatus.REJECTED);

        final boolean isAllTargetsAccepted = isAllTargetCmHandleStatusMatch(cmHandleIdToStatusAndDetailsAsMap,
                SubscriptionStatus.ACCEPTED);

        if (isAllTargetsAccepted) {
            return NcmpEventResponseCode.SUCCESSFULLY_APPLIED_SUBSCRIPTION;
        } else if (isAllTargetsRejected) {
            return NcmpEventResponseCode.SUBSCRIPTION_NOT_APPLICABLE;
        } else if (isAllTargetsPending) {
            return NcmpEventResponseCode.SUBSCRIPTION_PENDING;
        } else {
            return NcmpEventResponseCode.PARTIALLY_APPLIED_SUBSCRIPTION;
        }
    }

    private boolean isAllTargetCmHandleStatusMatch(
            final Map<String, Map<String, String>> cmHandleIdToStatusAndDetailsAsMap,
            final SubscriptionStatus subscriptionStatus) {
        return cmHandleIdToStatusAndDetailsAsMap.values().stream()
                .allMatch(entryset -> entryset.containsValue(subscriptionStatus.toString()));
    }

    private CmSubscriptionNcmpOutEvent fromSubscriptionEventResponse(
            final CmSubscriptionDmiOutEvent cmSubscriptionDmiOutEvent,
            final NcmpEventResponseCode ncmpEventResponseCode) {

        final CmSubscriptionNcmpOutEvent cmSubscriptionNcmpOutEvent =
                cmSubscriptionDmiOutEventToCmSubscriptionNcmpOutEventMapper.toCmSubscriptionNcmpOutEvent(
                        cmSubscriptionDmiOutEvent);
        cmSubscriptionNcmpOutEvent.getData().setStatusCode(Integer.parseInt(ncmpEventResponseCode.getStatusCode()));
        cmSubscriptionNcmpOutEvent.getData().setStatusMessage(ncmpEventResponseCode.getStatusMessage());

        return cmSubscriptionNcmpOutEvent;
    }
}
