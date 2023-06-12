/*
 *  ============LICENSE_START=======================================================
 *  Copyright (c) 2023 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.events.avc


import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.CloudEventUtils
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.jackson.PojoCloudEventDataMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.cps.ncmp.api.impl.config.config.CloudEventsPublisherConfig
import org.onap.cps.ncmp.api.impl.events.CloudEventsPublisher
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.avc.v1.AvcEventV1
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers

import java.time.Duration

@SpringBootTest(classes = [CloudEventsPublisher, CloudEventsPublisherConfig, AvcEventConsumer, ObjectMapper, JsonObjectMapper])
@Testcontainers
@DirtiesContext
class AvcEventConsumerSpec extends MessagingBaseSpec {

    @SpringBean
    CloudEventsPublisher cloudEventsPublisher = new CloudEventsPublisher<CloudEvent>(cloudEventKafkaTemplate)

    @SpringBean
    AvcEventConsumer acvEventConsumer = new AvcEventConsumer(cloudEventsPublisher)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    def kafkaConsumer = new KafkaConsumer<>(cloudEventConsumerConfigProperties('ncmp-group'))

    def 'Consume and forward valid message'() {
        given: 'consumer has a subscription on a topic'
            def cmEventsTopicName = 'cm-events'
            acvEventConsumer.cmEventsTopicName = cmEventsTopicName
            kafkaConsumer.subscribe([cmEventsTopicName] as List<String>)
        and: 'an event is sent'
            def jsonData = TestUtils.getResourceFileContent('sampleAvcInputEvent.json')
            def testEventData = jsonObjectMapper.convertJsonString(jsonData, AvcEventV1.class)
            def testCloudEventSent = CloudEventBuilder.v1().withData(objectMapper.writeValueAsBytes(testEventData))
                .withId('sample-eventid')
                .withType('org.onap.ncmp.cm.avc:1.0.0')
                .withDataContentType('application/json')
                .withSource(URI.create('DMI'))
                .withExtension('correlationid', 'cmhandle1')
                .build()
        and: 'event has header information'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>(cmEventsTopicName, 0, 0, 'sample-eventid', testCloudEventSent)
        when: 'the event is consumed'
            acvEventConsumer.consumeAndForward(consumerRecord)
        and: 'the topic is polled'
            def records = kafkaConsumer.poll(Duration.ofMillis(1500))
        then: 'poll returns one record'
            assert records.size() == 1
        and: 'record can be converted to AVC event'
            def record = records.iterator().next() as ConsumerRecord<String, CloudEvent>
            def convertedAvcEvent = CloudEventUtils.mapData(record.value(), PojoCloudEventDataMapper.from(objectMapper, AvcEventV1.class)).getValue()
        and: 'we have correct headers forwarded where correlation id matches'
            record.headers().forEach(header -> {
                if (header.key().equals('ce_correlationid')) {
                    assert header.value() == 'cmhandle1'.getBytes()
                }
            })
        and: 'event id differs(as per requirement) between consumed and forwarded'
            record.headers().forEach(header -> {
                if (header.key().equals('ce_id')) {
                    assert header.value() != 'sample-eventid'.getBytes()
                }
            })
        and: 'the event payload still matches'
            assert testEventData == convertedAvcEvent
    }

}