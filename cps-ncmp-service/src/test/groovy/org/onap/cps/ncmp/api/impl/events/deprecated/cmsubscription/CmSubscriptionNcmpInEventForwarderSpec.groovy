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

package org.onap.cps.ncmp.api.impl.events.deprecated.cmsubscription

import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.map.IMap
import io.cloudevents.CloudEvent
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.deprecated.subscriptions.SubscriptionPersistence
import org.onap.cps.ncmp.api.impl.deprecated.subscriptions.SubscriptionStatus
import org.onap.cps.ncmp.api.impl.utils.CmSubscriptionEventCloudMapper
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent.TargetCmHandle
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.cmsubscription1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent
import org.onap.cps.ncmp.events.cmsubscription1_0_0.ncmp_to_dmi.CmHandle
import org.onap.cps.ncmp.events.cmsubscription1_0_0.ncmp_to_dmi.CmSubscriptionDmiInEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.util.concurrent.BlockingVariable
import java.util.concurrent.TimeUnit

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper, CmSubscriptionNcmpInEventForwarder])
class CmSubscriptionNcmpInEventForwarderSpec extends MessagingBaseSpec {

    @Autowired
    CmSubscriptionNcmpInEventForwarder objectUnderTest
    @SpringBean
    InventoryPersistence mockInventoryPersistence = Mock(InventoryPersistence)
    @SpringBean
    EventsPublisher<CloudEvent> mockSubscriptionEventPublisher = Mock(EventsPublisher<CloudEvent>)
    @SpringBean
    IMap<String, Set<String>> mockForwardedSubscriptionEventCache = Mock(IMap<String, Set<String>>)
    @SpringBean
    CmSubscriptionEventCloudMapper subscriptionEventCloudMapper = new CmSubscriptionEventCloudMapper(new ObjectMapper())
    @SpringBean
    CmSubscriptionNcmpOutEventPublisher mockCmSubscriptionNcmpOutEventPublisher = Mock(CmSubscriptionNcmpOutEventPublisher)
    @SpringBean
    SubscriptionPersistence mockSubscriptionPersistence = Mock(SubscriptionPersistence)
    @SpringBean
    CmSubscriptionNcmpInEventMapper cmSubscriptionNcmpInEventMapper = Mappers.getMapper(CmSubscriptionNcmpInEventMapper)
    @SpringBean
    CmSubscriptionNcmpInEventToCmSubscriptionDmiInEventMapper cmSubscriptionNcmpInEventToCmSubscriptionDmiInEventMapper = Mappers.getMapper(CmSubscriptionNcmpInEventToCmSubscriptionDmiInEventMapper)
    @Autowired
    JsonObjectMapper jsonObjectMapper
    @Autowired
    ObjectMapper objectMapper

