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
import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.impl.event.avc.SubscriptionOutcomeMapper
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence
import org.onap.cps.ncmp.api.impl.utils.DataNodeHelper
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.event.model.SubscriptionEvent
import org.onap.cps.ncmp.event.model.SubscriptionEventOutcome
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.exceptions.OperationNotYetSupportedException
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.util.concurrent.BlockingVariable

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper, SubscriptionEventForwarder])
class SubscriptionEventForwarderSpec extends MessagingBaseSpec {

    @Autowired
    SubscriptionEventForwarder objectUnderTest

    @SpringBean
    InventoryPersistence mockInventoryPersistence = Mock(InventoryPersistence)
    @SpringBean
    SubscriptionPersistence mockSubscriptionPersistence = Mock(SubscriptionPersistence)
    @SpringBean
    EventsPublisher<SubscriptionEvent> mockSubscriptionEventPublisher = Mock(EventsPublisher<SubscriptionEvent>)
    @SpringBean
    EventsPublisher<SubscriptionEventOutcome> mockOutcomeEventPublisher = Mock(EventsPublisher<SubscriptionEventOutcome>)
    @SpringBean
    IMap<String, Set<String>> mockForwardedSubscriptionEventCache = Mock(IMap<String, Set<String>>)
    @SpringBean
    SubscriptionOutcomeMapper mockSubscriptionOutcomeMapper = Mock(SubscriptionOutcomeMapper)
    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Forward valid CM create subscription and simulate timeout where #scenario'() {
        given: 'an event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionCreationEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEvent.class)
        and: 'a data node'
            def dataNode = new DataNodeBuilder().withDataspace('NCMP-Admin')
                .withAnchor('AVC-Subscriptions').withXpath('/subscription-registry/subscription')
                .withLeaves([status:'PENDING', cmHandleId:'CMHandle3']).build()
        and: 'the InventoryPersistence returns private properties for the supplied CM Handles'
            1 * mockInventoryPersistence.getYangModelCmHandles(["CMHandle1", "CMHandle2", "CMHandle3"]) >> [
                createYangModelCmHandleWithDmiProperty(1, 1,"shape","circle"),
                createYangModelCmHandleWithDmiProperty(2, 1,"shape","square"),
                createYangModelCmHandleWithDmiProperty(3, 2,"shape","triangle")
            ]
        and: 'the thread creation delay is reduced to 2 seconds for testing'
            objectUnderTest.dmiResponseTimeoutInMs = 2000
        and: 'a Blocking Variable is used for the Asynchronous call with a timeout of 5 seconds'
            def block = new BlockingVariable<Object>(5)
        when: 'the valid event is forwarded'
            objectUnderTest.forwardCreateSubscriptionEvent(testEventSent)
        then: 'An asynchronous call is made to the blocking variable'
            block.get()
        then: 'the event is added to the forwarded subscription event cache'
            1 * mockForwardedSubscriptionEventCache.put("SCO-9989752cm-subscription-001", ["DMIName1", "DMIName2"] as Set)
        and: 'the event is forwarded twice with the CMHandle private properties and provides a valid listenable future'
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
        and: 'a separate thread has been created where the map is polled'
            1 * mockForwardedSubscriptionEventCache.containsKey("SCO-9989752cm-subscription-001") >> true
            1 * mockForwardedSubscriptionEventCache.get(_) >> (DMINamesInMap)
            1 * mockSubscriptionPersistence.getDataNodesForSubscriptionEvent() >> [dataNode]
            DataNodeHelper.getDataNodeLeaves(_) >> [[status:'PENDING', cmHandleId:'CMHandle3'] as Map]
            DataNodeHelper.getCmHandleIdToStatus(_) >> [['PENDING', 'CMHandle3']]
            1 * mockSubscriptionOutcomeMapper.toSubscriptionEventOutcome(_) >> new SubscriptionEventOutcome()
        and: 'the subscription id is removed from the event cache map returning the asynchronous blocking variable'
            1 * mockForwardedSubscriptionEventCache.remove("SCO-9989752cm-subscription-001") >> {block.set(_)}
        where:
            scenario                                  | DMINamesInMap
            'there are dmis which have not responded' | ["DMIName1", "DMIName2"] as Set
            'all dmis have responded '                | [] as Set
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
