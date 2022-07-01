/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.impl.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddedModuleSyncCacheConfig {

    public static final String MODULE_SYNC_CM_HANDLE_MAP = "moduleSyncEmbeddedCache";

    @Value("${hazelcast.cache.modulesync.ttl-sec:30000}")
    private Integer timeToLive;
    @Value("${hazelcast.cache.modulesync.cache-backup-count:3}")
    private Integer cacheBackupCount;
    @Value("${hazelcast.cache.modulesync.async-backup-count:3}")
    private Integer cacheAsyncBackupCount;

    /**
     * Module Sync Watchdog Map.
     *
     * @return Distributed map which contains the cmHandleIds for ModuleSync which are worked upon.
     */
    @Bean(name = "moduleSyncCache")
    public Map<String, String> moduleSyncCache() {
        return Hazelcast.newHazelcastInstance(config()).getMap(MODULE_SYNC_CM_HANDLE_MAP);
    }

    private Config config() {
        final Config config = new Config();
        config.addMapConfig(mapConfig());
        return config;
    }

    private MapConfig mapConfig() {
        final MapConfig mapConfig = new MapConfig(MODULE_SYNC_CM_HANDLE_MAP);
        mapConfig.setTimeToLiveSeconds(timeToLive);
        mapConfig.setBackupCount(cacheBackupCount);
        mapConfig.setAsyncBackupCount(cacheAsyncBackupCount);
        return mapConfig;
    }


}
