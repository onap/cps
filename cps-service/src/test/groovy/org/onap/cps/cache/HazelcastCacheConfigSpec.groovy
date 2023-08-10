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

package org.onap.cps.cache

import spock.lang.Specification

class HazelcastCacheConfigSpec extends Specification {

    def objectUnderTest = new HazelcastCacheConfig()

    def 'Create Hazelcast instance with a #scenario'() {
        given: 'a cluster name'
            objectUnderTest.clusterName = 'my cluster'
        when: 'an hazelcast instance is created (name has to be unique)'
            def result = objectUnderTest.createHazelcastInstance(scenario, config)
        then: 'the instance is created and has the correct name'
            assert result.name == scenario
        and: 'if applicable it has a map config with the expected name'
            if (expectMapConfig) {
                assert result.config.mapConfigs.values()[0].name == 'my map config'
            } else {
                assert result.config.mapConfigs.isEmpty()
            }
        and: 'if applicable it has a queue config with the expected name'
            if (expectQueueConfig) {
                assert result.config.queueConfigs.values()[0].name == 'my queue config'
            } else {
                assert result.config.queueConfigs.isEmpty()
            }
        and: 'if applicable it has a set config with the expected name'
            if (expectSetConfig) {
                assert result.config.setConfigs.values()[0].name == 'my set config'
            } else {
                assert result.config.setConfigs.isEmpty()
            }
        where: 'the following configs are used'
            scenario       | config                                                    || expectMapConfig | expectQueueConfig | expectSetConfig
            'Map Config'   | HazelcastCacheConfig.createMapConfig('my map config')     || true            | false             | false
            'Queue Config' | HazelcastCacheConfig.createQueueConfig('my queue config') || false           | true              | false
            'Set Config'   | HazelcastCacheConfig.createSetConfig('my set config')     || false           | false             | true
    }

}
