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
import org.onap.cps.ncmp.events.cmsubscription1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class CloudEventMapperUtilsSpec extends Specification {

    @Autowired
    ObjectMapper objectMapper

    def 'Cloud event to Concrete Event'() {
        expect: 'Events transformed correctly'
            assert result == (CloudEventMapperUtils.toTargetEvent(testCloudEvent(), targetClass) != null)
        where: 'below are the scenarios'
            scenario                  | targetClass                     || result
            'Correct concrete type'   | CmSubscriptionNcmpInEvent.class || true
            'Incorrect concrete type' | List.class                      || false
            'Null concrete type'      | null                            || false
    }

    def testCloudEvent() {
        return CloudEventBuilder.v1().withData(objectMapper.writeValueAsBytes(new CmSubscriptionNcmpInEvent()))
            .withId("cmhandle1")
            .withSource(URI.create('test-source'))
            .withDataSchema(URI.create('test'))
            .withType('org.onap.cm.events.cm-subscription')
            .build()
    }
}
