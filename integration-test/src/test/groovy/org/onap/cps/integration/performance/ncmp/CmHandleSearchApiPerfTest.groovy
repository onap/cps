/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import org.onap.cps.integration.performance.base.NcmpPerfTestBase
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters

class CmHandleSearchApiPerfTest extends NcmpPerfTestBase {

    def 'CM Handle Query without any parameters (conditions).'() {
        given: 'no cm handle query parameters'
            def noCmHandleQueryParameters = new CmHandleQueryParameters()
        when: 'executing a cm handle (object) query'
            resourceMeter.start()
            def result = networkCmProxyInventoryController.searchCmHandles(noCmHandleQueryParameters, false)
            resourceMeter.stop()
        then: 'the response status is OK'
            assert result.statusCode.value() == 200
        and: 'the response contains all the cm handles in the network'
            assert result.body.size() == TOTAL_CM_HANDLES
        and: 'record the resource usage'
            recordAndAssertResourceUsage('CM Handle API Query Performance A', 6.0 , resourceMeter.totalTimeInSeconds, 0.0)
    }

}
