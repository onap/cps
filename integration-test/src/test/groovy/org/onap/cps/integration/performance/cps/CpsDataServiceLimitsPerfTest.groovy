/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration.performance.cps

import java.time.OffsetDateTime
import org.onap.cps.api.CpsDataService
import org.onap.cps.integration.performance.base.CpsPerfTestBase

import static org.onap.cps.spi.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsDataServiceLimitsPerfTest extends CpsPerfTestBase {

    CpsDataService objectUnderTest

    def setup() { objectUnderTest = cpsDataService }

    def 'Create 33,000 books (note further tests depend on this running first).'() {
        given: 'an anchor containing a bookstore with one category'
            cpsAdminService.createAnchor(CPS_PERFORMANCE_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'limitsAnchor')
            def parentNodeData = '{"bookstore": { "categories": [{ "code": 1, "name": "Test", "books" : [] }] }}'
            cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, 'limitsAnchor', parentNodeData, OffsetDateTime.now())
        when: '33,000 books are added'
            stopWatch.start()
            for (int i = 1; i <= 33_000; i+=100) {
                def booksData = '{"books":[' + (i..<i+100).collect {'{ "title": "' + it + '" }' }.join(',') + ']}'
                cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, 'limitsAnchor', '/bookstore/categories[@code=1]', booksData, OffsetDateTime.now())
            }
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'the operation completes within 10 seconds'
            recordAndAssertPerformance("Creating 33,000 books", 10_000, durationInMillis)
    }

    def 'Get data nodes from multiple xpaths 32K (2^15) limit exceeded.'() {
        given: '33,000 xpaths'
            def xpaths = (1..33_000).collect { "/bookstore/categories[@code=1]/books[@title='${it}']".toString() }
        when: 'a single operation is executed to get all datanodes with given xpaths'
            def results = objectUnderTest.getDataNodesForMultipleXpaths(CPS_PERFORMANCE_TEST_DATASPACE, 'limitsAnchor', xpaths, OMIT_DESCENDANTS)
        then: '33,000 data nodes are returned'
            assert results.size() == 33_000
    }

    def 'Delete multiple data nodes 32K (2^15) limit exceeded.'() {
        given: 'existing data nodes'
            def countOfDataNodesBeforeDelete = countDataNodes()
        and: 'a list of 33,000 xpaths'
            def xpaths = (1..33_000).collect { "/bookstore/categories[@code=1]/books[@title='${it}']".toString() }
        when: 'a single operation is executed to delete all datanodes with given xpaths'
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'limitsAnchor', xpaths, OffsetDateTime.now())
        then: '33,000 data nodes are deleted'
            def countOfDataNodesAfterDelete = countDataNodes()
            assert countOfDataNodesBeforeDelete - countOfDataNodesAfterDelete == 33_000
    }

    def 'Delete data nodes from multiple anchors 32K (2^15) limit exceeded.'() {
        given: '33,000 anchor names'
            def anchorNames = (1..33_000).collect { "size-of-this-name-does-not-matter-for-limit-" + it }
        when: 'a single operation is executed to delete all datanodes in given anchors'
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, anchorNames, OffsetDateTime.now())
        then: 'a database exception is not thrown'
            noExceptionThrown()
    }

    def 'Clean up test data.'() {
        when:
            stopWatch.start()
            cpsDataService.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'limitsAnchor', OffsetDateTime.now())
            cpsAdminService.deleteAnchor(CPS_PERFORMANCE_TEST_DATASPACE, 'limitsAnchor')
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'test data is deleted in 10 seconds'
            recordAndAssertPerformance("Deleting test data", 10_000, durationInMillis)
    }

    def countDataNodes() {
        def results = objectUnderTest.getDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'limitsAnchor', '/bookstore/categories[@code=1]', DIRECT_CHILDREN_ONLY)
        return results[0].childDataNodes.size()
    }

}
