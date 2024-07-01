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
import org.onap.cps.ncmp.api.data.models.DatastoreType
import org.onap.cps.ncmp.impl.cmnotificationsubscription.EventsFacade
import org.onap.cps.ncmp.impl.cmnotificationsubscription.MappersFacade
import org.onap.cps.ncmp.impl.cmnotificationsubscription.cache.DmiCacheHandler
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate
import org.onap.cps.ncmp.impl.cmnotificationsubscription.utils.CmSubscriptionPersistenceService
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.client_to_ncmp.NcmpInEvent
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_client.NcmpOutEvent
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_dmi.DmiInEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class CmSubscriptionHandlerImplSpec extends Specification {

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockCmSubscriptionPersistenceService = Mock(CmSubscriptionPersistenceService);
    def mockCmSubscriptionComparator = Mock(CmSubscriptionComparator);
    def mockMappersFacade = Mock(MappersFacade);
    def mockEventsFacade = Mock(EventsFacade);
    def mockDmiCacheHandler = Mock(DmiCacheHandler);

    def objectUnderTest = new CmSubscriptionHandlerImpl(mockCmSubscriptionPersistenceService,
        mockCmSubscriptionComparator, mockMappersFacade,
        mockEventsFacade, mockDmiCacheHandler)

    def testSubscriptionDetailsMap = ["dmi-1": new DmiCmSubscriptionDetails([], CmSubscriptionStatus.PENDING)]

    def 'Consume valid and unique CmNotificationSubscriptionNcmpInEvent create message'() {
        given: 'a cmNotificationSubscriptionNcmp in event with unique subscription id'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
            def testEventConsumed = jsonObjectMapper.convertJsonString(jsonData, NcmpInEvent.class)
            def testListOfDeltaPredicates = [new DmiCmSubscriptionPredicate(['ch1'].toSet(), DatastoreType.PASSTHROUGH_OPERATIONAL, ['/a/b'].toSet())]
            mockCmSubscriptionPersistenceService.isUniqueSubscriptionId("test-id") >> true
        and: 'relevant details is extracted from the event'
            def subscriptionId = testEventConsumed.getData().getSubscriptionId()
            def predicates = testEventConsumed.getData().getPredicates()
        and: 'the cache handler returns for relevant subscription id'
            1 * mockDmiCacheHandler.get("test-id") >> testSubscriptionDetailsMap
        and: 'the delta predicates is returned'
            1 * mockCmSubscriptionComparator.getNewDmiSubscriptionPredicates(_) >> testListOfDeltaPredicates
        and: 'the DMI in event mapper returns cm notification subscription event'
            def testDmiInEvent = new DmiInEvent()
            1 * mockMappersFacade
                .toDmiInEvent(testListOfDeltaPredicates) >> testDmiInEvent
        when: 'the valid and unique event is consumed'
            objectUnderTest.processSubscriptionCreateRequest(subscriptionId, predicates)
        then: 'the subscription cache handler is called once'
            1 * mockDmiCacheHandler.add('test-id', _)
        and: 'the events handler method to publish DMI event is called correct number of times with the correct parameters'
            testSubscriptionDetailsMap.size() * mockEventsFacade.publishDmiInEvent(
                "test-id", "dmi-1", "subscriptionCreateRequest", testDmiInEvent)
        and: 'we schedule to send the response after configured time from the cache'
            1 * mockEventsFacade.publishNcmpOutEvent(
                "test-id", "subscriptionCreateResponse", null, true)
    }

    def 'Consume valid and Overlapping Cm Notification Subscription NcmpIn Event'() {
        given: 'a cmNotificationSubscriptionNcmp in event with unique subscription id'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
            def testEventConsumed = jsonObjectMapper.convertJsonString(jsonData, NcmpInEvent.class)
            def noDeltaPredicates = []
            mockCmSubscriptionPersistenceService.isUniqueSubscriptionId("test-id") >> true
        and: 'the cache handler returns for relevant subscription id'
            1 * mockDmiCacheHandler.get("test-id") >> testSubscriptionDetailsMap
        and: 'the delta predicates is returned'
            1 * mockCmSubscriptionComparator.getNewDmiSubscriptionPredicates(_) >> noDeltaPredicates
        when: 'the valid and unique event is consumed'
            objectUnderTest.processSubscriptionCreateRequest('test-id', noDeltaPredicates)
        then: 'the subscription cache handler is called once'
            1 * mockDmiCacheHandler.add('test-id', _)
        and: 'the subscription details are updated in the cache'
            1 * mockDmiCacheHandler.updateDmiSubscriptionStatusPerDmi('test-id', _, CmSubscriptionStatus.ACCEPTED)
        and: 'we schedule to send the response after configured time from the cache'
            1 * mockEventsFacade.publishNcmpOutEvent(
                "test-id", "subscriptionCreateResponse", null, true)
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
            1 * mockMappersFacade.toNcmpOutEventForRejectedRequest(
                "test-id", _) >> testNcmpOutEvent
        when: 'the valid but non-unique event is consumed'
            objectUnderTest.processSubscriptionCreateRequest(subscriptionId, predicates)
        then: 'the events handler method to publish DMI event is never called'
            0 * mockEventsFacade.publishDmiInEvent(_, _, _, _)
        and: 'the events handler method to publish NCMP out event is called once'
            1 * mockEventsFacade.publishNcmpOutEvent(
                'test-id', 'subscriptionCreateResponse', testNcmpOutEvent, false)
    }

    def 'Consume valid CmNotificationSubscriptionNcmpInEvent delete message'() {
        given: 'a cmNotificationSubscriptionNcmp in event for delete'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
            def testEventConsumed = jsonObjectMapper.convertJsonString(jsonData, NcmpInEvent.class)
        and: 'relevant details is extracted from the event'
            def subscriptionId = testEventConsumed.getData().getSubscriptionId()
            def predicates = testEventConsumed.getData().getPredicates()
        and: 'the cache handler returns for relevant subscription id'
            1 * mockDmiCacheHandler.get('test-id') >> testSubscriptionDetailsMap
        when: 'the valid and unique event is consumed'
            objectUnderTest.processSubscriptionDeleteRequest(subscriptionId, predicates)
        then: 'the subscription cache handler is called once'
            1 * mockDmiCacheHandler.add('test-id', predicates)
        and: 'the mapper handler to get DMI in event is called once'
            1 * mockMappersFacade.toDmiInEvent(_)
        and: 'the events handler method to publish DMI event is called correct number of times with the correct parameters'
            testSubscriptionDetailsMap.size() * mockEventsFacade.publishDmiInEvent(
                'test-id', 'dmi-1', 'subscriptionDeleteRequest', _)
        and: 'we schedule to send the response after configured time from the cache'
            1 * mockEventsFacade.publishNcmpOutEvent(
                'test-id', 'subscriptionDeleteResponse', null, true)
    }
}
