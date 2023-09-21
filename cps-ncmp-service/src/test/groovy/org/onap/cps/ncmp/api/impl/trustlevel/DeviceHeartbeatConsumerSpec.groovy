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
import com.hazelcast.map.IMap
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.ncmp.events.trustlevel.DeviceTrustLevel
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class DeviceHeartbeatConsumerSpec extends Specification {

    def mockTrustLevelPerCmHandle = Mock(IMap<String, TrustLevel>)
    def objectMapper = new ObjectMapper()

    def objectUnderTest = new DeviceHeartbeatConsumer(mockTrustLevelPerCmHandle)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Operations to be done in an empty trust level per cm handle map for #scenario'() {
        given: 'an event with trustlevel as #trustLevel'
            def jsonData = TestUtils.getResourceFileContent('deviceTrustLevel.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, DeviceTrustLevel.class)
            testEventSent.getData().setTrustLevel(trustLevel)
            def incomingEvent = testCloudEvent(testEventSent)
        and: 'transformed as a kafka record'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('test-device-heartbeat', 0, 0, 'cmhandle1', incomingEvent)
            consumerRecord.headers().add('ce_id', objectMapper.writeValueAsBytes('cmhandle1'))
        when: 'the event is consumed'
            objectUnderTest.heartbeatListener(consumerRecord)
        then: 'cmhandles are stored'
            trustLevelPerCmHandleInvocationForAdd * mockTrustLevelPerCmHandle.put(_,_)
        where: 'below scenarios are applicable'
            scenario         | trustLevel       || trustLevelPerCmHandleInvocationForAdd
            'None trust'     | 'NONE'           || 1
            'Complete trust' | 'COMPLETE'       || 1
    }

    def 'Invalid trust'() {
        when: 'we provide an invalid trust in the event'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('test-device-heartbeat', 0, 0, 'cmhandle1', testCloudEvent(null))
            consumerRecord.headers().add('ce_id', objectMapper.writeValueAsBytes('cmhandle1'))
            objectUnderTest.heartbeatListener(consumerRecord)
        then: 'no interaction with the trust level per cm handle map'
            0 * mockTrustLevelPerCmHandle.put(_)
        and: 'control flow returns without any exception'
            noExceptionThrown()
    }

    def testCloudEvent(trustLevel) {
        return CloudEventBuilder.v1().withData(objectMapper.writeValueAsBytes(trustLevel))
            .withId("cmhandle1")
            .withSource(URI.create('DMI'))
            .withDataSchema(URI.create('test'))
            .withType('org.onap.cm.events.trustlevel-notification')
            .build()
    }

}
