/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2022-2023 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.event.avc

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.inventory.InventoryPersistenceImpl
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.event.model.SubscriptionEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.utils.CpsValidator
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [InventoryPersistenceImpl, CpsDataService, CpsModuleService, CpsAdminService,
        SubscriptionEventConsumer, ObjectMapper, JsonObjectMapper])
class SubscriptionEventConsumerSpec extends MessagingBaseSpec {

    @SpringBean
    CpsValidator stubbedCpsValidator = Stub()

    @SpringBean
    CpsAdminService stubbedCpsAdminService = Stub()

    @SpringBean
    CpsModuleService stubbedCpsModuleService = Stub()

    @SpringBean
    CpsDataService stubbedCpsDataService = Stub()

    @SpringBean
    InventoryPersistenceImpl stubbedInventoryPersistenceImpl = Stub()

    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))

    def objectUnderTest = new SubscriptionEventConsumer(stubbedInventoryPersistenceImpl)

    def 'Consume valid message'() {
        given: 'an event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionCreationEvent.json')
            def testEventSent = spiedJsonObjectMapper.convertJsonString(jsonData, SubscriptionEvent.class)
        and: 'dataCategory is set'
            testEventSent.getEvent().getDataType().setDataCategory(dataCategory)
        when: 'the valid event is consumed'
            objectUnderTest.consumeSubscriptionEvent(testEventSent)
        then: 'no exception is thrown'
            noExceptionThrown()
        where: 'data category is changed'
            dataCategory << [ 'CM' , 'FM' ]
    }
}
