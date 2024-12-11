/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.impl.cache;

import com.hazelcast.config.MapConfig;
import com.hazelcast.map.IMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminCacheConfig extends HazelcastCacheConfig {

    private static final MapConfig adminCacheMapConfig = createMapConfig("adminCacheMapConfig");

    /**
     * Distributed instance admin cache map for cm handles by state for use of gauge metrics.
     *
     * @return configured map of cm handles by state.
     */
    @Bean
    public IMap<String, Integer> cmHandlesByState() {
        final IMap<String, Integer> cmHandlesByState = getOrCreateHazelcastInstance(adminCacheMapConfig).getMap(
                "cmHandlesByState");

        cmHandlesByState.putIfAbsent("advisedCmHandlesCount", 0);
        cmHandlesByState.putIfAbsent("readyCmHandlesCount", 0);
        cmHandlesByState.putIfAbsent("lockedCmHandlesCount", 0);
        cmHandlesByState.putIfAbsent("deletingCmHandlesCount", 0);
        cmHandlesByState.putIfAbsent("deletedCmHandlesCount", 0);

        return cmHandlesByState;
    }
}
