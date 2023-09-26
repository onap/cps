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

package org.onap.cps.ncmp.api.impl.events.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.core.builder.CloudEventBuilder
import org.onap.cps.ncmp.events.cmsubscription1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class CloudEventMapperSpec extends Specification {

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Cloud event to target event type'() {
        given: 'a cloud event with valid payload'
            def cloudEvent = testCloudEvent(new CmSubscriptionNcmpInEvent())
        when: 'the cloud event mapped to target event'
            def result = CloudEventMapper.toTargetEvent((cloudEvent), CmSubscriptionNcmpInEvent.class)
        then: 'the cloud event is mapped'
            assert result instanceof CmSubscriptionNcmpInEvent
    }

    def 'Cloud event to target event type when it is #scenario'() {
        given: 'a cloud event with invalid payload'
            def cloudEvent = testCloudEvent(payload)
        when: 'the cloud event mapped to target event'
            def result = CloudEventMapper.toTargetEvent(cloudEvent, CmSubscriptionNcmpInEvent.class)
        then: 'result is null'
            assert result == null
        where: 'below are the scenarios'
            scenario               | payload
            'invalid payload type' | ArrayList.class
            'without payload'      | null
    }

    def testCloudEvent(payload) {
        return CloudEventBuilder.v1().withData(jsonObjectMapper.asJsonBytes(payload))
            .withId("cmhandle1")
            .withSource(URI.create('test-source'))
            .withDataSchema(URI.create('test'))
            .withType('org.onap.cm.events.cm-subscription')
            .build()
    }
}
