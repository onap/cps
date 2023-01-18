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
import org.springframework.util.StopWatch

import java.util.concurrent.TimeUnit

class CpsDataPersistenceServiceDeletePerfTest extends CpsPersistencePerfSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    static def NUMBER_OF_CHILDREN = 100
    static def NUMBER_OF_GRAND_CHILDREN = 50
    static def NUMBER_OF_LISTS = 100
    static def NUMBER_OF_LIST_ELEMENTS = 50
    static def ALLOWED_SETUP_TIME_MS = TimeUnit.SECONDS.toMillis(10)

    def stopWatch = new StopWatch()

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Create a node with many descendants (please note, subsequent tests depend on this running first).'() {
        given: 'a node with a large number of descendants is created'
            stopWatch.start()
            createLineage(objectUnderTest, NUMBER_OF_CHILDREN, NUMBER_OF_GRAND_CHILDREN, false)
            stopWatch.stop()
            def setupDurationInMillis = stopWatch.getTotalTimeMillis()
        and: 'setup duration is under #ALLOWED_SETUP_TIME_MS milliseconds'
            assert setupDurationInMillis < ALLOWED_SETUP_TIME_MS
    }

    def 'Delete 5 children with grandchildren'() {
        when: 'child nodes are deleted'
            stopWatch.start()
            (1..5).each {
                def childPath = "${PERF_TEST_PARENT}/perf-test-child-${it}".toString();
                objectUnderTest.deleteDataNode(PERF_DATASPACE, PERF_ANCHOR, childPath)
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 1000 milliseconds'
            assert deleteDurationInMillis < 1000
    }

    def 'Delete 50 grandchildren (that have no descendants)'() {
        when: 'target nodes are deleted'
            stopWatch.start()
            (1..50).each {
                def grandchildPath = "${PERF_TEST_PARENT}/perf-test-child-6/perf-test-grand-child-${it}".toString();
                objectUnderTest.deleteDataNode(PERF_DATASPACE, PERF_ANCHOR, grandchildPath)
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 500 milliseconds'
            assert deleteDurationInMillis < 500
    }

    def 'Delete 1 large data node with many descendants'() {
        when: 'parent node is deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNode(PERF_DATASPACE, PERF_ANCHOR, PERF_TEST_PARENT)
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 2500 milliseconds'
            assert deleteDurationInMillis < 2500
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Create a node with many list elements (please note, subsequent tests depend on this running first).'() {
        given: 'a node with a large number of descendants is created'
            stopWatch.start()
            createLineage(objectUnderTest, NUMBER_OF_LISTS, NUMBER_OF_LIST_ELEMENTS, true)
            stopWatch.stop()
            def setupDurationInMillis = stopWatch.getTotalTimeMillis()
        and: 'setup duration is under #ALLOWED_SETUP_TIME_MS milliseconds'
            assert setupDurationInMillis < ALLOWED_SETUP_TIME_MS
    }

    def 'Delete 5 whole lists with many elements'() {
        when: 'list nodes are deleted'
            stopWatch.start()
            (1..5).each {
                def childPath = "${PERF_TEST_PARENT}/perf-test-list-${it}".toString();
                objectUnderTest.deleteListDataNode(PERF_DATASPACE, PERF_ANCHOR, childPath)
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 1500 milliseconds'
            assert deleteDurationInMillis < 1500
    }

    def 'Delete 10 list elements with keys'() {
        when: 'list elements are deleted'
            stopWatch.start()
            (1..10).each {
                def key = it.toString()
                def grandchildPath = "${PERF_TEST_PARENT}/perf-test-list-6[@key='${key}']"
                objectUnderTest.deleteListDataNode(PERF_DATASPACE, PERF_ANCHOR, grandchildPath)
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 1000 milliseconds'
            println(deleteDurationInMillis)
            assert deleteDurationInMillis < 1000
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Delete root node with many descendants'() {
        given: 'a node with a large number of descendants is created'
            createLineage(objectUnderTest, NUMBER_OF_CHILDREN, NUMBER_OF_GRAND_CHILDREN, false)
        when: 'root node is deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNode(PERF_DATASPACE, PERF_ANCHOR, '/')
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 200 milliseconds'
            assert deleteDurationInMillis < 200
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Delete data nodes for an anchor'() {
        given: 'a node with a large number of descendants is created'
            createLineage(objectUnderTest, NUMBER_OF_CHILDREN, NUMBER_OF_GRAND_CHILDREN, false)
        when: 'data nodes are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes(PERF_DATASPACE, PERF_ANCHOR)
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 200 milliseconds'
            assert deleteDurationInMillis < 200
    }
}
