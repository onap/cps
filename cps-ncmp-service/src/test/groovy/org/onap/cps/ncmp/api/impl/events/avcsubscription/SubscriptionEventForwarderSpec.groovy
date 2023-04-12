/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.events.avcsubscription

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.event.model.SubscriptionEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.exceptions.OperationNotYetSupportedException
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class SubscriptionEventForwarderSpec extends MessagingBaseSpec {

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockSubscriptionEventPublisher = Mock(EventsPublisher<SubscriptionEvent>)
    def objectUnderTest = new SubscriptionEventForwarder(mockInventoryPersistence, mockSubscriptionEventPublisher)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Forward valid CM create subscription'() {
        given: 'an event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionCreationEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEvent.class)
        and: 'the InventoryPersistence returns private properties for the supplied CM Handles'
            1 * mockInventoryPersistence.getYangModelCmHandles(["CMHandle1", "CMHandle2", "CMHandle3"]) >> [
                createYangModelCmHandleWithDmiProperty(1, 1,"shape","circle"),
                createYangModelCmHandleWithDmiProperty(2, 1,"shape","square"),
                createYangModelCmHandleWithDmiProperty(3, 2,"shape","triangle")
            ]
        when: 'the valid event is forwarded'
            objectUnderTest.forwardCreateSubscriptionEvent(testEventSent)
        then: 'the event is forwarded twice with the CMHandle private properties and provides a valid listenable future'
            1 * mockSubscriptionEventPublisher.publishEvent("ncmp-dmi-cm-avc-subscription-DMIName1", "SCO-9989752-cm-subscription-001-DMIName1",
                subscriptionEvent -> {
                    Map targets = subscriptionEvent.getEvent().getPredicates().getTargets().get(0)
                    targets["CMHandle1"] == ["shape":"circle"]
                    targets["CMHandle2"] == ["shape":"square"]
                }
            )
            1 * mockSubscriptionEventPublisher.publishEvent("ncmp-dmi-cm-avc-subscription-DMIName2", "SCO-9989752-cm-subscription-001-DMIName2",
                subscriptionEvent -> {
                    Map targets = subscriptionEvent.getEvent().getPredicates().getTargets().get(0)
                    targets["CMHandle3"] == ["shape":"triangle"]
                }
            )
    }

    def 'Forward CM create subscription where target CM Handles are #scenario'() {
        given: 'an event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionCreationEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEvent.class)
        and: 'the target CMHandles are set to #scenario'
            testEventSent.getEvent().getPredicates().setTargets(invalidTargets)
        when: 'the event is forwarded'
            objectUnderTest.forwardCreateSubscriptionEvent(testEventSent)
        then: 'an operation not yet supported exception is thrown'
            thrown(OperationNotYetSupportedException)
        where:
            scenario   | invalidTargets
            'null'     | null
            'empty'    | []
            'wildcard' | ['CMHandle*']
    }

    static def createYangModelCmHandleWithDmiProperty(id, dmiId,propertyName, propertyValue) {
        return new YangModelCmHandle(id:"CMHandle" + id, dmiDataServiceName: "DMIName" + dmiId, dmiProperties: [new YangModelCmHandle.Property(propertyName,propertyValue)])
    }

}
