/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.datajobs.subscription.dmi

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import org.onap.cps.events.EventProducer
import org.onap.cps.ncmp.config.CpsApplicationContext
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.CmHandle
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.Data
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.DataJobSubscriptionDmiInEvent
import org.onap.cps.ncmp.utils.events.CloudEventMapper
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [CpsApplicationContext, ObjectMapper, JsonObjectMapper, CloudEventBuilder])
class EventProducerSpec extends Specification {

    def mockEventProducer = Mock(EventProducer)

    def objectUnderTest = new DmiEventProducer(mockEventProducer)

    def 'Create and Send Cm Notification Subscription DMI In Event'() {
        given: 'a cm subscription for a dmi plugin'
            def subscriptionId = 'test-subscription-id'
            def dmiPluginName = 'test-dmiplugin'
            def eventType = 'subscriptionCreateRequest'
            def dmiInEvent = new DataJobSubscriptionDmiInEvent(data: new Data(cmHandles: [new CmHandle(cmhandleId: 'test-1', privateProperties: [:])]))
        and: 'also we have target topic for dmiPlugin'
            objectUnderTest.dmiInEventTopic = 'dmiplugin-test-topic'
        when: 'the event is sent'
            objectUnderTest.send(subscriptionId, dmiPluginName, eventType, dmiInEvent)
        then: 'the event contains the required attributes'
            1 * mockEventProducer.sendCloudEvent(_, _, _) >> {
                args ->
                    {
                        assert args[0] == 'dmiplugin-test-topic'
                        assert args[1] == subscriptionId
                        def dmiInEventAsCloudEvent = (args[2] as CloudEvent)
                        assert dmiInEventAsCloudEvent.getExtension('correlationid') == subscriptionId + '#' + dmiPluginName
                        assert dmiInEventAsCloudEvent.type == 'subscriptionCreateRequest'
                        assert dmiInEventAsCloudEvent.source.toString() == 'NCMP'
                        assert CloudEventMapper.toTargetEvent(dmiInEventAsCloudEvent, DataJobSubscriptionDmiInEvent) == dmiInEvent
                    }
            }
    }
}
