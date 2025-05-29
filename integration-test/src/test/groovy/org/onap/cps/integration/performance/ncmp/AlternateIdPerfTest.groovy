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

import org.onap.cps.integration.ResourceMeter
import org.onap.cps.integration.base.CpsIntegrationSpecBase

/**
 * This test does not depend on common performance test data. Hence it just extends the integration spec base.
 */
class AlternateIdPerfTest extends CpsIntegrationSpecBase {

    def resourceMeter = new ResourceMeter()

    def NETWORK_SIZE = 10_000
    def altIdPrefix = '/a=1/b=2/c=3/'

    def setup() {
        registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'tagA', NETWORK_SIZE, 1, altIdPrefix)
    }

    def cleanup() {
        deregisterSequenceOfCmHandles(DMI1_URL, NETWORK_SIZE, 1)
    }

    def 'Alternate Id Lookup Performance.'() {
        when: 'perform a 10 lookups by alternate id'  // Increase to 1,000 for more accurate result while tuning
            resourceMeter.start()
            (1..10).each {
                networkCmProxyInventoryFacade.getNcmpServiceCmHandle("${altIdPrefix}alt=${it}")
            }
            resourceMeter.stop()
        then: 'record the result. Not asserted, just recorded in See https://lf-onap.atlassian.net/browse/CPS-2605'
            println "*** CPS-2605 Execution time: ${resourceMeter.totalTimeInSeconds} ms"
    }

    def 'Alternate Id Longest Match Performance.'() {
        given: 'an offset at 90% of the network size, so matches are not at the start...'
            def offset = (int) (0.9 * NETWORK_SIZE)
        when: 'perform a 100 longest matches'
            resourceMeter.start()
            (1..100).each {
                def target = "${altIdPrefix}alt=${it + offset}/d=4/e=5/f=6/g=7"
                alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(target, "/")
            }
            resourceMeter.stop()
        then: 'record the result. Not asserted, just recorded in See https://lf-onap.atlassian.net/browse/CPS-2743?focusedCommentId=83220'
            println "*** CPS-2743 Execution time: ${resourceMeter.totalTimeInSeconds} ms"
    }
}
