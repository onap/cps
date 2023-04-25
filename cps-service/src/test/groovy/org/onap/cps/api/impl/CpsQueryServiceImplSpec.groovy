/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.api.impl

import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.PaginationOption
import org.onap.cps.spi.utils.CpsValidator
import spock.lang.Specification

class CpsQueryServiceImplSpec extends Specification {
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockCpsValidator = Mock(CpsValidator)

    def objectUnderTest = new CpsQueryServiceImpl(mockCpsDataPersistenceService, mockCpsValidator)

    def 'Query data nodes by cps path with #fetchDescendantsOption.'() {
        given: 'a dataspace name, an anchor name and a cps path'
            def dataspaceName = 'some-dataspace'
            def anchorName = 'some-anchor'
            def cpsPath = '/cps-path'
        when: 'queryDataNodes is invoked'
            objectUnderTest.queryDataNodes(dataspaceName, anchorName, cpsPath, fetchDescendantsOption)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataPersistenceService.queryDataNodes(dataspaceName, anchorName, cpsPath, fetchDescendantsOption)
        and: 'the CpsValidator is called on the dataspaceName, schemaSetName and anchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName, anchorName)
        where: 'all fetch descendants options are supported'
            fetchDescendantsOption << [FetchDescendantsOption.OMIT_DESCENDANTS, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS,
                                       FetchDescendantsOption.DIRECT_CHILDREN_ONLY, new FetchDescendantsOption(10)]
    }

    def 'Query data nodes across all anchors by cps path with #fetchDescendantsOption.'() {
        given: 'a dataspace name, an anchor name and a cps path'
            def dataspaceName = 'some-dataspace'
            def cpsPath = '/cps-path'
            def paginationOption = new PaginationOption(pageIndex, pageSize)
        when: 'queryDataNodes is invoked'
            objectUnderTest.queryDataNodesAcrossAnchors(dataspaceName, cpsPath, fetchDescendantsOption, paginationOption)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataPersistenceService.queryDataNodesAcrossAnchors(dataspaceName, cpsPath, fetchDescendantsOption, paginationOption)
        and: 'the CpsValidator is called on the dataspaceName, schemaSetName and anchorName'
            1 * mockCpsValidator.validateNameCharacters(dataspaceName)
        where: 'following fetch descendants options and pagination options are used'
            scenario                                          | fetchDescendantsOption                         | pageIndex | pageSize
            'No descendants with negative pagination option'  | FetchDescendantsOption.OMIT_DESCENDANTS        | -1        | -1
            'No descendants with positive pagination option'  | FetchDescendantsOption.OMIT_DESCENDANTS        | 5         | 4
            'all descendants with positive pagination option' | FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS | 1         | 1
            'all descendants with negative pagination option' | FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS | -5        | -1
    }
}
