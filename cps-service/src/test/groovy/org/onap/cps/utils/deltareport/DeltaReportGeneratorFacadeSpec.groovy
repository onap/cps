/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG
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

package org.onap.cps.utils.deltareport

import org.onap.cps.impl.DataNodeBuilder
import org.onap.cps.impl.DeltaReportBuilder
import spock.lang.Specification

class DeltaReportGeneratorFacadeSpec extends Specification {

    def mockDeltaReportGenerator = Mock(DeltaReportGenerator)
    def mockGroupedDeltaReportGenerator = Mock(GroupedDeltaReportGenerator)

    def objectUnderTest = new DeltaReportGeneratorFacade(mockDeltaReportGenerator, mockGroupedDeltaReportGenerator)

    static def sourceDataNodes = [new DataNodeBuilder().withXpath('/parent').build()]
    static def targetDataNodes = [new DataNodeBuilder().withXpath('/parent').build()]


    def 'Create delta reports with grouping enabled.'() {
        given: 'source and target data nodes'
        def expectedDeltaReports = [new DeltaReportBuilder().withXpath('/parent').actionCreate()
                                            .withTargetData([name: 'a']).build()]
        when: 'create delta reports is invoked with grouping enabled'
            def result = objectUnderTest.createDeltaReports(sourceDataNodes, targetDataNodes, true)
        then: 'delta report generator returns expected delta reports'
            1 * mockGroupedDeltaReportGenerator.createCondensedDeltaReports(sourceDataNodes, targetDataNodes) >> expectedDeltaReports
        and: 'result contains the expected delta reports'
            assert result == expectedDeltaReports
    }

    def 'Create delta reports with grouping disabled.'() {
        given: 'source and target data nodes'
        def expectedDeltaReports = [new DeltaReportBuilder().withXpath('/parent').actionCreate()
                                            .withTargetData([name: 'a']).build()]
        when: 'create delta reports is invoked with grouping disabled'
            def result = objectUnderTest.createDeltaReports(sourceDataNodes, targetDataNodes, false)
        then: 'delta report generator returns expected delta reports'
            1 * mockDeltaReportGenerator.createDeltaReports(sourceDataNodes, targetDataNodes) >> expectedDeltaReports
        and: 'result contains the expected delta reports'
            assert result == expectedDeltaReports
    }
}