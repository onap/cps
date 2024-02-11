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

package org.onap.cps.integration.performance.ncmp

import java.util.stream.Collectors
import org.onap.cps.api.CpsQueryService
import org.onap.cps.integration.ResourceMeter
import org.onap.cps.integration.performance.base.NcmpPerfTestBase

import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS

class CmHandleQueryPerfTest extends NcmpPerfTestBase {

    CpsQueryService objectUnderTest
    ResourceMeter resourceMeter = new ResourceMeter()

    def setup() { objectUnderTest = cpsQueryService }

    def 'JVM warmup.'() {
        when: 'the JVM is warmed up'
            def iterations = 1000 // set this to 15000 for more accurate results (but test takes much longer)
            resourceMeter.start()
            (1..iterations).forEach {
                cpsDataService.getDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR,
                        '/dmi-registry/cm-handles[@id="cm-' + it + '"]', OMIT_DESCENDANTS)
                objectUnderTest.queryDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR,
                        '/dmi-registry/cm-handles[@alternate-id="alt-' + it + '"]', OMIT_DESCENDANTS)
            }
            resourceMeter.stop()
        then: 'resource usage is as expected'
            recordAndAssertResourceUsage('JVM warmup for CmHandleQueryPerfTest',
                    15, resourceMeter.totalTimeInSeconds,
                    300, resourceMeter.totalMemoryUsageInMB)
    }

    def 'Query CM Handle IDs by a property name and value.'() {
        when: 'a cps-path query on name-value pair is performed (without getting descendants)'
            resourceMeter.start()
            def cpsPath = '//additional-properties[@name="neType" and @value="RadioNode"]/ancestor::cm-handles'
            def dataNodes = objectUnderTest.queryDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR, cpsPath, OMIT_DESCENDANTS)
        and: 'the ids of the result are extracted and converted to xpath'
            def xpaths = dataNodes.stream().limit(999).map(dataNode -> "/dmi-registry/cm-handles[@id='${dataNode.leaves.id}']".toString() ).collect(Collectors.toSet())
        and: 'a single get is executed to get all the parent objects and their descendants'
            def result = cpsDataService.getDataNodesForMultipleXpaths(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR, xpaths, INCLUDE_ALL_DESCENDANTS)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the required operations are performed within required time'
            recordAndAssertResourceUsage("CpsPath Registry attributes Query", 0.4, durationInSeconds, 50, resourceMeter.getTotalMemoryUsageInMB())
        and: 'all but 1 (other node) are returned'
            result.size() == 999
        and: 'the tree contains all the expected descendants too'
            assert countDataNodesInTree(result) == 5 * 999
    }

    def 'CM-handle is looked up by id.'() {
        given: 'we will take average time of a number of lookups'
            int numberOfLookups = 100
        when: 'CM-handles are looked up by ID'
            int count = 0
            resourceMeter.start()
            for (int i = 1; i <= numberOfLookups; i++) {
                count += cpsDataService.getDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR,
                        '/dmi-registry/cm-handles[@id="cm-' + i + '"]', OMIT_DESCENDANTS).size()
            }
            resourceMeter.stop()
        then: 'expected amount of data was returned'
            assert count == numberOfLookups
        and: 'performance is as expected'
            recordAndAssertResourceUsage('Look up CM-handle by id',
                    0.002, resourceMeter.totalTimeInSeconds / numberOfLookups,
                    15, resourceMeter.totalMemoryUsageInMB)
    }

    def 'CM-handle is looked up by alternate-id.'() {
        given: 'we will take average time of a number of lookups'
            int numberOfLookups = 100
        when: 'CM-handles are looked up by alternate ID'
            int count = 0
            resourceMeter.start()
            for (int i = 1; i <= numberOfLookups; i++) {
                count += cpsQueryService.queryDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR,
                        '/dmi-registry/cm-handles[@alternate-id="alt-' + i + '"]', OMIT_DESCENDANTS).size()
            }
            resourceMeter.stop()
        then: 'expected amount of data was returned'
            assert count == numberOfLookups
        and: 'performance is as expected'
            recordAndAssertResourceUsage('Look up CM-handle by alternate-id',
                    0.010, (double)(resourceMeter.totalTimeInSeconds / numberOfLookups),
                    15, resourceMeter.totalMemoryUsageInMB)
    }

}
