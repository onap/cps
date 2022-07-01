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
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core infrastructure of the hazelcast distributed map for Module Sync and Data Sync use cases.
 */
@Configuration
public class SynchronizationSemaphoresConfig {

    private static final int TIME_TO_LIVE_SECONDS = 30;

    private static final int BACKUP_COUNT = 3;

    private static final int ASYNC_BACKUP_COUNT = 3;

    @Bean
    public Map<String, String> moduleSyncSemaphore() {
        return Hazelcast.newHazelcastInstance(initializeDefaultMapConfig("moduleSyncSemaphoreConfig"))
                .getMap("moduleSyncSemaphore");
    }

    @Bean
    public Map<String, String> dataSyncSemaphore() {
        return Hazelcast.newHazelcastInstance(initializeDefaultMapConfig("dataSyncSemaphoreConfig"))
                .getMap("dataSyncSemaphore");
    }

    private Config initializeDefaultMapConfig(final String mapConfigName) {
        final Config config = new Config();
        final MapConfig mapConfig = new MapConfig(mapConfigName);
        mapConfig.setTimeToLiveSeconds(TIME_TO_LIVE_SECONDS);
        mapConfig.setBackupCount(BACKUP_COUNT);
        mapConfig.setAsyncBackupCount(ASYNC_BACKUP_COUNT);
        config.addMapConfig(mapConfig);
        return config;
    }
}
