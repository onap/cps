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

import org.onap.cps.integration.performance.base.CpsPerfTestBase

import static org.onap.cps.spi.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class QueryPerfTest extends CpsPerfTestBase {

    def objectUnderTest

    def setup() { objectUnderTest = cpsQueryService }

    def 'Query complete data trees with #scenario.'() {
        when: 'query data nodes'
            stopWatch.start()
            def result = objectUnderTest.queryDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, anchor, cpspath, INCLUDE_ALL_DESCENDANTS)
            stopWatch.stop()
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'all data is read within #durationLimit ms'
            recordAndAssertPerformance("Query 1 anchor ${scenario}", durationLimit, durationInMillis)
        where: 'the following queries are used'
            scenario                     | anchor       | cpspath                                                             || durationLimit | expectedNumberOfDataNodes
            'top element'                | 'openroadm1' | '/openroadm-devices'                                                || 150           | 2151
            'leaf condition'             | 'openroadm2' | '//openroadm-device[@ne-state="inservice"]'                         || 350           | 2150
            'ancestors'                  | 'openroadm3' | '//openroadm-device/ancestor::openroadm-devices'                    || 150           | 2151
            'leaf condition + ancestors' | 'openroadm4' | '//openroadm-device[@status="success"]/ancestor::openroadm-devices' || 250           | 2151
    }

    def 'Query complete data trees across all anchors with #scenario.'() {
        when: 'query data nodes across all anchors'
            stopWatch.start()
            def result = objectUnderTest.queryDataNodesAcrossAnchors('cpsPerformanceDataspace', cpspath, INCLUDE_ALL_DESCENDANTS)
            stopWatch.stop()
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'all data is read within #durationLimit ms'
            recordAndAssertPerformance("Query across anchors ${scenario}", durationLimit, durationInMillis)
        where: 'the following queries are used'
            scenario                     | cpspath                                                             || durationLimit | expectedNumberOfDataNodes
            // FIXME Current implementation of queryDataNodesAcrossAnchors throws NullPointerException for next case
            // 'top element'                | '/openroadm-devices'                                                || 1             | 2151 * 5
            'leaf condition'             | '//openroadm-device[@ne-state="inservice"]'                         || 1200          | 2150 * 5
            'ancestors'                  | '//openroadm-device/ancestor::openroadm-devices'                    || 7000          | 2151 * 5
            'leaf condition + ancestors' | '//openroadm-device[@status="success"]/ancestor::openroadm-devices' || 600           | 2151 * 5
    }

    def 'Query with #scenario.'() {
        when: 'query data nodes'
            stopWatch.start()
            def result = objectUnderTest.queryDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, anchor, cpspath, fetchDescendantsOption)
            stopWatch.stop()
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'all data is read within #durationLimit ms'
            recordAndAssertPerformance("Query with ${scenario}", durationLimit, durationInMillis)
        where: 'the following queries are used'
            scenario                       | fetchDescendantsOption  | anchor       | cpspath                                                                 || durationLimit | expectedNumberOfDataNodes
            'omit descendants'             | OMIT_DESCENDANTS        | 'openroadm1' | '//openroadm-device[@ne-state="inservice"]'                             || 50            | 25
            'direct descendants'           | DIRECT_CHILDREN_ONLY    | 'openroadm2' | '//openroadm-device[@ne-state="inservice"]'                             || 180           | 50
            'all descendants'              | INCLUDE_ALL_DESCENDANTS | 'openroadm3' | '//openroadm-device[@ne-state="inservice"]'                             || 200           | 2150
            'ancestors omit descendants'   | OMIT_DESCENDANTS        | 'openroadm4' | '//openroadm-device[@ne-state="inservice"]/ancestor::openroadm-devices' || 50            | 1
            'ancestors direct descendants' | DIRECT_CHILDREN_ONLY    | 'openroadm5' | '//openroadm-device[@ne-state="inservice"]/ancestor::openroadm-devices' || 120           | 26
            'ancestors all descendants'    | INCLUDE_ALL_DESCENDANTS | 'openroadm1' | '//openroadm-device[@ne-state="inservice"]/ancestor::openroadm-devices' || 200           | 2151
    }

}
