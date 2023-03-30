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

package org.onap.cps.spi.impl

import org.onap.cps.spi.model.DataNodeBuilder
import org.springframework.util.StopWatch

class CpsPersistencePerfSpecBase extends CpsPersistenceSpecBase {

    static final String PERF_TEST_DATA = '/data/perf-test.sql'
    static final String PERF_DATASPACE = 'PERF-DATASPACE'
    static final String PERF_ANCHOR = 'PERF-ANCHOR'
    static final String PERF_TEST_PARENT = '/perf-parent-1'

    static def xpathsToAllGrandChildren = []

    static def PERFORMANCE_RECORD = []

    def stopWatch = new StopWatch()

    def cleanupSpec() {
        println('#############################################################################')
        println('##             P E R F O R M A N C E   T E S T   R E S U L T S             ##')
        println('#############################################################################')
        PERFORMANCE_RECORD.sort().each { println(it) }
        PERFORMANCE_RECORD.clear()
    }

    def createLineage(cpsDataPersistenceService, numberOfChildren, numberOfGrandChildren, createLists) {
        xpathsToAllGrandChildren = []
        (1..numberOfChildren).each {
            if (createLists) {
                def xpathFormat = "${PERF_TEST_PARENT}/perf-test-list-${it}[@key='%d']"
                def listElements = goForthAndMultiply(xpathFormat, numberOfGrandChildren)
                cpsDataPersistenceService.addListElements(PERF_DATASPACE, PERF_ANCHOR, PERF_TEST_PARENT, listElements)
            } else {
                def xpathFormat = "${PERF_TEST_PARENT}/perf-test-child-${it}/perf-test-grand-child-%d"
                def grandChildren = goForthAndMultiply(xpathFormat, numberOfGrandChildren)
                def child = new DataNodeBuilder()
                    .withXpath("${PERF_TEST_PARENT}/perf-test-child-${it}")
                    .withChildDataNodes(grandChildren)
                    .build()
                cpsDataPersistenceService.addChildDataNode(PERF_DATASPACE, PERF_ANCHOR, PERF_TEST_PARENT, child)
            }
        }
    }

    def goForthAndMultiply(xpathFormat, numberOfGrandChildren) {
        def grandChildren = []
        (1..numberOfGrandChildren).each {
            def xpath = String.format(xpathFormat as String, it)
            def grandChild = new DataNodeBuilder().withXpath(xpath).build()
            xpathsToAllGrandChildren.add(grandChild.xpath)
            grandChildren.add(grandChild)
        }
        return grandChildren
    }

    def recordAndAssertPerformance(String shortTitle, thresholdInMs, recordedTimeInMs) {
        def pass = recordedTimeInMs <= thresholdInMs
        if (shortTitle.length()>40) {
            shortTitle = shortTitle.substring(0,40)
        }
        def record = String.format('%2d.%-40s limit%,7d took %,7d ms ', PERFORMANCE_RECORD.size()+1, shortTitle, thresholdInMs, recordedTimeInMs)
        record += pass?'PASS':'FAIL'
        PERFORMANCE_RECORD.add(record)
        assert recordedTimeInMs <= thresholdInMs
        return true
    }
}