    def 'Forward valid CM create subscription and simulate timeout'() {
        given: 'a ncmp in event'
            def ncmpInEventJsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionNcmpInEvent.json')
            def ncmpInEventJson = jsonObjectMapper.convertJsonString(ncmpInEventJsonData, CmSubscriptionNcmpInEvent.class)
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
            objectUnderTest.forwardCreateSubscriptionEvent(ncmpInEventJson, 'subscriptionCreated')
        then: 'An asynchronous call is made to the blocking variable'
            block.get()
        then: 'the event is added to the forwarded subscription event cache'
            1 * mockForwardedSubscriptionEventCache.put("SCO-9989752cm-subscription-001", ["DMIName1","DMIName2"] as Set, 600, TimeUnit.SECONDS)
        and: 'the event is forwarded twice with the CMHandle private properties and provides a valid listenable future'
            1 * mockSubscriptionEventPublisher.publishCloudEvent("ncmp-dmi-cm-avc-subscription-DMIName1", "SCO-9989752-cm-subscription-001-DMIName1",
                cloudEvent -> {
                    def targets = toTargetEvent(cloudEvent, CmSubscriptionDmiInEvent.class).getData().getPredicates().getTargets()
                    def cmHandle2 = createCmHandle('CMHandle2', ['shape': 'square'] as Map)
                    def cmHandle1 = createCmHandle('CMHandle1', ['shape': 'circle'] as Map)
                    targets == [cmHandle2, cmHandle1]
                }
            )
            1 * mockSubscriptionEventPublisher.publishCloudEvent("ncmp-dmi-cm-avc-subscription-DMIName2", "SCO-9989752-cm-subscription-001-DMIName2",
                cloudEvent -> {
                    def targets = toTargetEvent(cloudEvent, CmSubscriptionDmiInEvent.class).getData().getPredicates().getTargets()
                    def cmHandle3 = createCmHandle('CMHandle3', ['shape':'triangle'] as Map)
                    targets == [cmHandle3]
                }
            )
        and: 'a separate thread has been created where the map is polled'
            1 * mockForwardedSubscriptionEventCache.containsKey("SCO-9989752cm-subscription-001") >> true
            1 * mockCmSubscriptionNcmpOutEventPublisher.sendResponse(*_)
        and: 'the subscription id is removed from the event cache map returning the asynchronous blocking variable'
            1 * mockForwardedSubscriptionEventCache.remove("SCO-9989752cm-subscription-001") >> { block.set(_) }
    }

    def 'Forward CM create subscription where target CM Handles are #scenario'() {
        given: 'a ncmp in event'
            def ncmpInEventJsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionNcmpInEvent.json')
            def ncmpInEventJson = jsonObjectMapper.convertJsonString(ncmpInEventJsonData, CmSubscriptionNcmpInEvent.class)
        and: 'the target CMHandles are set to #scenario'
            ncmpInEventJson.getData().getPredicates().setTargets(invalidTargets)
        when: 'the event is forwarded'
            objectUnderTest.forwardCreateSubscriptionEvent(ncmpInEventJson, 'some-event-type')
        then: 'an operation not supported exception is thrown'
            thrown(UnsupportedOperationException)
        where:
            scenario   | invalidTargets
            'null'     | null
            'empty'    | []
            'wildcard' | ['CMHandle*']
    }

