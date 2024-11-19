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

package org.onap.cps.ncmp.impl.cache;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NamedConfig;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.config.RestEndpointGroup;
import com.hazelcast.config.SetConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * Core infrastructure of the hazelcast distributed cache.
 */
@Slf4j
public class HazelcastCacheConfig {

    @Value("${hazelcast.cluster-name}")
    protected String clusterName;

    @Value("${hazelcast.instance-config-name}")
    protected String instanceConfigName;

    @Value("${hazelcast.mode.kubernetes.enabled}")
    protected boolean cacheKubernetesEnabled;

    @Value("${hazelcast.mode.kubernetes.service-name}")
    protected String cacheKubernetesServiceName;

    protected HazelcastInstance getOrCreateHazelcastInstance(final NamedConfig namedConfig) {
        return Hazelcast.getOrCreateHazelcastInstance(defineInstanceConfig(instanceConfigName, namedConfig));

    }

    private Config defineInstanceConfig(final String instanceConfigName, final NamedConfig namedConfig) {
        final Config config = getHazelcastInstanceConfig(instanceConfigName);
        config.setClusterName(clusterName);
        config.setClassLoader(org.onap.cps.spi.model.Dataspace.class.getClassLoader());
        configureDataStructures(namedConfig, config);
        exposeClusterInformation(config);
        updateDiscoveryMode(config);
        return config;
    }

    private static void configureDataStructures(final NamedConfig namedConfig, final Config config) {
        if (namedConfig instanceof MapConfig) {
            config.addMapConfig((MapConfig) namedConfig);
        }
        if (namedConfig instanceof QueueConfig) {
            config.addQueueConfig((QueueConfig) namedConfig);
        }
        if (namedConfig instanceof SetConfig) {
            config.addSetConfig((SetConfig) namedConfig);
        }
    }

    private Config getHazelcastInstanceConfig(final String instanceConfigName) {
        final HazelcastInstance hazelcastInstance = Hazelcast.getHazelcastInstanceByName(instanceConfigName);
        Config config = null;
        if (hazelcastInstance != null) {
            config = hazelcastInstance.getConfig();
        } else {
            config = new Config(instanceConfigName);
        }
        return config;
    }

    protected static MapConfig createMapConfig(final String configName) {
        final MapConfig mapConfig = new MapConfig(configName);
        mapConfig.setBackupCount(1);
        return mapConfig;
    }

    protected static QueueConfig createQueueConfig(final String configName) {
        final QueueConfig commonQueueConfig = new QueueConfig(configName);
        commonQueueConfig.setBackupCount(1);
        return commonQueueConfig;
    }

    protected static SetConfig createSetConfig(final String configName) {
        final SetConfig commonSetConfig = new SetConfig(configName);
        commonSetConfig.setBackupCount(1);
        return commonSetConfig;
    }

    protected void updateDiscoveryMode(final Config config) {
        if (cacheKubernetesEnabled) {
            log.info("Enabling kubernetes mode with service-name : {}", cacheKubernetesServiceName);
            config.getNetworkConfig().getJoin().getKubernetesConfig().setEnabled(true)
                .setProperty("service-name", cacheKubernetesServiceName);
        }
    }

    protected void exposeClusterInformation(final Config config) {
        config.getNetworkConfig().getRestApiConfig().setEnabled(true)
                .enableGroups(RestEndpointGroup.HEALTH_CHECK, RestEndpointGroup.CLUSTER_READ);
    }

}
