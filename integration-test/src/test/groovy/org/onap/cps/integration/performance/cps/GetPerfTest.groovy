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

import org.onap.cps.api.CpsDataService
import org.onap.cps.integration.performance.base.CpsPerfTestBase

import static org.onap.cps.spi.FetchDescendantsOption.DIRECT_CHILD_ONLY
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class GetPerfTest extends CpsPerfTestBase {

    CpsDataService objectUnderTest

    def setup() { objectUnderTest = cpsDataService }

    def 'Read top-level node with #scenario.'() {
        when: 'get data nodes from 1 anchor'
            stopWatch.start()
            def result = objectUnderTest.getDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, anchor, '/openroadm-devices', fetchDescendantsOption)
            stopWatch.stop()
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'all data is read within #durationLimit ms'
            recordAndAssertPerformance("Read datatrees with ${scenario}", durationLimit, durationInMillis)
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  | anchor       || durationLimit | expectedNumberOfDataNodes
            'no descendants'     | OMIT_DESCENDANTS        | 'openroadm1' || 50            | 1
            'direct descendants' | DIRECT_CHILD_ONLY       | 'openroadm2' || 100           | 1 + OPENROADM_DEVICES_PER_ANCHOR
            'all descendants'    | INCLUDE_ALL_DESCENDANTS | 'openroadm3' || 200           | 1 + OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
    }

    def 'Read data trees for multiple xpaths'() {
        given: 'a collection of xpaths to get'
            def xpaths = (1..OPENROADM_DEVICES_PER_ANCHOR).collect { "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']" }
        when: 'get data nodes from 1 anchor'
            stopWatch.start()
            def result = objectUnderTest.getDataNodesForMultipleXpaths(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm4', xpaths, INCLUDE_ALL_DESCENDANTS)
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'requested nodes and their descendants are returned'
            assert countDataNodesInTree(result) == OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
        and: 'all data is read within 200 ms'
            recordAndAssertPerformance("Read datatrees for multiple xpaths", 200, durationInMillis)
    }

    def 'Read for multiple xpaths to non-existing datanodes'() {
        given: 'a collection of xpaths to get'
            def xpaths = (1..50).collect { "/path/to/non-existing/node[@id='" + it + "']" }
        when: 'get data nodes from 1 anchor'
            stopWatch.start()
            def result = objectUnderTest.getDataNodesForMultipleXpaths(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm4', xpaths, INCLUDE_ALL_DESCENDANTS)
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'no data is returned'
            assert result.isEmpty()
        and: 'the operation completes within within 20 ms'
            recordAndAssertPerformance("Read non-existing xpaths", 20, durationInMillis)
    }

    def 'Read complete data trees using #scenario.'() {
        when: 'get data nodes for 5 anchors'
            stopWatch.start()
            (1..5).each {
                def result = objectUnderTest.getDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm' + it, xpath, INCLUDE_ALL_DESCENDANTS)
                assert countDataNodesInTree(result) == expectedNumberOfDataNodes
            }
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'all data is read within #durationLimit ms'
            recordAndAssertPerformance("Read datatrees using ${scenario}", durationLimit, durationInMillis)
        where: 'the following xpaths are used'
            scenario                | xpath                                  || durationLimit | expectedNumberOfDataNodes
            'openroadm root'        | '/'                                    || 600           | 1 + OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
            'openroadm top element' | '/openroadm-devices'                   || 600           | 1 + OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
            'openroadm whole list'  | '/openroadm-devices/openroadm-device'  || 600           | OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
    }

}
