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

package org.onap.cps.ncmp.api.impl.events.cmsubscription;

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
import org.onap.cps.ncmp.api.models.CmSubscriptionEvent;
import org.onap.cps.ncmp.api.models.CmSubscriptionStatus;
import org.onap.cps.ncmp.events.cmsubscription1_0_0.ncmp_to_client.CmSubscriptionNcmpOutEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CmSubscriptionNcmpOutEventPublisher {

    private final SubscriptionPersistence subscriptionPersistence;

    private final EventsPublisher<CloudEvent> outcomeEventsPublisher;

    private final CmSubscriptionEventToCmSubscriptionNcmpOutEventMapper
            cmSubscriptionEventToCmSubscriptionNcmpOutEventMapper;

    private final SubscriptionOutcomeCloudMapper subscriptionOutcomeCloudMapper;

    @Value("${app.ncmp.avc.subscription-outcome-topic:subscription-response}")
    private String subscriptionOutcomeEventTopic;

    /**
     * This is for construction of outcome message to be published for client apps.
     *
     * @param cmSubscriptionEvent event produced by Dmi Plugin
     */
    public void sendResponse(final CmSubscriptionEvent cmSubscriptionEvent, final String eventType) {
        final CmSubscriptionNcmpOutEvent cmSubscriptionNcmpOutEvent =
                formCmSubscriptionNcmpOutEvent(cmSubscriptionEvent);
        final String subscriptionClientId = cmSubscriptionEvent.getClientId();
        final String subscriptionName = cmSubscriptionEvent.getSubscriptionName();
        final String subscriptionEventId = subscriptionClientId + subscriptionName;
        final CloudEvent subscriptionOutcomeCloudEvent =
                subscriptionOutcomeCloudMapper.toCloudEvent(cmSubscriptionNcmpOutEvent,
                subscriptionEventId, eventType);
        outcomeEventsPublisher.publishCloudEvent(subscriptionOutcomeEventTopic,
                subscriptionEventId, subscriptionOutcomeCloudEvent);
    }

    private CmSubscriptionNcmpOutEvent formCmSubscriptionNcmpOutEvent(
            final CmSubscriptionEvent cmSubscriptionEvent) {
        final Map<String, Map<String, String>> cmHandleIdToStatusAndDetailsAsMap =
                DataNodeHelper.cmHandleIdToStatusAndDetailsAsMapFromDataNode(
                        subscriptionPersistence.getCmHandlesForSubscriptionEvent(
                                cmSubscriptionEvent.getClientId(),
                                cmSubscriptionEvent.getSubscriptionName()));
        final List<CmSubscriptionStatus> cMsubscriptionStatusList =
                mapCmHandleIdStatusDetailsMapToSubscriptionStatusList(cmHandleIdToStatusAndDetailsAsMap);
        cmSubscriptionEvent.setCmSubscriptionStatus(cMsubscriptionStatusList);
        return fromCmSubscriptionEvent(cmSubscriptionEvent,
                decideOnNcmpEventResponseCodeForSubscription(cmHandleIdToStatusAndDetailsAsMap));
    }

    private static List<CmSubscriptionStatus> mapCmHandleIdStatusDetailsMapToSubscriptionStatusList(
            final Map<String, Map<String, String>> cmHandleIdToStatusAndDetailsAsMap) {
        return cmHandleIdToStatusAndDetailsAsMap.entrySet()
                .stream().map(entryset -> {
                    final CmSubscriptionStatus cmSubscriptionStatus = new CmSubscriptionStatus();
                    final String cmHandleId = entryset.getKey();
                    final Map<String, String> statusAndDetailsMap = entryset.getValue();
                    final String status = statusAndDetailsMap.get("status");
                    final String details = statusAndDetailsMap.get("details");
                    cmSubscriptionStatus.setId(cmHandleId);
                    cmSubscriptionStatus.setStatus(SubscriptionStatus.fromString(status));
                    cmSubscriptionStatus.setDetails(details);
                    return cmSubscriptionStatus;
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

    private CmSubscriptionNcmpOutEvent fromCmSubscriptionEvent(
            final CmSubscriptionEvent cmSubscriptionEvent,
            final NcmpEventResponseCode ncmpEventResponseCode) {

        final CmSubscriptionNcmpOutEvent cmSubscriptionNcmpOutEvent =
                cmSubscriptionEventToCmSubscriptionNcmpOutEventMapper.toCmSubscriptionNcmpOutEvent(
                        cmSubscriptionEvent);
        cmSubscriptionNcmpOutEvent.getData().setStatusCode(Integer.parseInt(ncmpEventResponseCode.getStatusCode()));
        cmSubscriptionNcmpOutEvent.getData().setStatusMessage(ncmpEventResponseCode.getStatusMessage());

        return cmSubscriptionNcmpOutEvent;
    }
}
