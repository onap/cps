/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.ncmp.api.impl.subscriptions

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NO_TIMESTAMP

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class SubscriptionPersistenceSpec extends Specification {

    private static final String SUBSCRIPTION_DATASPACE_NAME = "NCMP-Admin";
    private static final String SUBSCRIPTION_ANCHOR_NAME = "AVC-Subscriptions";
    private static final String SUBSCRIPTION_REGISTRY_PARENT = "/subscription-registry";

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def mockCpsDataService = Mock(CpsDataService)

    def objectUnderTest = new SubscriptionPersistenceImpl(jsonObjectMapper, mockCpsDataService)

   def 'save a subscription event' () {
       given: 'a yang model subscription event'
            def yangModelSubscriptionEvent = new YangModelSubscriptionEvent();
            yangModelSubscriptionEvent.setClientId('some-client-id')
            yangModelSubscriptionEvent.setSubscriptionName('some-subscription-name')
            yangModelSubscriptionEvent.setTagged(true)
            yangModelSubscriptionEvent.setTopic('some-topic')
           def predicates = new YangModelSubscriptionEvent.Predicates(datastore: 'some-datastore',
               targetCmHandles: [new YangModelSubscriptionEvent.TargetCmHandle('cmhandle1'), new YangModelSubscriptionEvent.TargetCmHandle('cmhandle2')])
            yangModelSubscriptionEvent.setPredicates(predicates)
       when: 'the yangModelSubscriptionEvent is saved'
            objectUnderTest.saveSubscriptionEvent(yangModelSubscriptionEvent)
       then: 'the cpsDataService is called with the correct data'
            1 * mockCpsDataService.saveListElements(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                SUBSCRIPTION_REGISTRY_PARENT,
                '{"subscription":[{' +
                    '"subscriptionName":"some-subscription-name","topic":"some-topic",' +
                    '"predicates":{"datastore":"some-datastore","targetCmHandles":[{"cmHandleId":"cmhandle1","status":"PENDING"},{"cmHandleId":"cmhandle2","status":"PENDING"}]},' +
                    '"clientID":"some-client-id","isTagged":true}]}',
                NO_TIMESTAMP)
   }

}
