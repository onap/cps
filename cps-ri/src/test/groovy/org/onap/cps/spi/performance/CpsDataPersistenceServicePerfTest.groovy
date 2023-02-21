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
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.repository.AnchorRepository
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.repository.FragmentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

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

    @Sql([CLEAR_DATA, PERF_TEST_DATA])
    def 'Create a node with many descendants (please note, subsequent tests depend on this running first).'() {
        given: 'a node with a large number of descendants is created'
            stopWatch.start()
            createLineage(objectUnderTest, NUMBER_OF_CHILDREN, NUMBER_OF_GRAND_CHILDREN, false)
            stopWatch.stop()
            def setupDurationInMillis = stopWatch.getTotalTimeMillis()
        and: 'setup duration is under 10 seconds'
            recordAndAssertPerformance('Setup', 10000, setupDurationInMillis)
    }

    def 'Get data node with many descendants by xpath #scenario'() {
        when: 'get parent is executed with all descendants'
            stopWatch.start()
            def result = objectUnderTest.getDataNodes(PERF_DATASPACE, PERF_ANCHOR, xpath, INCLUDE_ALL_DESCENDANTS)
            stopWatch.stop()
            def readDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'read duration is under #allowedDuration milliseconds'
            recordAndAssertPerformance("Get ${scenario}", allowedDuration, readDurationInMillis)
        and: 'data node is returned with all the descendants populated'
            assert countDataNodes(result[0]) == TOTAL_NUMBER_OF_NODES
        where: 'the following xPaths are used'
            scenario | xpath            || allowedDuration
            'parent' | PERF_TEST_PARENT || 3500
            'root'   | ''               || 500
    }

    def 'Query parent data node with many descendants by cps-path'() {
        when: 'query is executed with all descendants'
            stopWatch.start()
            def result = objectUnderTest.queryDataNodes(PERF_DATASPACE, PERF_ANCHOR, '//perf-parent-1' , INCLUDE_ALL_DESCENDANTS)
            stopWatch.stop()
            def readDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'read duration is under 500 milliseconds'
            recordAndAssertPerformance('Query with many descendants', 500, readDurationInMillis)
        and: 'data node is returned with all the descendants populated'
            assert countDataNodes(result) == TOTAL_NUMBER_OF_NODES
    }

    def 'Performance of finding multiple xpaths'() {
        when: 'we query for all grandchildren (except 1 for fun) with the new native method'
            xpathsToAllGrandChildren.remove(0)
            stopWatch.start()
            def result = objectUnderTest.getDataNodesForMultipleXpaths(PERF_DATASPACE, PERF_ANCHOR, xpathsToAllGrandChildren, INCLUDE_ALL_DESCENDANTS)
            stopWatch.stop()
            def readDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'the returned number of entities equal to the number of children * number of grandchildren'
            assert result.size() == xpathsToAllGrandChildren.size()
        and: 'it took less then 3000ms'
            recordAndAssertPerformance('Find multiple xpaths', 3000, readDurationInMillis)
    }

    def 'Query many descendants by cps-path with #scenario'() {
        when: 'query is executed with all descendants'
            stopWatch.start()
            def result = objectUnderTest.queryDataNodes(PERF_DATASPACE, PERF_ANCHOR,  '//perf-test-grand-child-1', descendantsOption)
            stopWatch.stop()
            def readDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'read duration is under #allowedDuration milliseconds'
            assert readDurationInMillis < allowedDuration
            recordAndAssertPerformance("Query many descendants by cpspath (${scenario})", allowedDuration, readDurationInMillis)
        and: 'data node is returned with all the descendants populated'
            assert result.size() == NUMBER_OF_CHILDREN
        where: 'the following options are used'
            scenario                                        | descendantsOption        || allowedDuration
            'omit descendants                             ' | OMIT_DESCENDANTS         || 150
            'include descendants (although there are none)' | INCLUDE_ALL_DESCENDANTS  || 150
    }

    def 'Update data nodes with descendants'() {
        given: 'a list of xpaths to data nodes with descendants (xpath for each child)'
            def xpaths = (1..20).collect {
                "${PERF_TEST_PARENT}/perf-test-child-${it}".toString()
            }
        and: 'the correct number of data nodes are fetched'
            def dataNodes = objectUnderTest.getDataNodesForMultipleXpaths(PERF_DATASPACE, PERF_ANCHOR, xpaths, INCLUDE_ALL_DESCENDANTS)
            assert dataNodes.size() == 20
            assert countDataNodes(dataNodes) == 20 + 20 * 50
        when: 'the fragment entities are updated by the data nodes'
            stopWatch.start()
            objectUnderTest.updateDataNodesAndDescendants(PERF_DATASPACE, PERF_ANCHOR, dataNodes)
            stopWatch.stop()
            def updateDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'update duration is under 600 milliseconds'
            recordAndAssertPerformance('Update data nodes with descendants', 600, updateDurationInMillis)
    }

    def 'Update data nodes without descendants'() {
        given: 'a list of xpaths to data nodes without descendants (xpath for each grandchild)'
            def xpaths = []
            for (int childIndex = 21; childIndex <= 40; childIndex++) {
                xpaths.addAll((1..50).collect {
                    "${PERF_TEST_PARENT}/perf-test-child-${childIndex}/perf-test-grand-child-${it}".toString()
                })
            }
        and: 'the correct number of data nodes are fetched'
            def dataNodes = objectUnderTest.getDataNodesForMultipleXpaths(PERF_DATASPACE, PERF_ANCHOR, xpaths, OMIT_DESCENDANTS)
            assert dataNodes.size() == 20 * 50
            assert countDataNodes(dataNodes) == 20 * 50
        when: 'the fragment entities are updated by the data nodes'
            stopWatch.start()
            objectUnderTest.updateDataNodesAndDescendants(PERF_DATASPACE, PERF_ANCHOR, dataNodes)
            stopWatch.stop()
            def updateDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'update duration is under 1400 milliseconds'
            recordAndAssertPerformance('Update data nodes without descendants', 1400, updateDurationInMillis)
    }
}
