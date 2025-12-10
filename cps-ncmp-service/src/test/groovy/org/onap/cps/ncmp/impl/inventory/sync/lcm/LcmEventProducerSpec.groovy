/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.inventory.sync.lcm

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.onap.cps.events.EventProducer
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.springframework.kafka.KafkaException
import spock.lang.Specification

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED

class LcmEventProducerSpec extends Specification {

    def mockEventProducer = Mock(EventProducer)
    def lcmEventObjectCreator = new LcmEventObjectCreator()
    def meterRegistry = new SimpleMeterRegistry()

    def objectUnderTest = new LcmEventProducer(mockEventProducer, lcmEventObjectCreator, meterRegistry)

    def cmHandleTransitionPair = new CmHandleTransitionPair(
        new YangModelCmHandle(id: 'ch-1',  additionalProperties: [], publicProperties: []),
        new YangModelCmHandle(id: 'ch-1', compositeState: new CompositeState(cmHandleState: ADVISED), additionalProperties: [], publicProperties: [])
    )

    def 'Create and send lcm event where notifications are #scenario.'() {
        given: 'notificationsEnabled is #notificationsEnabled'
            objectUnderTest.notificationsEnabled = notificationsEnabled
        when: 'event send for (batch of) 1 cm handle transition pair (new cm handle going to READY)'
            objectUnderTest.sendLcmEventBatchAsynchronously([cmHandleTransitionPair])
        then: 'producer is called #expectedTimesMethodCalled times with correct identifiers'
            expectedTimesMethodCalled * mockEventProducer.sendLegacyEvent(_, 'ch-1', _, _) >> {
                args -> {
                    def eventHeaders = args[2]
                    assert UUID.fromString(eventHeaders.get('eventId')) != null
                    assert eventHeaders.get('eventCorrelationId') == 'ch-1'
                }
            }
        and: 'metrics are recorded with correct tags'
            def timer = meterRegistry.find('cps.ncmp.lcm.events.send').timer()
            if (notificationsEnabled) {
                assert timer.count() == 1
                assert timer.id.tags.containsAll(Tag.of('oldCmHandleState', 'N/A'), Tag.of('newCmHandleState', 'ADVISED'))
            } else {
                assert timer == null
            }
        where: 'the following values are used'
            scenario   | notificationsEnabled || expectedTimesMethodCalled
            'enabled'  | true                 || 1
            'disabled' | false                || 0
    }

    def 'Exception while sending message.'(){
        given: 'notifications are enabled'
            objectUnderTest.notificationsEnabled = true
        when: 'producer set to throw an exception'
            mockEventProducer.sendLegacyEvent(*_) >> { throw new KafkaException('sending failed')}
        and: 'attempt to send events'
            objectUnderTest.sendLcmEventBatchAsynchronously([cmHandleTransitionPair])
        then: 'the exception is just logged and not bubbled up'
            noExceptionThrown()
        and: 'metrics are not recorded'
            assert  meterRegistry.find('cps.ncmp.lcm.events.send').timer() == null
    }

}
