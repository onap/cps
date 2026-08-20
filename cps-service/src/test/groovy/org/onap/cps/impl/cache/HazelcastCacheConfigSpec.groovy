/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.impl.cache

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import spock.lang.Specification

class HazelcastCacheConfigSpec extends Specification {

    def objectUnderTest = new HazelcastCacheConfig()

    def cleanupSpec() {
        Hazelcast.getHazelcastInstanceByName('my instance config').shutdown()
    }

    def 'Create Hazelcast instance with a #scenario'() {
        given: 'a cluster name and instance config name'
            objectUnderTest.clusterName = 'my cluster'
            objectUnderTest.instanceConfigName = 'my instance config'
        when: 'a hazelcast instance is created (name has to be unique)'
            def result = objectUnderTest.getOrCreateHazelcastInstance(hazelcastCacheConfig)
        then: 'the instance is created and has the correct name'
            assert result.name == 'my instance config'
        and: 'if applicable it has a map config with the expected name'
            if (expectMapConfig) {
                assert result.config.mapConfigs.values()[0].name == 'my map config'
            } else {
                assert result.config.queueConfigs.values()[0].name == 'my queue config'
            }
        where: 'the following configs are used'
            scenario       | hazelcastCacheConfig                                             || expectMapConfig
            'Map Config'   | HazelcastCacheConfig.createDefaultMapConfig('my map config')     || true
            'Queue Config' | HazelcastCacheConfig.createDefaultQueueConfig('my queue config') || false
    }

    def 'Verify deployment network configs for Distributed Caches'() {
        given: 'the Trust Level Per Dmi Plugin Cache config'
            def networkingConfig = Hazelcast.getHazelcastInstanceByName('my instance config').config.networkConfig
        expect: 'system created instance with correct config'
            assert networkingConfig.join.autoDetectionConfig.enabled
            assert !networkingConfig.join.kubernetesConfig.enabled
    }

    def 'Enabling Kubernetes Cache.'() {
        given: 'a sample configuration'
            def sampleConfig = new Config()
        when: 'kubernetes properties are enabled/disabled in teh main hazelcast configuration'
            objectUnderTest.cacheKubernetesServiceName = scenario
            objectUnderTest.cacheKubernetesEnabled = cacheKubernetesEnabled
        and: 'Update the discovery mode on the sample configuration'
            objectUnderTest.updateDiscoveryMode(sampleConfig)
        then: 'applied properties are reflected'
            assert sampleConfig.networkConfig.join.kubernetesConfig.enabled == cacheKubernetesEnabled
            if (cacheKubernetesEnabled) {
                assert sampleConfig.networkConfig.join.kubernetesConfig.properties.get('service-name') == scenario
            }
        where: 'kubernetes cache enabled/disabled'
            scenario                    | cacheKubernetesEnabled
            'kubernetes cache enabled'  | true
            'kubernetes cache disabled' | false
    }

}
