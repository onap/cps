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

package org.onap.cps.ncmp.impl.inventory.trustlevel

import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import com.hazelcast.map.impl.proxy.MapProxyImpl
import org.onap.cps.ncmp.api.inventory.models.TrustLevel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [TrustLevelCacheConfig])
class TrustLevelCacheConfigSpec extends Specification {

    @Autowired
    @Qualifier(TrustLevelCacheConfig.TRUST_LEVEL_PER_DMI_PLUGIN)
    private IMap<String, TrustLevel> trustLevelPerDmiPlugin

    @Autowired
    @Qualifier(TrustLevelCacheConfig.TRUST_LEVEL_PER_CM_HANDLE)
    private IMap<String, TrustLevel> trustLevelPerCmHandleId

    def cleanupSpec() {
        Hazelcast.getHazelcastInstanceByName('cps-and-ncmp-hazelcast-instance-test-config').shutdown()
    }

    def 'Trust level cache configurations: #scenario'() {
        given: 'get the relevant cache bean'
            def hazelcastMap = scenario == 'trustlevel per cm handle' ? trustLevelPerCmHandleId : trustLevelPerDmiPlugin
        when: 'get the map config for #scenario'
            def hazelcastInstance = ((MapProxyImpl) hazelcastMap).getNodeEngine().getHazelcastInstance();
            def mapConfig = hazelcastInstance.getConfig().findMapConfig(hazelcastMap.getName())
        then: 'the map config has the correct (default) backup counts'
            assert mapConfig.backupCount == 1
            assert mapConfig.asyncBackupCount == 0
        and: 'near cache is only enabled for trustlevel per plugin'
            assert mapConfig.isNearCacheEnabled() == expectNearCacheEnabled
        where: 'the following caches are used'
            scenario                   | expectNearCacheEnabled
            'trustlevel per cm handle' | true
            'trustlevel per plugin'    | false
    }

}
