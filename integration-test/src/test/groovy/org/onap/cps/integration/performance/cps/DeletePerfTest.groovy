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

import org.onap.cps.spi.exceptions.DataNodeNotFoundException

import java.time.OffsetDateTime
import org.onap.cps.api.CpsDataService
import org.onap.cps.integration.performance.base.CpsPerfTestBase

class DeletePerfTest extends CpsPerfTestBase {

    CpsDataService objectUnderTest

    def setup() { objectUnderTest = cpsDataService }

    def 'Create test data (please note, subsequent tests depend on this running first).'() {
        when: 'multiple anchors with a node with a large number of descendants is created'
            stopWatch.start()
            def data = generateOpenRoadData(50)
            addAnchorsWithData(10, CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, 'delete', data)
            stopWatch.stop()
            def setupDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'setup duration is under 40 seconds'
            recordAndAssertPerformance('Delete test setup', 40_000, setupDurationInMillis)
    }

    def 'Delete 10 container nodes'() {
        when: 'child nodes are deleted'
            stopWatch.start()
            (1..10).each {
                def childPath = "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']/org-openroadm-device"
                objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete1', childPath, OffsetDateTime.now())
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 300 milliseconds'
            recordAndAssertPerformance('Delete 10 containers', 300, deleteDurationInMillis)
    }

    def 'Batch delete 50 container nodes'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..50).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']/org-openroadm-device"
            }
        when: 'child nodes are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete2', xpathsToDelete, OffsetDateTime.now())
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 300 milliseconds'
            recordAndAssertPerformance('Batch delete 50 containers', 300, deleteDurationInMillis)
    }

    def 'Delete 20 list elements'() {
        when: 'list elements are deleted'
            stopWatch.start()
            (1..20).each {
                def listElementXpath = "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-1']/org-openroadm-device/degree[@degree-number=" + it + "]"
                objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete3', listElementXpath, OffsetDateTime.now())
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 300 milliseconds'
            recordAndAssertPerformance('Delete 20 lists elements', 300, deleteDurationInMillis)
    }

    def 'Batch delete 1000 list elements'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = []
            for (int childIndex = 1; childIndex <= 50; childIndex++) {
                xpathsToDelete.addAll((1..20).collect {
                    "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-${childIndex}']/org-openroadm-device/degree[@degree-number=${it}]".toString()
                })
            }
        when: 'list elements are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete4', xpathsToDelete, OffsetDateTime.now())
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 300 milliseconds'
            recordAndAssertPerformance('Batch delete 1000 lists elements', 300, deleteDurationInMillis)
    }

    def 'Delete 10 whole lists'() {
        when: 'lists are deleted'
            stopWatch.start()
            (1..10).each {
                def childPath = "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']/org-openroadm-device/degree"
                objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete5', childPath, OffsetDateTime.now())
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 300 milliseconds'
            recordAndAssertPerformance('Delete 10 whole lists', 300, deleteDurationInMillis)
    }

    def 'Batch delete 30 whole lists'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..30).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']/org-openroadm-device/degree"
            }
        when: 'lists are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete6', xpathsToDelete, OffsetDateTime.now())
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 300 milliseconds'
            recordAndAssertPerformance('Batch delete 30 whole lists', 300, deleteDurationInMillis)
    }

    def 'Delete 1 large data node'() {
        when: 'parent node is deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete7', '/openroadm-devices', OffsetDateTime.now())
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 300 milliseconds'
            recordAndAssertPerformance('Delete one large node', 300, deleteDurationInMillis)
    }

    def 'Delete root node with many descendants'() {
        when: 'root node is deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete8', '/', OffsetDateTime.now())
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 300 milliseconds'
            recordAndAssertPerformance('Delete root node', 300, deleteDurationInMillis)
    }

    def 'Delete data nodes for an anchor'() {
        when: 'data nodes are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete9', OffsetDateTime.now())
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 300 milliseconds'
            recordAndAssertPerformance('Delete data nodes for anchor', 300, deleteDurationInMillis)
    }

    def 'Batch delete 100 non-existing nodes'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect { "/path/to/non-existing/node[@id='" + it + "']" }
        when: 'child nodes are deleted'
            stopWatch.start()
            try {
                objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete10', xpathsToDelete, OffsetDateTime.now())
            } catch (DataNodeNotFoundException ignored) {}
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 300 milliseconds'
            recordAndAssertPerformance('Batch delete 100 non-existing', 300, deleteDurationInMillis)
    }

    def 'Clean up test data'() {
        given: 'a list of anchors to delete'
            def anchorNames = (1..10).collect {'delete' + it}
        when: 'data nodes are deleted'
            stopWatch.start()
            cpsAdminService.deleteAnchors(CPS_PERFORMANCE_TEST_DATASPACE, anchorNames)
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 1000 milliseconds'
            recordAndAssertPerformance('Delete test cleanup', 1000, deleteDurationInMillis)
    }

}
