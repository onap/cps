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

package org.onap.cps.integration.base

import org.springframework.util.StopWatch

import java.time.OffsetDateTime

class PerformanceSpecBase extends CpsIntegrationSpecBase {

    def static initialized = false

    static def PERFORMANCE_RECORD = []

    def stopWatch = new StopWatch()

    def cleanupSpec() {
        println('#############################################################################')
        println('##        C P S   P E R F O R M A N C E   T E S T   R E S U L T S          ##')
        println('#############################################################################')
        PERFORMANCE_RECORD.sort().each {
            println(it)
        }
        println('#############################################################################')
        PERFORMANCE_RECORD.clear()
    }

    def setup() {
        if (!initialized) {
            setupPerformanceInfraStructure()
            createInitialData()
            initialized = true
        }
    }

    def createInitialData() {
        def data = readResourceFile('largeModelData.json')
        stopWatch.start()
        (1..10).each {
            addAnchorWithData("anchor${it}", data)
        }
        stopWatch.stop()
        def durationInMillis = stopWatch.getTotalTimeMillis()
        recordAndAssertPerformance('Creating anchors with large data tree', 2000, durationInMillis)
    }

    def setupPerformanceInfraStructure() {
        cpsAdminService.createDataspace(PERFORMANCE_TEST_DATASPACE)
        def modelAsString = readResourceFile('largeModel.yang')
        cpsModuleService.createSchemaSet(PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, [largeModel: modelAsString])
    }

    def addAnchorWithData(anchorName, data) {
        cpsAdminService.createAnchor(PERFORMANCE_TEST_DATASPACE, LARGE_SCHEMA_SET, anchorName)
        cpsDataService.saveData(PERFORMANCE_TEST_DATASPACE, anchorName, data, OffsetDateTime.now())
    }

    def recordAndAssertPerformance(String shortTitle, thresholdInMs, recordedTimeInMs) {
        def pass = recordedTimeInMs <= thresholdInMs
        if (shortTitle.length() > 40) {
            shortTitle = shortTitle.substring(0, 40)
        }
        def record = String.format('%2d.%-40s limit%,7d took %,7d ms ', PERFORMANCE_RECORD.size() + 1, shortTitle, thresholdInMs, recordedTimeInMs)
        record += pass ? 'PASS' : 'FAIL'
        PERFORMANCE_RECORD.add(record)
        assert recordedTimeInMs <= thresholdInMs
        return true
    }
}
