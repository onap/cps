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

import java.time.OffsetDateTime
import org.onap.cps.api.CpsDataService
import org.onap.cps.integration.performance.base.CpsPerfTestBase

import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class UpdatePerfTest extends CpsPerfTestBase {
    static final def UPDATE_TEST_ANCHOR = 'updateTestAnchor'
    static final def INNER_NODE_JSON = readResourceDataFile('openroadm/innerNode.json')

    CpsDataService objectUnderTest
    def now = OffsetDateTime.now()

    def setup() {
        objectUnderTest = cpsDataService
        cpsAnchorService.createAnchor(CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, UPDATE_TEST_ANCHOR)
        cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR,
                '{ "openroadm-devices": { "openroadm-device": []}}', now)
    }

    def cleanup() {
        cpsAnchorService.deleteAnchor(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR)
    }

    def 'Replace single data node and descendants: #scenario.'() {
        given: 'replacement JSON for node containing list of device nodes'
            def jsonData = '{ "openroadm-devices": ' + generateJsonForOpenRoadmDevices(startId, totalNodes, changeLeaves) + '}'
        when: 'the container node is updated'
            resourceMeter.start()
            objectUnderTest.updateDataNodeAndDescendants(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR, '/', jsonData, now)
            resourceMeter.stop()
        then: 'there are the expected number of totalNodes nodes'
            assert totalNodes == countDataNodes('//openroadm-device')
        and: 'data leaves have expected values'
            assert totalNodes == countDataNodes(changeLeaves? '//openroadm-device[@status="fail"]'
                                                            : '//openroadm-device[@status="success"]')
        then: 'update completes within expected time and memory used is within limit'
            recordAndAssertResourceUsage(scenario,
                    timeLimit, resourceMeter.getTotalTimeInSeconds(),
                    memoryLimit, resourceMeter.getTotalMemoryUsageInMB())
        where:
            scenario                           | totalNodes | startId | changeLeaves || timeLimit | memoryLimit
            'Update Warmup #1 (ignore)'        | 100        | 1       | false        ||      10.0 | 250
            'Update Warmup #2 (ignore)'        | 0          | 1       | true         ||       1.0 | 100
            'Replace 0 nodes with 100'         | 100        | 1       | false        ||       6.0 | 250
            'Replace 100 using same data'      | 100        | 1       | false        ||       6.0 | 250
            'Replace 100 with new leaf values' | 100        | 1       | true         ||       6.0 | 250
            'Replace 100 with 100 new nodes'   | 100        | 101     | false        ||       6.0 | 250
            'Replace 50 existing and 50 new'   | 100        | 151     | true         ||       6.0 | 250
            'Replace 100 nodes with 0'         | 0          | 1       | false        ||      0.35 | 100
    }

    def 'Replace list content: #scenario.'() {
        given: 'replacement JSON for list of device nodes'
            def jsonListData = generateJsonForOpenRoadmDevices(startId, totalNodes, changeLeaves)
        when: 'the container node is updated'
            resourceMeter.start()
            objectUnderTest.replaceListContent(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR, '/openroadm-devices', jsonListData, now)
            resourceMeter.stop()
        then: 'there are the expected number of totalNodes nodes'
            assert totalNodes == countDataNodes('//openroadm-device')
        and: 'data leaves have expected values'
            assert totalNodes == countDataNodes(changeLeaves? '//openroadm-device[@status="fail"]'
                                                            : '//openroadm-device[@status="success"]')
        then: 'update completes within expected time and memory used is within limit'
            recordAndAssertResourceUsage(scenario,
                    timeLimit, resourceMeter.getTotalTimeInSeconds(),
                    memoryLimit, resourceMeter.getTotalMemoryUsageInMB())
        where:
            scenario                                   | totalNodes | startId | changeLeaves || timeLimit | memoryLimit
            'Replace list of 0 with 100'               | 100        | 1       | false        ||       6.0 | 300
            'Replace list of 100 using same data'      | 100        | 1       | false        ||       6.0 | 300
            'Replace list of 100 with new leaf values' | 100        | 1       | true         ||       6.0 | 300
            'Replace list with 100 new nodes'          | 100        | 101     | false        ||       6.0 | 300
            'Replace list with 50 existing and 50 new' | 100        | 151     | true         ||       6.0 | 300
            'Replace list of 100 nodes with 1'         | 1          | 1       | false        ||      0.35 | 100
    }

    def 'Update leaves for 100 data nodes.'() {
        given: 'there are 200 existing data nodes'
            def jsonListData = generateJsonForOpenRoadmDevices(1, 200, false)
            objectUnderTest.replaceListContent(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR, '/openroadm-devices', jsonListData, now)
        and: 'JSON for updated data leaves of 100 nodes'
            def jsonDataUpdated  = "{'openroadm-device':[" + (1..100).collect {"{'device-id':'C201-7-1A-" + it + "','status':'fail','ne-state':'jeopardy'}" }.join(",") + "]}"
        when: 'update is performed for leaves'
            resourceMeter.start()
            objectUnderTest.updateNodeLeaves(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR, "/openroadm-devices", jsonDataUpdated, now)
            resourceMeter.stop()
        then: 'data leaves have expected values'
            assert 100 == countDataNodes('//openroadm-device[@status="fail"]')
        and: 'update completes within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Update leaves for 100 data nodes',
                    0.5, resourceMeter.getTotalTimeInSeconds(),
                    120, resourceMeter.getTotalMemoryUsageInMB())
    }

    def generateJsonForOpenRoadmDevices(int startId, int total, boolean changeLeaves) {
        return '{ "openroadm-device": [' + (startId..<startId+total).collect {makeInnerNodeJson(it, changeLeaves) }.join(',') + ']}}'
    }

    def makeInnerNodeJson(nodeId, changeLeaf) {
        def nodeJson = INNER_NODE_JSON.replace('NODE_ID_HERE', nodeId.toString())
        if (changeLeaf) {
            nodeJson = nodeJson.replace('"status": "success"', '"status": "fail"')
        }
        return nodeJson
    }

    def countDataNodes(cpsPathQuery) {
        return cpsQueryService.queryDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR, cpsPathQuery, OMIT_DESCENDANTS).size()
    }

}
