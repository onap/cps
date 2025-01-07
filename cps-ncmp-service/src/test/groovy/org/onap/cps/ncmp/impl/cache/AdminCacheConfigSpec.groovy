/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration;
import spock.lang.Specification;

@SpringBootTest
@ContextConfiguration(classes = [AdminCacheConfig])
class AdminCacheConfigSpec extends Specification {

    @Autowired
    IMap<String, Integer> cmHandlesByState

    def cleanupSpec() {
        Hazelcast.getHazelcastInstanceByName('cps-and-ncmp-hazelcast-instance-test-config').shutdown()
    }

    def 'Hazelcast cache for cm handle by state gauge'() {
        expect: 'system is able to create an instance of the cm handle by state cache'
            assert null != cmHandlesByState
        and: 'there is at least 1 instance'
            assert Hazelcast.allHazelcastInstances.size() > 0
        and: 'Hazelcast cache instance for cm handle by state is present'
            assert Hazelcast.getHazelcastInstanceByName('cps-and-ncmp-hazelcast-instance-test-config').getMap('cmHandlesByState') != null
        and: 'the cache already contains 5 entries, an entry for each state'
            def cmHandleByState =  Hazelcast.getHazelcastInstanceByName('cps-and-ncmp-hazelcast-instance-test-config').getMap('cmHandlesByState')
            assert cmHandleByState.size() == 5
            assert cmHandleByState.containsKey('advisedCmHandlesCount')
            assert cmHandleByState.containsKey('lockedCmHandlesCount')
            assert cmHandleByState.containsKey('readyCmHandlesCount')
            assert cmHandleByState.containsKey('deletingCmHandlesCount')
            assert cmHandleByState.containsKey('deletedCmHandlesCount')
    }
}
