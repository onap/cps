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

import com.hazelcast.collection.ISet;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.SetConfig;
import com.hazelcast.map.IMap;
import org.onap.cps.cache.HazelcastCacheConfig;
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TrustLevelCacheConfig extends HazelcastCacheConfig {

    private static final SetConfig untrustworthyCmHandlesSetConfig =
            createSetConfig("untrustworthyCmHandlesSetConfig");

    private static final MapConfig trustLevelPerDmiPluginCacheConfig =
            createMapConfig("trustLevelPerDmiPluginCacheConfig");

    /**
     * Distributed collection of untrustworthy cm handles.
     *
     * @return instance of distributed set of untrustworthy cm handles.
     */
    @Bean
    public ISet<String> untrustworthyCmHandlesSet() {
        return createHazelcastInstance("untrustworthyCmHandlesSet",
                untrustworthyCmHandlesSetConfig).getSet("untrustworthyCmHandlesSet");
    }

    /**
     * Distributed instance of trust level cache containing the trust level per dmi plugin service(name).
     *
     * @return configured map of dmi-plugin name as keys to trust-level for values.
     */
    @Bean
    public IMap<String, TrustLevel> trustLevelPerDmiPlugin() {
        return createHazelcastInstance("hazelcastInstanceTrustLevelPerDmiPluginMap",
                trustLevelPerDmiPluginCacheConfig).getMap("trustLevelPerDmiPlugin");
    }

}
