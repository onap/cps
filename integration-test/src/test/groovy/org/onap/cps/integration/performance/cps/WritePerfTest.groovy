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
import org.onap.cps.integration.performance.base.CpsPerfTestBase

class WritePerfTest extends CpsPerfTestBase {

    def 'Writing openroadm data has linear time.'() {
        given: 'an empty anchor exists for openroadm'
            cpsAdminService.createAnchor(CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, 'writeAnchor')
        and: 'a list of device nodes to add'
            def jsonData = generateOpenRoadData(totalNodes)
        when: 'device nodes are added'
            stopWatch.start()
            cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor', jsonData, OffsetDateTime.now())
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'the operation takes less than #expectedDuration'
            recordAndAssertPerformance("Writing ${totalNodes} devices", expectedDuration, durationInMillis)
        cleanup:
            cpsDataService.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor', OffsetDateTime.now())
            cpsAdminService.deleteAnchor(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor')
        where:
            totalNodes || expectedDuration
            50         ||   3_000
            100        ||   5_000
            200        ||  10_000
            400        ||  20_000
//          800        ||  40_000
//          1600       ||  80_000
//          3200       || 160_000
    }

    def 'Writing bookstore data has exponential time.'() {
        given: 'an anchor containing a bookstore with a single category'
            cpsAdminService.createAnchor(CPS_PERFORMANCE_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'writeAnchor')
            def parentNodeData = '{"bookstore": { "categories": [{ "code": 1, "name": "Test", "books" : [] }] }}'
            cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor', parentNodeData, OffsetDateTime.now())
        and: 'a list of books to add'
            def booksData = '{"books":[' + (1..totalBooks).collect {'{ "title": "' + it + '" }' }.join(',') + ']}'
        when: 'books are added'
            stopWatch.start()
            cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor', '/bookstore/categories[@code=1]', booksData, OffsetDateTime.now())
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'the operation takes less than #expectedDuration'
            recordAndAssertPerformance("Writing ${totalBooks} books", expectedDuration, durationInMillis)
        cleanup:
            cpsDataService.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor', OffsetDateTime.now())
            cpsAdminService.deleteAnchor(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor')
        where:
            totalBooks || expectedDuration
            400        ||     200
            800        ||     500
            1600       ||   1_000
            3200       ||   3_000
            6400       ||  10_000
//          12800      ||  30_000
//          25600      || 120_000
//          51200      || 600_000
    }

}
