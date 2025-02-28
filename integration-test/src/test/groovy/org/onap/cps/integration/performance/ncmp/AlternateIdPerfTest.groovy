/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
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
import org.onap.cps.integration.base.CpsIntegrationSpecBase

/**
 * This test does not depend on common performance test data. Hence it just extends the integration spec base.
 */
class AlternateIdPerfTest extends CpsIntegrationSpecBase {

    def resourceMeter = new ResourceMeter()

    def 'Alternate Id Lookup Performance.'() {
        given: 'register 1,000 cm handles (with alternative ids)'
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'tagA', 1000, 1)
        when: 'perform a 1,000 lookups by alternate id'
            resourceMeter.start()
            (1..1000).each {
                networkCmProxyInventoryFacade.getNcmpServiceCmHandle("alt=${it}")
            }
            resourceMeter.stop()
        then: 'record the result. Not asserted, just recorded in See https://lf-onap.atlassian.net/browse/CPS-2605'
            println "*** CPS-2605 Execution time: ${resourceMeter.totalTimeInSeconds} ms"
        cleanup: 'deregister test cm handles'
            deregisterSequenceOfCmHandles(DMI1_URL, 1000, 1)
    }
}
