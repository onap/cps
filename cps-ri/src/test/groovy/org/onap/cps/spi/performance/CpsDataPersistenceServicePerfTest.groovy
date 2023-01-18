/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

import org.onap.cps.spi.impl.CpsPersistencePerfSpecBase
import org.springframework.util.StopWatch
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.repository.AnchorRepository
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.repository.FragmentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

import java.util.concurrent.TimeUnit

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsDataPersistenceServicePerfTest extends CpsPersistencePerfSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    @Autowired
    DataspaceRepository dataspaceRepository

    @Autowired
    AnchorRepository anchorRepository

    @Autowired
    FragmentRepository fragmentRepository

    static def NUMBER_OF_CHILDREN = 200
    static def NUMBER_OF_GRAND_CHILDREN = 50
    static def TOTAL_NUMBER_OF_NODES = 1 + NUMBER_OF_CHILDREN + (NUMBER_OF_CHILDREN * NUMBER_OF_GRAND_CHILDREN)  //  Parent + Children +  Grand-children
    static def ALLOWED_SETUP_TIME_MS = TimeUnit.SECONDS.toMillis(10)
    static def ALLOWED_READ_TIME_AL_NODES_MS = 500

    def stopWatch = new StopWatch()
    def readStopWatch = new StopWatch()

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

    def 'Get data node with many descendants by xpath #scenario'() {
        when: 'get parent is executed with all descendants'
            stopWatch.start()
            def result = objectUnderTest.getDataNode(PERF_DATASPACE, PERF_ANCHOR, xpath, INCLUDE_ALL_DESCENDANTS)
            stopWatch.stop()
            def readDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'read duration is under #ALLOWED_READ_TIME_AL_NODES_MS milliseconds'
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
            stopWatch.start()
            def result = objectUnderTest.queryDataNodes(PERF_DATASPACE, PERF_ANCHOR, '//perf-parent-1' , INCLUDE_ALL_DESCENDANTS)
            stopWatch.stop()
            def readDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'read duration is under #ALLOWED_READ_TIME_AL_NODES_MS milliseconds'
            assert readDurationInMillis < ALLOWED_READ_TIME_AL_NODES_MS
        and: 'data node is returned with all the descendants populated'
            assert countDataNodes(result) == TOTAL_NUMBER_OF_NODES
    }

    def 'Performance of finding multiple xpaths'() {
        when: 'we query for all grandchildren (except 1 for fun) with the new native method'
            xpathsToAllGrandChildren.remove(0)
            readStopWatch.start()
            def result = objectUnderTest.getDataNodes(PERF_DATASPACE, PERF_ANCHOR, xpathsToAllGrandChildren, INCLUDE_ALL_DESCENDANTS)
            readStopWatch.stop()
            def readDurationInMillis = readStopWatch.getTotalTimeMillis()
        then: 'the returned number of entities equal to the number of children * number of grandchildren'
            assert result.size() == xpathsToAllGrandChildren.size()
        and: 'it took less then 4000ms'
            assert readDurationInMillis < 4000
    }

    def 'Query many descendants by cps-path with #scenario'() {
        when: 'query is executed with all descendants'
            stopWatch.start()
            def result = objectUnderTest.queryDataNodes(PERF_DATASPACE, PERF_ANCHOR,  '//perf-test-grand-child-1', descendantsOption)
            stopWatch.stop()
            def readDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'read duration is under #allowedDuration milliseconds'
            assert readDurationInMillis < allowedDuration
        and: 'data node is returned with all the descendants populated'
            assert result.size() == NUMBER_OF_CHILDREN
        where: 'the following options are used'
            scenario                                        | descendantsOption        || allowedDuration
            'omit descendants                             ' | OMIT_DESCENDANTS         || 150
            'include descendants (although there are none)' | INCLUDE_ALL_DESCENDANTS  || 150
    }
}
