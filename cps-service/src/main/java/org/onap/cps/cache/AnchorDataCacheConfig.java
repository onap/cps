/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

import com.hazelcast.config.MapConfig;
import com.hazelcast.map.IMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core infrastructure of the hazelcast distributed cache for anchor data config use cases.
 */
@Configuration
public class AnchorDataCacheConfig extends HazelcastCacheConfig {

    private static final MapConfig anchorDataCacheMapConfig = createMapConfig("anchorDataCacheMapConfig");

    /**
     * Distributed instance of anchor data cache that contains module prefix by anchor name as properties.
     *
     * @return configured map of anchor data cache
     */
    @Bean
    public IMap<String, AnchorDataCacheEntry> anchorDataCache() {
        return createHazelcastInstance("hazelCastInstanceCpsCore", anchorDataCacheMapConfig).getMap("anchorDataCache");
    }
}
