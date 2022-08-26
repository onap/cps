/*
 * ============LICENSE_START========================================================
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

package org.onap.cps.ncmp.api.impl.config.embeddedcache;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.onap.cps.spi.model.DataNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core infrastructure of the hazelcast distributed caches for Module Sync and Data Sync use cases.
 */
@Configuration
public class SynchronizationCacheConfig {

    private static Map<String, Integer> timeToLivePerConfig = new HashMap<>(3);

    static {
        timeToLivePerConfig.put("moduleSyncWorkQueueConfig", (int) TimeUnit.MINUTES.toSeconds(30));
        timeToLivePerConfig.put("moduleSyncInProgressOnCmHandlesConfig", (int) TimeUnit.MINUTES.toSeconds(1));
        timeToLivePerConfig.put("dataSyncSemaphoreConfig", (int) TimeUnit.MINUTES.toSeconds(30));
    }

    /**
     * Module Sync Distributed Queue Instance.
     *
     * @return queue of cm handles (data nodes) that need module sync
     */
    @Bean
    public BlockingQueue<DataNode> moduleSyncWorkQueue() {
        return createHazelcastInstance("moduleSyncWorkQueue",
            "moduleSyncWorkQueueConfig")
            .getQueue("moduleSyncWorkQueue");
    }

    /**
     * Module Sync Progress.
     *
     * @return Map of cm handles (ids) and objects (not used really) for which module sync has started or been completed
     */
    @Bean
    public Map<String, Object> moduleSyncStartedOnCmHandles() {
        return createHazelcastInstance("moduleSyncStartedOnCmHandles",
            "moduleSyncInProgressOnCmHandlesConfig")
            .getMap("moduleSyncStartedOnCmHandles");
    }

    /**
     * Data Sync Distributed Map Instance.
     *
     * @return configured map of data sync semaphore
     */
    @Bean
    public Map<String, String> dataSyncSemaphores() {
        return createHazelcastInstance("dataSyncSemaphores", "dataSyncSemaphoreConfig")
            .getMap("dataSyncSemaphore");
    }

    private HazelcastInstance createHazelcastInstance(
        final String hazelcastInstanceName, final String configMapName) {
        return Hazelcast.newHazelcastInstance(initializeConfig(hazelcastInstanceName, configMapName));
    }

    private Config initializeConfig(final String instanceName, final String configName) {
        final Config config = new Config(instanceName);
        final MapConfig mapConfig = new MapConfig(configName);
        mapConfig.setTimeToLiveSeconds(timeToLivePerConfig.get(configName));
        mapConfig.setBackupCount(3);
        mapConfig.setAsyncBackupCount(3);
        config.addMapConfig(mapConfig);
        config.setClusterName("synchronization-caches");
        return config;
    }
}
