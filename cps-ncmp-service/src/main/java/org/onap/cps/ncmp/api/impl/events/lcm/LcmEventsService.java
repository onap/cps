/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.events.lcm;

import static org.onap.cps.ncmp.api.impl.events.NcmpCloudEventBuilder.createCloudEventExtension;

import io.cloudevents.CloudEvent;
import io.micrometer.core.annotation.Timed;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.events.EventsPublisher;
import org.onap.cps.ncmp.api.impl.events.NcmpCloudEventBuilder;
import org.onap.cps.ncmp.events.lcm.v2.LcmEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Service;

/**
 * LcmEventsService to call the publisher and publish on the dedicated topic.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmEventsService {

    private final EventsPublisher<CloudEvent> eventsPublisher;

    @Value("${app.lcm.events.topic:ncmp-events}")
    private String topicName;

    @Value("${notification.enabled:true}")
    private boolean notificationsEnabled;

    /**
     * Publish the LcmEvent with header to the public topic.
     *
     * @param cmHandleId     Cm Handle Id
     * @param lcmEvent       Lcm Event
     */
    @Timed(value = "cps.ncmp.lcm.events.publish", description = "Time taken to publish a LCM event")
    public void publishLcmEvent(final String cmHandleId, final LcmEvent lcmEvent) {
        if (notificationsEnabled) {
            try {
                final Map<String, String> extensions = createCloudEventExtension("correlationid", cmHandleId);
                final CloudEvent lcmCloudEvent =
                        NcmpCloudEventBuilder.builder().type(LcmEvent.class.getTypeName())
                                .event(lcmEvent).extensions(extensions).setCloudEvent().build();
                eventsPublisher.publishCloudEvent(topicName, cmHandleId, lcmCloudEvent);
            } catch (final KafkaException e) {
                log.error("Unable to publish message to topic : {} and cause : {}", topicName, e.getMessage());
            }
        } else {
            log.debug("Notifications disabled.");
        }
    }
}
