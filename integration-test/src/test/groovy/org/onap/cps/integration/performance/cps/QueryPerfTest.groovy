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

import static org.onap.cps.api.parameters.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

import org.onap.cps.api.CpsQueryService
import org.onap.cps.integration.performance.base.CpsPerfTestBase
import org.onap.cps.api.parameters.PaginationOption

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
        and: 'all data is read #expectedDuration seconds with a margin of 100%'
            recordAndAssertResourceUsage("CPS:Query 1 anchor ${scenario}", expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario                     | cpsPath                                                             || expectedDuration | expectedNumberOfDataNodes
            'top element'                | '/openroadm-devices'                                                || 0.89             | OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE + 1
            'leaf condition'             | '//openroadm-device[@ne-state="inservice"]'                         || 1.1              | OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
            'ancestors'                  | '//openroadm-device/ancestor::openroadm-devices'                    || 1.3              | OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE + 1
            'leaf condition + ancestors' | '//openroadm-device[@status="success"]/ancestor::openroadm-devices' || 0.93             | OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE + 1
            'non-existing data'          | '/path/to/non-existing/node[@id="1"]'                               || 0.01             | 0
    }

    def 'Query complete data trees across all anchors with #scenario.'() {
        given: 'expected number of data nodes to be returned'
            def expectedNumberOfDataNodes = OPENROADM_ANCHORS * (OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE + additionalDataNodesPerDevice)
        when: 'query data nodes across all anchors'
            resourceMeter.start()
            def result = objectUnderTest.queryDataNodesAcrossAnchors(CPS_PERFORMANCE_TEST_DATASPACE, cpspath, INCLUDE_ALL_DESCENDANTS, PaginationOption.NO_PAGINATION)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the expected number of nodes is returned'
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
        and: 'all data is read #expectedDuration seconds with a margin of 100%'
            recordAndAssertResourceUsage("CPS:Query across anchors ${scenario}", expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB(), REFERENCE_GRAPH)
        where: 'the following parameters are used'
            scenario                     | cpspath                                                             | additionalDataNodesPerDevice || expectedDuration
            'top element'                | '/openroadm-devices'                                                | 1                            || 3.0
            'leaf condition'             | '//openroadm-device[@ne-state="inservice"]'                         | 0                            || 3.1
            'ancestors'                  | '//openroadm-device/ancestor::openroadm-devices'                    | 1                            || 3.2
            'leaf condition + ancestors' | '//openroadm-device[@status="success"]/ancestor::openroadm-devices' | 1                            || 3.0
    }

    def 'Query with leaf condition and #scenario.'() {
        given: 'expected number of data nodes to be returned'
            def expectedTotalNumberOfDataNodes = OPENROADM_DEVICES_PER_ANCHOR * expectedDataNodesPerDevice
        when: 'query data nodes (using a fresh anchor with identical data for each test)'
            resourceMeter.start()
            def result = objectUnderTest.queryDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm2', '//openroadm-device[@status="success"]', fetchDescendantsOption)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the expected number of nodes is returned'
            assert countDataNodesInTree(result) == expectedTotalNumberOfDataNodes
        and: 'all data is read #expectedDuration seconds with a margin of 100%'
            recordAndAssertResourceUsage("CPS:Query with ${scenario}", expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  || expectedDataNodesPerDevice     | expectedDuration
            'no descendants'     | OMIT_DESCENDANTS        || 1                              | 0.10
            'direct descendants' | DIRECT_CHILDREN_ONLY    || 2                              | 0.15
            'all descendants'    | INCLUDE_ALL_DESCENDANTS || OPENROADM_DATANODES_PER_DEVICE | 1.10
    }

    def 'Query ancestors with #scenario.'() {
        when: 'query data nodes (using a fresh anchor with identical data for each test)'
            resourceMeter.start()
            def result = objectUnderTest.queryDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm3', '//openroadm-device[@ne-state="inservice"]/ancestor::openroadm-devices', fetchDescendantsOption)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the expected number of nodes is returned'
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
        and: 'all data is read #expectedDuration seconds with a margin of 100%'
            recordAndAssertResourceUsage("CPS:Query ancestors with ${scenario}", expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  || expectedDuration | expectedNumberOfDataNodes
            'no descendants'     | OMIT_DESCENDANTS        || 0.08             | 1
            'direct descendants' | DIRECT_CHILDREN_ONLY    || 0.09             | 1 + OPENROADM_DEVICES_PER_ANCHOR
            'all descendants'    | INCLUDE_ALL_DESCENDANTS || 1.0              | 1 + OPENROADM_DEVICES_PER_ANCHOR * OPENROADM_DATANODES_PER_DEVICE
    }

    def 'Query data leaf with #scenario.'() {
        when: 'query data leaf is called'
            resourceMeter.start()
            def result = objectUnderTest.queryDataLeaf(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', cpsPath, String)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the expected number of results is returned'
            assert result.size() == expectedNumberOfValues
        and: 'all data is read #expectedDuration seconds with a margin of 100%'
            recordAndAssertResourceUsage("CPS:Query data leaf ${scenario}", expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario                     | cpsPath                                             || expectedDuration | expectedNumberOfValues
            'unique leaf value'          | '/openroadm-devices/openroadm-device/@device-id'    || 0.05             | OPENROADM_DEVICES_PER_ANCHOR
            'common leaf value'          | '/openroadm-devices/openroadm-device/@ne-state'     || 0.013            | 1
            'non-existing data leaf'     | '/openroadm-devices/openroadm-device/@non-existing' || 0.01             | 0
    }

}
