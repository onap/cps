/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.integration.performance.base

import org.onap.cps.integration.base.CpsIntegrationSpecBase

import java.text.DecimalFormat

abstract class PerfTestBase extends CpsIntegrationSpecBase {

    static def LARGE_SCHEMA_SET = 'largeSchemaSet'
    static def PERFORMANCE_TXT_RECORDS = []
    static def PERFORMANCE_CSV_RECORDS = []
    static def DEFAULT_TIME_LIMIT_FACTOR = 2                    // Allow 100% margin on top of expected (average) value
    static def VERY_FAST_TEST_THRESHOLD = 0.01                  // Definition of a very vast test (hard to measure)
    static def DEFAULT_TIME_LIMIT_FACTOR_FOR_VERY_FAST_TEST = 3 // Allow 200% margin on very fast test (accuracy is an issue)
    static def REFERENCE_GRAPH = true
    static def CSV_PREFIX = "### CSV ### "                      // To be used in plot-job to extract CSV data from logs
    static def TIME_FORMAT = new DecimalFormat("0.####")

    def cleanupSpec() {
        printTextRecords()
        printCsvRecordsWithPrefix()
    }

    def setup() {
        if (!isInitialised()) {
            setupPerformanceInfraStructure()
            createInitialData()
        }
    }

    abstract def printTitle()

    abstract def isInitialised()

    abstract def setupPerformanceInfraStructure()

    abstract def createInitialData()

    def recordAndAssertResourceUsage(String title, double expectedAverageTimeInSec, double recordedTimeInSec, double memoryUsageInMB, boolean referenceGraph, double timeLimitFactor) {
        def testPassed = recordedTimeInSec <= timeLimitFactor * expectedAverageTimeInSec
        addRecord(title, expectedAverageTimeInSec, recordedTimeInSec, memoryUsageInMB, testPassed)
        if (referenceGraph) {
            addCsvRecord('Reference', title, expectedAverageTimeInSec, recordedTimeInSec)
        }
        addCsvRecord('All', title, expectedAverageTimeInSec, recordedTimeInSec)
        assert recordedTimeInSec <= timeLimitFactor * expectedAverageTimeInSec
        return true
    }

    def recordAndAssertResourceUsage(String title, double expectedAverageTimeInSec, double recordedTimeInSec, double memoryUsageInMB, boolean referenceGraph) {
        def timeLimitFactor = DEFAULT_TIME_LIMIT_FACTOR
        if (expectedAverageTimeInSec <= VERY_FAST_TEST_THRESHOLD) {
            timeLimitFactor = DEFAULT_TIME_LIMIT_FACTOR_FOR_VERY_FAST_TEST
        }
        recordAndAssertResourceUsage(title, expectedAverageTimeInSec, recordedTimeInSec, memoryUsageInMB, referenceGraph, timeLimitFactor)

    }

    def recordAndAssertResourceUsage(String title, double expectedAverageTimeInSec, double recordedTimeInSec, double memoryUsageInMB) {
        recordAndAssertResourceUsage(title, expectedAverageTimeInSec, recordedTimeInSec, memoryUsageInMB, false)
    }

    def addRecord(shortTitle, double expectedAverageTimeInSec, double recordedTimeInSec, double memoryUsageInMB, boolean testPassed) {
        if (shortTitle.length() > 40) {
            shortTitle = shortTitle.substring(0, 40)
        }
        def record = String.format('%2d.%-40s limit %8.3f took %8.3f sec %,8.2f MB used ', PERFORMANCE_TXT_RECORDS.size() + 1, shortTitle, expectedAverageTimeInSec, recordedTimeInSec, memoryUsageInMB)
        record += testPassed ? 'PASS' : 'FAIL'
        PERFORMANCE_TXT_RECORDS.add(record)
    }

    def addCsvRecord(tabName, title, double expectedAverageTimeInSec, double recordedTimeInSec) {
        def csvRecord = String.format('%s,%s,%s,%s', tabName, title, TIME_FORMAT.format(expectedAverageTimeInSec), TIME_FORMAT.format(recordedTimeInSec))
        PERFORMANCE_CSV_RECORDS.add(csvRecord)
    }

    def printTextRecords() {
        println('##################################################################################################')
        printTitle()
        println('##################################################################################################')
        PERFORMANCE_TXT_RECORDS.sort().each {
            println(it)
        }
        PERFORMANCE_TXT_RECORDS.clear()
    }

    def printCsvRecordsWithPrefix() {
        println('#####################################   C S V   F O R M A T   ####################################')
        PERFORMANCE_CSV_RECORDS.sort().each {
            println CSV_PREFIX + it
        }
        PERFORMANCE_CSV_RECORDS.clear()
        println('##################################################################################################')
    }

}
