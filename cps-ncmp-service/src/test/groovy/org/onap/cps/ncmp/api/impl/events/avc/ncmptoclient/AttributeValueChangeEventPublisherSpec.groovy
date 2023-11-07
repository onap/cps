/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.events.avc.ncmptoclient

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.utils.AttributeValueChangeEventCloudMapper
import org.onap.cps.ncmp.api.impl.utils.CmSubscriptionEventCloudMapper
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChange
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChangeEvent
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper, AttributeValueChangeEventPublisher])
class AttributeValueChangeEventPublisherSpec extends MessagingBaseSpec {

    @Autowired
    AttributeValueChangeEventPublisher objectUnderTest
    @SpringBean
    EventsPublisher<CloudEvent> mockAttributeValueChangeEventPublisher = Mock(EventsPublisher<CloudEvent>)
    @SpringBean
    AttributeValueChangeEventCloudMapper attributeValueChangeEventCloudMapper = new AttributeValueChangeEventCloudMapper(new ObjectMapper())

    def 'Publish an attribute value change event'() {
        given: ''
            def someEventKey = "someEventKey"
            def someAttributeName = "someAttributeName"
            def someOldAttributeValue = "someOldAttributeValue"
            def someNewAttributeValue = "someNewAttributeValue"
        when: ''
            objectUnderTest.publishAttributeValueChangeEvent(someEventKey, someAttributeName, someOldAttributeValue, someNewAttributeValue)
        then: ''
            1 * mockAttributeValueChangeEventPublisher.publishCloudEvent("cm-events", someEventKey,
                cloudEvent -> {
                    def attributeValueChanges = toTargetEvent(cloudEvent, AttributeValueChangeEvent.class).getData().getAttributeValueChange()
                    def attributeValueChange = attributeValueChangeObject(someAttributeName, someOldAttributeValue, someNewAttributeValue)
                    attributeValueChanges == [attributeValueChange]
                })
    }

    static def attributeValueChangeObject(attributeName, oldAttributeValue, newAttributeValue) {
        def attributeValueChange = new AttributeValueChange()
        attributeValueChange.setAttributeName(attributeName)
        attributeValueChange.setOldAttributeValue(oldAttributeValue)
        attributeValueChange.setNewAttributeValue(newAttributeValue)
        return attributeValueChange
    }

}
