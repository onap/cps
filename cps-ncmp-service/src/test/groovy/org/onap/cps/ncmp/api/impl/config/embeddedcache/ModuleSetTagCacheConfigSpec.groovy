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

package org.onap.cps.ncmp.api.impl.config.embeddedcache

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import org.onap.cps.spi.model.ModuleReference
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ModuleSetTagCacheConfig])
class ModuleSetTagCacheConfigSpec extends Specification {

    @Autowired
    private Map<String, Collection<ModuleReference>> moduleSetTagCache

    def 'Embedded (hazelcast) caches for module set tag.'() {
        expect: 'system is able to create an instance of a map to hold module set tags'
            assert null != moduleSetTagCache
        and: 'there is at least 1 instance'
            assert Hazelcast.allHazelcastInstances.size() > 0
        and: 'hazelcast instance name of module set tag is correct'
            assert Hazelcast.allHazelcastInstances.name.contains('moduleSetTags')
    }

    def 'Verify configs of module set tag distributed object.'(){
        given: 'the Module Set Tag Map config'
            def moduleSetTagCacheConfig =  Hazelcast.getHazelcastInstanceByName('moduleSetTags').config
            def moduleSetTagMapConfig =  moduleSetTagCacheConfig.mapConfigs.get('moduleSetTagCacheMapConfig')
        and: 'Module Set Tag Map has the correct settings'
            assert moduleSetTagMapConfig.backupCount == 3
            assert moduleSetTagMapConfig.asyncBackupCount == 3
        and: 'all instances are part of same cluster'
            def testClusterName = 'cps-and-ncmp-test-caches'
            assert moduleSetTagCacheConfig.clusterName == testClusterName
    }

    def 'Verify deployment network configs of distributed cache of module set tag object.'() {
        given: 'network config of module set tag cache'
            def moduleSetTagNetworkConfig = Hazelcast.getHazelcastInstanceByName('moduleSetTags').config.networkConfig
        expect: 'module set tag cache has the correct settings'
            assert moduleSetTagNetworkConfig.join.autoDetectionConfig.enabled
            assert !moduleSetTagNetworkConfig.join.kubernetesConfig.enabled
    }

    def 'Verify network of module set tag cache'() {
        given: 'Synchronization config object and test configuration'
        def objectUnderTest = new ModuleSetTagCacheConfig()
        def testConfig = new Config()
        when: 'kubernetes properties are enabled'
            objectUnderTest.cacheKubernetesEnabled = true
            objectUnderTest.cacheKubernetesServiceName = 'test-service-name'
        and: 'method called to update the discovery mode'
            objectUnderTest.updateDiscoveryMode(testConfig)
        then: 'applied properties are reflected'
            assert testConfig.networkConfig.join.kubernetesConfig.enabled
            assert testConfig.networkConfig.join.kubernetesConfig.properties.get('service-name') == 'test-service-name'

    }
}
