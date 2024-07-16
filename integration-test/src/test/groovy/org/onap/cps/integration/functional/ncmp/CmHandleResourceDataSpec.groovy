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

package org.onap.cps.integration.functional.ncmp

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.data.models.CmResourceAddress
import org.onap.cps.ncmp.impl.data.NetworkCmProxyFacade
import spock.util.concurrent.PollingConditions

import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_OPERATIONAL

class CmHandleResourceDataSpec extends CpsIntegrationSpecBase {

    NetworkCmProxyFacade objectUnderTest

    def setup() {
        dmiDispatcher.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
        registerCmHandle(DMI_URL, 'ch-1', NO_MODULE_SET_TAG)
        objectUnderTest = networkCmProxyFacade
    }

    def cleanup() {
        deregisterCmHandle(DMI_URL, 'ch-1')
    }

    def 'CM handle resource data fetched from dmi plugin service.'() {
        when: 'get resource data operational for the given cm resource address is called'
            def cmResourceAddress = new CmResourceAddress(PASSTHROUGH_OPERATIONAL.datastoreName, 'ch-1', 'parent/child')
            objectUnderTest.getResourceDataForCmHandle(cmResourceAddress, '(a=1,b=2)', 'my-client-topic', false, null)
        then: 'dmi resource data url is encoded correctly'
            new PollingConditions().within(10, () -> {
                assert dmiDispatcher.dmiResourceDataUrl != null
            })
            def recordedDmiResourceDataUrl = dmiDispatcher.dmiResourceDataUrl
            assert '/dmi/v1/ch/ch-1/data/ds/ncmp-datastore%3Apassthrough-operational?resourceIdentifier=parent%2Fchild&options=%28a%3D1%2Cb%3D2%29&topic=my-client-topic' == recordedDmiResourceDataUrl
    }
}
