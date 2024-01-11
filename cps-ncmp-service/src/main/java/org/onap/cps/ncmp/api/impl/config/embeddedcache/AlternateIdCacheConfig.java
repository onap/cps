/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2024 Nordix Foundation
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
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.cache.HazelcastCacheConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AlternateIdCacheConfig extends HazelcastCacheConfig {

    private static final MapConfig alternateIdToCmHandleCacheConfig =
            createMapConfig("alternateIdToCmHandleCacheConfig");

    private static final MapConfig cmHandleToAlternateIdCacheConfig =
            createMapConfig("cmHandleToAlternateIdCacheConfig");

    /**
     * Distributed instance of alternate id cache containing the alternate id for a cm handle id.
     *
     * @return configured map of alternate Ids as keys to cm handle Ids for values.
     */
    @Bean
    public Map<String, String> alternateIdToCmHandle() {
        return createHazelcastInstance("hazelcastInstanceAlternateIdPerCmHandleMap",
                alternateIdToCmHandleCacheConfig).getMap("alternateIdToCmHandle");
    }

    /**
     * Distributed instance of alternate id cache containing the cm handle id for an alternate id.
     *
     * @return configured map of cm handle Ids as keys to alternate Ids for values.
     */
    @Bean
    public Map<String, String> cmHandleToAlternateId() {
        return createHazelcastInstance("hazelcastInstanceCmHandlePerAlternateIdMap",
                cmHandleToAlternateIdCacheConfig).getMap("cmHandleToAlternateId");
    }
}
