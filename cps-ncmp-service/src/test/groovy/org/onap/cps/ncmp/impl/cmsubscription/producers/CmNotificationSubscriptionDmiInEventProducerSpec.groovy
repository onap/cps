/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2024 Nordix Foundation.
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

package org.onap.cps.ncmp.impl.cmsubscription.producers

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.ncmp_to_dmi.CmHandle
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.ncmp_to_dmi.CmNotificationSubscriptionDmiInEvent
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.ncmp_to_dmi.Data
import org.onap.cps.ncmp.utils.events.CloudEventMapper
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class CmNotificationSubscriptionDmiInEventProducerSpec extends Specification {

    def mockEventsPublisher = Mock(EventsPublisher)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def objectUnderTest = new CmNotificationSubscriptionDmiInEventProducer(mockEventsPublisher, jsonObjectMapper)

    def 'Create and Publish Cm Notification Subscription DMI In Event'() {
        given: 'a cm subscription for a dmi plugin'
            def subscriptionId = 'test-subscription-id'
            def dmiPluginName = 'test-dmiplugin'
            def eventType = 'subscriptionCreateRequest'
            def cmNotificationSubscriptionDmiInEvent = new CmNotificationSubscriptionDmiInEvent(data: new Data(cmHandles: [new CmHandle(cmhandleId: 'test-1', privateProperties: [:])]))
        and: 'also we have target topic for dmiPlugin'
            objectUnderTest.cmNotificationSubscriptionDmiInEventTopic = 'dmiplugin-test-topic'
        when: 'the event is published'
            objectUnderTest.publishCmNotificationSubscriptionDmiInEvent(subscriptionId, dmiPluginName, eventType, cmNotificationSubscriptionDmiInEvent)
        then: 'the event contains the required attributes'
            1 * mockEventsPublisher.publishCloudEvent(_, _, _) >> {
                args ->
                    {
                        assert args[0] == 'dmiplugin-test-topic'
                        assert args[1] == subscriptionId
                        def cmNotificationSubscriptionDmiInEventAsCloudEvent = (args[2] as CloudEvent)
                        assert cmNotificationSubscriptionDmiInEventAsCloudEvent.getExtension('correlationid') == subscriptionId + '#' + dmiPluginName
                        assert cmNotificationSubscriptionDmiInEventAsCloudEvent.type == 'subscriptionCreateRequest'
                        assert cmNotificationSubscriptionDmiInEventAsCloudEvent.source.toString() == 'NCMP'
                        assert CloudEventMapper.toTargetEvent(cmNotificationSubscriptionDmiInEventAsCloudEvent, CmNotificationSubscriptionDmiInEvent) == cmNotificationSubscriptionDmiInEvent
                    }
            }
    }
}
