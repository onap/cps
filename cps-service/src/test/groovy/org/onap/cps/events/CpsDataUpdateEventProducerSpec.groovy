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
class CpsDataUpdateEventProducerSpec extends Specification {

    static def CREATE_ACTION = CREATE.value()
    static def REPLACE_ACTION = REPLACE.value()
    static def REMOVE_ACTION = REMOVE.value()

    def mockEventProducer = Mock(EventProducer)
    def objectMapper = new ObjectMapper();
    def mockCpsNotificationService = Mock(CpsNotificationService)

    def objectUnderTest = new CpsDataUpdateEventsProducer(mockEventProducer, mockCpsNotificationService)

    def setup() {
        mockCpsNotificationService.isNotificationEnabled('dataspace01', 'anchor01') >> true
        objectUnderTest.topicName = 'cps-core-event'
    }

    def 'Create and send cps event with #scenario.'() {
        given: 'an anchor'
            def anchor = new Anchor('anchor01', 'dataspace01', 'schema01');
        and: 'notificationsEnabled is #notificationsEnabled and it will be true as default'
            objectUnderTest.notificationsEnabled = true
        and: 'cpsChangeEventNotificationsEnabled is also true'
            objectUnderTest.cpsChangeEventNotificationsEnabled = true
        when: 'service is called to send data update event'
            objectUnderTest.sendCpsDataUpdateEvent(anchor, xpath, actionInRequest, OffsetDateTime.now())
        then: 'the event contains the required attributes'
            1 * mockEventProducer.sendCloudEvent('cps-core-event', 'dataspace01:anchor01', _) >> {
            args ->
                {
                    def cpsDataUpdatedEvent = (args[2] as CloudEvent)
                    assert cpsDataUpdatedEvent.getExtension('correlationid') == 'dataspace01:anchor01'
                    assert cpsDataUpdatedEvent.type == 'org.onap.cps.events.model.CpsDataUpdatedEvent'
                    assert cpsDataUpdatedEvent.source.toString() == 'CPS'
                    def actualEventOperation = CloudEventUtils.mapData(cpsDataUpdatedEvent, PojoCloudEventDataMapper.from(objectMapper, CpsDataUpdatedEvent.class)).getValue().eventPayload.action.value()
                    assert actualEventOperation == expectedAction
                }
            }
        where: 'the following values are used'
            scenario                                 | xpath        | actionInRequest || expectedAction
            'empty xpath'                            | ''           | CREATE_ACTION   || CREATE_ACTION
            'root xpath and create action'           | '/'          | CREATE_ACTION   || CREATE_ACTION
            'root xpath and replace action'          | '/'          | REPLACE_ACTION  || REPLACE_ACTION
            'root xpath and remove action'           | '/'          | REMOVE_ACTION   || REMOVE_ACTION
            'not root xpath and replace action'      | 'test'       | REPLACE_ACTION  || REPLACE_ACTION
            'root node xpath and create action'      | '/test'      | CREATE_ACTION   || CREATE_ACTION
            'non root node xpath and replace action' | '/test/path' | CREATE_ACTION   || CREATE_ACTION
            'non root node xpath and remove action'  | '/test/path' | REMOVE_ACTION   || REMOVE_ACTION
    }

    def 'Send cps event when no timestamp provided.'() {
        given: 'an anchor'
            def anchor = new Anchor('anchor01', 'dataspace01', 'schema01');
        and: 'notificationsEnabled is true'
            objectUnderTest.notificationsEnabled = true
        and: 'cpsChangeEventNotificationsEnabled is true'
            objectUnderTest.cpsChangeEventNotificationsEnabled = true
        when: 'service is called to send data event'
            objectUnderTest.sendCpsDataUpdateEvent(anchor, '/', CREATE_ACTION, null)
        then: 'the event is sent'
            1 * mockEventProducer.sendCloudEvent('cps-core-event', 'dataspace01:anchor01', _)
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
            objectUnderTest.sendCpsDataUpdateEvent(anchor, '/', CREATE_ACTION, null)
        then: 'the event is only sent when all related flags are true'
            expectedCallsToProducer * mockEventProducer.sendCloudEvent(*_)
        where: 'the following flags are used'
            notificationsEnabled | cpsChangeEventNotificationsEnabled | cpsNotificationServiceisNotificationEnabled  || expectedCallsToProducer
            false                | true                               | true                                         || 0
            true                 | false                              | true                                         || 0
            true                 | true                               | false                                        || 0
            true                 | true                               | true                                         || 1
    }

}
