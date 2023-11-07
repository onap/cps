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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.events.EventsPublisher;
import org.onap.cps.ncmp.api.impl.utils.AttributeValueChangeEventCloudMapper;
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChange;
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChangeEvent;
import org.onap.cps.ncmp.events.avc.ncmp_to_client.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class AttributeValueChangeEventPublisher {

    private final EventsPublisher<CloudEvent> eventsPublisher;
    private final AttributeValueChangeEventCloudMapper attributeValueChangeEventCloudMapper;

    @Value("${app.ncmp.avc.cm-events-topic}")
    private String trustLevelTopic;

    private static AttributeValueChangeEvent getAttributeValueChangeEvent(final String attributeName,
                                                                          final String newAttributeValue) {
        final AttributeValueChange attributeValueChange = new AttributeValueChange();
        attributeValueChange.setAttributeName(attributeName);
        if (newAttributeValue != null && !newAttributeValue.isEmpty()) {
            attributeValueChange.setNewAttributeValue(newAttributeValue);
        }
        final Data payload = new Data();
        payload.setAttributeValueChange(Collections.singletonList(attributeValueChange));
        final AttributeValueChangeEvent attributeValueChangeEvent = new AttributeValueChangeEvent();
        attributeValueChangeEvent.setData(payload);
        return attributeValueChangeEvent;
    }

    /**
     * Publish attribute value change event.
     *
     * @param eventKey id of the cmHandle being registered
     */
    public void publishAttributeValueChangeEvent(final String eventKey, final String attributeName,
                                                 final String newAttributeValue) {
        final AttributeValueChangeEvent attributeValueChangeEvent =
            getAttributeValueChangeEvent(attributeName, newAttributeValue);
        final String eventType = AttributeValueChangeEvent.class.getTypeName();
        final CloudEvent attributeValueChangeCloudEvent =
            attributeValueChangeEventCloudMapper.toCloudEvent(attributeValueChangeEvent, eventKey, eventType);
        eventsPublisher.publishCloudEvent(trustLevelTopic, eventKey, attributeValueChangeCloudEvent);
    }
}
