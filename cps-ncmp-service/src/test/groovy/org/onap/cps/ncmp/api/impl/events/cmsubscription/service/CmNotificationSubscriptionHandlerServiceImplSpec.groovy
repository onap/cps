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

package org.onap.cps.ncmp.api.impl.events.cmsubscription.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.impl.events.cmsubscription.CmNotificationSubscriptionNcmpOutEventProducer
import org.onap.cps.ncmp.api.impl.events.cmsubscription.DmiCmNotificationSubscriptionCacheHandler
import org.onap.cps.ncmp.api.impl.events.cmsubscription.mapper.CmNotificationSubscriptionNcmpOutEventMapper
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.CmNotificationSubscriptionNcmpInEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class CmNotificationSubscriptionHandlerServiceImplSpec extends Specification{

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def mockCmNotificationSubscriptionPersistenceService = Mock(CmNotificationSubscriptionPersistenceService);
    def mockCmNotificationSubscriptionNcmpOutEventMapper = Mock(CmNotificationSubscriptionNcmpOutEventMapper);
    def mockCmNotificationSubscriptionNcmpOutEventProducer = Mock(CmNotificationSubscriptionNcmpOutEventProducer);
    def mockDmiCmNotificationSubscriptionCacheHandler = Mock(DmiCmNotificationSubscriptionCacheHandler);

    def objectUnderTest = new CmNotificationSubscriptionHandlerServiceImpl(mockCmNotificationSubscriptionPersistenceService, mockCmNotificationSubscriptionNcmpOutEventMapper, mockCmNotificationSubscriptionNcmpOutEventProducer, mockDmiCmNotificationSubscriptionCacheHandler)

    def 'Consume valid and unique CmNotificationSubscription create message'() {
        given: 'a cmNotificationSubscriptionNcmp in event'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
            def testEventConsumed = jsonObjectMapper.convertJsonString(jsonData, CmNotificationSubscriptionNcmpInEvent.class)
            mockCmNotificationSubscriptionPersistenceService.isUniqueSubscriptionId('cm-subscription-001') >> true
        when: 'the valid and unique event is consumed'
            objectUnderTest.processSubscriptionCreateRequest(testEventConsumed)
        then: 'the subscription cache handler is called once'
            1 * mockDmiCmNotificationSubscriptionCacheHandler.add('cm-subscription-001',_)
    }

    def 'Consume valid and but non-unique CmNotificationSubscription create message'() {
        given: 'a cmNotificationSubscriptionNcmp in event'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
            def testEventConsumed = jsonObjectMapper.convertJsonString(jsonData, CmNotificationSubscriptionNcmpInEvent.class)
            mockCmNotificationSubscriptionPersistenceService.isUniqueSubscriptionId('cm-subscription-001') >> false
        when: 'the valid but non-unique event is consumed'
            objectUnderTest.processSubscriptionCreateRequest(testEventConsumed)
        then: 'the subscription out event publisher is called once'
            1 * mockCmNotificationSubscriptionNcmpOutEventProducer.publishCmNotificationSubscriptionNcmpOutEvent('cm-subscription-001', 'subscriptionCreateResponse', _, false)
    }
}
