/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
 *  Modifications Copyright (C) 2024 TechMahindra Ltd.
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
import org.onap.cps.utils.ContentType
import org.onap.cps.integration.performance.base.CpsPerfTestBase

import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

class UpdatePerfTest extends CpsPerfTestBase {
    static final def UPDATE_TEST_ANCHOR = 'updateTestAnchor'
    static final def INNER_NODE_JSON = readResourceDataFile('openroadm/innerNode.json')

    CpsDataService objectUnderTest
    def now = OffsetDateTime.now()

    def setup() { objectUnderTest = cpsDataService }

    def 'Test setup for CPS Update API.'() {
        given: 'an anchor and empty container node for OpenROADM devices'
            cpsAnchorService.createAnchor(CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, UPDATE_TEST_ANCHOR)
            cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR,
                    '{ "openroadm-devices": { "openroadm-device": []}}', now)
        expect: 'no device nodes exist yet'
            assert 0 == countDataNodes('/openroadm-devices/openroadm-device')
    }

    def 'JVM warm up for update tests: #scenario.'() {
        given: 'replacement JSON for node containing list of device nodes'
            def jsonData = '{ "openroadm-devices": ' + generateJsonForOpenRoadmDevices(startId, totalNodes, changeLeaves) + '}'
        when: 'the container node is updated'
            objectUnderTest.updateDataNodeAndDescendants(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR, '/', jsonData, now, ContentType.JSON)
        then: 'there are the expected number of total nodes'
            assert totalNodes == countDataNodes('/openroadm-devices/openroadm-device')
        where:
            scenario                           | totalNodes | startId | changeLeaves
            'Replace 0 nodes with 100'         | 100        | 1       | false
            'Replace 100 using same data'      | 100        | 1       | false
            'Replace 100 with new leaf values' | 100        | 1       | true
            'Replace 100 with 100 new nodes'   | 100        | 101     | false
            'Replace 50 existing and 50 new'   | 100        | 151     | true
            'Replace 100 nodes with 0'         | 0          | 1       | false
    }

    def 'Replace single data node and descendants: #scenario.'() {
        given: 'replacement JSON for node containing list of device nodes'
            def jsonData = '{ "openroadm-devices": ' + generateJsonForOpenRoadmDevices(startId, totalNodes, changeLeaves) + '}'
        when: 'the container node is updated'
            resourceMeter.start()
            objectUnderTest.updateDataNodeAndDescendants(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR, '/', jsonData, now, ContentType.JSON)
            resourceMeter.stop()
        then: 'there are the expected number of total nodes'
            assert totalNodes == countDataNodes('/openroadm-devices/openroadm-device')
        and: 'data leaves have expected values'
            assert totalNodes == countDataNodes(changeLeaves? '/openroadm-devices/openroadm-device[@status="fail"]'
                                                            : '/openroadm-devices/openroadm-device[@status="success"]')
        and: 'update completes within expected time and memory used is within limit'
            recordAndAssertResourceUsage(scenario,
                    timeLimit, resourceMeter.getTotalTimeInSeconds(),
                    memoryLimit, resourceMeter.getTotalMemoryUsageInMB())
        where:
            scenario                           | totalNodes | startId | changeLeaves || timeLimit   | memoryLimit
            'Replace 0 nodes with 100'         | 100        | 1       | false        ||       3.99  | 200
            'Replace 100 using same data'      | 100        | 1       | false        ||       7.46  | 200
            'Replace 100 with new leaf values' | 100        | 1       | true         ||       7.87  | 200
            'Replace 100 with 100 new nodes'   | 100        | 101     | false        ||       13.85 | 200
            'Replace 50 existing and 50 new'   | 100        | 151     | true         ||       10.82 | 200
            'Replace 100 nodes with 0'         | 0          | 1       | false        ||       8.91  | 200
    }

    def 'Replace list content: #scenario.'() {
        given: 'replacement JSON for list of device nodes'
            def jsonListData = generateJsonForOpenRoadmDevices(startId, totalNodes, changeLeaves)
        when: 'the container node is updated'
            resourceMeter.start()
            objectUnderTest.replaceListContent(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR, '/openroadm-devices', jsonListData, now, ContentType.JSON)
            resourceMeter.stop()
        then: 'there are the expected number of total nodes'
            assert totalNodes == countDataNodes('/openroadm-devices/openroadm-device')
        and: 'data leaves have expected values'
            assert totalNodes == countDataNodes(changeLeaves? '/openroadm-devices/openroadm-device[@status="fail"]'
                                                            : '/openroadm-devices/openroadm-device[@status="success"]')
        and: 'update completes within expected time and memory used is within limit'
            recordAndAssertResourceUsage(scenario,
                    timeLimit, resourceMeter.getTotalTimeInSeconds(),
                    memoryLimit, resourceMeter.getTotalMemoryUsageInMB())
        where:
            scenario                                   | totalNodes | startId | changeLeaves || timeLimit   | memoryLimit
            'Replace list of 0 with 100'               | 100        | 1       | false        ||       4.01  | 200
            'Replace list of 100 using same data'      | 100        | 1       | false        ||       5.53  | 200
            'Replace list of 100 with new leaf values' | 100        | 1       | true         ||       6.96  | 200
            'Replace list with 100 new nodes'          | 100        | 101     | false        ||       12.82 | 200
            'Replace list with 50 existing and 50 new' | 100        | 151     | true         ||       10.42 | 200
            'Replace list of 100 nodes with 1'         | 1          | 1       | false        ||       9.26  | 200
    }

    def 'Update leaves for 100 data nodes.'() {
        given: 'there are 200 existing data nodes'
            def jsonListData = generateJsonForOpenRoadmDevices(1, 200, false)
            objectUnderTest.replaceListContent(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR, '/openroadm-devices', jsonListData, now, ContentType.JSON)
        and: 'JSON for updated data leaves of 100 nodes'
            def jsonDataUpdated  = "{'openroadm-device':[" + (1..100).collect {"{'device-id':'C201-7-1A-" + it + "','status':'fail','ne-state':'jeopardy'}" }.join(",") + "]}"
        when: 'update is performed for leaves'
            resourceMeter.start()
            objectUnderTest.updateNodeLeaves(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR, "/openroadm-devices", jsonDataUpdated, now, ContentType.JSON)
            resourceMeter.stop()
        then: 'data leaves have expected values'
            assert 100 == countDataNodes('/openroadm-devices/openroadm-device[@status="fail"]')
        and: 'update completes within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Update leaves for 100 data nodes',
                    0.35, resourceMeter.getTotalTimeInSeconds(),
                    120, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Clean up for CPS Update API.'() {
        cleanup: 'test anchor and data nodes'
            cpsAnchorService.deleteAnchor(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR)
    }

    def generateJsonForOpenRoadmDevices(int startId, int totalNodes, boolean changeLeaves) {
        return '{ "openroadm-device": [' + (0..<totalNodes).collect {makeInnerNodeJson(it + startId, changeLeaves) }.join(',') + ']}}'
    }

    def makeInnerNodeJson(int nodeId, boolean changeLeaf) {
        def nodeJson = INNER_NODE_JSON.replace('NODE_ID_HERE', nodeId.toString())
        if (changeLeaf) {
            nodeJson = nodeJson.replace('"status": "success"', '"status": "fail"')
        }
        return nodeJson
    }

    def countDataNodes(String cpsPathQuery) {
        return cpsQueryService.queryDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, UPDATE_TEST_ANCHOR, cpsPathQuery, OMIT_DESCENDANTS).size()
    }

}
