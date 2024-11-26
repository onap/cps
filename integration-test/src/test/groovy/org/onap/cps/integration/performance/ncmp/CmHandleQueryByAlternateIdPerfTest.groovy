/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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
import org.onap.cps.integration.performance.base.NcmpPerfTestBase
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher

import java.util.stream.Collectors

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DATASPACE_NAME
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS

class CmHandleQueryByAlternateIdPerfTest extends NcmpPerfTestBase {

    AlternateIdMatcher objectUnderTest
    ResourceMeter resourceMeter = new ResourceMeter()

    def setup() { objectUnderTest = alternateIdMatcher }

    def 'Query cm handle by longest match alternate id'() {
        when: 'an alternate id as cps path query'
            resourceMeter.start()
            def cpsPath = "/a/b/c/d-5/e/f/g/h/i"
            def dataNodes = objectUnderTest.getYangModelCmHandleByLongestMatchingAlternateId(cpsPath, '/')
        and: 'the ids of the result are extracted and converted to xpath'
            def cpsXpaths = dataNodes.stream().map(dataNode -> "/dmi-registry/cm-handles[@id='${dataNode.leaves.id}']".toString() ).collect(Collectors.toSet())
        and: 'a single get is executed to get all the parent objects and their descendants'
            cpsDataService.getDataNodesForMultipleXpaths(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cpsXpaths, OMIT_DESCENDANTS)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
            print 'Total time in seconds to query ch handle by alternate id: ' + durationInSeconds
        then: 'the required operations are performed within required time and memory limit'
            recordAndAssertResourceUsage('Look up cm-handle by longest match alternate-id', 1, durationInSeconds, 300, resourceMeter.getTotalMemoryUsageInMB())
    }
}
