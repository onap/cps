/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.events.cmsubscription.service

import org.onap.cps.api.CpsQueryService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class CmSubscriptionValidationServiceImplSpec extends Specification {

    def mockCpsQueryService = Mock(CpsQueryService)
    def objectUnderTest = new CmSubscriptionValidationServiceImpl(mockCpsQueryService)

    def 'Validate datastore #datastore for Cm Subscription'() {
        when: 'we check against incoming datastore'
            def result = objectUnderTest.isValidDataStore(datastore)
        then: 'the datastores are validated for the use case'
            assert result == isValid
        where: 'following datastores are checked'
            scenario            | datastore                            || isValid
            'Valid datastore'   | 'ncmp-datastore:passthrough-running' || true
            'Invalid datastore' | 'invalid-ds'                         || false
    }

    def 'Validate uniqueness of incoming subscription ID'() {
        given: 'a valid cps path for querying'
            def cpsPathQuery = "//filter/subscribers[text()='some-sub']"
        and: 'relevant datanodes are returned'
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions', cpsPathQuery, FetchDescendantsOption.OMIT_DESCENDANTS) >>
                    dataNodes
        when: 'a subscription ID is validated'
            def result = objectUnderTest.isValidSubscriptionId('some-sub')
        then: 'result is as expected'
            assert result == isValidSubscriptionId
        where: 'following scenarios are used'
            scenario                  | dataNodes           || isValidSubscriptionId
            'datanodes present'       | [new DataNode()]    || false
            'no datanodes present'    | []                  || true
    }
}
