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

package org.onap.cps.integration.performance.base

import org.onap.cps.integration.base.CpsIntegrationSpecBase

abstract class PerfTestBase extends CpsIntegrationSpecBase {

    static def LARGE_SCHEMA_SET = 'largeSchemaSet'
    static def PERFORMANCE_RECORD = []

    def cleanupSpec() {
        println('##################################################################################################')
        printTitle()
        println('##################################################################################################')
        PERFORMANCE_RECORD.sort().each {
            println(it)
        }
        println('##################################################################################################')
        PERFORMANCE_RECORD.clear()
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

    def recordAndAssertResourceUsage(String shortTitle, double thresholdInSec, double recordedTimeInSec, memoryLimit, memoryUsageInMB) {
        def pass = recordedTimeInSec <= thresholdInSec && memoryUsageInMB <= memoryLimit
        if (shortTitle.length() > 40) {
            shortTitle = shortTitle.substring(0, 40)
        }
        def record = String.format('%2d.%-40s limit %8.3f took %8.3f sec %,8.2f MB used ', PERFORMANCE_RECORD.size() + 1, shortTitle, thresholdInSec, recordedTimeInSec, memoryUsageInMB)
        record += pass ? 'PASS' : 'FAIL'
        PERFORMANCE_RECORD.add(record)
        assert recordedTimeInSec <= thresholdInSec
        assert memoryUsageInMB <= memoryLimit
        return true
    }
}
