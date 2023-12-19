/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.events.deprecated.cmsubscription

import com.fasterxml.jackson.databind.ObjectMapper
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.events.cmsubscription1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [JsonObjectMapper, ObjectMapper])
class ClientCmSubscriptionNcmpInEventMapperSpec extends Specification {

    CmSubscriptionNcmpInEventToCmSubscriptionDmiInEventMapper objectUnderTest = Mappers.getMapper(CmSubscriptionNcmpInEventToCmSubscriptionDmiInEventMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Map clients subscription event to ncmps subscription event'() {
        given: 'a Subscription Event'
            def jsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionNcmpInEvent.json')
            def testEventToMap = jsonObjectMapper.convertJsonString(jsonData, CmSubscriptionNcmpInEvent.class)
        when: 'the client event is mapped to a ncmp subscription event'
            def result = objectUnderTest.toCmSubscriptionDmiInEvent(testEventToMap)
        then: 'the resulting ncmp subscription event contains the correct clientId'
            assert result.getData().getSubscription().getClientID() == "SCO-9989752"
        and: 'subscription name'
            assert result.getData().getSubscription().getName() == "cm-subscription-001"
        and: 'is tagged value is false'
            assert result.getData().getSubscription().getIsTagged() == false
        and: 'data category is CM'
            assert result.getData().getDataType().getDataCategory() == 'CM'
        and: 'predicate targets is null'
            assert result.getData().getPredicates().getTargets() == []
        and: 'datastore is passthrough-running'
            assert result.getData().getPredicates().getDatastore() == 'ncmp-datastore:passthrough-running'
    }

}
