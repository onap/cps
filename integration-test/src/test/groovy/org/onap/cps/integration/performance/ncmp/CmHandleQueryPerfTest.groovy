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

package org.onap.cps.integration.performance.ncmp

import org.onap.cps.integration.ResourceMeter
import java.util.stream.Collectors
import org.onap.cps.api.CpsQueryService
import org.onap.cps.integration.performance.base.NcmpRegistryPerfTestBase
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS

class CmHandleQueryPerfTest extends NcmpRegistryPerfTestBase {

    CpsQueryService objectUnderTest
    ResourceMeter resourceMeter = new ResourceMeter()

    def setup() { objectUnderTest = cpsQueryService }

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
            def durationInMillis = resourceMeter.getTotalTimeMillis()
        then: 'the required operations are performed within 1200 ms'
            recordAndAssertPerformance("CpsPath Registry attributes Query", 250, durationInMillis, 150, resourceMeter.getTotalMemoryUsedMB())
        and: 'all but 1 (other node) are returned'
            result.size() == 999
        and: 'the tree contains all the expected descendants too'
            assert countDataNodesInTree(result) == 5 * 999
    }

}
