/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2022 Nordix Foundation
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

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSyncDataStoreConfig {

    @Value("${hazelcast.datasync.ttl-sec:30000}")
    private int timeToLiveSeconds;
    @Value("${hazelcast.datasync.backup-count:3}")
    private int backupCount;
    @Value("${hazelcast.datasync.async-backup-count:3}")
    private int asyncBackupCount;

    /**
     * Data Sync Watchdog Map.
     *
     * @return Distributed map which contains the cmHandleIds for DataSync which are worked upon.
     */
    @Bean(name = "dataSyncDataStore")
    public Map<String, String> dataSyncEmbeddedDataStore() {
        return Hazelcast.newHazelcastInstance(initializeDefaultMapConfig()).getMap("dataSyncEmbeddedDataStore");
    }

    private Config initializeDefaultMapConfig() {
        final Config config = new Config();
        final MapConfig mapConfig = new MapConfig("dataSyncEmbeddedDataStore");
        mapConfig.setTimeToLiveSeconds(timeToLiveSeconds);
        mapConfig.setBackupCount(backupCount);
        mapConfig.setAsyncBackupCount(asyncBackupCount);
        config.addMapConfig(mapConfig);
        return config;
    }
}
