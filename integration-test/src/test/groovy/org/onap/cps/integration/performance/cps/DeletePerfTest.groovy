/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
 *  Modifications Copyright (C) 2025 TechMahindra Ltd.
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

import org.onap.cps.api.exceptions.DataNodeNotFoundException
import org.onap.cps.utils.ContentType
import java.time.OffsetDateTime
import org.onap.cps.api.CpsDataService
import org.onap.cps.integration.performance.base.CpsPerfTestBase

class DeletePerfTest extends CpsPerfTestBase {

    CpsDataService objectUnderTest

    def setup() { objectUnderTest = cpsDataService }

    def 'Create test data (please note, subsequent tests depend on this running first).'() {
        when: 'multiple anchors with a node with a large number of descendants is created'
            resourceMeter.start()
            def data = generateOpenRoadData(300)
            addAnchorsWithData(10, CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, 'delete', data, ContentType.JSON)
            resourceMeter.stop()
            def setupDurationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'setup duration is within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Delete test setup', 103, setupDurationInSeconds, 800, resourceMeter.getTotalMemoryUsageInMB())
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
            def deleteDurationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Delete 100 containers', 3.9, deleteDurationInSeconds, 20, resourceMeter.getTotalMemoryUsageInMB())
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
            def deleteDurationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Batch delete 100 containers', 0.87, deleteDurationInSeconds, 2, resourceMeter.getTotalMemoryUsageInMB())
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
            def deleteDurationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Delete 100 lists elements', 4.22, deleteDurationInSeconds, 20, resourceMeter.getTotalMemoryUsageInMB())
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
            def deleteDurationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Batch delete 100 lists elements', 0.87, deleteDurationInSeconds, 2, resourceMeter.getTotalMemoryUsageInMB())
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
            def deleteDurationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Delete 100 whole lists', 5.4, deleteDurationInSeconds, 20, resourceMeter.getTotalMemoryUsageInMB())
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
            def deleteDurationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Batch delete 100 whole lists', 1.98, deleteDurationInSeconds, 3, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Delete 1 large data node'() {
        when: 'parent node is deleted'
            resourceMeter.start()
            objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete7', '/openroadm-devices', OffsetDateTime.now())
            resourceMeter.stop()
            def deleteDurationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Delete one large node', 2.35, deleteDurationInSeconds, 1, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Delete root node with many descendants'() {
        when: 'root node is deleted'
            resourceMeter.start()
            objectUnderTest.deleteDataNode(CPS_PERFORMANCE_TEST_DATASPACE, 'delete8', '/', OffsetDateTime.now())
            resourceMeter.stop()
            def deleteDurationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Delete root node', 2.23, deleteDurationInSeconds, 1, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Delete data nodes for an anchor'() {
        when: 'data nodes are deleted'
            resourceMeter.start()
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'delete9', OffsetDateTime.now())
            resourceMeter.stop()
            def deleteDurationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Delete data nodes for anchor', 2.25, deleteDurationInSeconds, 1, resourceMeter.getTotalMemoryUsageInMB())
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
            def deleteDurationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Batch delete 100 non-existing', 0.74, deleteDurationInSeconds, 3, resourceMeter.getTotalMemoryUsageInMB())
    }

    def 'Clean up test data'() {
        given: 'a list of anchors to delete'
            def anchorNames = (1..10).collect {'delete' + it}
        when: 'data nodes are deleted'
            resourceMeter.start()
            cpsAnchorService.deleteAnchors(CPS_PERFORMANCE_TEST_DATASPACE, anchorNames)
            resourceMeter.stop()
            def deleteDurationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'delete duration is within expected time and memory used is within limit'
            recordAndAssertResourceUsage('Delete test cleanup', 11.09, deleteDurationInSeconds, 1, resourceMeter.getTotalMemoryUsageInMB())
    }

}
