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

import static org.onap.cps.spi.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
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
    static def TOTAL_NUMBER_OF_NODES = 1 + NUMBER_OF_CHILDREN + (NUMBER_OF_CHILDREN * NUMBER_OF_GRAND_CHILDREN)

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

    def 'Get data node with many descendants by xpath: #scenario'() {
        when: 'get parent is executed with all descendants'
            stopWatch.start()
            def result = objectUnderTest.getDataNodes(PERF_DATASPACE, PERF_ANCHOR, xpath, fetchDescendantsOption)
            stopWatch.stop()
            def readDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'data node is returned with all the descendants populated'
            assert result.size() == 1
            assert countDataNodes(result) == expectedNodeCount
        and: 'read duration is under #allowedDuration milliseconds'
            recordAndAssertPerformance("Get ${scenario}", allowedDuration, readDurationInMillis)
        where: 'the following xPaths are used'
            scenario                      | xpath            | fetchDescendantsOption  | expectedNodeCount      || allowedDuration
            'large node omit descendants' | PERF_TEST_PARENT | OMIT_DESCENDANTS        | 1                      || 30
            'root xpath omit descendants' | '/'              | OMIT_DESCENDANTS        | 1                      || 15
            'large node direct children'  | PERF_TEST_PARENT | DIRECT_CHILDREN_ONLY    | 1 + NUMBER_OF_CHILDREN || 30
            'root xpath direct children'  | '/'              | DIRECT_CHILDREN_ONLY    | 1 + NUMBER_OF_CHILDREN || 30
            'large node all descendants'  | PERF_TEST_PARENT | INCLUDE_ALL_DESCENDANTS | TOTAL_NUMBER_OF_NODES  || 100
            'root xpath all descendants'  | '/'              | INCLUDE_ALL_DESCENDANTS | TOTAL_NUMBER_OF_NODES  || 350
    }

    def 'Performance of finding multiple xpaths: 10,000 nodes with no descendants'() {
        when: 'we query for all grandchildren (except 1 for fun)'
            xpathsToAllGrandChildren.remove(0)
            stopWatch.start()
            def result = objectUnderTest.getDataNodesForMultipleXpaths(PERF_DATASPACE, PERF_ANCHOR, xpathsToAllGrandChildren, descendantsOption)
            stopWatch.stop()
            def readDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'the returned number of entities equal to the number of children * number of grandchildren'
            assert result.size() == xpathsToAllGrandChildren.size()
            assert countDataNodes(result) == xpathsToAllGrandChildren.size()
        and: 'it took less then #allowedDuration ms'
            recordAndAssertPerformance("Get 10,000 grandchildren ${scenario}", allowedDuration, readDurationInMillis)
        where: 'the following options are used'
            scenario              | descendantsOption        || allowedDuration
            'omit descendants'    | OMIT_DESCENDANTS         || 500
            'direct children'     | DIRECT_CHILDREN_ONLY     || 3500
            'include descendants' | INCLUDE_ALL_DESCENDANTS  || 500
    }

    def 'Performance of finding multiple xpaths: 200 nodes with descendants'() {
        given: 'a list of xpaths to get'
            def xpaths = (1..200).collect {
                "${PERF_TEST_PARENT}/perf-test-child-${it}".toString()
            }
        when: 'we query for multiple children'
            stopWatch.start()
            def result = objectUnderTest.getDataNodesForMultipleXpaths(PERF_DATASPACE, PERF_ANCHOR, xpaths, descendantsOption)
            stopWatch.stop()
            def readDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'the returned number of entities equal to the number of children * number of grandchildren'
            assert result.size() == 200
            assert countDataNodes(result) == expectedResultSize
        and: 'it took less then #allowedDuration ms'
            recordAndAssertPerformance("Get 100 children ${scenario}", allowedDuration, readDurationInMillis)
        where: 'the following options are used'
            scenario              | descendantsOption       | expectedResultSize || allowedDuration
            'omit descendants'    | OMIT_DESCENDANTS        | 200                || 30
            'direct children'     | DIRECT_CHILDREN_ONLY    | 10200              || 350
            'include descendants' | INCLUDE_ALL_DESCENDANTS | 10200              || 200
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

    def 'Query many descendants by cps-path with #scenario'() {
        when: 'query is executed with all descendants'
            stopWatch.start()
            def result = objectUnderTest.queryDataNodes(PERF_DATASPACE, PERF_ANCHOR,  '//perf-test-grand-child-1', descendantsOption)
            stopWatch.stop()
            def readDurationInMillis = stopWatch.getTotalTimeMillis()
        then: 'read duration is under #allowedDuration milliseconds'
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
        then: 'update duration is under 600 milliseconds'
            recordAndAssertPerformance('Update data nodes without descendants', 600, updateDurationInMillis)
    }
}
