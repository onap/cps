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

package org.onap.cps.ncmp.api.impl.util.deprecated

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.core.builder.CloudEventBuilder
import org.onap.cps.ncmp.api.impl.utils.SubscriptionOutcomeCloudMapper
import org.onap.cps.ncmp.events.cmsubscription1_0_0.ncmp_to_client.CmSubscriptionNcmpOutEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class SubscriptionOutcomeCloudMapperSpec extends Specification {

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    def spyObjectMapper = Spy(ObjectMapper)

    def objectUnderTest = new SubscriptionOutcomeCloudMapper(spyObjectMapper)

    def 'Map the subscription outcome to cloud event'() {
        given: 'a subscription event'
            def jsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionNcmpOutEvent.json')
            def testEventData = jsonObjectMapper.convertJsonString(jsonData, CmSubscriptionNcmpOutEvent.class)
            def testCloudEvent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(testEventData))
                .withId('some-id')
                .withType('subscriptionCreatedStatus')
                .withSource(URI.create('NCMP'))
                .withExtension('correlationid', 'test-cmhandle1').build()
        when: 'the subscription event map to data of cloud event'
            SubscriptionOutcomeCloudMapper.randomId = 'some-id'
            def resultCloudEvent = objectUnderTest.toCloudEvent(testEventData, 'some-event-key', 'subscriptionCreatedStatus')
        then: 'the subscription event resulted having expected values'
            resultCloudEvent.getData() == testCloudEvent.getData()
            resultCloudEvent.getId() == testCloudEvent.getId()
            resultCloudEvent.getType() == testCloudEvent.getType()
            resultCloudEvent.getSource() == testCloudEvent.getSource()
            resultCloudEvent.getDataSchema() == URI.create('urn:cps:org.onap.cps.ncmp.events.cmsubscription1_0_0.ncmp_to_client.CmSubscriptionNcmpOutEvent:1.0.0')
    }

    def 'Map the subscription outcome to cloud event with JSON processing exception'() {
        given: 'a json processing exception during process'
            def jsonProcessingException = new JsonProcessingException('The Cloud Event could not be constructed')
            spyObjectMapper.writeValueAsBytes(_) >> { throw jsonProcessingException }
        and: 'a cloud event having a subscription outcome in the data part'
            def jsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionNcmpOutEvent.json')
            def testEventData = jsonObjectMapper.convertJsonString(jsonData, CmSubscriptionNcmpOutEvent.class)
        when: 'the subscription outcome map to cloud event'
            def expectedResult = objectUnderTest.toCloudEvent(testEventData, 'some-key', 'some-event-type')
        then: 'no exception is thrown since it has been handled already'
            noExceptionThrown()
        and: 'expected result should be null'
            expectedResult == null
    }

}
