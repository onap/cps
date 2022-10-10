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
import com.hazelcast.config.NamedConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import java.util.concurrent.BlockingQueue;
import org.onap.cps.spi.model.DataNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core infrastructure of the hazelcast distributed caches for Module Sync and Data Sync use cases.
 */
@Configuration
public class SynchronizationCacheConfig {

    private static final QueueConfig commonQueueConfig = createQueueConfig();
    private static final MapConfig moduleSyncStartedConfig = createMapConfig("moduleSyncStartedConfig");
    private static final MapConfig dataSyncSemaphoresConfig = createMapConfig("dataSyncSemaphoresConfig");

    /**
     * Module Sync Distributed Queue Instance.
     *
     * @return queue of cm handles (data nodes) that need module sync
     */
    @Bean
    public BlockingQueue<DataNode> moduleSyncWorkQueue() {
        return createHazelcastInstance("moduleSyncWorkQueue", commonQueueConfig)
            .getQueue("moduleSyncWorkQueue");
    }

    /**
     * Module Sync started (and maybe finished) on cm handles (ids).
     *
     * @return Map of cm handles (ids) and objects (not used really) for which module sync has started or been completed
     */
    @Bean
    public IMap<String, Object> moduleSyncStartedOnCmHandles() {
        return createHazelcastInstance("moduleSyncStartedOnCmHandles", moduleSyncStartedConfig)
            .getMap("moduleSyncStartedOnCmHandles");
    }

    /**
     * Data Sync Distributed Map Instance.
     *
     * @return configured map of data sync semaphores
     */
    @Bean
    public IMap<String, Boolean> dataSyncSemaphores() {
        return createHazelcastInstance("dataSyncSemaphores", dataSyncSemaphoresConfig)
            .getMap("dataSyncSemaphores");
    }

    private HazelcastInstance createHazelcastInstance(
        final String hazelcastInstanceName, final NamedConfig namedConfig) {
        return Hazelcast.newHazelcastInstance(initializeConfig(hazelcastInstanceName, namedConfig));
    }

    private Config initializeConfig(final String instanceName, final NamedConfig namedConfig) {
        final Config config = new Config(instanceName);
        if (namedConfig instanceof MapConfig) {
            config.addMapConfig((MapConfig) namedConfig);
        }
        if (namedConfig instanceof QueueConfig) {
            config.addQueueConfig((QueueConfig) namedConfig);
        }
        config.setClusterName("synchronization-caches");
        return config;
    }

    private static QueueConfig createQueueConfig() {
        final QueueConfig commonQueueConfig = new QueueConfig("defaultQueueConfig");
        commonQueueConfig.setBackupCount(3);
        commonQueueConfig.setAsyncBackupCount(3);
        return commonQueueConfig;
    }

    private static MapConfig createMapConfig(final String configName) {
        final MapConfig mapConfig = new MapConfig(configName);
        mapConfig.setBackupCount(3);
        mapConfig.setAsyncBackupCount(3);
        return mapConfig;
    }

}
