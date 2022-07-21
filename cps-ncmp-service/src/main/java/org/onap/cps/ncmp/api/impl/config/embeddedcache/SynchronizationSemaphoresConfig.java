/*
 *  ===========LICENSE_START========================================================
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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core infrastructure of the hazelcast distributed map for Module Sync and Data Sync use cases.
 */
@Configuration
public class SynchronizationSemaphoresConfig {

    private static final int TIME_TO_LIVE_IN_SECONDS = (int) TimeUnit.MINUTES.toSeconds(30);

    /**
     * Module Sync Distributed Map Instance.
     *
     * @return configured map of module sync semaphores
     */
    @Bean
    public ConcurrentMap<String, Boolean> moduleSyncSemaphores() {
        return createHazelcastInstance("moduleSyncSemaphores", "moduleSyncSemaphoresConfig")
                .getMap("moduleSyncSemaphores");
    }

    /**
     * Data Sync Distributed Map Instance.
     *
     * @return configured map of data sync semaphores
     */
    @Bean
    public ConcurrentMap<String, Boolean> dataSyncSemaphores() {
        return createHazelcastInstance("dataSyncSemaphores", "dataSyncSemaphoresConfig")
                .getMap("dataSyncSemaphores");
    }

    private HazelcastInstance createHazelcastInstance(
            final String hazelcastInstanceName, final String configMapName) {
        return Hazelcast.newHazelcastInstance(
                initializeDefaultMapConfig(hazelcastInstanceName, configMapName));
    }

    private Config initializeDefaultMapConfig(final String instanceName, final String configName) {
        final Config config = new Config(instanceName);
        final MapConfig mapConfig = new MapConfig(configName);
        mapConfig.setTimeToLiveSeconds(TIME_TO_LIVE_IN_SECONDS);
        mapConfig.setBackupCount(3);
        mapConfig.setAsyncBackupCount(3);
        config.addMapConfig(mapConfig);
        config.setClusterName("synchronization-semaphores");
        return config;
    }
}
