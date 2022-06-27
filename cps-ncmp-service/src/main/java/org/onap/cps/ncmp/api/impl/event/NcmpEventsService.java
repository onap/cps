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

package org.onap.cps.ncmp.api.impl.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.ncmp.cmhandle.lcm.event.NcmpEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * NcmpEventService to map the event correctly and publish to the public topic.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class NcmpEventsService {

    private final NcmpEventsPublisher ncmpEventsPublisher;

    @Value("${app.ncmp.events.topic:ncmp-events}")
    private String topicName;

    /**
     * Publish the NcmpEvent to the public topic.
     *
     * @param cmHandleId Cm Handle Id
     * @param ncmpEvent  Ncmp Event
     */
    public void publishNcmpEvent(final String cmHandleId, final NcmpEvent ncmpEvent) {

        ncmpEventsPublisher.publishEvent(topicName, cmHandleId, ncmpEvent);

    }
}
