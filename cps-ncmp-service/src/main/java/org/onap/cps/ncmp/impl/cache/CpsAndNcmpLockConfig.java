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

package org.onap.cps.ncmp.impl.cache;

import com.hazelcast.config.MapConfig;
import com.hazelcast.map.IMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CpsAndNcmpLockConfig extends HazelcastCacheConfig {

    // Lock names for different use cases ( to be used as cpsAndNcmpLock keys)
    public static final String MODULE_SYNC_WORK_QUEUE_LOCK_NAME = "workQueueLock";

    private static final MapConfig cpsAndNcmpLockConfig = createMapConfig("cpsAndNcmpLockConfig");

    /**
     * Distributed instance used for locking purpose for various use cases in cps-and-ncmp.
     * The key of the map entry is name of the lock and should be based on the use case we are locking.
     *
     * @return configured map of lock object to have distributed coordination.
     */
    @Bean
    public IMap<String, String> cpsAndNcmpLock() {
        return getOrCreateHazelcastInstance(cpsAndNcmpLockConfig).getMap("cpsAndNcmpLock");
    }


}
