/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 Deutsche Telekom AG
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

import org.onap.cps.api.model.DataNode
import org.onap.cps.api.model.DeltaReport
import spock.lang.Specification

class DeltaReportGeneratorFacadeSpec extends Specification {

    def mockDeltaReportGenerator = Mock(DeltaReportGenerator)
    def mockGroupedDeltaReportGenerator = Mock(GroupedDeltaReportGenerator)

    def objectUnderTest = new DeltaReportGeneratorFacade(mockDeltaReportGenerator, mockGroupedDeltaReportGenerator)

    def 'Create delta reports with grouping enabled.'() {
        given: 'source and target data nodes'
            def sourceDataNodes = [Mock(DataNode)]
            def targetDataNodes = [Mock(DataNode)]
        and: 'grouped delta report generator returns expected delta reports'
            def expectedDeltaReports = [Mock(DeltaReport)]
            mockGroupedDeltaReportGenerator.createCondensedDeltaReports(_, _) >> expectedDeltaReports
        when: 'create delta reports is invoked with grouping enabled'
            def result = objectUnderTest.createDeltaReports(sourceDataNodes, targetDataNodes, true)
        then: 'result contains the expected delta reports'
            result == expectedDeltaReports
    }

    def 'Create delta reports with grouping disabled.'() {
        given: 'source and target data nodes'
            def sourceDataNodes = [Mock(DataNode)]
            def targetDataNodes = [Mock(DataNode)]
        and: 'delta report generator returns expected delta reports'
            def expectedDeltaReports = [Mock(DeltaReport)]
            mockDeltaReportGenerator.createDeltaReports(_, _) >> expectedDeltaReports
        when: 'create delta reports is invoked with grouping disabled'
            def result = objectUnderTest.createDeltaReports(sourceDataNodes, targetDataNodes, false)
        then: 'result contains the expected delta reports'
            result == expectedDeltaReports
    }

    def 'Create delta reports handles empty collections.'() {
        given: 'empty source and target data nodes'
            def sourceDataNodes = []
            def targetDataNodes = []
        and: 'delta report generator returns empty delta reports'
            mockDeltaReportGenerator.createDeltaReports(_, _) >> []
        when: 'create delta reports is invoked with grouping disabled'
            def result = objectUnderTest.createDeltaReports(sourceDataNodes, targetDataNodes, false)
        then: 'result is an empty list'
            result == []
    }

    def 'Create delta reports with #scenario nodes.'() {
        given: 'data nodes collection'
            def sourceDataNodes = sourceNodes
            def targetDataNodes = targetNodes
        and: 'appropriate generator returns expected delta reports'
            if (grouping) {
                mockGroupedDeltaReportGenerator.createCondensedDeltaReports(_, _) >> expectedReports
            } else {
                mockDeltaReportGenerator.createDeltaReports(_, _) >> expectedReports
            }
        when: 'create delta reports is invoked'
            def result = objectUnderTest.createDeltaReports(sourceDataNodes, targetDataNodes, grouping)
        then: 'result contains expected delta reports'
            result == expectedReports
        where: 'test cases'
            scenario              | sourceNodes           | targetNodes           | grouping | expectedReports
            'multiple source'     | [Mock(DataNode), Mock(DataNode)] | [Mock(DataNode)] | false    | [Mock(DeltaReport), Mock(DeltaReport)]
            'multiple target'     | [Mock(DataNode)]      | [Mock(DataNode), Mock(DataNode)] | false    | [Mock(DeltaReport)]
            'multiple both'       | [Mock(DataNode), Mock(DataNode)] | [Mock(DataNode), Mock(DataNode)] | false    | [Mock(DeltaReport), Mock(DeltaReport), Mock(DeltaReport)]
            'grouped multiple'    | [Mock(DataNode), Mock(DataNode)] | [Mock(DataNode), Mock(DataNode)] | true     | [Mock(DeltaReport)]
    }
}

