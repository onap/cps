/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.event.lcm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.ncmp.cmhandle.lcm.event.NcmpEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * NcmpEventService to call the publisher and publish on the dedicated topic.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmEventsService {

    private final LcmEventsPublisher lcmEventsPublisher;

    @Value("${app.lcm.events.topic:ncmp-events}")
    private String topicName;

    @Value("${notification.enabled:true}")
    private boolean notificationsEnabled;

    /**
     * Publish the NcmpEvent to the public topic.
     *
     * @param cmHandleId Cm Handle Id
     * @param ncmpEvent  Ncmp Event
     */
    public void publishLcmEvent(final String cmHandleId, final NcmpEvent ncmpEvent) {
        if (notificationsEnabled) {
            lcmEventsPublisher.publishEvent(topicName, cmHandleId, ncmpEvent);
        } else {
            log.debug("Notifications disabled.");
        }
    }
}
