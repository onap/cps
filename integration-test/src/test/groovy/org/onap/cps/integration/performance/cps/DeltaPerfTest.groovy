/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Deutsche Telekom AG
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration.performance.cps

import org.onap.cps.api.CpsDeltaService
import org.onap.cps.integration.performance.base.CpsPerfTestBase
import org.onap.cps.utils.ContentType
import spock.lang.Ignore

import static org.onap.cps.api.parameters.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

class DeltaPerfTest extends CpsPerfTestBase{

    CpsDeltaService objectUnderTest

    def setup() {
        objectUnderTest = cpsDeltaService
    }

    def jsonPayload = generateModifiedOpenRoadData(1000, 200, 200, 200)

    def 'Setup source anchor (please note, subsequent tests depend on this running first).'() {
        when: 'anchor is created'
            resourceMeter.start()
            def data = generateOpenRoadData(300)
            addAnchorsWithData(1, CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, 'source-anchor', data, ContentType.JSON)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'setup duration is below accepted margin of the expected average'
            recordAndAssertResourceUsage('CPS: Delta test setup', 20, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB(), false)
    }
    
    def 'Setup target anchor (please note, subsequent tests depend on this running first).'() {
        when: 'anchor with modified node data is created'
            resourceMeter.start()
            def data = generateModifiedOpenRoadData(OPENROADM_DEVICES_PER_ANCHOR, 200, 200, 200)
            addAnchorsWithData(1, CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, 'target-anchor', data, ContentType.JSON)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the anchor is created within expected time'
            recordAndAssertResourceUsage('CPS: Creating modified openroadm anchor', 25, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB(), false)
    }

    def 'Get delta between 2 anchors with grouping enabled and #scenario'() {
        when: 'attempt to get delta between two 2 anchors'
            resourceMeter.start()
            10.times {
                objectUnderTest.getDeltaByDataspaceAndAnchors(CPS_PERFORMANCE_TEST_DATASPACE, 'source-anchor1', 'target-anchor1', xpath, fetchDescendantsOption, true)
            }
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Delta between 2 anchors', expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | xpath                                                             | fetchDescendantsOption  || expectedDuration
            'no descendants'     | '/openroadm-devices/openroadm-device[@device-id=\'C201-7-1A-1\']' | OMIT_DESCENDANTS        || 2.0
            'direct descendants' | '/'                                                               | DIRECT_CHILDREN_ONLY    || 3.0
            'all descendants'    | '/'                                                               | INCLUDE_ALL_DESCENDANTS || 23.0
    }

    @Ignore
    def 'Get delta between 2 anchors with grouping disabled and #scenario'() {
        when: 'attempt to get delta between two 2 anchors'
            resourceMeter.start()
            10.times {
                objectUnderTest.getDeltaByDataspaceAndAnchors(CPS_PERFORMANCE_TEST_DATASPACE, 'source-anchor1', 'target-anchor1', xpath, fetchDescendantsOption, false)
            }
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Delta between 2 anchors, without grouping', expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | xpath                                                             | fetchDescendantsOption  || expectedDuration
            'no descendants'     | '/openroadm-devices/openroadm-device[@device-id=\'C201-7-1A-1\']' | OMIT_DESCENDANTS        || 1.0
            'direct descendants' | '/openroadm-devices'                                              | DIRECT_CHILDREN_ONLY    || 2.0
            'all descendants'    | '/openroadm-devices'                                              | INCLUDE_ALL_DESCENDANTS || 20.0
    }

    @Ignore
    def 'Get delta between an anchor and JSON payload with grouping enabled and #scenario'() {
        when: 'attempt to get delta between an anchor and JSON payload'
            resourceMeter.start()
            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(CPS_PERFORMANCE_TEST_DATASPACE, 'source-anchor1', '/openroadm-devices', Collections.emptyMap(), jsonPayload, fetchDescendantsOption, true)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Delta between anchor and JSON, with grouping', expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  || expectedDuration
            'no descendants'     | OMIT_DESCENDANTS        || 4.0
            'direct descendants' | DIRECT_CHILDREN_ONLY    || 4.0
            'all descendants'    | INCLUDE_ALL_DESCENDANTS || 6.0
    }

