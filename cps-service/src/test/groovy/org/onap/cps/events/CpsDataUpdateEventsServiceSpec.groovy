/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 TechMahindra Ltd.
 * Copyright (C) 2024 Nordix Foundation.
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

import static org.onap.cps.events.model.Data.Operation.CREATE
import static org.onap.cps.events.model.Data.Operation.DELETE
import static org.onap.cps.events.model.Data.Operation.UPDATE

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.CloudEventUtils
import io.cloudevents.jackson.PojoCloudEventDataMapper
import org.onap.cps.events.model.CpsDataUpdatedEvent
import org.onap.cps.spi.api.model.Anchor
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import java.time.OffsetDateTime

@ContextConfiguration(classes = [ObjectMapper, JsonObjectMapper])
class CpsDataUpdateEventsServiceSpec extends Specification {
    def mockEventsPublisher = Mock(EventsPublisher)
    def objectMapper = new ObjectMapper();

    def objectUnderTest = new CpsDataUpdateEventsService(mockEventsPublisher)

    def 'Create and Publish cps update event where events are #scenario'() {
        given: 'an anchor, operation and observed timestamp'
            def anchor = new Anchor('anchor01', 'dataspace01', 'schema01');
            def operation = operationInRequest
            def observedTimestamp = OffsetDateTime.now()
        and: 'notificationsEnabled is #notificationsEnabled and it will be true as default'
            objectUnderTest.notificationsEnabled = true
        and: 'cpsChangeEventNotificationsEnabled is also true'
            objectUnderTest.cpsChangeEventNotificationsEnabled = true
        when: 'service is called to publish data update event'
            objectUnderTest.topicName = "cps-core-event"
            objectUnderTest.publishCpsDataUpdateEvent(anchor, xpath, operation, observedTimestamp)
        then: 'the event contains the required attributes'
            1 * mockEventsPublisher.publishCloudEvent('cps-core-event', 'dataspace01:anchor01', _) >> {
            args ->
                {
                    def cpsDataUpdatedEvent = (args[2] as CloudEvent)
                    assert cpsDataUpdatedEvent.getExtension('correlationid') == 'dataspace01:anchor01'
                    assert cpsDataUpdatedEvent.type == 'org.onap.cps.events.model.CpsDataUpdatedEvent'
                    assert cpsDataUpdatedEvent.source.toString() == 'CPS'
                    def actualEventOperation = CloudEventUtils.mapData(cpsDataUpdatedEvent, PojoCloudEventDataMapper.from(objectMapper, CpsDataUpdatedEvent.class)).getValue().data.operation
                    assert actualEventOperation == expectedOperation
                }
            }
        where: 'the following values are used'
        scenario                                   | xpath        | operationInRequest  || expectedOperation
        'empty xpath'                              | ''           | CREATE              || CREATE
        'root xpath and create operation'          | '/'          | CREATE              || CREATE
        'root xpath and update operation'          | '/'          | UPDATE              || UPDATE
        'root xpath and delete operation'          | '/'          | DELETE              || DELETE
        'not root xpath and update operation'      | 'test'       | UPDATE              || UPDATE
        'root node xpath and create operation'     | '/test'      | CREATE              || CREATE
        'non root node xpath and update operation' | '/test/path' | CREATE              || UPDATE
        'non root node xpath and delete operation' | '/test/path' | DELETE              || UPDATE
    }

    def 'publish cps update event when #scenario'() {
        given: 'an anchor, operation and observed timestamp'
            def anchor = new Anchor('anchor01', 'dataspace01', 'schema01');
            def operation = CREATE
            def observedTimestamp = OffsetDateTime.now()
        and: 'notificationsEnabled is #notificationsEnabled'
            objectUnderTest.notificationsEnabled = notificationsEnabled
        and: 'cpsChangeEventNotificationsEnabled is #cpsChangeEventNotificationsEnabled'
            objectUnderTest.cpsChangeEventNotificationsEnabled = cpsChangeEventNotificationsEnabled
        when: 'service is called to publish data update event'
            objectUnderTest.topicName = "cps-core-event"
            objectUnderTest.publishCpsDataUpdateEvent(anchor, '/', operation, observedTimestamp)
        then: 'the event contains the required attributes'
            expectedCallToPublisher * mockEventsPublisher.publishCloudEvent('cps-core-event', 'dataspace01:anchor01', _)
        where: 'below scenarios are present'
            scenario                                     | notificationsEnabled | cpsChangeEventNotificationsEnabled || expectedCallToPublisher
            'both notifications enabled'                 | true                 | true                               || 1
            'both notifications disabled'                 | false                | false                              || 0
            'only CPS change event notification enabled' | false                | true                               || 0
            'only overall notification enabled'          | true                 | false                              || 0

    }

    def 'publish cps update event when no timestamp provided'() {
        given: 'an anchor, operation and null timestamp'
            def anchor = new Anchor('anchor01', 'dataspace01', 'schema01');
            def operation = CREATE
            def observedTimestamp = null
        and: 'notificationsEnabled is true'
            objectUnderTest.notificationsEnabled = true
        and: 'cpsChangeEventNotificationsEnabled is true'
            objectUnderTest.cpsChangeEventNotificationsEnabled = true
        when: 'service is called to publish data update event'
            objectUnderTest.topicName = "cps-core-event"
            objectUnderTest.publishCpsDataUpdateEvent(anchor, '/', operation, observedTimestamp)
        then: 'the event is published'
            1 * mockEventsPublisher.publishCloudEvent('cps-core-event', 'dataspace01:anchor01', _)
    }
}
