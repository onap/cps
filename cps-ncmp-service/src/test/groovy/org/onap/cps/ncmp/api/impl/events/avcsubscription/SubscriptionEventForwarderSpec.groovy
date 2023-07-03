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
import io.cloudevents.CloudEvent
import io.cloudevents.core.CloudEventUtils
import io.cloudevents.core.data.PojoCloudEventData
import io.cloudevents.jackson.PojoCloudEventDataMapper
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent.TargetCmHandle
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.avcsubscription1_0_0.client_to_ncmp.SubscriptionEvent
import org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_dmi.CmHandle;
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.exceptions.OperationNotYetSupportedException
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.util.concurrent.BlockingVariable

import java.util.concurrent.TimeUnit

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper, SubscriptionEventForwarder])
class SubscriptionEventForwarderSpec extends MessagingBaseSpec {

    @Autowired
    SubscriptionEventForwarder objectUnderTest

    @SpringBean
    InventoryPersistence mockInventoryPersistence = Mock(InventoryPersistence)
    @SpringBean
    EventsPublisher<CloudEvent> mockSubscriptionEventPublisher = Mock(EventsPublisher<CloudEvent>)
    @SpringBean
    IMap<String, Set<String>> mockForwardedSubscriptionEventCache = Mock(IMap<String, Set<String>>)
    @SpringBean
    SubscriptionEventResponseOutcome mockSubscriptionEventResponseOutcome = Mock(SubscriptionEventResponseOutcome)
    @SpringBean
    SubscriptionPersistence mockSubscriptionPersistence = Mock(SubscriptionPersistence)
    @SpringBean
    SubscriptionEventMapper subscriptionEventMapper = Mappers.getMapper(SubscriptionEventMapper)
    @SpringBean
    ClientSubscriptionEventMapper clientSubscriptionEventMapper = Mappers.getMapper(ClientSubscriptionEventMapper)
    @Autowired
    JsonObjectMapper jsonObjectMapper

    def objectMapper = new ObjectMapper()

