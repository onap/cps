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

import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.sync.executor.AsyncTaskExecutor
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class ModuleSyncWatchdogSpec extends Specification {

    def mockSyncUtils = Mock(SyncUtils)

    def static testQueueCapacity = 50 + 2 * ModuleSyncWatchdog.MODULE_SYNC_BATCH_SIZE

    BlockingQueue<DataNode> moduleSyncWorkQueue = new ArrayBlockingQueue(testQueueCapacity)

    def moduleSyncStartedOnCmHandles = [:]

    def mockModuleSyncTasks = Mock(ModuleSyncTasks)

    def asyncTaskExecutor = new AsyncTaskExecutor()

    def objectUnderTest = new ModuleSyncWatchdog(mockSyncUtils, moduleSyncWorkQueue , moduleSyncStartedOnCmHandles,
            mockModuleSyncTasks, asyncTaskExecutor)

    void setup() {
        asyncTaskExecutor.asyncTaskParallelismLevel = 3;
        asyncTaskExecutor.setupThreadPool();
    }

    def 'Module sync #scenario , #numberOfAdvisedCmHandles advised cm handles.'() {
        given: 'sync utilities returns #numberOfAdvisedCmHandles advised cm handles'
            mockSyncUtils.getAdvisedCmHandles() >> createDataNodes(numberOfAdvisedCmHandles)
        when: ' module sync is started'
            objectUnderTest.moduleSyncAdvisedCmHandles()
        then: 'it performs #expectedNumberOfTaskExecutions tasks'
            expectedNumberOfTaskExecutions * mockModuleSyncTasks.performModuleSync(*_)
        where:
            scenario              |  numberOfAdvisedCmHandles                                         || expectedNumberOfTaskExecutions
            'less then 1 batch'   | 1                                                                 || 1
            'exactly 1 batch'     | ModuleSyncWatchdog.MODULE_SYNC_BATCH_SIZE                         || 1
            '2 batches'           | 2 * ModuleSyncWatchdog.MODULE_SYNC_BATCH_SIZE                     || 2
            'queue capacity'      | testQueueCapacity                                                 || 3
            'over queue capacity' | testQueueCapacity + 2 * ModuleSyncWatchdog.MODULE_SYNC_BATCH_SIZE || 3
    }

    def 'Reset failed cm handles.'() {
        given: 'sync utilities returns failed cm handles'
            def failedCmHandles = [new YangModelCmHandle()]
            mockSyncUtils.getModuleSyncFailedCmHandles() >> failedCmHandles
        when: ' reset failed cm handles is started'
            objectUnderTest.resetPreviouslyFailedCmHandles()
            Thread.sleep(1000)
        then: 'it is delegated to the module sync task (service)'
            1 * mockModuleSyncTasks.resetFailedCmHandles(failedCmHandles)
    }

    def createDataNodes(numberOfDataNodes) {
        def dataNodes = []
        (1..numberOfDataNodes).each {dataNodes.add(new DataNode())}
        return dataNodes
    }
}
