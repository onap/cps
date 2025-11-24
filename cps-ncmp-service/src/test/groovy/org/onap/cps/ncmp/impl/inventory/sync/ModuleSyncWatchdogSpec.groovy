/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.hazelcast.map.IMap
import org.onap.cps.init.actuator.ReadinessManager
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.slf4j.LoggerFactory
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue

class ModuleSyncWatchdogSpec extends Specification {

    def mockModuleOperationsUtils = Mock(ModuleOperationsUtils)

    def static testQueueCapacity = 50 + 2 * ModuleSyncWatchdog.MODULE_SYNC_BATCH_SIZE

    def moduleSyncWorkQueue = new ArrayBlockingQueue(testQueueCapacity)

    def mockModuleSyncStartedOnCmHandles = Mock(IMap<String, Object>)

    def mockModuleSyncTasks = Mock(ModuleSyncTasks)

    def mockCpsCommonLocks = Mock(IMap<String,String>)

    def mockReadinessManager = Mock(ReadinessManager)

    def objectUnderTest = new ModuleSyncWatchdog(mockModuleOperationsUtils, moduleSyncWorkQueue , mockModuleSyncStartedOnCmHandles, mockModuleSyncTasks, mockCpsCommonLocks, mockReadinessManager)

    def logAppender = Spy(ListAppender<ILoggingEvent>)

    void setup() {
        def logger = LoggerFactory.getLogger(ModuleSyncWatchdog)
        logger.setLevel(Level.INFO)
        logger.addAppender(logAppender)
        logAppender.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(ModuleSyncWatchdog.class)).detachAndStopAllAppenders()
    }

    def 'Module sync watchdog is triggered'(){
        given: 'the system is not ready to accept traffic'
            mockReadinessManager.isReady() >> false
        when: 'module sync is started'
            objectUnderTest.scheduledModuleSyncAdvisedCmHandles()
        then: 'an event is logged with level INFO'
            def loggingEvent = getLoggingEvent()
            assert loggingEvent.level == Level.INFO
        and: 'the log indicates that the system is not ready yet'
            assert loggingEvent.formattedMessage == 'System is not ready yet'
    }

    def 'Module sync advised cm handles with #scenario.'() {
        given: 'system is ready to accept traffic'
            mockReadinessManager.isReady() >> true
        and: 'module sync utilities returns #numberOfAdvisedCmHandles advised cm handles'
            mockModuleOperationsUtils.getAdvisedCmHandleIds() >> createCmHandleIds(numberOfAdvisedCmHandles)
        and: 'module sync utilities returns no failed (locked) cm handles'
            mockModuleOperationsUtils.getCmHandlesThatFailedModelSyncOrUpgrade() >> []
        and: 'the work queue can be locked'
            mockCpsCommonLocks.tryLock('workQueueLock') >> true
        when: ' module sync is started (using the scheduled method)'
            objectUnderTest.scheduledModuleSyncAdvisedCmHandles()
        then: 'it performs #expectedNumberOfTaskExecutions tasks'
            expectedNumberOfTaskExecutions * mockModuleSyncTasks.performModuleSync(*_)
        and: 'the executing thread is unlocked'
            1 * mockCpsCommonLocks.unlock('workQueueLock')
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
        given: 'system is ready to accept traffic'
            mockReadinessManager.isReady() >> true
        and: 'module sync utilities returns a advise cm handles'
            mockModuleOperationsUtils.getAdvisedCmHandleIds() >> createCmHandleIds(1)
        and: 'the work queue can be locked'
            mockCpsCommonLocks.tryLock('workQueueLock') >> true
        when: ' module sync is started'
            objectUnderTest.moduleSyncAdvisedCmHandles()
        then: 'it performs one task'
            1 * mockModuleSyncTasks.performModuleSync(*_)
    }

    def 'Module sync advised cm handle already handled by other thread.'() {
        given: 'system is ready to accept traffic'
            mockReadinessManager.isReady() >> true
        and: 'module sync utilities returns an advised cm handle'
            mockModuleOperationsUtils.getAdvisedCmHandleIds() >> createCmHandleIds(1)
        and: 'the work queue can be locked'
            mockCpsCommonLocks.tryLock('workQueueLock') >> true
        and: 'the semaphore cache indicates the cm handle is already being processed'
            mockModuleSyncStartedOnCmHandles.putIfAbsent(*_) >> 'Started'
        when: 'module sync is started'
            objectUnderTest.moduleSyncAdvisedCmHandles()
        then: 'it does NOT execute a task to process the (empty) batch'
            0 * mockModuleSyncTasks.performModuleSync(*_)
    }

    def 'Module sync with previous cm handle(s) left in work queue.'() {
        given: 'system is ready to accept traffic'
            mockReadinessManager.isReady() >> true
        and: 'there is still a cm handle in the queue'
            moduleSyncWorkQueue.offer('ch-1')
        when: 'module sync is started'
            objectUnderTest.moduleSyncAdvisedCmHandles()
        then: 'it does executes only one task to process the remaining handle in the queue'
            1 * mockModuleSyncTasks.performModuleSync(*_)
    }

    def 'Reset failed cm handles.'() {
        given: 'system is ready to accept traffic'
            mockReadinessManager.isReady() >> true
        and: 'module sync utilities returns failed cm handles'
            def failedCmHandles = [new YangModelCmHandle()]
            mockModuleOperationsUtils.getCmHandlesThatFailedModelSyncOrUpgrade() >> failedCmHandles
        when: 'reset failed cm handles is started'
            objectUnderTest.setPreviouslyLockedCmHandlesToAdvised()
        then: 'it is delegated to the module sync task (service)'
            1 * mockModuleSyncTasks.setCmHandlesToAdvised(failedCmHandles)
    }

    def 'Module Sync Locking.'() {
        given: 'system is ready to accept traffic'
            mockReadinessManager.isReady() >> true
        and: 'module sync utilities returns an advised cm handle'
            mockModuleOperationsUtils.getAdvisedCmHandleIds() >> createCmHandleIds(1)
        and: 'can be locked is : #canLock'
            mockCpsCommonLocks.tryLock('workQueueLock') >> canLock
        when: 'attempt to populate the work queue'
            objectUnderTest.populateWorkQueueIfNeeded()
        then: 'the queue remains empty is #expectQueueRemainsEmpty'
            assert moduleSyncWorkQueue.isEmpty() == expectQueueRemainsEmpty
        and: 'unlock is called only when thread is able to enter the critical section'
            expectedInvocationToUnlock * mockCpsCommonLocks.unlock('workQueueLock')
        where: 'the following lock states are applied'
            canLock || expectQueueRemainsEmpty || expectedInvocationToUnlock
            false   || true                    || 0
            true    || false                   || 1
    }

    def createCmHandleIds(numberOfCmHandles) {
        return (numberOfCmHandles > 0) ? (1..numberOfCmHandles).collect { 'ch-'+it } : []
    }

    def getLoggingEvent() {
        return logAppender.list[0]
    }
}
