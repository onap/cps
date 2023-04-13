/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.async

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.event.model.DMIAsyncBulkResponseEvent
import org.onap.cps.ncmp.event.model.NCMPAsyncBulkResponseEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers

import java.time.Duration

@SpringBootTest(classes = [EventsPublisher, AsyncBulkResponseEventConsumer, ObjectMapper, JsonObjectMapper])
@Testcontainers
@DirtiesContext
class AsyncBulkResponseEventConsumerSpec extends MessagingBaseSpec {

    @SpringBean
    EventsPublisher bulkResponseEventPublisher = new EventsPublisher<NCMPAsyncBulkResponseEvent>(kafkaTemplate)


    @SpringBean
    AsyncBulkResponseEventMapper asyncBulkResponseEventMapper = Mappers.getMapper(AsyncBulkResponseEventMapper.class)

    @SpringBean
    AsyncBulkResponseEventConsumer asyncBulkResponseEventConsumer =
            new AsyncBulkResponseEventConsumer(bulkResponseEventPublisher, asyncBulkResponseEventMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def kafkaConsumer = new KafkaConsumer<>(consumerConfigProperties('test'))

    def 'Consume and forward event'() {
        given: 'consumer has a subscription'
        kafkaConsumer.subscribe(['client-topic'] as List<String>)
        and: 'an event is sent'
        def jsonData = TestUtils.getResourceFileContent('dmiAsyncBulkResponseEvent.json')
        def testEventSent = jsonObjectMapper.convertJsonString(jsonData, DMIAsyncBulkResponseEvent.class)
        when: 'the event is consumed and transferred to client specified topic'
        asyncBulkResponseEventConsumer.consumeAndForward(testEventSent)
        and: 'the client specified topic is polled'
        def records = kafkaConsumer.poll(Duration.ofMillis(1500))
        then: 'poll returns one record'
        assert records.size() == 1
        and: 'consumed published event data is the same as sent event data'
        def record = records.iterator().next()
        assert testEventSent.getEvent().get('response-data').get('ietf-netconf-monitoring:netconf-state').get('schemas')
                .get('schema').get(0).get('identifier') == (jsonObjectMapper.convertJsonString(record.value(),
                DMIAsyncBulkResponseEvent).getEvent().get('response-data').get('ietf-netconf-monitoring:netconf-state')
                .get('schemas').get('schema').get(0).get('identifier'))
    }
}
