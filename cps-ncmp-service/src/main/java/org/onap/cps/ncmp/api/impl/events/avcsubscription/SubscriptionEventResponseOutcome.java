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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.onap.cps.ncmp.api.impl.events.EventsPublisher;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus;
import org.onap.cps.ncmp.api.impl.utils.DataNodeHelper;
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse;
import org.onap.cps.ncmp.events.avc.subscription.v1.SubscriptionEventOutcome;
import org.onap.cps.spi.model.DataNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionEventResponseOutcome {

    private final SubscriptionPersistence subscriptionPersistence;

    private final EventsPublisher<SubscriptionEventOutcome> outcomeEventsPublisher;

    private final SubscriptionOutcomeMapper subscriptionOutcomeMapper;

    @Value("${app.ncmp.avc.subscription-outcome-topic:cm-avc-subscription-response}")
    private String subscriptionOutcomeEventTopic;

    /**
     * This is for construction of outcome message to be published for client apps.
     *
     * @param subscriptionClientId client id of the subscription.
     * @param subscriptionName name of the subscription.
     */
    public void sendResponse(final String subscriptionClientId, final String subscriptionName) {
        final SubscriptionEventOutcome subscriptionEventOutcome = generateResponse(
                subscriptionClientId, subscriptionName);
        final Headers headers = new RecordHeaders();
        final String subscriptionEventId = subscriptionClientId + subscriptionName;
        outcomeEventsPublisher.publishEvent(subscriptionOutcomeEventTopic,
                subscriptionEventId, headers, subscriptionEventOutcome);
    }

    private SubscriptionEventOutcome generateResponse(final String subscriptionClientId,
                                                      final String subscriptionName) {
        final Collection<DataNode> dataNodes =
                subscriptionPersistence.getCmHandlesForSubscriptionEvent(subscriptionClientId, subscriptionName);
        final List<Map<String, Serializable>> dataNodeLeaves = DataNodeHelper.getDataNodeLeaves(dataNodes);
        final List<Collection<Serializable>> cmHandleIdToStatus =
                DataNodeHelper.getCmHandleIdToStatus(dataNodeLeaves);
        final Map<String, SubscriptionStatus> cmHandleIdToStatusMap =
                DataNodeHelper.getCmHandleIdToStatusMap(cmHandleIdToStatus);
        return formSubscriptionOutcomeMessage(cmHandleIdToStatus, subscriptionClientId, subscriptionName,
                isFullOutcomeResponse(cmHandleIdToStatusMap));
    }

    private boolean isFullOutcomeResponse(final Map<String, SubscriptionStatus> cmHandleIdToStatusMap) {
        return !cmHandleIdToStatusMap.values().contains(SubscriptionStatus.PENDING)
                && !cmHandleIdToStatusMap.values().contains(SubscriptionStatus.REJECTED);
    }

    private SubscriptionEventOutcome formSubscriptionOutcomeMessage(
            final List<Collection<Serializable>> cmHandleIdToStatus, final String subscriptionClientId,
            final String subscriptionName, final boolean isFullOutcomeResponse) {

        final SubscriptionEventResponse subscriptionEventResponse = toSubscriptionEventResponse(
                cmHandleIdToStatus, subscriptionClientId, subscriptionName);

        final SubscriptionEventOutcome subscriptionEventOutcome =
                subscriptionOutcomeMapper.toSubscriptionEventOutcome(subscriptionEventResponse);

        if (isFullOutcomeResponse) {
            subscriptionEventOutcome.setEventType(SubscriptionEventOutcome.EventType.COMPLETE_OUTCOME);
        } else {
            subscriptionEventOutcome.setEventType(SubscriptionEventOutcome.EventType.PARTIAL_OUTCOME);
        }

        return subscriptionEventOutcome;
    }

    private SubscriptionEventResponse toSubscriptionEventResponse(
            final List<Collection<Serializable>> cmHandleIdToStatus, final String subscriptionClientId,
            final String subscriptionName) {
        final Map<String, SubscriptionStatus> cmHandleIdToStatusMap =
                DataNodeHelper.getCmHandleIdToStatusMap(cmHandleIdToStatus);

        final SubscriptionEventResponse subscriptionEventResponse = new SubscriptionEventResponse();
        subscriptionEventResponse.setClientId(subscriptionClientId);
        subscriptionEventResponse.setSubscriptionName(subscriptionName);
        subscriptionEventResponse.setCmHandleIdToStatus(cmHandleIdToStatusMap);

        return subscriptionEventResponse;
    }
}
