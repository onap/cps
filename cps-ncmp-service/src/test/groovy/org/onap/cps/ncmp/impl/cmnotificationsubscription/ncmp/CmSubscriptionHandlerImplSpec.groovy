/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2024 Nordix Foundation.
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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.impl.cmnotificationsubscription.cache.DmiCacheHandler
import org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi.DmiCmSubscriptionDetailsPerDmiMapper
import org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi.DmiInEventMapper
import org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi.DmiInEventProducer
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate
import org.onap.cps.ncmp.impl.cmnotificationsubscription.utils.CmSubscriptionPersistenceService
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.client_to_ncmp.NcmpInEvent
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_client.NcmpOutEvent
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_dmi.DmiInEvent
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.api.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus.ACCEPTED
import static org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus.PENDING

class CmSubscriptionHandlerImplSpec extends Specification {

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockCmSubscriptionPersistenceService = Mock(CmSubscriptionPersistenceService)
    def mockCmSubscriptionComparator = Mock(CmSubscriptionComparator)
    def mockNcmpOutEventMapper = Mock(NcmpOutEventMapper)
    def mockDmiInEventMapper = Mock(DmiInEventMapper)
    def dmiCmSubscriptionDetailsPerDmiMapper = new DmiCmSubscriptionDetailsPerDmiMapper()
    def mockNcmpOutEventProducer = Mock(NcmpOutEventProducer)
    def mockDmiInEventProducer = Mock(DmiInEventProducer)
    def mockDmiCacheHandler = Mock(DmiCacheHandler)
    def mockInventoryPersistence = Mock(InventoryPersistence)

    def objectUnderTest = new CmSubscriptionHandlerImpl(mockCmSubscriptionPersistenceService,
        mockCmSubscriptionComparator, mockNcmpOutEventMapper, mockDmiInEventMapper, dmiCmSubscriptionDetailsPerDmiMapper,
        mockNcmpOutEventProducer, mockDmiInEventProducer, mockDmiCacheHandler, mockInventoryPersistence)

    def testDmiSubscriptionsPerDmi = ["dmi-1": new DmiCmSubscriptionDetails([], PENDING)]

    def 'Consume valid and unique CmNotificationSubscriptionNcmpInEvent create message'() {
        given: 'a cmNotificationSubscriptionNcmp in event with unique subscription id'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
            def testEventConsumed = jsonObjectMapper.convertJsonString(jsonData, NcmpInEvent.class)
            def testListOfDeltaPredicates = [new DmiCmSubscriptionPredicate(['ch1'].toSet(), PASSTHROUGH_OPERATIONAL, ['/a/b'].toSet())]
            mockCmSubscriptionPersistenceService.isUniqueSubscriptionId("test-id") >> true
        and: 'relevant details is extracted from the event'
            def subscriptionId = testEventConsumed.getData().getSubscriptionId()
            def predicates = testEventConsumed.getData().getPredicates()
        and: 'the cache handler returns for relevant subscription id'
            1 * mockDmiCacheHandler.get("test-id") >> testDmiSubscriptionsPerDmi
        and: 'the delta predicates is returned'
            1 * mockCmSubscriptionComparator.getNewDmiSubscriptionPredicates(_) >> testListOfDeltaPredicates
        and: 'the DMI in event mapper returns cm notification subscription event'
            def testDmiInEvent = new DmiInEvent()
            1 * mockDmiInEventMapper.toDmiInEvent(testListOfDeltaPredicates) >> testDmiInEvent
        when: 'the valid and unique event is consumed'
            objectUnderTest.processSubscriptionCreateRequest(subscriptionId, predicates)
        then: 'the subscription cache handler is called once'
            1 * mockDmiCacheHandler.add('test-id', _)
        and: 'the events handler method to publish DMI event is called correct number of times with the correct parameters'
            testDmiSubscriptionsPerDmi.size() * mockDmiInEventProducer.publishDmiInEvent(
                "test-id", "dmi-1", "subscriptionCreateRequest", testDmiInEvent)
        and: 'we schedule to send the response after configured time from the cache'
            1 * mockNcmpOutEventProducer.publishNcmpOutEvent('test-id', 'subscriptionCreateResponse', null, true)
    }

    def 'Consume valid and Overlapping Cm Notification Subscription NcmpIn Event'() {
        given: 'a cmNotificationSubscriptionNcmp in event with unique subscription id'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
            def testEventConsumed = jsonObjectMapper.convertJsonString(jsonData, NcmpInEvent.class)
            def noDeltaPredicates = []
            mockCmSubscriptionPersistenceService.isUniqueSubscriptionId("test-id") >> true
        and: 'the cache handler returns for relevant subscription id'
            1 * mockDmiCacheHandler.get('test-id') >> testDmiSubscriptionsPerDmi
        and: 'the delta predicates is returned'
            1 * mockCmSubscriptionComparator.getNewDmiSubscriptionPredicates(_) >> noDeltaPredicates
        when: 'the valid and unique event is consumed'
            objectUnderTest.processSubscriptionCreateRequest('test-id', noDeltaPredicates)
        then: 'the subscription cache handler is called once'
            1 * mockDmiCacheHandler.add('test-id', _)
        and: 'the subscription details are updated in the cache'
            1 * mockDmiCacheHandler.updateDmiSubscriptionStatus('test-id', _, ACCEPTED)
        and: 'we schedule to send the response after configured time from the cache'
            1 * mockNcmpOutEventProducer.publishNcmpOutEvent('test-id', 'subscriptionCreateResponse', null, true)
    }

