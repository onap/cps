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

package org.onap.cps.ncmp.api.impl.event.avc

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.utils.TestUtils
import org.spockframework.spring.SpringBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers

@SpringBootTest(classes = [SubscriptionEventConsumer])
@Testcontainers
@DirtiesContext
class AvcSubscriptionEventSpec extends MessagingBaseSpec {

    @SpringBean
    SubscriptionEventConsumer subscriptionEventConsumer = new SubscriptionEventConsumer();

    def kafkaConsumer = new KafkaConsumer<>(consumerConfigProperties('test'))

    def 'Consume valid message'() {
        given: 'consumer has a subscription'
            kafkaConsumer.subscribe(['avc-test-subscription'] as List<String>)
        and: 'an event is sent'
            def jsonData = TestUtils.getResourceFileContent('acvSubscriptionCreationEvent.json')
        when: 'the event is consumed'
            subscriptionEventConsumer.consumeSubscriptionEvent(jsonData)
        then: 'assert that object exists'
            //assert Blocking: Unsure how to correctly check logs from groovy & spock
    }

}