    def 'Forward valid CM create subscription and simulate timeout'() {
        given: 'an event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionCreationEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEvent.class)
        and: 'the some of the cm handles will be accepted and some of rejected'
            def cmHandlesToBeSavedInDb = [new TargetCmHandle('CMHandle1', SubscriptionStatus.ACCEPTED),
                                     new TargetCmHandle('CMHandle2',SubscriptionStatus.ACCEPTED),
                                     new TargetCmHandle('CMHandle3',SubscriptionStatus.REJECTED)]
        and: 'a yang model subscription event will be saved into the db'
            def yangModelSubscriptionEventWithAcceptedAndRejectedCmHandles = subscriptionEventMapper.toYangModelSubscriptionEvent(testEventSent)
            yangModelSubscriptionEventWithAcceptedAndRejectedCmHandles.getPredicates().setTargetCmHandles(cmHandlesToBeSavedInDb)
        and: 'the InventoryPersistence returns private properties for the supplied CM Handles'
            1 * mockInventoryPersistence.getYangModelCmHandles(["CMHandle1", "CMHandle2", "CMHandle3"]) >> [
                createYangModelCmHandleWithDmiProperty(1, 1,"shape","circle"),
                createYangModelCmHandleWithDmiProperty(2, 1,"shape","square")
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
            1 * mockForwardedSubscriptionEventCache.put("SCO-9989752cm-subscription-001", ["DMIName1"] as Set, 600, TimeUnit.SECONDS)
        and: 'the event is forwarded twice with the CMHandle private properties and provides a valid listenable future'
            1 * mockSubscriptionEventPublisher.publishCloudEvent("ncmp-dmi-cm-avc-subscription-DMIName1", "SCO-9989752-cm-subscription-001-DMIName1",
                cloudEvent -> {
                    def targets = toSubscriptionEvent(cloudEvent).getData().getPredicates().getTargets()
                    def cmHandle2 = createCmHandle('CMHandle2', ['shape':'square'] as Map)
                    def cmHandle1 = createCmHandle('CMHandle1', ['shape':'circle'] as Map)
                    targets == [cmHandle2, cmHandle1]
                }
            )
        and: 'the persistence service save the yang model subscription event'
            1 * mockSubscriptionPersistence.saveSubscriptionEvent(yangModelSubscriptionEventWithAcceptedAndRejectedCmHandles)
        and: 'a separate thread has been created where the map is polled'
            1 * mockForwardedSubscriptionEventCache.containsKey("SCO-9989752cm-subscription-001") >> true
            1 * mockSubscriptionEventResponseOutcome.sendResponse(*_)
        and: 'the subscription id is removed from the event cache map returning the asynchronous blocking variable'
            1 * mockForwardedSubscriptionEventCache.remove("SCO-9989752cm-subscription-001") >> {block.set(_)}
    }

    def 'Forward CM create subscription where target CM Handles are #scenario'() {
        given: 'an event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionCreationEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEvent.class)
        and: 'the target CMHandles are set to #scenario'
            testEventSent.getData().getPredicates().setTargets(invalidTargets)
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

    def 'Forward valid CM create subscription where targets are not associated to any existing CMHandles'() {
        given: 'an event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionCreationEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEvent.class)
        and: 'the cm handles will be rejected'
            def rejectedCmHandles = [new TargetCmHandle('CMHandle1', SubscriptionStatus.REJECTED),
                                     new TargetCmHandle('CMHandle2',SubscriptionStatus.REJECTED),
                                     new TargetCmHandle('CMHandle3',SubscriptionStatus.REJECTED)]
        and: 'a yang model subscription event will be saved into the db with rejected cm handles'
            def yangModelSubscriptionEventWithRejectedCmHandles = subscriptionEventMapper.toYangModelSubscriptionEvent(testEventSent)
            yangModelSubscriptionEventWithRejectedCmHandles.getPredicates().setTargetCmHandles(rejectedCmHandles)
        and: 'the InventoryPersistence returns no private properties for the supplied CM Handles'
            1 * mockInventoryPersistence.getYangModelCmHandles(["CMHandle1", "CMHandle2", "CMHandle3"]) >> []
        and: 'the thread creation delay is reduced to 2 seconds for testing'
            objectUnderTest.dmiResponseTimeoutInMs = 2000
        and: 'a Blocking Variable is used for the Asynchronous call with a timeout of 5 seconds'
            def block = new BlockingVariable<Object>(5)
        when: 'the valid event is forwarded'
            objectUnderTest.forwardCreateSubscriptionEvent(testEventSent)
        then: 'the event is not added to the forwarded subscription event cache'
            0 * mockForwardedSubscriptionEventCache.put("SCO-9989752cm-subscription-001", ["DMIName1", "DMIName2"] as Set)
        and: 'the event is not being forwarded with the CMHandle private properties and does not provides a valid listenable future'
            0 * mockSubscriptionEventPublisher.publishCloudEvent("ncmp-dmi-cm-avc-subscription-DMIName1", "SCO-9989752-cm-subscription-001-DMIName1",
                cloudEvent -> {
                    def targets = toSubscriptionEvent(cloudEvent).getData().getPredicates().getTargets()
                    def cmHandle2 = createCmHandle('CMHandle2', ['shape':'square'] as Map)
                    def cmHandle1 = createCmHandle('CMHandle1', ['shape':'circle'] as Map)
                    targets == [cmHandle2, cmHandle1]
                }
            )
            0 * mockSubscriptionEventPublisher.publishCloudEvent("ncmp-dmi-cm-avc-subscription-DMIName2", "SCO-9989752-cm-subscription-001-DMIName2",
                cloudEvent -> {
                    def targets = toSubscriptionEvent(cloudEvent).getData().getPredicates().getTargets()
                    def cmHandle3 = createCmHandle('CMHandle3', ['shape':'triangle'] as Map)
                    targets == [cmHandle3]
                }
            )
        and: 'a separate thread has been created where the map is polled'
            0 * mockForwardedSubscriptionEventCache.containsKey("SCO-9989752cm-subscription-001") >> true
            0 * mockForwardedSubscriptionEventCache.get(_)
        and: 'the subscription id is removed from the event cache map returning the asynchronous blocking variable'
            0 * mockForwardedSubscriptionEventCache.remove("SCO-9989752cm-subscription-001") >> {block.set(_)}
        and: 'the persistence service save target cm handles of the yang model subscription event as rejected '
            1 * mockSubscriptionPersistence.saveSubscriptionEvent(yangModelSubscriptionEventWithRejectedCmHandles)
        and: 'subscription outcome has been sent'
            1 * mockSubscriptionEventResponseOutcome.sendResponse('SCO-9989752', 'cm-subscription-001')
    }

    static def createYangModelCmHandleWithDmiProperty(id, dmiId,propertyName, propertyValue) {
        return new YangModelCmHandle(id:"CMHandle" + id, dmiDataServiceName: "DMIName" + dmiId, dmiProperties: [new YangModelCmHandle.Property(propertyName,propertyValue)])
    }

    static def createCmHandle(id, additionalProperties) {
        def cmHandle = new CmHandle();
        cmHandle.setId(id)
        cmHandle.setAdditionalProperties(additionalProperties)
        return cmHandle
    }

    def toSubscriptionEvent(cloudEvent) {
        final PojoCloudEventData<org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_dmi.SubscriptionEvent> deserializedCloudEvent = CloudEventUtils
            .mapData(cloudEvent, PojoCloudEventDataMapper.from(objectMapper,
                org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_dmi.SubscriptionEvent.class));
        if (deserializedCloudEvent == null) {
            return null;
        } else {
            return deserializedCloudEvent.getValue();
        }
    }

}
