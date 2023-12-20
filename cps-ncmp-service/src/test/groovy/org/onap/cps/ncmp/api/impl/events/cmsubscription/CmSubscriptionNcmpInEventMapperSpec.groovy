/*
 *  ============LICENSE_START=======================================================
 *  Copyright (c) 2024 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.events.cmsubscription

import com.fasterxml.jackson.databind.ObjectMapper
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.events.cmsubscription.CmSubscriptionNcmpInEventMapper
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification


@SpringBootTest(classes = [JsonObjectMapper, ObjectMapper])
class CmSubscriptionNcmpInEventMapperSpec extends Specification {

    CmSubscriptionNcmpInEventMapper objectUnderTest = Mappers.getMapper(CmSubscriptionNcmpInEventMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Map subscription event to yang model subscription event where #scenario'() {
        given: 'a Subscription Event'
            def jsonData = TestUtils.getResourceFileContent('cmSubscriptionNcmpInEvent.json')
            def testEventToMap = jsonObjectMapper.convertJsonString(jsonData, CmSubscriptionNcmpInEvent.class)
        when: 'the event is mapped to a yang model subscription'
            def result = objectUnderTest.toYangModelCmDataSubscriptionEvent(testEventToMap)
        then: 'the resulting yang model subscription event contains the correct clientId'
            assert result.name == "cm-subscription-001"
        and: 'cmhandle ids are correct'
            assert result.cmHandles.id == ["CMHandle1", "CMHandle2", "CMHandle3"]
        and: 'filter is set correctly'
            assert result.cmHandles.filters.id == [["ncmp-datastore:passthrough-running"],["ncmp-datastore:passthrough-running"],["ncmp-datastore:passthrough-running"]]
    }

    def 'Map empty subscription event to yang model subscription event'() {
        given: 'a new Subscription Event with no data'
            def testEventToMap = new CmSubscriptionNcmpInEvent()
        when: 'the event is mapped to a yang model subscription'
            def result = objectUnderTest.toYangModelCmDataSubscriptionEvent(testEventToMap)
        then: 'the resulting yang model subscription event contains null clientId'
            assert result.name == null
    }
}