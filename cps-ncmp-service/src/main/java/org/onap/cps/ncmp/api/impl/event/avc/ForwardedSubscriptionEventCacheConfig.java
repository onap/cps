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

package org.onap.cps.ncmp.api.impl.event.avc;

import com.hazelcast.config.MapConfig;
import com.hazelcast.map.IMap;
import java.util.Set;
import org.onap.cps.cache.HazelcastCacheConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core infrastructure of the hazelcast distributed cache for subscription forward config use cases.
 */
@Configuration
public class ForwardedSubscriptionEventCacheConfig extends HazelcastCacheConfig {

    private static final MapConfig forwardedSubscriptionEventCacheMapConfig =
        createMapConfig("forwardedSubscriptionEventCacheMapConfig");

    /**
     * Distributed instance of forwarded subscription information cache that contains subscription event
     * id by dmi names as properties.
     *
     * @return configured map of subscription event ids as keys to sets of dmi names for values
     */
    @Bean
    public IMap<String, Set<String>> forwardedSubscriptionEventCache() {
        return createHazelcastInstance("hazelCastInstanceSubscriptionEvents",
                forwardedSubscriptionEventCacheMapConfig).getMap("forwardedSubscriptionEventCache");
    }
}
