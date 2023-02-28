/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.integration.base.CpsPerformanceSpecBase
import org.onap.cps.spi.FetchDescendantsOption

class GetPerformanceSpec extends CpsPerformanceSpecBase {

    def objectUnderTest

    def setup() { objectUnderTest = cpsDataService }

    def 'Read complete data trees from multiple anchors using #scenario.'() {
        when: 'get data nodes for 10 anchors'
            stopWatch.start()
            (1..10).each {
                objectUnderTest.getDataNodes(CpsIntegrationSpecBase.PERFORMANCE_TEST_DATASPACE, "anchor${it}", xpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
            }
            stopWatch.stop()
            def durationInMillis = stopWatch.getTotalTimeMillis()
        then: 'all data is read within #durationLimit ms'
            recordAndAssertPerformance("Read datatrees using ${scenario}", durationLimit, durationInMillis)
        where: 'the following xpaths are used'
            scenario      | xpath        || durationLimit
            'root'        | '/'          || 2400
            'top element' | '/bookstore' ||  800
    }

}
