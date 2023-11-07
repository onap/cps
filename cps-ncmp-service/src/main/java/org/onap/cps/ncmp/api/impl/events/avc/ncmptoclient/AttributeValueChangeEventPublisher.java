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
import org.onap.cps.ncmp.api.impl.events.EventsPublisher;
import org.onap.cps.ncmp.api.impl.utils.AttributeValueChangeEventCloudMapper;
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChange;
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChangeEvent;
import org.onap.cps.ncmp.events.avc.ncmp_to_client.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttributeValueChangeEventPublisher {

    private final EventsPublisher<CloudEvent> eventsPublisher;
    private final AttributeValueChangeEventCloudMapper attributeValueChangeEventCloudMapper;

    @Value("${app.ncmp.avc.cm-events-topic}")
    private String attributeValueChangeTopic;

    /**
     * Publish attribute value change event.
     *
     * @param eventKey id of the cmHandle being registered
     */
    public void publishAttributeValueChangeEvent(final String eventKey, final String attributeName,
                                                 final String oldAttributeValue, final String newAttributeValue) {
        final AttributeValueChangeEvent avcEvent = createAvcEvent(attributeName,
                oldAttributeValue,
                newAttributeValue);
        final CloudEvent avcCloudEvent = attributeValueChangeEventCloudMapper.toCloudEvent(avcEvent,
                eventKey,
                AttributeValueChangeEvent.class.getTypeName());
        eventsPublisher.publishCloudEvent(attributeValueChangeTopic, eventKey, avcCloudEvent);
    }

    private static AttributeValueChangeEvent createAvcEvent(final String attributeName,
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
}
