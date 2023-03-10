/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.performance

import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.impl.CpsPersistencePerfSpecBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

class CpsDataPersistenceServiceDeletePerfTest extends CpsPersistencePerfSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Create a node with many descendants (please note, subsequent tests depend on this running first).'() {
        when: 'a node with a large number of descendants is created'
            stopWatch.start()
            createLineage(objectUnderTest, 150, 50, false)
            stopWatch.stop()
            def setupDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'setup duration is under 10 seconds'
            recordAndAssertPerformance('Setup', 10_000, setupDurationInMillis)
    }

    def 'Delete 5 children with grandchildren'() {
        when: 'child nodes are deleted'
            stopWatch.start()
            (1..5).each {
                def childPath = "${PERF_TEST_PARENT}/perf-test-child-${it}".toString()
                objectUnderTest.deleteDataNode(PERF_DATASPACE, PERF_ANCHOR, childPath)
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 150 milliseconds'
            recordAndAssertPerformance('Delete 5 children', 150, deleteDurationInMillis)
    }

    def 'Batch delete 100 children with grandchildren'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (6..105).collect {
                "${PERF_TEST_PARENT}/perf-test-child-${it}".toString()
            }
        when: 'child nodes are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(PERF_DATASPACE, PERF_ANCHOR, xpathsToDelete)
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 250 milliseconds'
            recordAndAssertPerformance('Batch delete 100 children', 250, deleteDurationInMillis)
    }

    def 'Delete 50 grandchildren (that have no descendants)'() {
        when: 'target nodes are deleted'
            stopWatch.start()
            (1..50).each {
                def grandchildPath = "${PERF_TEST_PARENT}/perf-test-child-106/perf-test-grand-child-${it}".toString()
                objectUnderTest.deleteDataNode(PERF_DATASPACE, PERF_ANCHOR, grandchildPath)
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 600 milliseconds'
            recordAndAssertPerformance('Delete 50 grandchildren', 600, deleteDurationInMillis)
    }

    def 'Batch delete 500 grandchildren (that have no descendants)'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = []
            for (int childIndex = 0; childIndex < 10; childIndex++) {
                xpathsToDelete.addAll((1..50).collect {
                    "${PERF_TEST_PARENT}/perf-test-child-${107+childIndex}/perf-test-grand-child-${it}".toString()
                })
            }
        when: 'target nodes are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(PERF_DATASPACE, PERF_ANCHOR, xpathsToDelete)
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 100 milliseconds'
            recordAndAssertPerformance('Batch delete 500 grandchildren', 100, deleteDurationInMillis)
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Create a node with many list elements (please note, subsequent tests depend on this running first).'() {
        when: 'a node with a large number of lists is created'
            stopWatch.start()
            createLineage(objectUnderTest, 150, 50, true)
            stopWatch.stop()
            def setupDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'setup duration is under 6 seconds'
            recordAndAssertPerformance('Setup lists', 6_000, setupDurationInMillis)
    }

    def 'Delete 5 whole lists'() {
        when: 'lists are deleted'
            stopWatch.start()
            (1..5).each {
                def childPath = "${PERF_TEST_PARENT}/perf-test-list-${it}".toString()
                objectUnderTest.deleteListDataNode(PERF_DATASPACE, PERF_ANCHOR, childPath)
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 130 milliseconds'
            recordAndAssertPerformance('Delete 5 whole lists', 130, deleteDurationInMillis)
    }

    def 'Batch delete 100 whole lists'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = (6..105).collect {
                "${PERF_TEST_PARENT}/perf-test-list-${it}".toString()
            }
        when: 'lists are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(PERF_DATASPACE, PERF_ANCHOR, xpathsToDelete)
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 600 milliseconds'
            recordAndAssertPerformance('Batch delete 100 whole lists', 600, deleteDurationInMillis)
    }

    def 'Delete 10 list elements'() {
        when: 'list elements are deleted'
            stopWatch.start()
            (1..10).each {
                def grandchildPath = "${PERF_TEST_PARENT}/perf-test-list-106[@key='${it}']".toString()
                objectUnderTest.deleteListDataNode(PERF_DATASPACE, PERF_ANCHOR, grandchildPath)
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 180 milliseconds'
            recordAndAssertPerformance('Delete 10 lists elements', 180, deleteDurationInMillis)
    }

    def 'Batch delete 500 list elements'() {
        given: 'a list of xpaths to delete'
            def xpathsToDelete = []
            for (int childIndex = 0; childIndex < 10; childIndex++) {
                xpathsToDelete.addAll((1..50).collect {
                    "${PERF_TEST_PARENT}/perf-test-list-${107+childIndex}[@key='${it}']".toString()
                })
            }
        when: 'list elements are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(PERF_DATASPACE, PERF_ANCHOR, xpathsToDelete)
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 70 milliseconds'
            recordAndAssertPerformance('Batch delete 500 lists elements', 70, deleteDurationInMillis)
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Delete 1 large data node'() {
        given: 'a node with a large number of descendants is created'
            createLineage(objectUnderTest, 50, 50, false)
            createLineage(objectUnderTest, 50, 50, true)
        when: 'parent node is deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNode(PERF_DATASPACE, PERF_ANCHOR, PERF_TEST_PARENT)
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 220 milliseconds'
            recordAndAssertPerformance('Delete one large node', 220, deleteDurationInMillis)
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Batch delete 1 large data node'() {
        given: 'a node with a large number of descendants is created'
            createLineage(objectUnderTest, 50, 50, false)
            createLineage(objectUnderTest, 50, 50, true)
        when: 'parent node is batch deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(PERF_DATASPACE, PERF_ANCHOR, [PERF_TEST_PARENT])
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 220 milliseconds'
            recordAndAssertPerformance('Batch delete one large node', 220, deleteDurationInMillis)
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Delete root node with many descendants'() {
        given: 'a node with a large number of descendants is created'
            createLineage(objectUnderTest, 50, 50, false)
            createLineage(objectUnderTest, 50, 50, true)
        when: 'root node is deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNode(PERF_DATASPACE, PERF_ANCHOR, '/')
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 300 milliseconds'
            recordAndAssertPerformance('Delete root node', 300, deleteDurationInMillis)
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Delete data nodes for an anchor'() {
        given: 'a node with a large number of descendants is created'
            createLineage(objectUnderTest, 50, 50, false)
            createLineage(objectUnderTest, 50, 50, true)
        when: 'data nodes are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(PERF_DATASPACE, PERF_ANCHOR)
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 300 milliseconds'
            recordAndAssertPerformance('Delete data nodes for anchor', 300, deleteDurationInMillis)
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Delete data nodes for multiple anchors'() {
        given: 'a node with a large number of descendants is created'
            createLineage(objectUnderTest, 50, 50, false)
            createLineage(objectUnderTest, 50, 50, true)
        when: 'data nodes are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(PERF_DATASPACE, [PERF_ANCHOR])
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 300 milliseconds'
            recordAndAssertPerformance('Delete data nodes for anchors', 300, deleteDurationInMillis)
    }

}
