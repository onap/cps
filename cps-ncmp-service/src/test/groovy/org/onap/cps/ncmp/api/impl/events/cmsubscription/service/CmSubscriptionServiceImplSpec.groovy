/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2024 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.events.cmsubscription.service

import org.onap.cps.api.CpsQueryService
import org.onap.cps.ncmp.api.impl.operations.DatastoreType
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class CmSubscriptionServiceImplSpec extends Specification {

    def mockCpsQueryService = Mock(CpsQueryService)

    def objectUnderTest = new CmSubscriptionServiceImpl(mockCpsQueryService)

    def 'Query ongoing cm subscription'() {
        given: 'a list of datanodes and a valid cm subscription query'
            def dataNodes = [new DataNode(xpath: '/cps/path', leaves: ['subscribers': 'sub-1']),
                             new DataNode(xpath: '/cps/path/child[@param=\'value\']', leaves: ['subscribers': 'sub-2'])]
            def cpsPathQuery = "/datastores/datastore[@name='ncmp-datastore:passthrough-running']/cm-handles/cm-handle[@id='ch-1']/filters/filter[@xpath='/']";
        and: 'the list of datanodes is returned for query data node'
            1 * mockCpsQueryService.queryDataNodes('NCMP-Admin', 'cm-data-subscriptions',
                cpsPathQuery, FetchDescendantsOption.OMIT_DESCENDANTS) >> dataNodes
        when: 'we check for an ongoing cm subscription'
            def response = objectUnderTest.isOngoingCmSubscription(DatastoreType.PASSTHROUGH_RUNNING, 'ch-1', '/')
        then: 'we get expected response'
            assert response
    }
}
