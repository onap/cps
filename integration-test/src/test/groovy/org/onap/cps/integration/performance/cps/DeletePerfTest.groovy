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

import java.util.concurrent.TimeUnit

class DeletePerfTest extends CpsPerfTestBase {

    CpsDataService objectUnderTest

    def setup() { objectUnderTest = cpsDataService }

    def 'Create test data (please note, subsequent tests depend on this running first).'() {
        when: 'multiple anchors with a node with a large number of descendants is created'
            stopWatch.start()
            def data = generateOpenRoadData(300)
            addAnchorsWithData(10, CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, 'delete', data)
            stopWatch.stop()
            def setupDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'setup duration is within expected time'
            recordAndAssertPerformance('Delete test setup', TimeUnit.SECONDS.toMillis(75), setupDurationInMillis)
    }

    def 'Delete 100 container nodes'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']/org-openroadm-device"
            }
        when: 'child nodes are deleted'
            stopWatch.start()
            xpathsToDelete.each {
                objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete1', it, OffsetDateTime.now())
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is within expected time'
            recordAndAssertPerformance('Delete 100 containers', TimeUnit.SECONDS.toMillis(2), deleteDurationInMillis)
    }

    def 'Batch delete 100 container nodes'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']/org-openroadm-device"
            }
        when: 'child nodes are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete2', xpathsToDelete, OffsetDateTime.now())
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is within expected time'
            recordAndAssertPerformance('Batch delete 100 containers', 500, deleteDurationInMillis)
    }

    def 'Delete 100 list elements'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']"
            }
        when: 'list elements are deleted'
            stopWatch.start()
            xpathsToDelete.each {
                objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete3', it, OffsetDateTime.now())
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is within expected time'
            recordAndAssertPerformance('Delete 100 lists elements', TimeUnit.SECONDS.toMillis(2), deleteDurationInMillis)
    }

    def 'Batch delete 100 list elements'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']"
            }
        when: 'list elements are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete4', xpathsToDelete, OffsetDateTime.now())
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is within expected time'
            recordAndAssertPerformance('Batch delete 100 lists elements', 500, deleteDurationInMillis)
    }

    def 'Delete 100 whole lists'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']/org-openroadm-device/degree"
            }
        when: 'lists are deleted'
            stopWatch.start()
            xpathsToDelete.each {
                objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete5', it, OffsetDateTime.now())
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is within expected time'
            recordAndAssertPerformance('Delete 100 whole lists', TimeUnit.SECONDS.toMillis(5), deleteDurationInMillis)
    }

    def 'Batch delete 100 whole lists'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']/org-openroadm-device/degree"
            }
        when: 'lists are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete6', xpathsToDelete, OffsetDateTime.now())
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is within expected time'
            recordAndAssertPerformance('Batch delete 100 whole lists', TimeUnit.SECONDS.toMillis(4), deleteDurationInMillis)
    }

    def 'Delete 1 large data node'() {
        when: 'parent node is deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete7', '/openroadm-devices', OffsetDateTime.now())
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is within expected time'
            recordAndAssertPerformance('Delete one large node', TimeUnit.SECONDS.toMillis(2), deleteDurationInMillis)
    }

    def 'Delete root node with many descendants'() {
        when: 'root node is deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete8', '/', OffsetDateTime.now())
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is within expected time'
            recordAndAssertPerformance('Delete root node', TimeUnit.SECONDS.toMillis(2), deleteDurationInMillis)
    }

    def 'Delete data nodes for an anchor'() {
        when: 'data nodes are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete9', OffsetDateTime.now())
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is within expected time'
            recordAndAssertPerformance('Delete data nodes for anchor', TimeUnit.SECONDS.toMillis(2), deleteDurationInMillis)
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
        then: 'delete duration is within expected time'
            recordAndAssertPerformance('Batch delete 100 non-existing', TimeUnit.SECONDS.toMillis(7), deleteDurationInMillis)
    }

    def 'Clean up test data'() {
        given: 'a list of anchors to delete'
            def anchorNames = (1..10).collect {'delete' + it}
        when: 'data nodes are deleted'
            stopWatch.start()
            cpsAdminService.deleteAnchors(CPS_PERFORMANCE_TEST_DATASPACE, anchorNames)
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is within expected time'
            recordAndAssertPerformance('Delete test cleanup', TimeUnit.SECONDS.toMillis(10), deleteDurationInMillis)
    }

}
