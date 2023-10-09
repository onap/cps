/*
 *  ============LICENSE_START=======================================================
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
import java.util.Collection;
import java.util.Map;
import org.onap.cps.cache.HazelcastCacheConfig;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModuleSetTagCacheConfig extends HazelcastCacheConfig {

    private static final MapConfig moduleSetTagCacheMapConfig = createMapConfig("moduleSetTagCacheMapConfig");

    /**
     * Map instance for cached ModulesSetTags.
     *
     * @return configured map of ModuleSetTags
     */
    @Bean
    public Map<String, Collection<ModuleReference>> moduleSetTagCache() {
        return createHazelcastInstance("moduleSetTags", moduleSetTagCacheMapConfig)
                .getMap("moduleSetTagCache");
    }
}
