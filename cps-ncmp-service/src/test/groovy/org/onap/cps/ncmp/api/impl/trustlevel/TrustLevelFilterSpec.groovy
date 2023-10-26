/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.trustlevel

import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import spock.lang.Specification

class TrustLevelFilterSpec extends Specification {

    def mockNetworkCmProxyDataService = Mock(NetworkCmProxyDataService)
    def trustLevelPerDmiPlugin = [:]
    def trustLevelPerCmHandle = [ 'my trusted cm handle': TrustLevel.COMPLETE, 'my untrusted cm handle': TrustLevel.NONE ]

    def objectUnderTest = new TrustLevelFilter(mockNetworkCmProxyDataService, trustLevelPerDmiPlugin, trustLevelPerCmHandle)

    def 'Filter cm handle ids for the given trust level'() {
        given: 'the cache has been initialised and "knows" about my-dmi'
            trustLevelPerDmiPlugin.put('my-dmi', TrustLevel.COMPLETE)
        and: 'network data service returns cm handles for my-dmi'
            mockNetworkCmProxyDataService.getAllCmHandleIdsByDmiPluginIdentifier('my-dmi') >> ['my trusted cm handle', 'my untrusted cm handle']
        when: 'attempt to get cm handles trust level COMPLETE'
            def result = objectUnderTest.getCmHandleIdsByTrustLevel(TrustLevel.COMPLETE)
        then: 'the result contain my trusted cm handle'
            assert result.size() == 1
            assert result[0] == 'my trusted cm handle'
    }
}
