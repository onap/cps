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
            resourceMeter.start()
            def data = generateOpenRoadData(300)
            addAnchorsWithData(10, CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, 'delete', data)
            resourceMeter.stop()
            def setupDurationInMillis = resourceMeter.getTotalTimeMillis()
        then: 'setup duration is within expected time and memory used is within limit'
            recordAndAssertPerformance('Delete test setup', TimeUnit.SECONDS.toMillis(200), setupDurationInMillis, 200, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Delete 100 container nodes'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']/org-openroadm-device"
            }
        when: 'child nodes are deleted'
            resourceMeter.start()
            xpathsToDelete.each {
                objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete1', it, OffsetDateTime.now())
            }
            resourceMeter.stop()
            def deleteDurationInMillis = resourceMeter.getTotalTimeMillis()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertPerformance('Delete 100 containers', TimeUnit.SECONDS.toMillis(2), deleteDurationInMillis, 30, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Batch delete 100 container nodes'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']/org-openroadm-device"
            }
        when: 'child nodes are deleted'
            resourceMeter.start()
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete2', xpathsToDelete, OffsetDateTime.now())
            resourceMeter.stop()
            def deleteDurationInMillis = resourceMeter.getTotalTimeMillis()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertPerformance('Batch delete 100 containers', 500, deleteDurationInMillis, 5, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Delete 100 list elements'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']"
            }
        when: 'list elements are deleted'
            resourceMeter.start()
            xpathsToDelete.each {
                objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete3', it, OffsetDateTime.now())
            }
            resourceMeter.stop()
            def deleteDurationInMillis = resourceMeter.getTotalTimeMillis()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertPerformance('Delete 100 lists elements', TimeUnit.SECONDS.toMillis(2), deleteDurationInMillis, 20, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Batch delete 100 list elements'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']"
            }
        when: 'list elements are deleted'
            resourceMeter.start()
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete4', xpathsToDelete, OffsetDateTime.now())
            resourceMeter.stop()
            def deleteDurationInMillis = resourceMeter.getTotalTimeMillis()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertPerformance('Batch delete 100 lists elements', 500, deleteDurationInMillis, 2, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Delete 100 whole lists'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']/org-openroadm-device/degree"
            }
        when: 'lists are deleted'
            resourceMeter.start()
            xpathsToDelete.each {
                objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete5', it, OffsetDateTime.now())
            }
            resourceMeter.stop()
            def deleteDurationInMillis = resourceMeter.getTotalTimeMillis()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertPerformance('Delete 100 whole lists', TimeUnit.SECONDS.toMillis(5), deleteDurationInMillis, 30, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Batch delete 100 whole lists'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect {
                "/openroadm-devices/openroadm-device[@device-id='C201-7-1A-" + it + "']/org-openroadm-device/degree"
            }
        when: 'lists are deleted'
            resourceMeter.start()
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete6', xpathsToDelete, OffsetDateTime.now())
            resourceMeter.stop()
            def deleteDurationInMillis = resourceMeter.getTotalTimeMillis()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertPerformance('Batch delete 100 whole lists', TimeUnit.SECONDS.toMillis(4), deleteDurationInMillis, 5, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Delete 1 large data node'() {
        when: 'parent node is deleted'
            resourceMeter.start()
            objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete7', '/openroadm-devices', OffsetDateTime.now())
            resourceMeter.stop()
            def deleteDurationInMillis = resourceMeter.getTotalTimeMillis()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertPerformance('Delete one large node', TimeUnit.SECONDS.toMillis(2), deleteDurationInMillis, 2, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Delete root node with many descendants'() {
        when: 'root node is deleted'
            resourceMeter.start()
            objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete8', '/', OffsetDateTime.now())
            resourceMeter.stop()
            def deleteDurationInMillis = resourceMeter.getTotalTimeMillis()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertPerformance('Delete root node', TimeUnit.SECONDS.toMillis(2), deleteDurationInMillis, 2, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Delete data nodes for an anchor'() {
        when: 'data nodes are deleted'
            resourceMeter.start()
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete9', OffsetDateTime.now())
            resourceMeter.stop()
            def deleteDurationInMillis = resourceMeter.getTotalTimeMillis()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertPerformance('Delete data nodes for anchor', TimeUnit.SECONDS.toMillis(2), deleteDurationInMillis, 2, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Batch delete 100 non-existing nodes'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (1..100).collect { "/path/to/non-existing/node[@id='" + it + "']" }
        when: 'child nodes are deleted'
            resourceMeter.start()
            try {
                objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete10', xpathsToDelete, OffsetDateTime.now())
            } catch (DataNodeNotFoundException ignored) {}
            resourceMeter.stop()
            def deleteDurationInMillis = resourceMeter.getTotalTimeMillis()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertPerformance('Batch delete 100 non-existing', TimeUnit.SECONDS.toMillis(7), deleteDurationInMillis, 5, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Clean up test data'() {
        given: 'a list of anchors to delete'
            def anchorNames = (1..10).collect {'delete' + it}
        when: 'data nodes are deleted'
            resourceMeter.start()
            cpsAdminService.deleteAnchors(CPS_PERFORMANCE_TEST_DATASPACE, anchorNames)
            resourceMeter.stop()
            def deleteDurationInMillis = resourceMeter.getTotalTimeMillis()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertPerformance('Delete test cleanup', TimeUnit.SECONDS.toMillis(10), deleteDurationInMillis, 2, resourceMeter.getTotalMemoryUsageInMB())
    }

}
