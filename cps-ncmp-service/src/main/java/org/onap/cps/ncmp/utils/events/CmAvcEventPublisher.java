/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 Nordix Foundation
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

package org.onap.cps.ncmp.utils.events;

import io.cloudevents.CloudEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.events.EventsPublisher;
import org.onap.cps.ncmp.events.avc.ncmp_to_client.Avc;
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AvcEvent;
import org.onap.cps.ncmp.events.avc.ncmp_to_client.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CmAvcEventPublisher {

    private final EventsPublisher<CloudEvent> eventsPublisher;

    @Value("${app.ncmp.avc.inventory-events-topic}")
    private String ncmpInventoryEventsTopicName;

    /**
     * Publish attribute value change event.
     *
     * @param eventKey id of the cmHandle being registered
     */
    public void publishAvcEvent(final String eventKey, final String attributeName,
                                final String oldAttributeValue, final String newAttributeValue) {
        final AvcEvent avcEvent = buildAvcEvent(attributeName, oldAttributeValue, newAttributeValue);

        final Map<String, String> extensions = createAvcEventExtensions(eventKey);
        final CloudEvent avcCloudEvent =
                NcmpEvent.builder().type(AvcEvent.class.getTypeName())
                        .data(avcEvent).extensions(extensions).build().asCloudEvent();

        eventsPublisher.publishCloudEvent(ncmpInventoryEventsTopicName, eventKey, avcCloudEvent);
    }

    private AvcEvent buildAvcEvent(final String attributeName,
                                   final String oldAttributeValue,
                                   final String newAttributeValue) {
        final Avc avc = new Avc();
        avc.setAttributeName(attributeName);
        avc.setOldAttributeValue(oldAttributeValue);
        avc.setNewAttributeValue(newAttributeValue);

        final Data payload = new Data();
        payload.setAttributeValueChange(Collections.singletonList(avc));
        final AvcEvent avcEvent = new AvcEvent();
        avcEvent.setData(payload);
        return avcEvent;
    }

    private Map<String, String> createAvcEventExtensions(final String eventKey) {
        final Map<String, String> extensions = new HashMap<>();
        extensions.put("correlationid", eventKey);
        return extensions;
    }
}