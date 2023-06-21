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

class UpdatePerfTest extends CpsPerfTestBase {

    CpsDataService objectUnderTest
    def now = OffsetDateTime.now()

    def setup() { objectUnderTest = cpsDataService }

    def 'Update 1 data node with descendants'() {
        given: 'a list of data nodes to update as JSON'
            def parentNodeXpath = "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-10']"
            def jsonData = readResourceDataFile('openroadm/innerNode.json').replace('NODE_ID_HERE', '10')
        when: 'the fragment entities are updated by the data nodes'
            stopWatch.start()
            objectUnderTest.updateDataNodeAndDescendants(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm1', parentNodeXpath, jsonData, now)
            stopWatch.stop()
            def updateDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'update duration is under 1000 milliseconds'
            recordAndAssertPerformance('Update 1 data node', 600, updateDurationInMillis)
    }

    def 'Batch update 10 data nodes with descendants'() {
        given: 'a list of data nodes to update as JSON'
            def innerNodeJson = readResourceDataFile('openroadm/innerNode.json')
            def nodesJsonData = (20..30).collectEntries {[
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']",
                innerNodeJson.replace('NODE_ID_HERE', it.toString())
            ]}
        when: 'the fragment entities are updated by the data nodes'
            stopWatch.start()
            objectUnderTest.updateDataNodesAndDescendants(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm2', nodesJsonData, now)
            stopWatch.stop()
            def updateDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'update duration is under 5000 milliseconds'
            recordAndAssertPerformance('Update 10 data nodes', 4000, updateDurationInMillis)
    }

    def 'Update leaves for 1 data node'() {
        given: 'Updated json for openroadm data'
            def jsonDataUpdated  = "{'openroadm-device':{'device-id':'C201-7-1A-10','status':'fail','ne-state':'jeopardy'}}"
            def jsonDataOriginal = "{'openroadm-device':{'device-id':'C201-7-1A-10','status':'success','ne-state':'inservice'}}"
        when: 'update is performed for leaves'
            stopWatch.start()
            objectUnderTest.updateNodeLeaves(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm3', "/openroadm-devices", jsonDataUpdated, now)
            objectUnderTest.updateNodeLeaves(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm3', "/openroadm-devices", jsonDataOriginal, now)
            stopWatch.stop()
            def updateDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'update duration is under 750 milliseconds'
            recordAndAssertPerformance('Update leaves for 1 data node', 750, updateDurationInMillis)
    }

    def 'Batch update leaves for 50 data nodes'() {
        given: 'Updated json for openroadm data'
            def jsonDataUpdated  = "{'openroadm-device':[" + (1..50).collect { "{'device-id':'C201-7-1A-" + it + "','status':'fail','ne-state':'jeopardy'}" }.join(",") + "]}"
            def jsonDataOriginal = "{'openroadm-device':[" + (1..50).collect { "{'device-id':'C201-7-1A-" + it + "','status':'success','ne-state':'inservice'}" }.join(",") + "]}"
        when: 'update is performed for leaves'
            stopWatch.start()
            objectUnderTest.updateNodeLeaves(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm4', "/openroadm-devices", jsonDataUpdated, now)
            objectUnderTest.updateNodeLeaves(CPS_PERFORMANCE_TEST_DATASPACE, 'openroadm4', "/openroadm-devices", jsonDataOriginal, now)
            stopWatch.stop()
            def updateDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'update duration is under 3500 milliseconds'
            recordAndAssertPerformance('Batch update leaves for 50 data nodes', 3500, updateDurationInMillis)
    }

}
