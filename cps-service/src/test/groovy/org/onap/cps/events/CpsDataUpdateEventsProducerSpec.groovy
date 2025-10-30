/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024-2025 TechMahindra Ltd.
 * Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.events

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.CloudEventUtils
import io.cloudevents.jackson.PojoCloudEventDataMapper
import org.onap.cps.api.CpsNotificationService
import org.onap.cps.api.model.Anchor
import org.onap.cps.events.model.CpsDataUpdatedEvent
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import java.time.OffsetDateTime

import static org.onap.cps.events.model.EventPayload.Action.CREATE
import static org.onap.cps.events.model.EventPayload.Action.REMOVE
import static org.onap.cps.events.model.EventPayload.Action.REPLACE

@ContextConfiguration(classes = [ObjectMapper, JsonObjectMapper])
class CpsDataUpdateEventsProducerSpec extends Specification {
    def mockEventsProducer = Mock(EventsProducer)
    def objectMapper = new ObjectMapper();
    def mockCpsNotificationService = Mock(CpsNotificationService)
    def jsonObjectMapper = new JsonObjectMapper(objectMapper)

    def objectUnderTest = new CpsDataUpdateEventsProducer(mockEventsProducer, jsonObjectMapper, mockCpsNotificationService)

    def setup() {
        mockCpsNotificationService.isNotificationEnabled('dataspace01', 'anchor01') >> true
        objectUnderTest.topicName = 'cps-core-event'
    }

    static def deltaReport = []

    def 'Create and send cps event with #scenario.'() {
        given: 'an anchor, action and observed timestamp'
            def anchor = new Anchor('anchor01', 'dataspace01', 'schema01');
            def action = actionInRequest.value()
        and: 'notificationsEnabled is #notificationsEnabled and it will be true as default'
            objectUnderTest.notificationsEnabled = true
        and: 'cpsChangeEventNotificationsEnabled is also true'
            objectUnderTest.cpsChangeEventNotificationsEnabled = true
        when: 'service is called to send data REPLACE event'
            objectUnderTest.sendCpsDataUpdateEvent(anchor, xpath, action, deltaReport, OffsetDateTime.now())
        then: 'the event contains the required attributes'
            1 * mockEventsProducer.sendCloudEvent('cps-core-event', 'dataspace01:anchor01', _) >> {
            args ->
                {
                    def cpsDataUpdatedEvent = (args[2] as CloudEvent)
                    assert cpsDataUpdatedEvent.getExtension('correlationid') == 'dataspace01:anchor01'
                    assert cpsDataUpdatedEvent.type == 'org.onap.cps.events.model.CpsDataUpdatedEvent'
                    assert cpsDataUpdatedEvent.source.toString() == 'CPS'
                    def actualEventOperation = CloudEventUtils.mapData(cpsDataUpdatedEvent, PojoCloudEventDataMapper.from(objectMapper, CpsDataUpdatedEvent.class)).getValue().eventPayload.action.value()
                    assert actualEventOperation == expectedAction.value()
                }
            }
        where: 'the following values are used'
            scenario                                 | xpath        | actionInRequest || expectedAction
            'empty xpath'                            | ''           | CREATE          || CREATE
            'root xpath and create action'           | '/'          | CREATE          || CREATE
            'root xpath and replace action'          | '/'          | REPLACE         || REPLACE
            'root xpath and remove action'           | '/'          | REMOVE          || REMOVE
            'not root xpath and replace action'      | 'test'       | REPLACE         || REPLACE
            'root node xpath and create action'      | '/test'      | CREATE          || CREATE
            'non root node xpath and replace action' | '/test/path' | CREATE          || CREATE
            'non root node xpath and remove action'  | '/test/path' | REMOVE          || REMOVE
    }

    def 'Send cps event when no timestamp provided.'() {
        given: 'an anchor, action and null timestamp'
            def anchor = new Anchor('anchor01', 'dataspace01', 'schema01');
        and: 'notificationsEnabled is true'
            objectUnderTest.notificationsEnabled = true
        and: 'cpsChangeEventNotificationsEnabled is true'
            objectUnderTest.cpsChangeEventNotificationsEnabled = true
        when: 'service is called to send data event'
            objectUnderTest.sendCpsDataUpdateEvent(anchor, '/', CREATE.value(), deltaReport, null)
        then: 'the event is sent'
            1 * mockEventsProducer.sendCloudEvent('cps-core-event', 'dataspace01:anchor01', _)
    }

    def 'Enabling and disabling sending cps events.'() {
        given: 'an anchor'
            def anchor = new Anchor('anchor02', 'some dataspace', 'some schema');
        and: 'notificationsEnabled is #notificationsEnabled'
            objectUnderTest.notificationsEnabled = notificationsEnabled
        and: 'cpsChangeEventNotificationsEnabled is #cpsChangeEventNotificationsEnabled'
            objectUnderTest.cpsChangeEventNotificationsEnabled = cpsChangeEventNotificationsEnabled
        and: 'notification service enabled is: #cpsNotificationServiceisNotificationEnabled'
            mockCpsNotificationService.isNotificationEnabled(_, 'anchor02') >> cpsNotificationServiceisNotificationEnabled
        when: 'service is called to send data event'
            objectUnderTest.sendCpsDataUpdateEvent(anchor, '/', CREATE.value(), deltaReport, null)
        then: 'the event is only sent when all related flags are true'
            expectedCallsToProducer * mockEventsProducer.sendCloudEvent(*_)
        where: 'the following flags are used'
            notificationsEnabled | cpsChangeEventNotificationsEnabled | cpsNotificationServiceisNotificationEnabled  || expectedCallsToProducer
            false                | true                               | true                                         || 0
            true                 | false                              | true                                         || 0
            true                 | true                               | false                                        || 0
            true                 | true                               | true                                         || 1
    }

}
