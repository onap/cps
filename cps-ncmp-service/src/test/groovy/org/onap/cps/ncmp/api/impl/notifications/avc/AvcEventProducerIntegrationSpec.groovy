/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2022 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.notifications.avc

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.event.model.AvcEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers

import java.time.Duration

@SpringBootTest(classes = [AvcEventProducer, AvcEventConsumer, ObjectMapper, JsonObjectMapper])
@Testcontainers
@DirtiesContext
class AvcEventProducerIntegrationSpec extends MessagingBaseSpec {

    @SpringBean
    AvcEventProducer avcEventProducer = new AvcEventProducer(kafkaTemplate);

    @SpringBean
    AvcEventConsumer acvEventConsumer = new AvcEventConsumer(avcEventProducer)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def kafkaConsumer = new KafkaConsumer<>(consumerConfigProperties('test'))

    def 'Consume and forward valid message'() {
        given: 'consumer has a subscription'
            kafkaConsumer.subscribe(['clientTopic'] as List<String>)
        and: 'an event is sent'
            def jsonData = TestUtils.getResourceFileContent('avcEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, AvcEvent.class)
        when: 'the event is consumed'
            acvEventConsumer.consumeAndForward(testEventSent)
        and: 'the topic is polled'
            def records = kafkaConsumer.poll(Duration.ofMillis(1500))
        then: 'poll returns one record'
            assert records.size() == 1
        and: 'consumed forwarded event id is event id'
            def record = records.iterator().next()
            assert testEventSent.eventId.equalsIgnoreCase(
                    jsonObjectMapper.convertJsonString(record.value(), AvcEvent).getEventId())
    }

}
