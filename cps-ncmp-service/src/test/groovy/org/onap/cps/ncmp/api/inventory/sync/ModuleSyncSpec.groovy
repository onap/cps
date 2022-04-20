/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.inventory.sync


import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import spock.lang.Specification

class ModuleSyncSpec extends Specification {

    def mockSyncUtils = Mock(SyncUtils)

    def objectUnderTest = new ModuleSyncWatchdog(mockSyncUtils)

    def 'Schedule a Cm-Handle Sync for ADVISED Cm-Handles'() {
        given: 'a cm handle'
            def yangModelCmHandle1 = new YangModelCmHandle()
            def yangModelCmHandle2 = new YangModelCmHandle()
        and: 'sync utilities return a cm handle twice'
            mockSyncUtils.getAnAdvisedCmHandle() >>> [yangModelCmHandle1, yangModelCmHandle2, null]
        when: 'module sync poll is executed'
            objectUnderTest.executeAdvisedCmHandlePoll()
        then: 'each cm handle is updated to state "READY"'
            1 * mockSyncUtils.updateCmHandleState(yangModelCmHandle1, "READY")
            1 * mockSyncUtils.updateCmHandleState(yangModelCmHandle2, "READY")
    }

}
