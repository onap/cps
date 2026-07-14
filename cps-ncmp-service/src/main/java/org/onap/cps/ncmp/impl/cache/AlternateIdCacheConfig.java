/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.cps.impl.cache.HazelcastCacheConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlternateIdCacheConfig extends HazelcastCacheConfig {

    private static final MapConfig cmHandleIdPerReferenceMapConfig =
            createGenericMapConfig("cmHandleIdPerReferenceMapConfig");

    /**
     * Distributed instance of a bidirectional cm handle reference map.
     * Stores both cmHandleId->cmHandleId and alternateId->cmHandleId
     *
     * @return configured CmHandleIdPerReferenceMap wrapper
     */
    @Bean
    public CmHandleIdPerReferenceMap cmHandleIdPerReferenceMap() {
        final IMap<String, String> iMap =
                getOrCreateHazelcastInstance(cmHandleIdPerReferenceMapConfig).getMap("cmHandleIdPerReference");
        return new CmHandleIdPerReferenceMap(iMap);
    }

}
