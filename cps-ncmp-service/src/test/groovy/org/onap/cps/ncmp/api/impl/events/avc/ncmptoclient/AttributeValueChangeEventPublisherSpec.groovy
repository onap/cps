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
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.utils.AttributeValueChangeEventCloudMapper
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChange
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChangeEvent
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent

class AttributeValueChangeEventPublisherSpec extends MessagingBaseSpec {

    EventsPublisher<CloudEvent> mockEventsPublisher = Mock(EventsPublisher<CloudEvent>)
    AttributeValueChangeEventCloudMapper attributeValueChangeEventCloudMapper = new AttributeValueChangeEventCloudMapper(new ObjectMapper())
    AttributeValueChangeEventPublisher objectUnderTest = new AttributeValueChangeEventPublisher(mockEventsPublisher,
        attributeValueChangeEventCloudMapper)

    def 'Publish an attribute value change event'() {
        given: 'the event key'
            def someEventKey = 'someEventKey'
        and: 'the name of the attribute being changed'
            def someAttributeName = 'someAttributeName'
        and: 'the old value of the attribute'
            def someOldAttributeValue = 'someOldAttributeValue'
        and: 'the new value of the attribute'
            def someNewAttributeValue = 'someNewAttributeValue'
        when: 'an attribute value change event is published'
            objectUnderTest.publishAttributeValueChangeEvent(someEventKey, someAttributeName, someOldAttributeValue, someNewAttributeValue)
        then: 'the cloud event publisher is invoked with the correct data'
            1 * mockEventsPublisher.publishCloudEvent(null, someEventKey,
                cloudEvent -> {
                    def attributeValueChanges = toTargetEvent(cloudEvent, AttributeValueChangeEvent.class).getData().getAttributeValueChange()
                    def attributeValueChange = new AttributeValueChange(attributeName: someAttributeName,
                        oldAttributeValue: someOldAttributeValue,
                        newAttributeValue: someNewAttributeValue)
                    attributeValueChanges == [attributeValueChange]
                })
    }

}
