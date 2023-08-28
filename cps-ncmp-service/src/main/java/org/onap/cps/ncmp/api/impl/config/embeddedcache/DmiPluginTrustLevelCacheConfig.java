/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2023 Nordix Foundation
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

import com.hazelcast.config.MapConfig;
import com.hazelcast.map.IMap;
import java.util.Map;
import org.onap.cps.cache.HazelcastCacheConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DmiPluginTrustLevelCacheConfig extends HazelcastCacheConfig {

    private static final MapConfig dmiPluginTrustLevelCacheConfig =
            createMapConfig("dmiPluginTrustLevelCacheConfig");

    /**
     * Distributed instance of trust level cache that contains dmi-plugin name by trust level
     * and health check URL.
     *
     * @return configured map of dmi-plugin name as keys to trust-level and health-check URL for values.
     */
    @Bean
    public IMap<String, Map<String, String>> dmiPluginTrustLevelCache() {
        return createHazelcastInstance("hazelcastInstanceDmiPluginTrustLevelMap",
                dmiPluginTrustLevelCacheConfig).getMap("dmiPluginTrustLevelCache");
    }

}
