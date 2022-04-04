/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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
import org.onap.cps.spi.exceptions.DataValidationException
import spock.lang.Specification

class CpsQueryServiceImplSpec extends Specification {
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)

    def objectUnderTest = new CpsQueryServiceImpl()

    def setup() {
        objectUnderTest.cpsDataPersistenceService = mockCpsDataPersistenceService
    }

    def 'Query data nodes by cps path with #fetchDescendantsOption.'() {
        given: 'a dataspace name, an anchor name and a cps path'
            def dataspaceName = 'some-dataspace'
            def anchorName = 'some-anchor'
            def cpsPath = '/cps-path'
        when: 'queryDataNodes is invoked'
            objectUnderTest.queryDataNodes(dataspaceName, anchorName, cpsPath, fetchDescendantsOption)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataPersistenceService.queryDataNodes(dataspaceName, anchorName, cpsPath, fetchDescendantsOption)
        where: 'all fetch descendants options are supported'
            fetchDescendantsOption << FetchDescendantsOption.values()
    }

    def 'Query data nodes by cps path with invalid #scenario.'() {
        when: 'queryDataNodes is invoked'
            objectUnderTest.queryDataNodes(dataspaceName, anchorName, '/cps-path', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the persistence service is not invoked with'
            0 * mockCpsDataPersistenceService.queryDataNodes(_, _, _, _)
        and: 'a data validation exception is thrown'
            def exception = thrown(DataValidationException)
        and: 'details contains invalid token encountered'
            exception.details.contains('invalid token encountered at position')
        where: 'the following parameters are used'
            scenario                     | dataspaceName                 | anchorName
            'dataspace name'             | 'dataspace names with spaces' | 'anchorName'
            'anchor name'                | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name'  | 'dataspace name with spaces'  | 'anchor name with spaces'
    }

}
