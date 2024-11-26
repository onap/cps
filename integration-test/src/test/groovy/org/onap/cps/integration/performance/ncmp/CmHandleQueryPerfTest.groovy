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

import org.onap.cps.api.CpsQueryService
import org.onap.cps.integration.ResourceMeter
import org.onap.cps.integration.performance.base.NcmpPerfTestBase

import java.util.stream.Collectors

import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

class CmHandleQueryPerfTest extends NcmpPerfTestBase {

    static def MILLISECONDS = 0.001

    CpsQueryService objectUnderTest
    ResourceMeter resourceMeter = new ResourceMeter()

    def setup() { objectUnderTest = cpsQueryService }

    def 'JVM warmup.'() {
        when: 'the JVM is warmed up'
            def iterations = 2500 // set this to 15000 for very accurate results (but test takes much longer)
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
                    30, resourceMeter.totalTimeInSeconds,
                    300, resourceMeter.totalMemoryUsageInMB)
    }

    def 'Query CM Handle IDs by a property name and value.'() {
        when: 'a cps-path query on name-value pair is performed (without getting descendants)'
            resourceMeter.start()
            def cpsPath = '//additional-properties[@name="neType" and @value="RadioNode"]/ancestor::cm-handles'
            def dataNodes = objectUnderTest.queryDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR, cpsPath, OMIT_DESCENDANTS)
        and: 'the ids of the result are extracted and converted to xpath'
            def xpaths = dataNodes.stream().map(dataNode -> "/dmi-registry/cm-handles[@id='${dataNode.leaves.id}']".toString() ).collect(Collectors.toSet())
        and: 'a single get is executed to get all the parent objects and their descendants'
            def result = cpsDataService.getDataNodesForMultipleXpaths(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR, xpaths, INCLUDE_ALL_DESCENDANTS)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
        then: 'the required operations are performed within required time'
            recordAndAssertResourceUsage("CpsPath Registry attributes Query", 3.96, durationInSeconds, 400, resourceMeter.getTotalMemoryUsageInMB())
        and: 'all nodes are returned'
            result.size() == TOTAL_CM_HANDLES
        and: 'the tree contains all the expected descendants too'
            assert countDataNodesInTree(result) == 5 * TOTAL_CM_HANDLES
    }

    def 'CM-handle is looked up by id.'() {
        when: 'CM-handles are looked up by cm-handle-id 100 times'
            int count = 0
            resourceMeter.start()
            (1..100).each {
                count += cpsDataService.getDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR,
                        '/dmi-registry/cm-handles[@id="cm-' + it + '"]', OMIT_DESCENDANTS).size()
            }
            resourceMeter.stop()
        then:
            assert count == 100
        and: 'average performance is as expected'
            def averageResponseTime = resourceMeter.totalTimeInSeconds / 100
            recordAndAssertResourceUsage('Look up CM-handle by id',
                    expectedAverageResponseTime, averageResponseTime,
                    15, resourceMeter.totalMemoryUsageInMB)
        where:
            expectedAverageResponseTime = 8 * MILLISECONDS
    }

    def 'CM-handle is looked up by alternate-id.'() {
        when: 'CM-handles are looked up by alternate-id 100 times'
            int count = 0
            resourceMeter.start()
            (1..100).each {
                count += cpsQueryService.queryDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR,
                        '/dmi-registry/cm-handles[@alternate-id="alt-' + it + '"]', OMIT_DESCENDANTS).size()
            }
            resourceMeter.stop()
        then:
            assert count == 100
        and: 'average performance is as expected'
            def averageResponseTime = resourceMeter.totalTimeInSeconds / 100
            recordAndAssertResourceUsage('Look up CM-handle by alternate-id',
                    expectedAverageResponseTime, averageResponseTime,
                    15, resourceMeter.totalMemoryUsageInMB)
        where:
            expectedAverageResponseTime = 20 * MILLISECONDS
    }

    def 'A batch of CM-handles is looked up by alternate-id.'() {
        given: 'a CPS Path Query to look up 100 alternate-ids in a single operation'
            def cpsPathQuery = '/dmi-registry/cm-handles[' + (1..100).collect { "@alternate-id='alt-${it}'" }.join(' or ') + ']'
        when: 'CM-handles are looked up by alternate-ids in a single query'
            resourceMeter.start()
            def count = cpsQueryService.queryDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR, cpsPathQuery, OMIT_DESCENDANTS).size()
            resourceMeter.stop()
        then: 'expected amount of data was returned'
            assert count == 100
        then: 'average performance is as expected'
            def averageResponseTime = resourceMeter.totalTimeInSeconds / 100
            recordAndAssertResourceUsage('Batch look up CM-handle by alternate-id',
                    expectedAverageResponseTime, averageResponseTime,
                    15, resourceMeter.totalMemoryUsageInMB)
        where:
            expectedAverageResponseTime = 4 * MILLISECONDS
    }

    def 'Find any CM-handle given moduleSetTag when there are 20K READY handles with same moduleSetTag.'() {
        given:
            def cpsPathQuery = "/dmi-registry/cm-handles[@module-set-tag='my-module-set-tag']"
        when: 'CM-handles are looked up by module-set-tag 100 times'
            int count = 0
            resourceMeter.start()
            (1..100).each {
                count += cpsQueryService.queryDataNodes(NCMP_PERFORMANCE_TEST_DATASPACE, REGISTRY_ANCHOR, cpsPathQuery, OMIT_DESCENDANTS).size()
            }
            resourceMeter.stop()
        then:
            assert count == TOTAL_CM_HANDLES * 100
        then: 'average performance is as expected'
            def averageResponseTime = resourceMeter.totalTimeInSeconds / 100
            recordAndAssertResourceUsage('Look up CM-handles by module-set-tag',
                    expectedAverageResponseTime, averageResponseTime,
                    500, resourceMeter.totalMemoryUsageInMB)
        where:
            expectedAverageResponseTime = 438 * MILLISECONDS
    }

}
