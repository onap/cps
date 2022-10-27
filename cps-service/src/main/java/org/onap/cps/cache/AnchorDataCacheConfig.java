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

package org.onap.cps.cache;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NamedConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core infrastructure of the hazelcast distributed cache for anchor data config use cases.
 */
@Configuration
public class AnchorDataCacheConfig {

    private static final MapConfig anchorDataCacheMapConfig = createMapConfig("anchorDataCacheMapConfig");

    /**
     * Distributed instance of anchor data cache that contains module prefix by anchor name as properties.
     *
     * @return configured map of anchor data cache
     */
    @Bean
    public IMap<String, AnchorDataCacheEntry> anchorDataCache() {
        return createHazelcastInstance("hazelCastInstanceCpsCore", anchorDataCacheMapConfig)
                .getMap("anchorDataCache");
    }

    private HazelcastInstance createHazelcastInstance(final String hazelcastInstanceName,
            final NamedConfig namedConfig) {
        return Hazelcast.newHazelcastInstance(initializeConfig(hazelcastInstanceName, namedConfig));
    }

    private Config initializeConfig(final String instanceName, final NamedConfig namedConfig) {
        final Config config = new Config(instanceName);
        config.addMapConfig((MapConfig) namedConfig);
        config.setClusterName("cps-service-caches");
        return config;
    }

    private static MapConfig createMapConfig(final String configName) {
        final MapConfig mapConfig = new MapConfig(configName);
        mapConfig.setBackupCount(3);
        mapConfig.setAsyncBackupCount(3);
        return mapConfig;
    }

}
