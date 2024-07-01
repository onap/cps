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

package org.onap.cps.ncmp.impl.cmsubscription.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.data.models.DatastoreType
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.CmNotificationSubscriptionNcmpInEvent
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.ncmp_to_dmi.CmNotificationSubscriptionDmiInEvent
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent
import org.onap.cps.ncmp.impl.cmsubscription.CmNotificationSubscriptionEventsFacade
import org.onap.cps.ncmp.impl.cmsubscription.CmNotificationSubscriptionMappersFacade
import org.onap.cps.ncmp.impl.cmsubscription.cache.DmiCacheHandler
import org.onap.cps.ncmp.impl.cmsubscription.models.CmNotificationSubscriptionStatus
import org.onap.cps.ncmp.impl.cmsubscription.models.DmiCmNotificationSubscriptionDetails
import org.onap.cps.ncmp.impl.cmsubscription.models.DmiCmNotificationSubscriptionPredicate
import org.onap.cps.ncmp.impl.cmsubscription.ncmp.CmNotificationSubscriptionComparator
import org.onap.cps.ncmp.impl.cmsubscription.utils.CmNotificationSubscriptionPersistenceService
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class CmNotificationSubscriptionHandlerImplSpec extends Specification{

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockCmNotificationSubscriptionPersistenceService = Mock(CmNotificationSubscriptionPersistenceService);
    def mockCmNotificationSubscriptionDelta = Mock(CmNotificationSubscriptionComparator);
    def mockCmNotificationSubscriptionMappersHandler = Mock(CmNotificationSubscriptionMappersFacade);
    def mockCmNotificationSubscriptionEventsHandler = Mock(CmNotificationSubscriptionEventsFacade);
    def mockDmiCmNotificationSubscriptionCacheHandler = Mock(DmiCacheHandler);

    def objectUnderTest = new CmNotificationSubscriptionHandlerImpl(mockCmNotificationSubscriptionPersistenceService,
        mockCmNotificationSubscriptionDelta, mockCmNotificationSubscriptionMappersHandler,
        mockCmNotificationSubscriptionEventsHandler, mockDmiCmNotificationSubscriptionCacheHandler)

    def testSubscriptionDetailsMap = ["dmi-1":new DmiCmNotificationSubscriptionDetails([], CmNotificationSubscriptionStatus.PENDING)]

    def 'Consume valid and unique CmNotificationSubscriptionNcmpInEvent create message'() {
        given: 'a cmNotificationSubscriptionNcmp in event with unique subscription id'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
            def testEventConsumed = jsonObjectMapper.convertJsonString(jsonData, CmNotificationSubscriptionNcmpInEvent.class)
            def testListOfDeltaPredicates = [new DmiCmNotificationSubscriptionPredicate(['ch1'].toSet(), DatastoreType.PASSTHROUGH_OPERATIONAL, ['/a/b'].toSet())]
            mockCmNotificationSubscriptionPersistenceService.isUniqueSubscriptionId("test-id") >> true
        and: 'relevant details is extracted from the event'
            def subscriptionId = testEventConsumed.getData().getSubscriptionId()
            def predicates = testEventConsumed.getData().getPredicates()
        and: 'the cache handler returns for relevant subscription id'
            1 * mockDmiCmNotificationSubscriptionCacheHandler.get("test-id") >> testSubscriptionDetailsMap
        and: 'the delta predicates is returned'
            1 * mockCmNotificationSubscriptionDelta.getNewDmiCmNotificationSubscriptionPredicates(_) >> testListOfDeltaPredicates
        and: 'the DMI in event mapper returns cm notification subscription event'
            def testDmiInEvent = new CmNotificationSubscriptionDmiInEvent()
            1 *  mockCmNotificationSubscriptionMappersHandler
                .toCmNotificationSubscriptionDmiInEvent(testListOfDeltaPredicates) >> testDmiInEvent
        when: 'the valid and unique event is consumed'
            objectUnderTest.processSubscriptionCreateRequest(subscriptionId, predicates)
        then: 'the subscription cache handler is called once'
            1 * mockDmiCmNotificationSubscriptionCacheHandler.add('test-id',_)
        and: 'the events handler method to publish DMI event is called correct number of times with the correct parameters'
            testSubscriptionDetailsMap.size() * mockCmNotificationSubscriptionEventsHandler.publishCmNotificationSubscriptionDmiInEvent(
                "test-id", "dmi-1", "subscriptionCreateRequest", testDmiInEvent)
        and: 'we schedule to send the response after configured time from the cache'
            1 * mockCmNotificationSubscriptionEventsHandler.publishCmNotificationSubscriptionNcmpOutEvent(
                "test-id", "subscriptionCreateResponse", null, true)
    }

    def 'Consume valid and Overlapping Cm Notification Subscription NcmpIn Event'() {
        given: 'a cmNotificationSubscriptionNcmp in event with unique subscription id'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
            def testEventConsumed = jsonObjectMapper.convertJsonString(jsonData, CmNotificationSubscriptionNcmpInEvent.class)
            def noDeltaPredicates = []
            mockCmNotificationSubscriptionPersistenceService.isUniqueSubscriptionId("test-id") >> true
        and: 'the cache handler returns for relevant subscription id'
            1 * mockDmiCmNotificationSubscriptionCacheHandler.get("test-id") >> testSubscriptionDetailsMap
        and: 'the delta predicates is returned'
            1 * mockCmNotificationSubscriptionDelta.getNewDmiCmNotificationSubscriptionPredicates(_) >> noDeltaPredicates
        when: 'the valid and unique event is consumed'
            objectUnderTest.processSubscriptionCreateRequest('test-id', noDeltaPredicates)
        then: 'the subscription cache handler is called once'
            1 * mockDmiCmNotificationSubscriptionCacheHandler.add('test-id', _)
        and: 'the subscription details are updated in the cache'
            1 * mockDmiCmNotificationSubscriptionCacheHandler.updateDmiCmNotificationSubscriptionStatusPerDmi('test-id', _, CmNotificationSubscriptionStatus.ACCEPTED)
        and: 'we schedule to send the response after configured time from the cache'
            1 * mockCmNotificationSubscriptionEventsHandler.publishCmNotificationSubscriptionNcmpOutEvent(
                "test-id", "subscriptionCreateResponse", null, true)
    }

    def 'Consume valid and but non-unique CmNotificationSubscription create message'() {
        given: 'a cmNotificationSubscriptionNcmp in event'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
            def testEventConsumed = jsonObjectMapper.convertJsonString(jsonData, CmNotificationSubscriptionNcmpInEvent.class)
            mockCmNotificationSubscriptionPersistenceService.isUniqueSubscriptionId('test-id') >> false
        and: 'relevant details is extracted from the event'
            def subscriptionId = testEventConsumed.getData().getSubscriptionId()
            def predicates = testEventConsumed.getData().getPredicates()
        and: 'the NCMP out in event mapper returns an event for rejected request'
            def testNcmpOutEvent = new CmNotificationSubscriptionNcmpOutEvent()
            1 * mockCmNotificationSubscriptionMappersHandler.toCmNotificationSubscriptionNcmpOutEventForRejectedRequest(
                "test-id",_) >> testNcmpOutEvent
        when: 'the valid but non-unique event is consumed'
            objectUnderTest.processSubscriptionCreateRequest(subscriptionId, predicates)
        then: 'the events handler method to publish DMI event is never called'
            0 * mockCmNotificationSubscriptionEventsHandler.publishCmNotificationSubscriptionDmiInEvent(_,_,_,_)
        and: 'the events handler method to publish NCMP out event is called once'
            1 * mockCmNotificationSubscriptionEventsHandler.publishCmNotificationSubscriptionNcmpOutEvent(
                'test-id', 'subscriptionCreateResponse', testNcmpOutEvent, false)
    }

    def 'Consume valid CmNotificationSubscriptionNcmpInEvent delete message'() {
        given: 'a cmNotificationSubscriptionNcmp in event for delete'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
            def testEventConsumed = jsonObjectMapper.convertJsonString(jsonData, CmNotificationSubscriptionNcmpInEvent.class)
        and: 'relevant details is extracted from the event'
            def subscriptionId = testEventConsumed.getData().getSubscriptionId()
            def predicates = testEventConsumed.getData().getPredicates()
        and: 'the cache handler returns for relevant subscription id'
            1 * mockDmiCmNotificationSubscriptionCacheHandler.get('test-id') >> testSubscriptionDetailsMap
        when: 'the valid and unique event is consumed'
            objectUnderTest.processSubscriptionDeleteRequest(subscriptionId, predicates)
        then: 'the subscription cache handler is called once'
            1 * mockDmiCmNotificationSubscriptionCacheHandler.add('test-id', predicates)
        and: 'the mapper handler to get DMI in event is called once'
            1 * mockCmNotificationSubscriptionMappersHandler.toCmNotificationSubscriptionDmiInEvent(_)
        and: 'the events handler method to publish DMI event is called correct number of times with the correct parameters'
            testSubscriptionDetailsMap.size() * mockCmNotificationSubscriptionEventsHandler.publishCmNotificationSubscriptionDmiInEvent(
                'test-id', 'dmi-1', 'subscriptionDeleteRequest', _)
        and: 'we schedule to send the response after configured time from the cache'
            1 * mockCmNotificationSubscriptionEventsHandler.publishCmNotificationSubscriptionNcmpOutEvent(
                'test-id', 'subscriptionDeleteResponse', null, true)
    }
}
