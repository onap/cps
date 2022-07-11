/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.event.lcm

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.cps.ncmp.api.impl.event.lcm.LcmEventsPublisher
import org.onap.cps.ncmp.api.utils.MessagingSpec
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.onap.ncmp.cmhandle.lcm.event.Event
import org.onap.ncmp.cmhandle.lcm.event.NcmpEvent
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers

import java.time.Duration

@SpringBootTest(classes = [LcmEventsPublisher, ObjectMapper, JsonObjectMapper])
@Testcontainers
@DirtiesContext
class LcmEventsPublisherSpec extends MessagingSpec {

    def kafkaConsumer = new KafkaConsumer<>(consumerConfigProperties('ncmp-group'))

    def testTopic = 'ncmp-events-test'

    @SpringBean
    LcmEventsPublisher ncmpEventsPublisher = new LcmEventsPublisher(kafkaTemplate)

    @Autowired
    JsonObjectMapper jsonObjectMapper


    def 'Produce and Consume Ncmp Event'() {
        given: 'event key and event data'
            def eventKey = 'ncmp'
            def eventData = new NcmpEvent(eventId: 'test-uuid',
                eventCorrelationId: 'cmhandle-as-correlationid',
                eventSchema: URI.create('org.onap.ncmp.cmhandle.lcm.event:v1'),
                eventSource: URI.create('org.onap.ncmp'),
                eventTime: '2022-12-31T20:30:40.000+0000',
                eventType: 'org.onap.ncmp.cmhandle.lcm.event',
                event: new Event(cmHandleId: 'cmhandle-test', cmhandleState: 'READY', cmhandleProperties: [['publicProperty1': 'value1'], ['publicProperty2': 'value2']]))
        and: 'we have an expected NcmpEvent'
            def expectedJsonString = TestUtils.getResourceFileContent('expectedNcmpEvent.json')
            def expectedNcmpEvent = jsonObjectMapper.convertJsonString(expectedJsonString, NcmpEvent.class)
        and: 'consumer has a subscription'
            kafkaConsumer.subscribe([testTopic] as List<String>)
        when: 'an event is published'
            ncmpEventsPublisher.publishEvent(testTopic, eventKey, eventData)
        and: 'topic is polled'
            def records = kafkaConsumer.poll(Duration.ofMillis(1500))
        then: 'no exception is thrown'
            noExceptionThrown()
        and: 'poll returns one record'
            assert records.size() == 1
        and: 'record key matches the expected event key'
            def record = records.iterator().next()
            assert eventKey == record.key
        and: 'record matches the expected event'
            assert expectedNcmpEvent == jsonObjectMapper.convertJsonString(record.value, NcmpEvent.class)

    }
}
