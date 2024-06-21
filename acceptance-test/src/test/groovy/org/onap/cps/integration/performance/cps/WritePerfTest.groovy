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

    static final def WRITE_TEST_ANCHOR = 'writeTestAnchor'

    def 'Writing openroadm data has linear time.'() {
        given: 'an empty anchor exists for openroadm'
            cpsAnchorService.createAnchor(CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, WRITE_TEST_ANCHOR)
        and: 'a list of device nodes to add'
            def jsonData = generateOpenRoadData(totalNodes)
        when: 'device nodes are added'
            resourceMeter.start()
            cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, WRITE_TEST_ANCHOR, jsonData, OffsetDateTime.now())
            resourceMeter.stop()
        then: 'the operation takes less than #expectedDuration and memory used is within limit'
            recordAndAssertResourceUsage("Writing ${totalNodes} devices",
                    expectedDuration, resourceMeter.getTotalTimeInSeconds(),
                    memoryLimit, resourceMeter.getTotalMemoryUsageInMB())
        cleanup:
            cpsAnchorService.deleteAnchor(CPS_PERFORMANCE_TEST_DATASPACE, WRITE_TEST_ANCHOR)
        where:
            totalNodes || expectedDuration | memoryLimit
            50         || 1.98             | 100
            100        || 3.84             | 200
            200        || 8.6              | 400
            400        || 16.37            | 500
    }

    def 'Writing bookstore data has exponential time.'() {
        given: 'an anchor containing a bookstore with a single category'
            cpsAnchorService.createAnchor(CPS_PERFORMANCE_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, WRITE_TEST_ANCHOR)
            def parentNodeData = '{"bookstore": { "categories": [{ "code": 1, "name": "Test", "books" : [] }] }}'
            cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, WRITE_TEST_ANCHOR, parentNodeData, OffsetDateTime.now())
        and: 'a list of books to add'
            def booksData = '{"books":[' + (1..totalBooks).collect {'{ "title": "' + it + '" }' }.join(',') + ']}'
        when: 'books are added'
            resourceMeter.start()
            cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, WRITE_TEST_ANCHOR, '/bookstore/categories[@code=1]', booksData, OffsetDateTime.now())
            resourceMeter.stop()
        then: 'the operation takes less than #expectedDuration and memory used is within limit'
            recordAndAssertResourceUsage("Writing ${totalBooks} books",
                    expectedDuration, resourceMeter.totalTimeInSeconds,
                    memoryLimit, resourceMeter.totalMemoryUsageInMB)
        cleanup:
            cpsAnchorService.deleteAnchor(CPS_PERFORMANCE_TEST_DATASPACE, WRITE_TEST_ANCHOR)
        where:
            totalBooks || expectedDuration | memoryLimit
            800        || 0.38             | 50
            1600       || 0.95             | 100
            3200       || 2.71             | 150
            6400       || 8.08             | 200
    }

    def 'Writing openroadm list data using saveListElements.'() {
        given: 'an anchor and empty container node for openroadm'
            cpsAnchorService.createAnchor(CPS_PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, WRITE_TEST_ANCHOR)
            cpsDataService.saveData(CPS_PERFORMANCE_TEST_DATASPACE, WRITE_TEST_ANCHOR,
                    '{ "openroadm-devices": { "openroadm-device": []}}', now)
        and: 'a list of device nodes to add'
            def innerNode = readResourceDataFile('openroadm/innerNode.json')
            def jsonListData = '{ "openroadm-device": [' +
                    (1..totalNodes).collect { innerNode.replace('NODE_ID_HERE', it.toString()) }.join(',') +
                    ']}'
        when: 'device nodes are added'
            resourceMeter.start()
            cpsDataService.saveListElements(CPS_PERFORMANCE_TEST_DATASPACE, WRITE_TEST_ANCHOR, '/openroadm-devices', jsonListData, OffsetDateTime.now())
            resourceMeter.stop()
        then: 'the operation takes less than #expectedDuration and memory used is within limit'
            recordAndAssertResourceUsage("Saving list of ${totalNodes} devices",
                    expectedDuration, resourceMeter.totalTimeInSeconds,
                    memoryLimit, resourceMeter.totalMemoryUsageInMB)
        cleanup:
            cpsAnchorService.deleteAnchor(CPS_PERFORMANCE_TEST_DATASPACE, WRITE_TEST_ANCHOR)
        where:
            totalNodes || expectedDuration | memoryLimit
            50         || 1.8              | 100
            100        || 3.93             | 200
            200        || 7.77             | 400
            400        || 16.59            | 500
    }

}
