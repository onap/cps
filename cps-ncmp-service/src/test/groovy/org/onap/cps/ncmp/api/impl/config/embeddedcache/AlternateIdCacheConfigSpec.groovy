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

package org.onap.cps.ncmp.api.impl.config.embeddedcache

import com.hazelcast.core.Hazelcast
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [AlternateIdCacheConfig])
class AlternateIdCacheConfigSpec extends Specification {

    @Autowired
    private Map<String, String> cmHandleIdPerAlternateId;
    @Autowired
    private Map<String, String> alternateIdPerCmHandleId;

    def 'Embedded (hazelcast) cache for alternate id - cm handle id caches.'() {
        expect: 'system is able to create an instance of the Alternate ID Cache'
            assert null != cmHandleIdPerAlternateId
            assert null != alternateIdPerCmHandleId
        and: 'there are at least 2 instances'
            assert Hazelcast.allHazelcastInstances.size() > 1
        and: 'Alternate ID Caches are present'
            assert Hazelcast.allHazelcastInstances.name.contains('hazelcastInstanceAlternateIdPerCmHandleIdMap')
                    && Hazelcast.allHazelcastInstances.name.contains('hazelcastInstanceCmHandleIdPerAlternateIdMap')
    }

    def 'Verify configs of the alternate id distributed objects.'(){
        when: 'retrieving the map config of module set tag'
            def alternateIdConfig =  Hazelcast.getHazelcastInstanceByName('hazelcastInstanceAlternateIdPerCmHandleIdMap').config
            def alternateIdMapConfig = alternateIdConfig.mapConfigs.get('alternateIdToCmHandleIdCacheConfig')
            def cmHandleIdConfig =  Hazelcast.getHazelcastInstanceByName('hazelcastInstanceCmHandleIdPerAlternateIdMap').config
            def cmHandleIdIdMapConfig = cmHandleIdConfig.mapConfigs.get('cmHandleIdToAlternateIdCacheConfig')
        then: 'the map configs have the correct number of backups'
            assert alternateIdMapConfig.backupCount == 3
            assert alternateIdMapConfig.asyncBackupCount == 3
            assert cmHandleIdIdMapConfig.backupCount == 3
            assert cmHandleIdIdMapConfig.asyncBackupCount == 3
    }
}
