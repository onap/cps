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

import java.util.concurrent.TimeUnit

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsToDataNodePerfTest extends CpsPersistenceSpecBase {

    static final String PERF_TEST_DATA = '/data/perf-test.sql'

    @Autowired
    CpsDataPersistenceService objectUnderTest

    static def PERF_TEST_PARENT = '/perf-parent-1'
    static def NUMBER_OF_CHILDREN = 200
    static def NUMBER_OF_GRAND_CHILDREN = 50
    static def TOTAL_NUMBER_OF_NODES = 1 + NUMBER_OF_CHILDREN + (NUMBER_OF_CHILDREN * NUMBER_OF_GRAND_CHILDREN)  //  Parent + Children +  Grand-children
    static def ALLOWED_SETUP_TIME_MS = TimeUnit.SECONDS.toMillis(10)
    static def ALLOWED_READ_TIME_AL_NODES_MS = 500

    def readStopWatch = new StopWatch()

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Create a node with many descendants (please note, subsequent tests depend on this running first).'() {
        given: 'a node with a large number of descendants is created'
            def setupStopWatch = new StopWatch()
            setupStopWatch.start()
            createLineage()
            setupStopWatch.stop()
            def setupDurationInMillis = setupStopWatch.getTime()
        and: 'setup duration is under #ALLOWED_SETUP_TIME_MS milliseconds'
            assert setupDurationInMillis < ALLOWED_SETUP_TIME_MS
    }

    def 'Get data node with many descendants by xpath #scenario'() {
        when: 'get parent is executed with all descendants'
            readStopWatch.start()
            def result = objectUnderTest.getDataNode('PERF-DATASPACE', 'PERF-ANCHOR', xpath, INCLUDE_ALL_DESCENDANTS)
            readStopWatch.stop()
            def readDurationInMillis = readStopWatch.getTime()
        then: 'read duration is under 500 milliseconds'
            assert readDurationInMillis < ALLOWED_READ_TIME_AL_NODES_MS
        and: 'data node is returned with all the descendants populated'
            assert countDataNodes(result) == TOTAL_NUMBER_OF_NODES
        where: 'the following xPaths are used'
            scenario || xpath
            'parent' || PERF_TEST_PARENT
            'root'   || ''
    }

    def 'Query parent data node with many descendants by cps-path'() {
        when: 'query is executed with all descendants'
            readStopWatch.reset()
            readStopWatch.start()
            def result = objectUnderTest.queryDataNodes('PERF-DATASPACE', 'PERF-ANCHOR', '//perf-parent-1' , INCLUDE_ALL_DESCENDANTS)
            readStopWatch.stop()
            def readDurationInMillis = readStopWatch.getTime()
        then: 'read duration is under 500 milliseconds'
            assert readDurationInMillis < ALLOWED_READ_TIME_AL_NODES_MS
        and: 'data node is returned with all the descendants populated'
            assert countDataNodes(result) == TOTAL_NUMBER_OF_NODES
    }

    def 'Query many descendants by cps-path with #scenario'() {
        when: 'query is executed with all descendants'
            readStopWatch.reset()
            readStopWatch.start()
            def result = objectUnderTest.queryDataNodes('PERF-DATASPACE', 'PERF-ANCHOR',  '//perf-test-grand-child-1', descendantsOption)
            readStopWatch.stop()
            def readDurationInMillis = readStopWatch.getTime()
        then: 'read duration is under 500 milliseconds'
            assert readDurationInMillis < alowedDuration
        and: 'data node is returned with all the descendants populated'
            assert result.size() == NUMBER_OF_CHILDREN
        where: 'the following options are used'
            scenario                                        | descendantsOption        || alowedDuration
            'omit descendants                             ' | OMIT_DESCENDANTS         || 150
            'include descendants (although there are none)' | INCLUDE_ALL_DESCENDANTS  || 1500
    }

    def createLineage() {
        (1..NUMBER_OF_CHILDREN).each {
            def childName = "perf-test-child-${it}".toString()
            def newChild = goForthAndMultiply(PERF_TEST_PARENT, childName)
            objectUnderTest.addChildDataNode('PERF-DATASPACE', 'PERF-ANCHOR', PERF_TEST_PARENT, newChild)
        }
    }

    def goForthAndMultiply(parentXpath, childName) {
        def children = []
        (1..NUMBER_OF_GRAND_CHILDREN).each {
            def child = new DataNodeBuilder().withXpath("${parentXpath}/${childName}/perf-test-grand-child-${it}").build()
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