    @Ignore
    def 'Get delta between an anchor and JSON payload with grouping disabled and #scenario'() {
        when: 'attempt to get delta between an anchor and JSON payload'
            resourceMeter.start()
            objectUnderTest.getDeltaByDataspaceAnchorAndPayload(CPS_PERFORMANCE_TEST_DATASPACE, 'source-anchor1', '/openroadm-devices', Collections.emptyMap(), jsonPayload, INCLUDE_ALL_DESCENDANTS, false)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is returned and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Delta between anchor and JSON, without grouping', expectedDuration, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
        where: 'the following parameters are used'
            scenario             | fetchDescendantsOption  || expectedDuration
            'no descendants'     | OMIT_DESCENDANTS        || 6.0
            'direct descendants' | DIRECT_CHILDREN_ONLY    || 6.0
            'all descendants'    | INCLUDE_ALL_DESCENDANTS || 7.0
    }

    @Ignore
    def 'Apply delta report to an anchor'() {
        given: 'a delta report between 2 anchors'
            def deltaReport = objectUnderTest.getDeltaByDataspaceAndAnchors(CPS_PERFORMANCE_TEST_DATASPACE, 'target-anchor1', 'source-anchor1', '/openroadm-devices', INCLUDE_ALL_DESCENDANTS, true)
            def deltaReportAsJson = jsonObjectMapper.asJsonString(deltaReport)
        when: 'attempt to apply the delta report to an anchor'
            resourceMeter.start()
            objectUnderTest.applyChangesInDeltaReport(CPS_PERFORMANCE_TEST_DATASPACE, 'target-anchor1', deltaReportAsJson)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the delta is applied and operation completes within expected time'
            recordAndAssertResourceUsage('CPS:Apply delta report to an anchor', 20.0, durationInSeconds, resourceMeter.getTotalMemoryUsageInMB())
    }
    
    def 'Clean up for CPS Delta API'() {
        cleanup: 'test anchors and data nodes'
            def anchors = ['source-anchor1', 'target-anchor1']
            cpsAnchorService.deleteAnchors(CPS_PERFORMANCE_TEST_DATASPACE, anchors)
    }

    def generateModifiedOpenRoadData(numberOfNodes, removeNodesCount, addNodesCount, updateCount) {
        def nodeIds = (1..numberOfNodes).toList()
        def nodeIdsAfterRemove = nodeIds.drop(removeNodesCount)
        def maxNodeId = nodeIdsAfterRemove.isEmpty() ? 1 : (nodeIdsAfterRemove.max() + 1)
        def newNodeIds = (maxNodeId..<(maxNodeId + addNodesCount)).toList()
        def finalNodeIds = nodeIdsAfterRemove + newNodeIds
        def updatedNodes = updateNodes(finalNodeIds, updateCount)
        return '{ "openroadm-devices": { "openroadm-device": [' + updatedNodes.join(',') + ']}}'
    }

    def updateNodes(List<Integer> nodeIds, int updateCount) {
        nodeIds.withIndex().collect { id, idx ->
            def changeLeaves = (idx < updateCount)
            makeInnerNodeJson(id as int, changeLeaves)
        }
    }

    def makeInnerNodeJson(int nodeId, boolean changeLeaf) {
        def innerNodeJson = readResourceDataFile('openroadm/innerNode.json')
        def nodeJson = innerNodeJson.replace('NODE_ID_HERE', nodeId.toString())
        if (changeLeaf) {
            nodeJson = nodeJson.replace('"status": "success"', '"status": "fail"')
        }
        return nodeJson
    }
}
