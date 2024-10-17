/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory.sync;

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.cp.lock.FencedLock;
import com.hazelcast.map.IMap;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.impl.cache.HazelcastCacheConfig;
import org.onap.cps.spi.model.DataNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core infrastructure of the hazelcast distributed caches for Module Sync and Data Sync use cases.
 */
@Slf4j
@Configuration
public class SynchronizationCacheConfig extends HazelcastCacheConfig {

    public static final int MODULE_SYNC_STARTED_TTL_SECS = 600;
    public static final int DATA_SYNC_SEMAPHORE_TTL_SECS = 1800;

    private static final QueueConfig commonQueueConfig = createQueueConfig("defaultQueueConfig");
    private static final MapConfig moduleSyncStartedConfig = createMapConfig("moduleSyncStartedConfig");
    private static final MapConfig dataSyncSemaphoresConfig = createMapConfig("dataSyncSemaphoresConfig");
    private static final String LOCK_NAME_FOR_WORK_QUEUE = "workQueueLock";

    /**
     * Module Sync Distributed Queue Instance.
     *
     * @return queue of cm handles (data nodes) that need module sync
     */
    @Bean
    public BlockingQueue<DataNode> moduleSyncWorkQueue() {
        return getOrCreateHazelcastInstance(commonQueueConfig).getQueue("moduleSyncWorkQueue");
    }

    /**
     * Module Sync started (and maybe finished) on cm handles (ids).
     *
     * @return Map of cm handles (ids) and objects (not used really) for which module sync has started or been completed
     */
    @Bean
    public IMap<String, Object> moduleSyncStartedOnCmHandles() {
        return getOrCreateHazelcastInstance(moduleSyncStartedConfig).getMap(
                "moduleSyncStartedOnCmHandles");
    }

    /**
     * Data Sync Distributed Map Instance.
     *
     * @return configured map of data sync semaphores
     */
    @Bean
    public IMap<String, Boolean> dataSyncSemaphores() {
        return getOrCreateHazelcastInstance(dataSyncSemaphoresConfig).getMap("dataSyncSemaphores");
    }

    /**
     * Retrieves a distributed lock used to control access to the work queue for module synchronization.
     * This lock ensures that the population and modification of the work queue are thread-safe and
     * protected from concurrent access across different nodes in the distributed system.
     * The lock guarantees that only one instance of the application can populate or modify the
     * module sync work queue at a time, preventing race conditions and potential data inconsistencies.
     * The lock is obtained using the Hazelcast CP Subsystem's {@link FencedLock}, which provides
     * strong consistency guarantees for distributed operations.
     * If the lock cannot be acquired (due to an error or timeout), an {@link IllegalStateException}
     * is thrown, signaling a failure to secure the lock for modifying the work queue.
     * Example use case includes controlling access to adding new CM handles to the work queue when
     * multiple instances are running in parallel.
     *
     * @return a {@link FencedLock} instance used for synchronizing access to the work queue.
     * @throws IllegalStateException if the lock cannot be retrieved or created.
     */
    @Bean
    public FencedLock workQueueLock() {
        try {
            final CPSubsystem cpSubsystem = Hazelcast.newHazelcastInstance().getCPSubsystem();
            final FencedLock workQueueLock = cpSubsystem.getLock(LOCK_NAME_FOR_WORK_QUEUE);
            log.debug("Successfully created lock '{}' for work queue.", LOCK_NAME_FOR_WORK_QUEUE);
            return workQueueLock;
        } catch (final Exception e) {
            log.error("Error while creating lock for work queue. {}", e.getMessage());
            throw new IllegalStateException("Error while creating lock for work queue.", e);
        }
    }
}
