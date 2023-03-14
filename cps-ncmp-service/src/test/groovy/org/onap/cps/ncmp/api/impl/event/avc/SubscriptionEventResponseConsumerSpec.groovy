/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.event.avc

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse
import org.spockframework.spring.SpringBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers
import java.time.Duration

@SpringBootTest(classes = [SubscriptionEventResponseConsumer])
@Testcontainers
@DirtiesContext
class SubscriptionEventResponseConsumerSpec extends MessagingBaseSpec {

    def kafkaConsumer = new KafkaConsumer<>(consumerConfigProperties('ncmp-group'))
    def testTopic = 'cm-avc-subscription-response'

    def cmHandleIdList1 = ['cm-handle-id-1','cm-handle-id-2']
    def cmHandleIdList2 = ['cm-handle-id-3','cm-handle-id-4']
    def demoResponseAccepted = new SubscriptionEventResponse(clientId: 'client-1', status: 'accepted', cmHandleIds: cmHandleIdList1)
    def demoResponseRejected = new SubscriptionEventResponse(clientId: 'client-2', status: 'rejected', cmHandleIds: cmHandleIdList2)

    @SpringBean
    SubscriptionEventResponseConsumer objectUnderTest = new SubscriptionEventResponseConsumer(kafkaTemplate)

    def 'Send partially completed outcome message successfully.'() {
        given: 'two subscription event responses with different values'
            objectUnderTest.consumedSubscriptionEventResponses.push(demoResponseAccepted)
            objectUnderTest.consumedSubscriptionEventResponses.push(demoResponseRejected)
            objectUnderTest.subscriptionOutcomeEventTopic = testTopic
        and: 'consumer has a subscription'
            kafkaConsumer.subscribe([testTopic] as List<String>)
        when: 'a subscription event outcome is published'
            objectUnderTest.publishSubscriptionEventResponse()
        and: 'topic is polled'
            def records = kafkaConsumer.poll(Duration.ofMillis(1500))
        then: 'poll returns one record'
            assert records.size() == 1
        and: 'the record value matches the expected outcome message'
            def record = records.iterator().next()
            def value = record.value()
            assert value.contains('"status":"PARTIALLY_COMPLETED"')
            assert value.contains('"acceptedCmHandleIds":["cm-handle-id-1","cm-handle-id-2"]')
            assert value.contains('"declinedCmHandleIds":["cm-handle-id-3","cm-handle-id-4"]')
    }

    def 'Consume valid message'() {
        when: 'the valid event is consumed'
            objectUnderTest.consumeSubscriptionEventResponse(demoResponseAccepted)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

}
