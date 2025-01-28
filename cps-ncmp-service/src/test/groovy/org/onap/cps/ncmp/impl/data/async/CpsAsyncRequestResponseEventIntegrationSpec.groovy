/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2022-2024 Nordix Foundation.
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

package org.onap.cps.ncmp.impl.data.async

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.mapstruct.factory.Mappers
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.event.model.ncmp.asyncm2m.DmiAsyncRequestResponseEvent
import org.onap.cps.ncmp.event.model.ncmp.asyncm2m.NcmpAsyncRequestResponseEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.ncmp.utils.events.MessagingBaseSpec
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers

import java.time.Duration

@SpringBootTest(classes = [EventsPublisher, AsyncRestRequestResponseEventConsumer, ObjectMapper, JsonObjectMapper])
@Testcontainers
@DirtiesContext
class NcmpAsyncRequestResponseEventProducerIntegrationSpec extends MessagingBaseSpec {

    @SpringBean
    EventsPublisher cpsAsyncRequestResponseEventPublisher =
        new EventsPublisher<NcmpAsyncRequestResponseEvent>(legacyEventKafkaTemplate, cloudEventKafkaTemplate);


    @SpringBean
    NcmpAsyncRequestResponseEventMapper ncmpAsyncRequestResponseEventMapper =
            Mappers.getMapper(NcmpAsyncRequestResponseEventMapper.class)

    @SpringBean
    AsyncRestRequestResponseEventConsumer ncmpAsyncRequestResponseEventConsumer =
            new AsyncRestRequestResponseEventConsumer(cpsAsyncRequestResponseEventPublisher,
                    ncmpAsyncRequestResponseEventMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def legacyEventKafkaConsumer = new KafkaConsumer<>(eventConsumerConfigProperties('test', StringDeserializer))

    def 'Consume and forward valid message'() {
        given: 'consumer has a subscription'
            legacyEventKafkaConsumer.subscribe(['test-topic'] as List<String>)
        and: 'an event is sent'
            def jsonData = TestUtils.getResourceFileContent('dmiAsyncRequestResponseEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, DmiAsyncRequestResponseEvent.class)
        when: 'the event is consumed'
            ncmpAsyncRequestResponseEventConsumer.consumeAndForward(testEventSent)
        and: 'the topic is polled'
            def records = legacyEventKafkaConsumer.poll(Duration.ofMillis(1500))
        then: 'poll returns one record'
            assert records.size() == 1
        and: 'consumed forwarded event id is the same as sent event id'
            def record = records.iterator().next()
            assert testEventSent.eventId.equalsIgnoreCase(jsonObjectMapper.convertJsonString(record.value(),
                    NcmpAsyncRequestResponseEvent).getForwardedEvent().getEventId())
    }

}
