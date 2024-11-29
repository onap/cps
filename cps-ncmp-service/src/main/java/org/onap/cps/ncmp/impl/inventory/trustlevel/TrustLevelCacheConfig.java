/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory.trustlevel;

import com.hazelcast.config.MapConfig;
import com.hazelcast.map.IMap;
import org.onap.cps.ncmp.api.inventory.models.TrustLevel;
import org.onap.cps.ncmp.impl.cache.HazelcastCacheConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrustLevelCacheConfig extends HazelcastCacheConfig {

    public static final String TRUST_LEVEL_PER_DMI_PLUGIN = "trustLevelPerDmiPlugin";

    public static final String TRUST_LEVEL_PER_CM_HANDLE = "trustLevelPerCmHandle";
    private static final MapConfig trustLevelPerCmHandleCacheConfig =
            createMapConfig("trustLevelPerCmHandleCacheConfig");

    private static final MapConfig trustLevelPerDmiPluginCacheConfig =
            createMapConfig("trustLevelPerDmiPluginCacheConfig");

    /**
     * Distributed instance of trust level cache containing the trust level per cm handle.
     *
     * @return configured map of cm handle name as keys to trust-level for values.
     */
    @Bean(TRUST_LEVEL_PER_CM_HANDLE)
    public IMap<String, TrustLevel> trustLevelPerCmHandle() {
        return getOrCreateHazelcastInstance(trustLevelPerCmHandleCacheConfig).getMap(TRUST_LEVEL_PER_CM_HANDLE);
    }

    /**
     * Distributed instance of trust level cache containing the trust level per dmi plugin service(name).
     *
     * @return configured map of dmi-plugin name as keys to trust-level for values.
     */
    @Bean(TRUST_LEVEL_PER_DMI_PLUGIN)
    public IMap<String, TrustLevel> trustLevelPerDmiPlugin() {
        return getOrCreateHazelcastInstance(
                trustLevelPerDmiPluginCacheConfig).getMap(TRUST_LEVEL_PER_DMI_PLUGIN);
    }

}
