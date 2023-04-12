/*
 *  ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.impl.events.avc

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.event.model.SubscriptionEvent
import org.onap.cps.ncmp.utils.KafkaDemoProducerConfig
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Specification

@SpringBootTest(classes = [KafkaDemoProducerConfig, ObjectMapper, JsonObjectMapper])
@DirtiesContext
class SubscriptionCreateProducerDemo extends Specification {

    @Value('${app.ncmp.avc.subscription-topic}')
    String subscriptionTopic;

    @Autowired
    KafkaTemplate<String, SubscriptionEvent> kafkaTemplate

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'produce subscription creation data event for testing'() {
        given: 'avc subscription creation event data'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionCreationEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEvent.class)
        and: 'test event is sent'
            kafkaTemplate.send(subscriptionTopic, "request-Id-98765", testEventSent);
        and: 'print json data to console'
            println(jsonData);
    }
}