/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

import org.apache.commons.lang3.time.StopWatch
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.impl.CpsPersistenceSpecBase
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS

class CpsToDataNodePerfTest extends CpsPersistenceSpecBase {

    static final String PERF_TEST_DATA = '/data/perf-test.sql'

    @Autowired
    CpsDataPersistenceService objectUnderTest

    def PERF_TEST_PARENT = '/perf-parent-1'

    def EXPECTED_NUMBER_OF_NODES = 10051  //  1 Parent + 50 Children + 10000 Grand-children

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Get data node by xpath with all descendants with many children'() {
        given: 'nodes and grandchildren have been persisted'
            def setupStopWatch = new StopWatch()
            setupStopWatch.start()
            createLineage()
            setupStopWatch.stop()
            def setupDurationInMillis = setupStopWatch.getTime()
        and: 'setup duration is under 8000 milliseconds'
            assert setupDurationInMillis < 8000
        when: 'get parent is executed with all descendants'
            def readStopWatch = new StopWatch()
            readStopWatch.start()
            def result = objectUnderTest.getDataNode('PERF-DATASPACE', 'PERF-ANCHOR', PERF_TEST_PARENT, INCLUDE_ALL_DESCENDANTS)
            readStopWatch.stop()
            def readDurationInMillis = readStopWatch.getTime()
        then: 'read duration is under 450 milliseconds'
            assert readDurationInMillis < 450
        and: 'data node is returned with all the descendants populated'
            assert countDataNodes(result) == EXPECTED_NUMBER_OF_NODES
        when: 'get root is executed with all descendants'
            readStopWatch.reset()
            readStopWatch.start()
            result = objectUnderTest.getDataNode('PERF-DATASPACE', 'PERF-ANCHOR', '', INCLUDE_ALL_DESCENDANTS)
            readStopWatch.stop()
            readDurationInMillis = readStopWatch.getTime()
        then: 'read duration is under 450 milliseconds'
            assert readDurationInMillis < 450
        and: 'data node is returned with all the descendants populated'
            assert countDataNodes(result) == EXPECTED_NUMBER_OF_NODES
        when: 'query is executed with all descendants'
            readStopWatch.reset()
            readStopWatch.start()
            result = objectUnderTest.queryDataNodes('PERF-DATASPACE', 'PERF-ANCHOR', '//perf-parent-1', INCLUDE_ALL_DESCENDANTS)
            readStopWatch.stop()
            readDurationInMillis = readStopWatch.getTime()
        then: 'read duration is under 450 milliseconds'
            assert readDurationInMillis < 450
        and: 'data node is returned with all the descendants populated'
            assert countDataNodes(result) == EXPECTED_NUMBER_OF_NODES
    }

    def createLineage() {
        def numOfChildren = 50
        def numOfGrandChildren = 200
        (1..numOfChildren).each {
            def childName = "perf-test-child-${it}".toString()
            def newChild = goForthAndMultiply(PERF_TEST_PARENT, childName, numOfGrandChildren)
            objectUnderTest.addChildDataNode('PERF-DATASPACE', 'PERF-ANCHOR', PERF_TEST_PARENT, newChild)
        }
    }

    def goForthAndMultiply(parentXpath, childName, numOfGrandChildren) {
        def children = []
        (1..numOfGrandChildren).each {
            def child = new DataNodeBuilder().withXpath("${parentXpath}/${childName}/${it}perf-test-grand-child").build()
            children.add(child)
        }
        return new DataNodeBuilder().withXpath("${parentXpath}/${childName}").withChildDataNodes(children).build()
    }

    def countDataNodes(dataNodes) {
        int nodeCount = 1
        for (DataNode parent : dataNodes) {
            for (DataNode child : parent.childDataNodes) {
                nodeCount = nodeCount + (countDataNodes(child))
            }
        }
        return nodeCount
    }
}
