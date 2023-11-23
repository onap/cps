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

package org.onap.cps.ncmp.api.impl.events.avc.ncmptoclient;

import io.cloudevents.CloudEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.impl.events.EventsPublisher;
import org.onap.cps.ncmp.api.impl.events.NcmpCloudEventBuilder;
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChange;
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChangeEvent;
import org.onap.cps.ncmp.events.avc.ncmp_to_client.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttributeValueChangeEventPublisher {

    private final EventsPublisher<CloudEvent> eventsPublisher;

    @Value("${app.ncmp.avc.cm-events-topic}")
    private String attributeValueChangeTopic;

    /**
     * Publish attribute value change event.
     *
     * @param eventKey id of the cmHandle being registered
     */
    public void publishAttributeValueChangeEvent(final String eventKey, final String attributeName,
                                                 final String oldAttributeValue, final String newAttributeValue) {
        final AttributeValueChangeEvent avcEvent = buildAttributeValueChangeEvent(attributeName,
                oldAttributeValue,
                newAttributeValue);

        final Map<String, String> extensions = createAvcEventExtensions(eventKey);
        final CloudEvent avcCloudEvent =
            NcmpCloudEventBuilder.builder().type(AttributeValueChangeEvent.class.getTypeName())
            .event(avcEvent).extensions(extensions).setCloudEvent().build();

        eventsPublisher.publishCloudEvent(attributeValueChangeTopic, eventKey, avcCloudEvent);
    }

    private AttributeValueChangeEvent buildAttributeValueChangeEvent(final String attributeName,
                                                                            final String oldAttributeValue,
                                                                            final String newAttributeValue) {
        final AttributeValueChange attributeValueChange = new AttributeValueChange();
        attributeValueChange.setAttributeName(attributeName);
        attributeValueChange.setOldAttributeValue(oldAttributeValue);
        attributeValueChange.setNewAttributeValue(newAttributeValue);

        final Data payload = new Data();
        payload.setAttributeValueChange(Collections.singletonList(attributeValueChange));
        final AttributeValueChangeEvent attributeValueChangeEvent = new AttributeValueChangeEvent();
        attributeValueChangeEvent.setData(payload);
        return attributeValueChangeEvent;
    }

    private Map<String, String> createAvcEventExtensions(final String eventKey) {
        final Map<String, String> extensions = new HashMap<>();
        extensions.put("correlationid", eventKey);
        return extensions;
    }
}
