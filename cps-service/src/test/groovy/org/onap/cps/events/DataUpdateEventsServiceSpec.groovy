/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 TechMahindra Ltd.
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

import org.onap.cps.events.model.CpsDataUpdatedEvent
import org.onap.cps.events.model.Data
import org.onap.cps.spi.model.Anchor
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import java.time.OffsetDateTime

@ContextConfiguration(classes = [ObjectMapper, JsonObjectMapper])
class DataUpdateEventsServiceSpec extends Specification {
    def mockEventsPublisher = Mock(EventsPublisher)
    def mockJsonObjectMapper = Mock(JsonObjectMapper)
    def topicName = "cps-events"
    def notificationsEnabled = true

    def objectUnderTest = new DataUpdateEventsService(mockEventsPublisher, mockJsonObjectMapper)

    def 'Create and Publish cps update event where events are #scenario'() {
        given: 'a cm handle id, Lcm Event, and headers'
            def anchor = new Anchor("anchor01", "dataspace01", "schema01");
            def operation = Data.Operation.UPDATE
            def observedTimestamp = OffsetDateTime.now()
            def dataUpdateEvent = new CpsDataUpdatedEvent(data: new Data(dataspaceName: 'dataspace01', anchorName: 'anchor01', schemaSetName: 'schema01', operation: Data.Operation.UPDATE, xpath: xpath, observedTimestamp: OffsetDateTime.now().toString()))
        and: 'notificationsEnabled is #notificationsEnabled and it will be true as default'
            objectUnderTest.notificationsEnabled = notificationsEnabled
        when: 'service is called to publish data update event'
            objectUnderTest.topicName = "cps-core-event"
            objectUnderTest.publishDataUpdateEvent(anchor, xpath, operation, observedTimestamp)
        then: 'the event contains the required attributes'
            expectedTimesMethodCalled * mockEventsPublisher.publishCloudEvent(_, _, _) >> {
            args ->
                {
                    assert args[0] == 'cps-core-event'
                    assert args[1] == 'dataspace01anchor01'
                    def cpsDataUpdatedEvent = (args[2] as CloudEvent)
                    assert cpsDataUpdatedEvent.getExtension('correlationid') == "dataspace01anchor01"
                    assert cpsDataUpdatedEvent.type == 'org.onap.cps.events.model.CpsDataUpdatedEvent'
                    assert cpsDataUpdatedEvent.source.toString() == 'CPS'
                }
            }
        where: 'the following values are used'
        scenario                           | notificationsEnabled | xpath        || expectedTimesMethodCalled
        'enabled with root xpath'          | true                 | '/'          || 1
        'enabled with empty xpath'         | true                 | ''           || 1
        'enabled with some xpath'          | true                 | '/test/path' || 1
        'enabled with container xpath'     | true                 | '/test'      || 1
        'enabled with non container xpath' | true                 | 'test'       || 1
        'disabled'                         | false                | '/test/path' || 0
    }
}
