/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.utils.events

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import org.onap.cps.events.EventsProducer
import org.onap.cps.ncmp.config.CpsApplicationContext
import org.onap.cps.ncmp.events.avc.ncmp_to_client.Avc
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AvcEvent
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [CpsApplicationContext, ObjectMapper, JsonObjectMapper])
class InventoryEventProducerSpec extends MessagingBaseSpec {

    def mockEventsProducer = Mock(EventsProducer<CloudEvent>)
    def objectUnderTest = new InventoryEventProducer(mockEventsProducer)

    def 'Send an attribute value change event'() {
        given: 'the event key'
            def someEventKey = 'someEventKey'
        and: 'the name of the attribute being changed'
            def someAttributeName = 'someAttributeName'
        and: 'the old value of the attribute'
            def someOldAttributeValue = 'someOldAttributeValue'
        and: 'the new value of the attribute'
            def someNewAttributeValue = 'someNewAttributeValue'
        when: 'an attribute value change event is sent'
            objectUnderTest.sendAvcEvent(someEventKey, someAttributeName, someOldAttributeValue, someNewAttributeValue)
        then: 'the cloud event producer is invoked with the correct data'
            1 * mockEventsProducer.sendCloudEvent(_, someEventKey,
                cloudEvent -> {
                    def actualAvcs = CloudEventMapper.toTargetEvent(cloudEvent, AvcEvent.class).data.attributeValueChange
                    def expectedAvc = new Avc(attributeName: someAttributeName,
                        oldAttributeValue: someOldAttributeValue,
                        newAttributeValue: someNewAttributeValue)
                    assert actualAvcs == [expectedAvc]
                })
    }

}
