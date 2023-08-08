/*
 * ============LICENSE_START========================================================
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

package org.onap.cps.ncmp.api.impl.utils

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.core.builder.CloudEventBuilder
import org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.CmSubscriptionDmiOutEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class SubscriptionEventResponseCloudMapperSpec extends Specification {

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    def spyObjectMapper = Spy(ObjectMapper)

    def objectUnderTest = new SubscriptionEventResponseCloudMapper(spyObjectMapper)

    def 'Map the cloud event to subscription event response'() {
        given: 'a cloud event having a subscription event response in the data part'
            def jsonData = TestUtils.getResourceFileContent('cmSubscriptionDmiOutEvent.json')
            def testEventData = jsonObjectMapper.convertJsonString(jsonData, CmSubscriptionDmiOutEvent.class)
            def testCloudEvent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(testEventData))
                .withId('some-event-id')
                .withType('subscriptionCreatedStatus')
                .withSource(URI.create('some-resource'))
                .withExtension('correlationid', 'test-cmhandle1').build()
        when: 'the cloud event map to subscription event response'
            def resultSubscriptionEvent = objectUnderTest.toCmSubscriptionDmiOutEvent(testCloudEvent)
        then: 'the subscription event resulted having expected values'
            resultSubscriptionEvent.getData() == testEventData.getData()
    }

    def 'Map null of the data of the cloud event to subscription event response'() {
        given: 'a cloud event having a null subscription event response in the data part'
            def testCloudEvent = CloudEventBuilder.v1()
                .withData(null)
                .withId('some-event-id')
                .withType('subscriptionCreatedStatus')
                .withSource(URI.create('some-resource'))
                .withExtension('correlationid', 'test-cmhandle1').build()
        when: 'the cloud event map to subscription event response'
            def resultSubscriptionEvent = objectUnderTest.toCmSubscriptionDmiOutEvent(testCloudEvent)
        then: 'the subscription event response resulted having a null value'
            resultSubscriptionEvent == null
    }
}
