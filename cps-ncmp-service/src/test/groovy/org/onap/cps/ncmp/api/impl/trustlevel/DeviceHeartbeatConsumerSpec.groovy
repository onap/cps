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
import com.hazelcast.collection.ISet
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class DeviceHeartbeatConsumerSpec extends Specification {

    def mockUntrustworthyCmHandlesSet = Mock(ISet<String>)
    def objectMapper = new ObjectMapper()

    def objectUnderTest = new DeviceHeartbeatConsumer(mockUntrustworthyCmHandlesSet)

    def 'Operations to be done in an empty untrustworthy set for #scenario'() {
        given: 'an event with trustlevel as #trustLevel'
            def incomingEvent = testCloudEvent(trustLevel)
        and: 'transformed as a kafka record'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('test-device-heartbeat', 0, 0, 'cmhandle1', incomingEvent)
            consumerRecord.headers().add('ce_id', objectMapper.writeValueAsBytes('cmhandle1'))
        when: 'the event is consumed'
            objectUnderTest.heartbeatListener(consumerRecord)
        then: 'untrustworthy cmhandles are stored'
            untrustworthyCmHandlesSetInvocationForAdd * mockUntrustworthyCmHandlesSet.add(_)
        and: 'trustworthy cmHandles will be removed from untrustworthy set'
            untrustworthyCmHandlesSetInvocationForContains * mockUntrustworthyCmHandlesSet.contains(_)

        where: 'below scenarios are applicable'
            scenario         | trustLevel          || untrustworthyCmHandlesSetInvocationForAdd | untrustworthyCmHandlesSetInvocationForContains
            'None trust'     | TrustLevel.NONE     || 1                                         | 0
            'Complete trust' | TrustLevel.COMPLETE || 0                                         | 1
    }

    def 'Invalid trust'() {
        when: 'we provide an invalid trust in the event'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('test-device-heartbeat', 0, 0, 'cmhandle1', testCloudEvent(null))
            consumerRecord.headers().add('ce_id', objectMapper.writeValueAsBytes('cmhandle1'))
            objectUnderTest.heartbeatListener(consumerRecord)
        then: 'no interaction with the untrustworthy cmhandles set'
            0 * mockUntrustworthyCmHandlesSet.add(_)
            0 * mockUntrustworthyCmHandlesSet.contains(_)
            0 * mockUntrustworthyCmHandlesSet.remove(_)
        and: 'control flow returns without any exception'
            noExceptionThrown()

    }

    def 'Remove trustworthy cmhandles from untrustworthy cmhandles set'() {
        given: 'an event with COMPLETE trustlevel'
            def incomingEvent = testCloudEvent(TrustLevel.COMPLETE)
        and: 'transformed as a kafka record'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('test-device-heartbeat', 0, 0, 'cmhandle1', incomingEvent)
            consumerRecord.headers().add('ce_id', objectMapper.writeValueAsBytes('cmhandle1'))
        and: 'untrustworthy cmhandles set contains cmhandle1'
            1 * mockUntrustworthyCmHandlesSet.contains(_) >> true
        when: 'the event is consumed'
            objectUnderTest.heartbeatListener(consumerRecord)
        then: 'cmhandle removed from untrustworthy cmhandles set'
            1 * mockUntrustworthyCmHandlesSet.remove(_) >> {
                args ->
                    {
                        args[0].equals('cmhandle1')
                    }
            }

    }

    def testCloudEvent(trustLevel) {
        return CloudEventBuilder.v1().withData(objectMapper.writeValueAsBytes(new DeviceTrustLevel(trustLevel)))
            .withId("cmhandle1")
            .withSource(URI.create('DMI'))
            .withDataSchema(URI.create('test'))
            .withType('org.onap.cm.events.trustlevel-notification')
            .build()
    }

}
