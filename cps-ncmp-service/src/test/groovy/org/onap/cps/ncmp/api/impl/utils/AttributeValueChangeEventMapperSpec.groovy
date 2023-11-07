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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.core.builder.CloudEventBuilder
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChangeEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class AttributeValueChangeEventMapperSpec extends Specification {

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    def spyObjectMapper = Spy(ObjectMapper)

    def objectUnderTest = new AttributeValueChangeEventCloudMapper(spyObjectMapper)

    def 'Map the attribute value change event to data of the cloud event'() {
        given: 'an attribute value change event'
            def jsonData = TestUtils.getResourceFileContent('attributeValueChangeEvent.json')
            def testEventData = jsonObjectMapper.convertJsonString(jsonData,
                AttributeValueChangeEvent.class)
            def testCloudEvent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(testEventData))
                .withId('someId')
                .withType('someType')
                .withSource(URI.create('ncmp.' + 'someEventKey'))
                .withExtension('correlationid', 'someCmId').build()
        when: 'the attribute value change event map to data of cloud event'
            AttributeValueChangeEventCloudMapper.randomId = 'someId'
            def resultCloudEvent = objectUnderTest.toCloudEvent(testEventData, 'someEventKey', 'someType')
        then: 'the attribute value change event resulted having expected values'
            resultCloudEvent.getData() == testCloudEvent.getData()
            resultCloudEvent.getId() == testCloudEvent.getId()
            resultCloudEvent.getType() == testCloudEvent.getType()
            resultCloudEvent.getSource() == URI.create('ncmp.' + 'someEventKey')
            resultCloudEvent.getDataSchema() == URI.create('urn:cps:org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChangeEvent:1.0.0')
    }

    def 'Map the attribute value change event with JSON processing exception'() {
        given: 'a json processing exception during process'
            def jsonProcessingException = new JsonProcessingException('The Cloud Event could not be constructed')
            spyObjectMapper.writeValueAsBytes(_) >> { throw jsonProcessingException }
        and: 'an attribute value change event'
            def jsonData = TestUtils.getResourceFileContent('attributeValueChangeEvent.json')
            def testEventData = jsonObjectMapper.convertJsonString(jsonData,
                AttributeValueChangeEvent.class)
        when: 'the attribute value change event map to cloud event'
            def expectedResult = objectUnderTest.toCloudEvent(testEventData, 'someKey', 'someEventType')
        then: 'no exception is thrown since it has been handled already'
            noExceptionThrown()
        and: 'expected result should be null'
            expectedResult == null
    }

}
