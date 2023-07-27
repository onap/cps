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
import org.onap.cps.ncmp.events.avcsubscription1_0_0.client_to_ncmp.SubscriptionEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.exceptions.CloudEventConstructionException
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class SubscriptionEventCloudMapperSpec extends Specification {

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    def 'Map the data of the cloud event to subscription event'() {
        given: 'a cloud event having a subscription event in the data part'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionEvent.json')
            def testEventData = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEvent.class)
            def testCloudEvent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(testEventData))
                .withId('some-event-id')
                .withType('subscriptionCreated')
                .withSource(URI.create('some-resource'))
                .withExtension('correlationid', 'test-cmhandle1').build()
        when: 'the cloud event map to subscription event'
            def resultSubscriptionEvent = SubscriptionEventCloudMapper.toSubscriptionEvent(testCloudEvent)
        then: 'the subscription event resulted having expected values'
            resultSubscriptionEvent.getData() == testEventData.getData()
    }

    def 'Map the null of the data of the cloud event to subscription event'() {
        given: 'a cloud event having a null subscription event in the data part'
            def testCloudEvent = CloudEventBuilder.v1()
                .withData(null)
                .withId('some-event-id')
                .withType('subscriptionCreated')
                .withSource(URI.create('some-resource'))
                .withExtension('correlationid', 'test-cmhandle1').build()
        when: 'the cloud event map to subscription event'
            def resultSubscriptionEvent = SubscriptionEventCloudMapper.toSubscriptionEvent(testCloudEvent)
        then: 'the subscription event resulted having a null value'
            resultSubscriptionEvent == null
    }

    def 'Map the subscription event to data of the cloud event'() {
        given: 'a subscription event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionCreationEventNcmpVersion.json')
            def testEventData = jsonObjectMapper.convertJsonString(jsonData,
                                org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_dmi.SubscriptionEvent.class)
            def testCloudEvent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(testEventData))
                .withId('some-id')
                .withType('subscriptionCreated')
                .withSource(URI.create('SCO-9989752'))
                .withExtension('correlationid', 'test-cmhandle1').build()
        when: 'the subscription event map to data of cloud event'
            SubscriptionEventCloudMapper.randomId = 'some-id'
            def resultCloudEvent = SubscriptionEventCloudMapper.toCloudEvent(testEventData, 'some-event-key', 'subscriptionCreated')
        then: 'the subscription event resulted having expected values'
            resultCloudEvent.getData() == testCloudEvent.getData()
            resultCloudEvent.getId() == testCloudEvent.getId()
            resultCloudEvent.getType() == testCloudEvent.getType()
            resultCloudEvent.getSource() == URI.create('SCO-9989752')
            resultCloudEvent.getDataSchema() == URI.create('urn:cps:org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_dmi.SubscriptionEvent:1.0.0')
    }

    def 'Map the subscription event to data of the cloud event with wrong content causes an exception'() {
        given: 'an empty ncmp subscription event'
            def testNcmpSubscriptionEvent = new org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_dmi.SubscriptionEvent()
        when: 'the subscription event map to data of cloud event'
            SubscriptionEventCloudMapper.toCloudEvent(testNcmpSubscriptionEvent, 'some-key', 'some-event-type')
        then: 'a run time exception is thrown'
            def exception = thrown(CloudEventConstructionException)
            exception.details == 'Invalid object to serialize or required headers is missing'
    }

}
