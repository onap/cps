/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.cache

import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import com.hazelcast.map.impl.proxy.MapProxyImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

@SpringBootTest(classes = [AdminCacheConfig])
@TestPropertySource(properties = ['hazelcast.instance-config-name=adminCacheConfigSpecInstance'])
class AdminCacheConfigSpec extends Specification {

    @Autowired
    IMap<String, Integer> cmHandlesByState

    def cleanupSpec() {
        Hazelcast.getHazelcastInstanceByName('adminCacheConfigSpecInstance').shutdown()
    }

    def 'Cache map configuration for cm handle state cache.'() {
        given: 'get configuration for cm handle state cache'
            def hazelcastInstance = ((MapProxyImpl) cmHandlesByState).getNodeEngine().getHazelcastInstance()
            def config = hazelcastInstance.getConfig().findMapConfig(cmHandlesByState.getName())
        expect: 'the default configuration is used'
            assert config.name == '*'
        and: 'the configuration has the correct (default) backup counts'
            assert config.backupCount == 1
            assert config.asyncBackupCount == 0
    }

}