    def 'Consume valid and but non-unique CmNotificationSubscription create message'() {
        given: 'a cmNotificationSubscriptionNcmp in event'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
            def testEventConsumed = jsonObjectMapper.convertJsonString(jsonData, NcmpInEvent.class)
            mockCmSubscriptionPersistenceService.isUniqueSubscriptionId('test-id') >> false
        and: 'relevant details is extracted from the event'
            def subscriptionId = testEventConsumed.getData().getSubscriptionId()
            def predicates = testEventConsumed.getData().getPredicates()
        and: 'the NCMP out in event mapper returns an event for rejected request'
            def testNcmpOutEvent = new NcmpOutEvent()
            1 * mockNcmpOutEventMapper.toNcmpOutEventForRejectedRequest(
                "test-id", _) >> testNcmpOutEvent
        when: 'the valid but non-unique event is consumed'
            objectUnderTest.processSubscriptionCreateRequest(subscriptionId, predicates)
        then: 'the events handler method to publish DMI event is never called'
            0 * mockDmiInEventProducer.publishDmiInEvent(_, _, _, _)
        and: 'the events handler method to publish NCMP out event is called once'
            1 * mockNcmpOutEventProducer.publishNcmpOutEvent('test-id', 'subscriptionCreateResponse', testNcmpOutEvent, false)
    }

    def 'Consume valid CmNotificationSubscriptionNcmpInEvent delete message'() {
        given: 'a test subscription id'
            def subscriptionId = 'test-id'
        and: 'the persistence service returns datanodes'
            1 * mockCmSubscriptionPersistenceService.getAllNodesForSubscriptionId(subscriptionId) >>
                [new DataNode(xpath: "/datastores/datastore[@name='ncmp-datastore:passthrough-running']/cm-handles/cm-handle[@id='ch-1']/filters/filter[@xpath='x/y']", leaves: ['xpath': 'x/y', 'subscriptionIds': ['test-id']]),
                new DataNode(xpath: "/datastores/datastore[@name='ncmp-datastore:passthrough-running']/cm-handles/cm-handle[@id='ch-2']/filters/filter[@xpath='y/z']", leaves: ['xpath': 'y/z', 'subscriptionIds': ['test-id']])]
        and: 'the inventory persistence returns yang model cm handles'
            1 * mockInventoryPersistence.getYangModelCmHandle('ch-1') >> new YangModelCmHandle(dmiServiceName: 'dmi-1')
            1 * mockInventoryPersistence.getYangModelCmHandle('ch-2') >> new YangModelCmHandle(dmiServiceName: 'dmi-2')
        when: 'the subscription delete request is processed'
            objectUnderTest.processSubscriptionDeleteRequest(subscriptionId)
        then: 'the method to publish a dmi event is called with correct parameters'
            1 * mockDmiInEventProducer.publishDmiInEvent(subscriptionId,'dmi-1','subscriptionDeleteRequest',_)
            1 * mockDmiInEventProducer.publishDmiInEvent(subscriptionId,'dmi-2','subscriptionDeleteRequest',_)
        and: 'the method to publish nmcp out event is called with correct parameters'
            1 * mockNcmpOutEventProducer.publishNcmpOutEvent(subscriptionId, 'subscriptionDeleteResponse', null, true)
    }

    def 'Delete a subscriber for fully overlapping subscriptions'() {
        given: 'a test subscription id'
            def subscriptionId = 'test-id'
        and: 'the persistence service returns datanodes with multiple subscribers'
            1 * mockCmSubscriptionPersistenceService.getAllNodesForSubscriptionId(subscriptionId) >>
                [new DataNode(xpath: "/datastores/datastore[@name='ncmp-datastore:passthrough-running']/cm-handles/cm-handle[@id='ch-1']/filters/filter[@xpath='x/y']", leaves: ['xpath': 'x/y', 'subscriptionIds': ['test-id','other-id']]),
                 new DataNode(xpath: "/datastores/datastore[@name='ncmp-datastore:passthrough-running']/cm-handles/cm-handle[@id='ch-2']/filters/filter[@xpath='y/z']", leaves: ['xpath': 'y/z', 'subscriptionIds': ['test-id','other-id']])]
        and: 'the inventory persistence returns yang model cm handles'
            1 * mockInventoryPersistence.getYangModelCmHandle('ch-1') >> new YangModelCmHandle(dmiServiceName: 'dmi-1')
            1 * mockInventoryPersistence.getYangModelCmHandle('ch-2') >> new YangModelCmHandle(dmiServiceName: 'dmi-2')
        and: 'the cache handler returns the relevant maps whenever called'
            2 * mockDmiCacheHandler.get(subscriptionId) >> ['dmi-1':[:],'dmi-2':[:]]
        when: 'the subscription delete request is processed'
            objectUnderTest.processSubscriptionDeleteRequest(subscriptionId)
        then: 'the method to publish a dmi event is never called'
            0 * mockDmiInEventProducer.publishDmiInEvent(_,_,_,_)
        and: 'the cache handler is called to remove subscriber from database per dmi'
            1 * mockDmiCacheHandler.removeFromDatabase('test-id', 'dmi-1')
            1 * mockDmiCacheHandler.removeFromDatabase('test-id', 'dmi-2')
        and: 'the method to publish nmcp out event is called with correct parameters'
            1 * mockNcmpOutEventProducer.publishNcmpOutEvent(subscriptionId, 'subscriptionDeleteResponse', null, false)
    }
}
