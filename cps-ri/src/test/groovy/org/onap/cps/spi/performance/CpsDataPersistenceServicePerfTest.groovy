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
        then: 'update duration is under 900 milliseconds'
            recordAndAssertPerformance('Update data nodes without descendants', 900, updateDurationInMillis)
    }
}
