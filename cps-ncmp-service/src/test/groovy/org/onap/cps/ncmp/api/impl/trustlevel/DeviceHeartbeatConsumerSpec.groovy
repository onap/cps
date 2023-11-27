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

package org.onap.cps.ncmp.api.impl.trustlevel

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.ncmp.events.trustlevel.DeviceTrustLevel
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class DeviceHeartbeatConsumerSpec extends Specification {

    def mockTrustLevelManager = Mock(TrustLevelManager)

    def objectUnderTest = new DeviceHeartbeatConsumer(mockTrustLevelManager)
    def objectMapper = new ObjectMapper()

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def static trustLevelString = '{"data":{"trustLevel": "COMPLETE"}}'

    def 'Consume a trustlevel event'() {
        given: 'an event from dmi with trust level complete'
            def payload = jsonObjectMapper.convertJsonString(trustLevelString, DeviceTrustLevel.class)
            def eventFromDmi = createTrustLevelEvent(payload)
        and: 'transformed to a consumer record with a cloud event id (ce_id)'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('test-device-heartbeat', 0, 0, 'sample-message-key', eventFromDmi)
            consumerRecord.headers().add('ce_id', objectMapper.writeValueAsBytes('cmhandle1'))
        when: 'the event is consumed'
            objectUnderTest.heartbeatListener(consumerRecord)
        then: 'cm handles are stored with correct trust level'
            1 * mockTrustLevelManager.handleUpdateOfTrustLevels('"cmhandle1"', 'COMPLETE')
    }

    def createTrustLevelEvent(eventPayload) {
        return CloudEventBuilder.v1().withData(objectMapper.writeValueAsBytes(eventPayload))
            .withId("cmhandle1")
            .withSource(URI.create('DMI'))
            .withDataSchema(URI.create('test'))
            .withType('org.onap.cps.ncmp.events.trustlevel.DeviceTrustLevel')
            .build()
    }

}
