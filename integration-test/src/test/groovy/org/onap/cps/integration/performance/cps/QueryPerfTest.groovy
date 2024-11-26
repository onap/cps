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
import org.onap.cps.api.parameters.PaginationOption

import static org.onap.cps.api.parameters.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

class QueryPerfTest extends CpsPerfTestBase {

    CpsQueryService objectUnderTest
    def setup() { objectUnderTest = cpsQueryService }

    def 'Query complete data trees with #scenario.'() {
        when: 'query data nodes (using a fresh anchor with identical data for each test)'
            resourceMeter.start()
            def result = objectUnderTest.queryDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', cpsPath, INCLUDE_ALL_DESCENDANTS)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the expected number of nodes is returned'
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
        and: 'all data is read within #durationLimit ms  and memory used is within limit'
            recordAndAssertResourceUsage("Query 1 anchor ${scenario}", durationLimit, durationInSeconds, memoryLimit, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario                     | cpsPath                                                             || durationLimit  | memoryLimit  | expectedNumberOfDataNodes
            'top element'                | '/openroadm-devices'                                                || 1.27           | 400          | OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE + 1
            'leaf condition'             | '//openroadm-device[@ne-state="inservice"]'                         || 1.3            | 400          | OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
            'ancestors'                  | '//openroadm-device/ancestor::openroadm-devices'                    || 1.46           | 400          | OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE + 1
            'leaf condition + ancestors' | '//openroadm-device[@status="success"]/ancestor::openroadm-devices' || 1.32           | 400          | OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE + 1
            'non-existing data'          | '/path/to/non-existing/node[@id="1"]'                               || 0.01           | 1            | 0
    }

    def 'Query complete data trees across all anchors with #scenario.'() {
        when: 'query data nodes across all anchors'
            resourceMeter.start()
            def result = objectUnderTest.queryDataNodesAcrossAnchors(CPS_PERFORMANCE_TEST_DATASPACE, cpspath, INCLUDE_ALL_DESCENDANTS, PaginationOption.NO_PAGINATION)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the expected number of nodes is returned'
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
        and: 'all data is read within #durationLimit ms and memory used is within limit'
            recordAndAssertResourceUsage("Query across anchors ${scenario}", durationLimit, durationInSeconds, memoryLimit, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario                     | cpspath                                                             || durationLimit  | memoryLimit   | expectedNumberOfDataNodes
            'top element'                | '/openroadm-devices'                                                || 3.76           | 600           | OPENROADM_ANCHORS * (OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE + 1)
            'leaf condition'             | '//openroadm-device[@ne-state="inservice"]'                         || 3.3            | 600           | OPENROADM_ANCHORS * (OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE)
            'ancestors'                  | '//openroadm-device/ancestor::openroadm-devices'                    || 3.96           | 600           | OPENROADM_ANCHORS * (OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE + 1)
            'leaf condition + ancestors' | '//openroadm-device[@status="success"]/ancestor::openroadm-devices' || 3.76           | 600           | OPENROADM_ANCHORS * (OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE + 1)
    }

    def 'Query with leaf condition and #scenario.'() {
        when: 'query data nodes (using a fresh anchor with identical data for each test)'
            resourceMeter.start()
            def result = objectUnderTest.queryDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm2', '//openroadm-device[@status="success"]', fetchDescendantsOption)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the expected number of nodes is returned'
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
        and: 'all data is read within #durationLimit ms and memory used is within limit'
            recordAndAssertResourceUsage("Query with ${scenario}", durationLimit, durationInSeconds, memoryLimit, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  || durationLimit  | memoryLimit   | expectedNumberOfDataNodes
            'no descendants'     | OMIT_DESCENDANTS        || 0.1            | 6             | OPENROADM_DEVICES_PER_ANCHOR
            'direct descendants' | DIRECT_CHILDREN_ONLY    || 0.16           | 12            | OPENROADM_DEVICES_PER_ANCHOR * 2
            'all descendants'    | INCLUDE_ALL_DESCENDANTS || 1.4            | 200           | OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
    }

    def 'Query ancestors with #scenario.'() {
        when: 'query data nodes (using a fresh anchor with identical data for each test)'
            resourceMeter.start()
            def result = objectUnderTest.queryDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm3', '//openroadm-device[@ne-state="inservice"]/ancestor::openroadm-devices', fetchDescendantsOption)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the expected number of nodes is returned'
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
        and: 'all data is read within #durationLimit ms and memory used is within limit'
            recordAndAssertResourceUsage("Query ancestors with ${scenario}", durationLimit, durationInSeconds, memoryLimit, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  || durationLimit  | memoryLimit | expectedNumberOfDataNodes
            'no descendants'     | OMIT_DESCENDANTS        || 0.09           | 3           | 1
            'direct descendants' | DIRECT_CHILDREN_ONLY    || 0.11           | 8           | 1 + OPENROADM_DEVICES_PER_ANCHOR
            'all descendants'    | INCLUDE_ALL_DESCENDANTS || 1.34           | 400         | 1 + OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
    }

}
