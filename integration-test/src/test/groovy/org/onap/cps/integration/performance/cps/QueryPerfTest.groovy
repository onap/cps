/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
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

package org.onap.cps.integration.performance.cps

import org.onap.cps.api.CpsQueryService
import org.onap.cps.integration.performance.base.CpsPerfTestBase

import static org.onap.cps.spi.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class QueryPerfTest extends CpsPerfTestBase {

    CpsQueryService objectUnderTest

    def setup() { objectUnderTest = cpsQueryService }

    def 'Query complete data trees with #scenario.'() {
        when: 'query data nodes (using a fresh anchor with identical data for each test)'
            stopWatch.start()
            def result = objectUnderTest.queryDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, anchor, cpsPath, INCLUDE_ALL_DESCENDANTS)
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'the expected number of nodes is returned'
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
        and: 'all data is read within #durationLimit ms'
            recordAndAssertPerformance("Query 1 anchor ${scenario}", durationLimit, durationInMillis)
        where: 'the following parameters are used'
            scenario                     | anchor       | cpsPath                                                             || durationLimit | expectedNumberOfDataNodes
            'top element'                | 'openroadm1' | '/openroadm-devices'                                                || 250           | 50 * 86 + 1
            'leaf condition'             | 'openroadm2' | '//openroadm-device[@ne-state="inservice"]'                         || 650           | 50 * 86
            'ancestors'                  | 'openroadm3' | '//openroadm-device/ancestor::openroadm-devices'                    || 250           | 50 * 86 + 1
            'leaf condition + ancestors' | 'openroadm4' | '//openroadm-device[@status="success"]/ancestor::openroadm-devices' || 500           | 50 * 86 + 1
    }

    def 'Query complete data trees across all anchors with #scenario.'() {
        when: 'query data nodes across all anchors'
            stopWatch.start()
            def result = objectUnderTest.queryDataNodesAcrossAnchors('cpsPerformanceDataspace', cpspath, INCLUDE_ALL_DESCENDANTS)
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'the expected number of nodes is returned'
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
        and: 'all data is read within #durationLimit ms'
            recordAndAssertPerformance("Query across anchors ${scenario}", durationLimit, durationInMillis)
        where: 'the following parameters are used'
            scenario                     | cpspath                                                             || durationLimit | expectedNumberOfDataNodes
            'top element'                | '/openroadm-devices'                                                || 1             | 5 * (50 * 86 + 1)
            'leaf condition'             | '//openroadm-device[@ne-state="inservice"]'                         || 2500          | 5 * (50 * 86)
            'ancestors'                  | '//openroadm-device/ancestor::openroadm-devices'                    || 12000         | 5 * (50 * 86 + 1)
            'leaf condition + ancestors' | '//openroadm-device[@status="success"]/ancestor::openroadm-devices' || 1000          | 5 * (50 * 86 + 1)
    }

    def 'Query with leaf condition and #scenario.'() {
        when: 'query data nodes (using a fresh anchor with identical data for each test)'
            stopWatch.start()
            def result = objectUnderTest.queryDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, anchor, '//openroadm-device[@status="success"]', fetchDescendantsOption)
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'the expected number of nodes is returned'
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
        and: 'all data is read within #durationLimit ms'
            recordAndAssertPerformance("Query with ${scenario}", durationLimit, durationInMillis)
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  | anchor       || durationLimit | expectedNumberOfDataNodes
            'no descendants'     | OMIT_DESCENDANTS        | 'openroadm1' || 100           | 50
            'direct descendants' | DIRECT_CHILDREN_ONLY    | 'openroadm2' || 400           | 50 * 2
            'all descendants'    | INCLUDE_ALL_DESCENDANTS | 'openroadm3' || 500           | 50 * 86
    }

    def 'Query ancestors with #scenario.'() {
        when: 'query data nodes (using a fresh anchor with identical data for each test)'
            stopWatch.start()
            def result = objectUnderTest.queryDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, anchor, '//openroadm-device[@ne-state="inservice"]/ancestor::openroadm-devices', fetchDescendantsOption)
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'the expected number of nodes is returned'
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
        and: 'all data is read within #durationLimit ms'
            recordAndAssertPerformance("Query ancestors with ${scenario}", durationLimit, durationInMillis)
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  | anchor       || durationLimit | expectedNumberOfDataNodes
            'no descendants'     | OMIT_DESCENDANTS        | 'openroadm1' || 100           | 1
            'direct descendants' | DIRECT_CHILDREN_ONLY    | 'openroadm2' || 250           | 1 + 50
            'all descendants'    | INCLUDE_ALL_DESCENDANTS | 'openroadm3' || 400           | 1 + 50 * 86
    }

}
