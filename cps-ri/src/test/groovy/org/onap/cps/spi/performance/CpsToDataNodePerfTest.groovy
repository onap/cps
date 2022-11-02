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
import static org.onap.cps.spi.impl.CpsPersistenceSpecBase.CLEAR_DATA

class CpsToDataNodePerfTest extends CpsPersistenceSpecBase {

    static final String SET_DATA = '/data/fragment.sql'

    @Autowired
    CpsDataPersistenceService objectUnderTest

    def XPATH_DATA_NODE_WITH_DESCENDANTS = '/parent-1'

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get data node by xpath with all descendants with many children'() {
        given: 'nodes and grandchildren have been persisted'
            def setupStopWatch = new StopWatch()
            setupStopWatch.start()
            createLineage()
            setupStopWatch.stop()
            def setupDurationInMillis = setupStopWatch.getTime()
        when: 'data node is requested with all descendants'
            def readStopWatch = new StopWatch()
            readStopWatch.start()
            def result = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_NAME1, XPATH_DATA_NODE_WITH_DESCENDANTS, INCLUDE_ALL_DESCENDANTS)
            readStopWatch.stop()
            def readDurationInMillis = readStopWatch.getTime()
        then: 'setup duration is under 8 seconds'
            assert setupDurationInMillis < 8000
        and: 'read duration is under 1500 milliseconds'
            assert readDurationInMillis < 1500
        and: 'data node is returned with all the descendants populated'
            assert countDataNodes(result) == 1533
    }

    def createLineage() {
        def numOfChildren = 30
        def numOfGrandChildren = 50
        (1..numOfChildren).each {
            def childName = "perf-test-child-${it}".toString()
            def newChild = goForthAndMultiply(XPATH_DATA_NODE_WITH_DESCENDANTS, childName, numOfGrandChildren)
            objectUnderTest.addChildDataNode(DATASPACE_NAME, ANCHOR_NAME1, XPATH_DATA_NODE_WITH_DESCENDANTS, newChild)
        }
    }

    def goForthAndMultiply(parentXpath, childName, numOfGrandChildren) {
        def children = []
        (1..numOfGrandChildren).each {
            def child = new DataNodeBuilder().withXpath("${parentXpath}/${childName}/${it}-grand-child").build()
            children.add(child)
        }
        return new DataNodeBuilder().withXpath("${parentXpath}/${childName}").withChildDataNodes(children).build()
    }

    def countDataNodes(DataNode dataNode) {
        int nodeCount = 1
        for (DataNode child : dataNode.childDataNodes) {
            nodeCount = nodeCount + (countDataNodes(child))
        }
        return nodeCount
    }
}
