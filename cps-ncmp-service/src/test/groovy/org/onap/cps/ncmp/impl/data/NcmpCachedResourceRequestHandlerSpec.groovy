/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

import org.onap.cps.api.CpsFacade
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.ncmp.api.data.models.CmResourceAddress
import org.onap.cps.ncmp.config.CpsApplicationContext
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import org.spockframework.spring.SpringBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Mono
import spock.lang.Specification

import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

@SpringBootTest
@ContextConfiguration(classes = [CpsApplicationContext])
class NcmpCachedResourceRequestHandlerSpec extends Specification {

    def mockCpsFacade = Mock(CpsFacade)
    def mockNetworkCmProxyQueryService = Mock(NetworkCmProxyQueryService)

    @SpringBean
    AlternateIdMatcher alternateIdMatcher = Mock()

    @SpringBean
    ApplicationContext applicationContext = Mock()

    def objectUnderTest = new NcmpCachedResourceRequestHandler(mockCpsFacade, mockNetworkCmProxyQueryService)

    def 'Execute a request with include descendants = #includeDescendants.'() {
        when: 'executing a request'
            objectUnderTest.executeRequest('ch-1', 'resource', includeDescendants)
        then: 'it is delegated to the ncmp query service with the correct option'
            1 * mockNetworkCmProxyQueryService.queryResourceDataOperational('ch-1', 'resource', expectedFetchDescendantsOption)
        where: 'the following options are used'
            includeDescendants || expectedFetchDescendantsOption
            true               || INCLUDE_ALL_DESCENDANTS
            false              || OMIT_DESCENDANTS
    }

    def 'Get resource data using NCMP operational datastore'() {
        given: 'a CmResourceAddress with OPERATIONAL datastore'
            def cmResourceAddress = new CmResourceAddress('ncmp-datastore:operational', 'ch-ref', 'resource')
        and: 'a mocked resolved cm handle id and data node map'
            def expectedData = [node1: 'value1']
            1 * alternateIdMatcher.getCmHandleId('ch-ref') >> 'ch-id'
            1 * mockCpsFacade.getDataNodesByAnchorV3('NFP-Operational', 'ch-id', 'resource', FetchDescendantsOption.getFetchDescendantsOption(includeDescendants)) >> expectedData
        when: 'the method is invoked'
            def result = objectUnderTest.getResourceDataForCmHandle(cmResourceAddress, 'options', null,null, includeDescendants, 'auth')
        then: 'a Mono with expected data is returned'
            assert result instanceof Mono
            assert result.block() == expectedData
        where: 'the following descendants options are used'
            includeDescendants << [true, false]
    }

    def 'Get resource data with unsupported datastore throws exception'() {
        given: 'a CmResourceAddress with unsupported datastore name'
            def cmResourceAddress = new CmResourceAddress('unsupported-datastore', 'some-cm-handle-ref', 'some-resource-id')
        when: 'the method is invoked'
            objectUnderTest.getResourceDataForCmHandle(cmResourceAddress, 'options', null, null, false, 'auth'
            ).block()
        then: 'an IllegalArgumentException is thrown'
            def exception = thrown(IllegalArgumentException)
            assert exception.message == 'Unsupported datastore name provided to fetch the cached data: unsupported-datastore'
    }

}
