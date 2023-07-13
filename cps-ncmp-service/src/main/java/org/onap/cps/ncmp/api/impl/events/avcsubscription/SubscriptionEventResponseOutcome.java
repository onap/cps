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
import java.util.Map;
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
        final Map<String, SubscriptionStatus> cmHandleIdToStatusMapFromDb =
                DataNodeHelper.getCmHandleIdToStatusMapFromDataNodes(
                        subscriptionPersistence.getCmHandlesForSubscriptionEvent(
                                subscriptionEventResponse.getData().getClientId(),
                                subscriptionEventResponse.getData().getSubscriptionName()));
        return fromSubscriptionEventResponse(subscriptionEventResponse,
                isFullOutcomeResponse(cmHandleIdToStatusMapFromDb));
    }

    private boolean isFullOutcomeResponse(final Map<String, SubscriptionStatus> cmHandleIdToStatusMap) {
        return !cmHandleIdToStatusMap.values().contains(SubscriptionStatus.PENDING);
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
