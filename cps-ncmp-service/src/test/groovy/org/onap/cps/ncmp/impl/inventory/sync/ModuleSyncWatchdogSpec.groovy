/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory.sync

import com.hazelcast.map.IMap
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.utils.Sleeper
import org.onap.cps.api.model.DataNode
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.Lock

class ModuleSyncWatchdogSpec extends Specification {

    def mockModuleOperationsUtils = Mock(ModuleOperationsUtils)

    def static testQueueCapacity = 50 + 2 * ModuleSyncWatchdog.MODULE_SYNC_BATCH_SIZE

    def moduleSyncWorkQueue = new ArrayBlockingQueue(testQueueCapacity)

    def mockModuleSyncStartedOnCmHandles = Mock(IMap<String, Object>)

    def mockModuleSyncTasks = Mock(ModuleSyncTasks)

    def spiedAsyncTaskExecutor = Spy(AsyncTaskExecutor)

    def mockCpsAndNcmpLock = Mock(IMap<String,String>)

    def spiedSleeper = Spy(Sleeper)

    def objectUnderTest = new ModuleSyncWatchdog(mockModuleOperationsUtils, moduleSyncWorkQueue , mockModuleSyncStartedOnCmHandles, mockModuleSyncTasks, spiedAsyncTaskExecutor, mockCpsAndNcmpLock, spiedSleeper)

    void setup() {
        spiedAsyncTaskExecutor.setupThreadPool()
    }

    def 'Module sync advised cm handles with #scenario.'() {
        given: 'module sync utilities returns #numberOfAdvisedCmHandles advised cm handles'
            mockModuleOperationsUtils.getAdvisedCmHandleIds() >> createCmHandleIds(numberOfAdvisedCmHandles)
        and: 'module sync utilities returns no failed (locked) cm handles'
            mockModuleOperationsUtils.getCmHandlesThatFailedModelSyncOrUpgrade() >> []
        and: 'the work queue is locked'
            mockCpsAndNcmpLock.tryLock("workQueueLock") >> true
        and: 'the executor has enough available threads'
            spiedAsyncTaskExecutor.getAsyncTaskParallelismLevel() >> 3
        when: ' module sync is started'
            objectUnderTest.moduleSyncAdvisedCmHandles()
        then: 'it performs #expectedNumberOfTaskExecutions tasks'
            expectedNumberOfTaskExecutions * spiedAsyncTaskExecutor.executeTask(*_)
        and: 'the executing thread is unlocked'
            1 * mockCpsAndNcmpLock.unlock("workQueueLock")
        where: 'the following parameter are used'
            scenario              | numberOfAdvisedCmHandles                                          || expectedNumberOfTaskExecutions
            'none at all'         | 0                                                                 || 0
            'less then 1 batch'   | 1                                                                 || 1
            'exactly 1 batch'     | ModuleSyncWatchdog.MODULE_SYNC_BATCH_SIZE                         || 1
            '2 batches'           | 2 * ModuleSyncWatchdog.MODULE_SYNC_BATCH_SIZE                     || 2
            'queue capacity'      | testQueueCapacity                                                 || 3
            'over queue capacity' | testQueueCapacity + 2 * ModuleSyncWatchdog.MODULE_SYNC_BATCH_SIZE || 3
    }

    def 'Module sync cm handles starts with no available threads.'() {
        given: 'module sync utilities returns a advise cm handles'
            mockModuleOperationsUtils.getAdvisedCmHandleIds() >> createCmHandleIds(1)
        and: 'the work queue is locked'
            mockCpsAndNcmpLock.tryLock("workQueueLock") >> true
        and: 'the executor first has no threads but has one thread on the second attempt'
            spiedAsyncTaskExecutor.getAsyncTaskParallelismLevel() >>> [ 0, 1 ]
        when: ' module sync is started'
            objectUnderTest.moduleSyncAdvisedCmHandles()
        then: 'it performs one task'
            1 * spiedAsyncTaskExecutor.executeTask(*_)
    }

    def 'Module sync advised cm handle already handled by other thread.'() {
        given: 'module sync utilities returns an advised cm handle'
            mockModuleOperationsUtils.getAdvisedCmHandleIds() >> createCmHandleIds(1)
        and: 'the work queue is locked'
            mockCpsAndNcmpLock.tryLock("workQueueLock") >> true
        and: 'the executor has a thread available'
            spiedAsyncTaskExecutor.getAsyncTaskParallelismLevel() >> 1
        and: 'the semaphore cache indicates the cm handle is already being processed'
            mockModuleSyncStartedOnCmHandles.putIfAbsent(*_) >> 'Started'
        when: ' module sync is started'
            objectUnderTest.moduleSyncAdvisedCmHandles()
        then: 'it does NOT execute a task to process the (empty) batch'
            0 * spiedAsyncTaskExecutor.executeTask(*_)
    }

    def 'Module sync with previous cm handle(s) left in work queue.'() {
        given: 'there is still a cm handle in the queue'
            moduleSyncWorkQueue.offer('ch-1')
        and: 'sync utilities returns many advise cm handles'
            mockModuleOperationsUtils.getAdvisedCmHandleIds() >> createCmHandleIds(500)
        and: 'the executor has plenty threads available'
            spiedAsyncTaskExecutor.getAsyncTaskParallelismLevel() >> 10
        when: ' module sync is started'
            objectUnderTest.moduleSyncAdvisedCmHandles()
        then: 'it does executes only one task to process the remaining handle in the queue'
            1 * spiedAsyncTaskExecutor.executeTask(*_)
    }

    def 'Reset failed cm handles.'() {
        given: 'module sync utilities returns failed cm handles'
            def failedCmHandles = [new YangModelCmHandle()]
            mockModuleOperationsUtils.getCmHandlesThatFailedModelSyncOrUpgrade() >> failedCmHandles
        when: 'reset failed cm handles is started'
            objectUnderTest.setPreviouslyLockedCmHandlesToAdvised()
        then: 'it is delegated to the module sync task (service)'
            1 * mockModuleSyncTasks.setCmHandlesToAdvised(failedCmHandles)
    }

    def 'Module Sync Locking.'() {
        given: 'module sync utilities returns an advised cm handle'
            mockModuleOperationsUtils.getAdvisedCmHandleIds() >> createCmHandleIds(1)
        and: 'can lock is : #canLock'
            mockCpsAndNcmpLock.tryLock("workQueueLock") >> canLock
        when: 'attempt to populate the work queue'
            objectUnderTest.populateWorkQueueIfNeeded()
        then: 'the queue remains empty is #expectQueueRemainsEmpty'
            assert moduleSyncWorkQueue.isEmpty() == expectQueueRemainsEmpty
        and: 'unlock is called only when thread a able to enter the critical section'
            expectedInvocationToUnlock * mockCpsAndNcmpLock.unlock("workQueueLock")
        where: 'the following lock states are applied'
            canLock || expectQueueRemainsEmpty || expectedInvocationToUnlock
            false   || true                    || 0
            true    || false                   || 1
    }

    def 'Sleeper gets interrupted.'() {
        given: 'sleeper gets interrupted'
            spiedSleeper.haveALittleRest(_) >> { throw new InterruptedException() }
        when: 'the watchdog attempts to sleep to save cpu cycles'
            objectUnderTest.preventBusyWait()
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def createCmHandleIds(numberOfCmHandles) {
        return (numberOfCmHandles > 0) ? (1..numberOfCmHandles).collect { 'ch-'+it } : []
    }
}
