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
import org.onap.cps.spi.impl.CpsPersistenceSpecBase
import org.onap.cps.spi.model.DataNodeBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.util.StopWatch

class CpsDataPersistenceServiceDeletePerfTest extends CpsPersistenceSpecBase {

    static final String PERF_TEST_DATA = '/data/perf-test.sql'

    @Autowired
    CpsDataPersistenceService objectUnderTest

    static def PERF_TEST_PARENT = '/perf-parent-1'
    static def NUMBER_OF_CHILDREN = 20
    static def NUMBER_OF_GRAND_CHILDREN = 50
    static def NUMBER_OF_LISTS = 20
    static def NUMBER_OF_LIST_ELEMENTS = 50

    def stopWatch = new StopWatch()

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Delete 50 grandchildren (that have no descendants)'() {
        given: 'a node with a large number of descendants is created'
            createLineage()
        when: 'target nodes are deleted'
            stopWatch.start()
            (1..50).each {
                def grandchildPath = "${PERF_TEST_PARENT}/perf-test-child-1/perf-test-grand-child-${it}".toString();
                objectUnderTest.deleteDataNode('PERF-DATASPACE', 'PERF-ANCHOR', grandchildPath)
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 1000 milliseconds'
            assert deleteDurationInMillis < 1000
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Delete 5 children with grandchildren'() {
        given: 'a node with a large number of descendants is created'
            createLineage()
        when: 'child nodes are deleted'
            stopWatch.start()
            (1..5).each {
                def childPath = "${PERF_TEST_PARENT}/perf-test-child-${it}".toString();
                objectUnderTest.deleteDataNode('PERF-DATASPACE', 'PERF-ANCHOR', childPath)
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 2500 milliseconds'
            assert deleteDurationInMillis < 2500
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Delete 1 large data node with many descendants'() {
        given: 'a node with a large number of descendants is created'
            createLineage()
        when: 'parent node is deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNode('PERF-DATASPACE', 'PERF-ANCHOR', PERF_TEST_PARENT)
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 1000 milliseconds'
            assert deleteDurationInMillis < 1000
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Delete root node with many descendants'() {
        given: 'a node with a large number of descendants is created'
            createLineage()
        when: 'root node is deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNode('PERF-DATASPACE', 'PERF-ANCHOR', '/')
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 100 milliseconds'
            assert deleteDurationInMillis < 100
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Delete data nodes for an anchor'() {
        given: 'a node with a large number of descendants is created'
            createLineage()
        when: 'data nodes are deleted'
            stopWatch.start()
            objectUnderTest.deleteDataNodes('PERF-DATASPACE', 'PERF-ANCHOR')
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 100 milliseconds'
            assert deleteDurationInMillis < 100
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Delete 5 whole lists with many elements'() {
        given: 'a node with a large number of descendants is created'
            createLineage()
        when: 'list elements are deleted'
            stopWatch.start()
            (1..5).each {
                def childPath = "${PERF_TEST_PARENT}/perf-test-list-${it}".toString();
                objectUnderTest.deleteListDataNode('PERF-DATASPACE', 'PERF-ANCHOR', childPath)
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 2000 milliseconds'
            assert deleteDurationInMillis < 2000
    }

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Delete 20 list elements with keys'() {
        given: 'a node with a large number of descendants is created'
            createLineage()
        when: 'list elements are deleted'
            stopWatch.start()
            (1..20).each {
                def key = it.toString()
                def grandchildPath = "${PERF_TEST_PARENT}/perf-test-list-1[@key='${key}']"
                objectUnderTest.deleteListDataNode('PERF-DATASPACE', 'PERF-ANCHOR', grandchildPath)
            }
            stopWatch.stop()
            def deleteDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'delete duration is under 6000 milliseconds'
            assert deleteDurationInMillis < 6000
    }

    def createLineage() {
        (1..NUMBER_OF_CHILDREN).each {
            def childName = "perf-test-child-${it}".toString()
            def child = goForthAndMultiply(PERF_TEST_PARENT, childName)
            objectUnderTest.addChildDataNode('PERF-DATASPACE', 'PERF-ANCHOR', PERF_TEST_PARENT, child)
        }
        (1..NUMBER_OF_LISTS).each {
            def listName = "perf-test-list-${it}".toString()
            def listElements = makeListElements(PERF_TEST_PARENT, listName)
            objectUnderTest.addListElements('PERF-DATASPACE', 'PERF-ANCHOR', PERF_TEST_PARENT, listElements)
        }
    }

    def goForthAndMultiply(parentXpath, childName) {
        def grandChildren = []
        (1..NUMBER_OF_GRAND_CHILDREN).each {
            def grandChild = new DataNodeBuilder().withXpath("${parentXpath}/${childName}/perf-test-grand-child-${it}").build()
            grandChildren.add(grandChild)
        }
        return new DataNodeBuilder().withXpath("${parentXpath}/${childName}").withChildDataNodes(grandChildren).build()
    }

    def makeListElements(parentXpath, childName) {
        def listElements = []
        (1..NUMBER_OF_LIST_ELEMENTS).each {
            def key = it.toString()
            def element = new DataNodeBuilder().withXpath("${parentXpath}/${childName}[@key='${key}']").build()
            listElements.add(element)
        }
        return listElements
    }
}
