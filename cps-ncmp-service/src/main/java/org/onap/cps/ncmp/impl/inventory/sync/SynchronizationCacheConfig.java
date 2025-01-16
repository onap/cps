/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2022-2025 Nordix Foundation
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
import com.hazelcast.map.IMap;
import java.util.concurrent.BlockingQueue;
import org.onap.cps.ncmp.impl.cache.HazelcastCacheConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core infrastructure of the hazelcast distributed caches for Module Sync and Data Sync use cases.
 */
@Configuration
public class SynchronizationCacheConfig extends HazelcastCacheConfig {

    public static final int MODULE_SYNC_STARTED_TTL_SECS = 600;
    public static final int DATA_SYNC_SEMAPHORE_TTL_SECS = 1800;

    private static final QueueConfig commonQueueConfig = createQueueConfig("defaultQueueConfig");
    private static final MapConfig moduleSyncStartedConfig =
            createMapConfigWithTimeToLiveInSeconds("moduleSyncStartedConfig", MODULE_SYNC_STARTED_TTL_SECS);
    private static final MapConfig dataSyncSemaphoresConfig = createMapConfig("dataSyncSemaphoresConfig");

    /**
     * Module Sync Distributed Queue Instance.
     *
     * @return queue of cm handle ids that need module sync
     */
    @Bean
    public BlockingQueue<String> moduleSyncWorkQueue() {
        return getOrCreateHazelcastInstance(commonQueueConfig).getQueue("moduleSyncWorkQueue");
    }

    /**
     * Module Sync started (and maybe finished) on cm handles (ids).
     *
     * @return Map of cm handles (ids) and objects (not used really) for which module sync has started or been completed
     */
    @Bean
    public IMap<String, Object> moduleSyncStartedOnCmHandles() {
        return getOrCreateHazelcastInstance(moduleSyncStartedConfig).getMap("moduleSyncStartedOnCmHandles");
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

}
