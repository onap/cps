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

package org.onap.cps.ncmp.api.impl.event.avc;

import static org.onap.cps.ncmp.event.model.SubscriptionEventOutcome.EventType.COMPLETE_OUTCOME;
import static org.onap.cps.ncmp.event.model.SubscriptionEventOutcome.EventType.PARTIAL_OUTCOME;

import com.hazelcast.map.IMap;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.events.avcsubscription.SubscriptionEventForwarder;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus;
import org.onap.cps.ncmp.api.impl.utils.DataNodeHelper;
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse;
import org.onap.cps.ncmp.event.model.SubscriptionEventOutcome;
import org.onap.cps.spi.model.DataNode;

@Slf4j
@RequiredArgsConstructor
public class ResponseTimeoutTask implements Runnable {

    private final IMap<String, Set<String>> forwardedSubscriptionEventCache;
    private final String subscriptionClientId;
    private final String subscriptionName;
    private final String subscriptionEventId;
    private final SubscriptionPersistence subscriptionPersistence;
    private final SubscriptionEventForwarder subscriptionEventForwarder;
    private final SubscriptionOutcomeMapper subscriptionOutcomeMapper;

    @Override
    public void run() {
        if (forwardedSubscriptionEventCache.containsKey(subscriptionEventId)) {
            final Set<String> dmiNames = forwardedSubscriptionEventCache.get(subscriptionEventId);

            final Collection<DataNode> dataNodes = subscriptionPersistence.getDataNodesForSubscriptionEvent();
            final List<Map<String, Serializable>> dataNodeLeaves = DataNodeHelper.getDataNodeLeaves(dataNodes);
            final List<Collection<Serializable>> cmHandleIdToStatus =
                    DataNodeHelper.getCmHandleIdToStatus(dataNodeLeaves);
            final SubscriptionEventOutcome subscriptionEventOutcome =
                    formSubscriptionOutcomeMessage(cmHandleIdToStatus);

            if (dmiNames.isEmpty()) {
                log.info("placeholder to create full outcome response for subscriptionEventId: {}.",
                    subscriptionEventId);
                subscriptionEventOutcome.setEventType(COMPLETE_OUTCOME);
            } else {
                log.info("placeholder to create partial outcome response for subscriptionEventId: {}.",
                    subscriptionEventId);
                subscriptionEventOutcome.setEventType(PARTIAL_OUTCOME);
            }
            subscriptionEventForwarder
                    .forwardOutcomeEventToClientApps(subscriptionEventOutcome, subscriptionEventId);
            forwardedSubscriptionEventCache.remove(subscriptionEventId);
        }
    }

    private SubscriptionEventOutcome formSubscriptionOutcomeMessage(
            final List<Collection<Serializable>> cmHandleIdToStatus) {
        final Map<String, SubscriptionStatus> cmHandleIdToStatusMap = new HashMap<>();
        final SubscriptionEventResponse forPojoConversionOnly = new SubscriptionEventResponse();
        forPojoConversionOnly.setClientId(subscriptionClientId);
        forPojoConversionOnly.setSubscriptionName(subscriptionName);

        for (final Collection<Serializable> cmHandleToStatusBucket: cmHandleIdToStatus) {
            final Iterator<Serializable> bucketIterator = cmHandleToStatusBucket.iterator();
            while (bucketIterator.hasNext()) {
                final String item = (String) bucketIterator.next();
                if ("PENDING".equals(item)) {
                    cmHandleIdToStatusMap.put((String) bucketIterator.next(),
                            SubscriptionStatus.PENDING);
                }
                if ("REJECTED".equals(item)) {
                    cmHandleIdToStatusMap.put((String) bucketIterator.next(),
                            SubscriptionStatus.REJECTED);
                }
                if ("ACCEPTED".equals(item)) {
                    cmHandleIdToStatusMap.put((String) bucketIterator.next(),
                            SubscriptionStatus.ACCEPTED);
                }
            }
        }
        forPojoConversionOnly.setCmHandleIdToStatus(cmHandleIdToStatusMap);

        return subscriptionOutcomeMapper.toSubscriptionEventOutcome(forPojoConversionOnly);
    }
}