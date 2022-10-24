/*
 *  ============LICENSE_START=======================================================
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

package org.onap.cps.spi.cache
import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [AnchorDataCacheConfig])
class AnchorDataCacheConfigSpec extends Specification {

    @Autowired
    private IMap<String, AnchorDataCacheEntry> anchorDataCache

    def 'Embedded (hazelcast) cache for Anchor Data.'() {
        expect: 'system is able to create an instance of the Module Sync Work Queue'
            assert null != anchorDataCache
        and: 'there is at least 1 instance'
            assert Hazelcast.allHazelcastInstances.size() > 0
        and: 'anchorDataCache is present'
            assert Hazelcast.allHazelcastInstances.name.contains('hazelCastInstanceCpsRi')
    }

    def 'Verify configs for Distributed Caches'(){
        given: 'the Anchor Data Cache config'
            def anchorDataCacheConfig =  Hazelcast.getHazelcastInstanceByName('hazelCastInstanceCpsRi').config.mapConfigs.get('anchorDataCacheMapConfig')
        expect: 'system created instance with correct config'
            assert anchorDataCacheConfig.backupCount == 3
            assert anchorDataCacheConfig.asyncBackupCount == 3
    }
}
