/*
 *  ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.impl.data

import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.data.models.CmResourceAddress
import org.onap.cps.ncmp.config.CpsApplicationContext
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import org.onap.cps.spi.api.model.DataNode
import org.spockframework.spring.SpringBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Mono
import spock.lang.Specification

import static org.onap.cps.spi.api.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.api.FetchDescendantsOption.OMIT_DESCENDANTS

@SpringBootTest
@ContextConfiguration(classes = [CpsApplicationContext])
class NcmpCachedResourceRequestHandlerSpec extends Specification {

    def cpsDataService = Mock(CpsDataService)
    def networkCmProxyQueryService= Mock(NetworkCmProxyQueryService)

    @SpringBean
    AlternateIdMatcher alternateIdMatcher = Mock()

    @SpringBean
    ApplicationContext applicationContext = Mock()

    def objectUnderTest = new NcmpCachedResourceRequestHandler(cpsDataService, networkCmProxyQueryService)

    def 'Execute a request with include descendants = #includeDescendants.'() {
        when: 'executing a request'
            objectUnderTest.executeRequest('ch-1', 'resource', includeDescendants)
        then: 'it is delegated to the ncmp query service with the correct option'
            1 * networkCmProxyQueryService.queryResourceDataOperational('ch-1','resource', expectedFetchDescendantsOption)
        where: 'the following options are used'
            includeDescendants || expectedFetchDescendantsOption
            true               || INCLUDE_ALL_DESCENDANTS
            false              || OMIT_DESCENDANTS
    }

    def 'Get resource data.'() {
        given: 'the data service returns 2 nodes for the given resource address'
            def cmResourceAddress = new CmResourceAddress('datastore','ch-1','resource')
            def dataNode1 = new DataNode(xpath:'p1')
            def dataNode2 = new DataNode(xpath:'p2')
            cpsDataService.getDataNodes('datastore','ch-1','resource',OMIT_DESCENDANTS) >> [dataNode1, dataNode2]
        when: 'getting the resource data'
            alternateIdMatcher.getCmHandleId('ch-1') >> 'ch-1'
            def result = objectUnderTest.getResourceDataForCmHandle(cmResourceAddress, 'options', 'topic', 'request id', false, 'authorization')
        then: 'the result is a "Mono" holding just the first data node'
            assert result instanceof Mono
            assert result.block() == dataNode1
    }

}
