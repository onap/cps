/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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
            cpsAnchorService.createAnchor(CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, 'writeAnchor')
        and: 'a list of device nodes to add'
            def jsonData = generateOpenRoadData(totalNodes)
        when: 'device nodes are added'
            resourceMeter.start()
            cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor', jsonData, OffsetDateTime.now())
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the operation takes less than #expectedDuration and memory used is within limit'
            recordAndAssertResourceUsage("Writing ${totalNodes} devices", expectedDuration, durationInSeconds, memoryLimit, resourceMeter.getTotalMemoryUsageInMB())
        cleanup:
            cpsDataService.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor', OffsetDateTime.now())
            cpsAnchorService.deleteAnchor(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor')
        where:
            totalNodes || expectedDuration | memoryLimit
            50         || 2                | 100
            100        || 4                | 200
            200        || 7                | 400
            400        || 14               | 500
    }

    def 'Writing bookstore data has exponential time.'() {
        given: 'an anchor containing a bookstore with a single category'
            cpsAnchorService.createAnchor(CPS_PERFORMANCE_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'writeAnchor')
            def parentNodeData = '{"bookstore": { "categories": [{ "code": 1, "name": "Test", "books" : [] }] }}'
            cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor', parentNodeData, OffsetDateTime.now())
        and: 'a list of books to add'
            def booksData = '{"books":[' + (1..totalBooks).collect {'{ "title": "' + it + '" }' }.join(',') + ']}'
        when: 'books are added'
            resourceMeter.start()
            cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor', '/bookstore/categories[@code=1]', booksData, OffsetDateTime.now())
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the operation takes less than #expectedDuration and memory used is within limit'
            recordAndAssertResourceUsage("Writing ${totalBooks} books", expectedDuration, durationInSeconds, memoryLimit, resourceMeter.getTotalMemoryUsageInMB())
        cleanup:
            cpsDataService.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor', OffsetDateTime.now())
            cpsAnchorService.deleteAnchor(CPS_PERFORMANCE_TEST_DATASPACE, 'writeAnchor')
        where:
            totalBooks || expectedDuration | memoryLimit
            800        || 0.5              | 50
            1600       || 1.5              | 100
            3200       || 6                | 150
            6400       || 18               | 200
    }

}
