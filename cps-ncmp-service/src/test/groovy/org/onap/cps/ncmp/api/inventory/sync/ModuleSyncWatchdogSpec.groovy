/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap

import org.onap.cps.ncmp.api.impl.event.lcm.LcmEventsCmHandleStateHandler
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification


class ModuleSyncWatchdogSpec extends Specification {

    def mockSyncUtils = Mock(SyncUtils)

    BlockingQueue<DataNode> moduleSyncWorkQueue = new ArrayBlockingQueue(1000)

    def moduleSyncStartedOnCmHandles = [:]

    def mockModuleSyncTasks = Mock(ModuleSyncTasks)

    def objectUnderTest = new ModuleSyncWatchdog(mockSyncUtils, moduleSyncWorkQueue , moduleSyncStartedOnCmHandles, mockModuleSyncTasks)

    def 'test'() {
        expect:
            objectUnderTest != null
    }
}
