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

import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

import org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.integration.ResourceMeter
import org.onap.cps.integration.performance.base.NcmpPerfTestBase
import java.util.stream.Collectors

class CmHandleQueryByAlternateIdPerfTest extends NcmpPerfTestBase {

    InventoryPersistence objectUnderTest
    ResourceMeter resourceMeter = new ResourceMeter()

    def setup() { objectUnderTest = inventoryPersistence }

    def 'Query cm handle by longest match alternate id'() {
        when: 'an alternate id as cps path query'
            resourceMeter.start()
            def cpsPath = "/a/b/c/d-5/e/f/g/h/i"
            def dataNodes = objectUnderTest.getCmHandleDataNodeByLongestMatchAlternateId(cpsPath, '/')
        and: 'the ids of the result are extracted and converted to xpath'
            def cpsXpaths = dataNodes.stream().map(dataNode -> "/dmi-registry/cm-handles[@id='${dataNode.leaves.id}']".toString() ).collect(Collectors.toSet())
        and: 'a single get is executed to get all the parent objects and their descendants'
            def result = cpsDataService.getDataNodesForMultipleXpaths(NcmpPersistence.NCMP_DATASPACE_NAME, NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR, cpsXpaths, OMIT_DESCENDANTS)
            resourceMeter.stop()
            def durationInSeconds = resourceMeter.getTotalTimeInSeconds()
            print 'Total time in seconds to query ch handle by alternate id: ' + durationInSeconds
        then: 'the required operations are performed within required time and memory limit'
            recordAndAssertResourceUsage('CpsPath Registry attributes Query', 2, durationInSeconds, 300, resourceMeter.getTotalMemoryUsageInMB())
        and: 'associated cm handle is returned'
            assert result[0].leaves.id == 'cm-handle-5'
    }
}