    def 'Forward valid CM create subscription where targets are not associated to any existing CMHandles'() {
        given: 'a ncmp in event'
            def ncmpInEventJsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionNcmpInEvent.json')
            def ncmpInEventJson = jsonObjectMapper.convertJsonString(ncmpInEventJsonData, CmSubscriptionNcmpInEvent.class)
        and: 'the InventoryPersistence returns no private properties for the supplied CM Handles'
            1 * mockInventoryPersistence.getYangModelCmHandles(["CMHandle1", "CMHandle2", "CMHandle3"]) >> []
        and: 'some rejected cm handles'
            def rejectedCmHandles = [new TargetCmHandle('CMHandle1', SubscriptionStatus.REJECTED, 'Cm handle does not exist'),
                                     new TargetCmHandle('CMHandle2', SubscriptionStatus.REJECTED, 'Cm handle does not exist'),
                                     new TargetCmHandle('CMHandle3', SubscriptionStatus.REJECTED, 'Cm handle does not exist')]
        and: 'a yang model subscription event will be saved into the db with rejected cm handles'
            def yangModelSubscriptionEvent = cmSubscriptionNcmpInEventMapper.toYangModelSubscriptionEvent(ncmpInEventJson)
            yangModelSubscriptionEvent.getPredicates().setTargetCmHandles(rejectedCmHandles)
        and: 'the thread creation delay is reduced to 2 seconds for testing'
            objectUnderTest.dmiResponseTimeoutInMs = 2000
        and: 'a Blocking Variable is used for the Asynchronous call with a timeout of 5 seconds'
            def block = new BlockingVariable<Object>(5)
        when: 'the valid event is forwarded'
            objectUnderTest.forwardCreateSubscriptionEvent(ncmpInEventJson, 'subscriptionCreatedStatus')
        then: 'the event is not added to the forwarded subscription event cache'
            0 * mockForwardedSubscriptionEventCache.put("SCO-9989752cm-subscription-001", ["DMIName1", "DMIName2"] as Set)
        and: 'the event is not being forwarded with the CMHandle private properties and does not provides a valid listenable future'
            0 * mockSubscriptionEventPublisher.publishCloudEvent("ncmp-dmi-cm-avc-subscription-DMIName1", "SCO-9989752-cm-subscription-001-DMIName1",
                cloudEvent -> {
                    def targets = toTargetEvent(cloudEvent, CmSubscriptionDmiInEvent.class).getData().getPredicates().getTargets()
                    def cmHandle2 = createCmHandle('CMHandle2', ['shape': 'square'] as Map)
                    def cmHandle1 = createCmHandle('CMHandle1', ['shape': 'circle'] as Map)
                    targets == [cmHandle2, cmHandle1]
                }
            )
            0 * mockSubscriptionEventPublisher.publishCloudEvent("ncmp-dmi-cm-avc-subscription-DMIName2", "SCO-9989752-cm-subscription-001-DMIName2",
                cloudEvent -> {
                    def targets = toTargetEvent(cloudEvent, CmSubscriptionDmiInEvent.class).getData().getPredicates().getTargets()
                    def cmHandle3 = createCmHandle('CMHandle3', ['shape': 'triangle'] as Map)
                    targets == [cmHandle3]
                }
            )
        and: 'a separate thread has been created where the map is polled'
            0 * mockForwardedSubscriptionEventCache.containsKey("SCO-9989752cm-subscription-001") >> true
            0 * mockForwardedSubscriptionEventCache.get(_)
        and: 'the subscription id is removed from the event cache map returning the asynchronous blocking variable'
            0 * mockForwardedSubscriptionEventCache.remove("SCO-9989752cm-subscription-001") >> {block.set(_)}
        and: 'the persistence service save target cm handles of the yang model subscription event as rejected'
            1 * mockSubscriptionPersistence.saveSubscriptionEvent(yangModelSubscriptionEvent)
        and: 'subscription outcome has been sent'
            1 * mockCmSubscriptionNcmpOutEventPublisher.sendResponse(_, 'subscriptionCreatedStatus')
    }

    def 'Extract domain name from URL for #scenario'() {
        when: 'a valid dmi name is provided'
            def domainName = objectUnderTest.toValidTopicSuffix(dmiName)
        then: 'domain name is as expected with no port information'
            assert domainName == expectedDomainName
        where: ''
            scenario                               | dmiName                            || expectedDomainName
            'insecure http url with port'          | 'http://www.onap-dmi:8080/xyz=123' || 'onap-dmi'
            'insecure http url without port'       | 'http://www.onap-dmi/xyz=123'      || 'onap-dmi'
            'secure https url with port'           | 'https://127.0.0.1:8080/xyz=123'   || '127.0.0.1'
            'secure https url without port'        | 'https://127.0.0.1/xyz=123'        || '127.0.0.1'
            'servername without protocol and port' | 'dminame1'                         || 'dminame1'
            'servername without protocol'          | 'www.onap-dmi:8080/xyz=123'        || 'www.onap-dmi:8080/xyz=123'

    }

    static def createYangModelCmHandleWithDmiProperty(id, dmiId, propertyName, propertyValue) {
        return new YangModelCmHandle(id: "CMHandle" + id, dmiDataServiceName: "DMIName" + dmiId, dmiProperties: [new YangModelCmHandle.Property(propertyName, propertyValue)])
    }

    static def createCmHandle(id, additionalProperties) {
        def cmHandle = new CmHandle();
        cmHandle.setId(id)
        cmHandle.setAdditionalProperties(additionalProperties)
        return cmHandle
    }

}
