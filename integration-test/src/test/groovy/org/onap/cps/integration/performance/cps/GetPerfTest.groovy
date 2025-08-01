/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.onap.cps.api.parameters.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

class GetPerfTest extends CpsPerfTestBase {

    CpsDataService objectUnderTest

    def setup() { objectUnderTest = cpsDataService }

    def 'Read top-level node with #scenario.'() {
        when: 'get data nodes from 1 anchor'
            resourceMeter.start()
            def result = objectUnderTest.getDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', '/openroadm-devices', fetchDescendantsOption)
            resourceMeter.stop()
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'all data is read within #expectedDuration seconds and a margin of 100%'
            recordAndAssertResourceUsage("CPS:Read datatree with ${scenario}", expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB(), referencegraph)
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  | referencegraph || expectedDuration | expectedNumberOfDataNodes
            'no descendants'     | OMIT_DESCENDANTS        | false          || 0.01             | 1
            'direct descendants' | DIRECT_CHILDREN_ONLY    | false          || 0.045            | 1 + OPENROADM_DEVICES_PER_ANCHOR
            'all descendants'    | INCLUDE_ALL_DESCENDANTS | true           || 1.1              | 1 + OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
    }

    def 'Read data trees for multiple xpaths'() {
        given: 'a collection of xpaths to get'
            def xpaths = (1..OPENROADM_DEVICES_PER_ANCHOR).collect { "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']" }
        when: 'get data nodes from 1 anchor'
            resourceMeter.start()
            def result = objectUnderTest.getDataNodesForMultipleXpaths(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm2', xpaths, INCLUDE_ALL_DESCENDANTS)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'requested nodes and their descendants are returned'
            assert countDataNodesInTree(result) == OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
        and: 'all data is read within expected time and a margin of 200%'
            recordAndAssertResourceUsage('CPS:Read datatrees for multiple xpaths', 2.0, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Read for multiple xpaths to non-existing datanodes'() {
        given: 'a collection of xpaths to get'
            def xpaths = (1..500).collect { "/path/to/non-existing/node[@id='" + it + "']" }
        when: 'get data nodes from 1 anchor'
            resourceMeter.start()
            def result = objectUnderTest.getDataNodesForMultipleXpaths(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm2', xpaths, INCLUDE_ALL_DESCENDANTS)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'no data is returned'
            assert result.isEmpty()
        and: 'the operation completes within within expected time'
            recordAndAssertResourceUsage('CPS:Read non-existing xpaths', 0.06, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Read complete data trees using #scenario.'() {
        when: 'get data nodes from 1 anchor'
            resourceMeter.start()
            def result = objectUnderTest.getDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm3', xpath, INCLUDE_ALL_DESCENDANTS)
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'all data is read within expected time and a margin of 100%'
            recordAndAssertResourceUsage("CPS:Read datatrees using ${scenario}", expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following xpaths are used'
            scenario                | xpath                                  || expectedDuration | expectedNumberOfDataNodes
            'openroadm root'        | '/'                                    || 1                | 1 + OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
            'openroadm top element' | '/openroadm-devices'                   || 0.95             | 1 + OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
            'openroadm whole list'  | '/openroadm-devices/openroadm-device'  || 1.06             | OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
    }

}
