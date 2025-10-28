/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.init

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.instance.impl.HazelcastInstanceFactory
import spock.lang.Specification

class ModelLoaderCoordinatorLockSpec extends Specification {
    def cpsCommonLocks = HazelcastInstanceFactory.getOrCreateHazelcastInstance(new Config('hazelcastInstanceName')).getMap('cpsCommonLocks')

    def objectUnderTest = new ModelLoaderCoordinatorLock(cpsCommonLocks)

    def cleanupSpec() {
        Hazelcast.getHazelcastInstanceByName('hazelcastInstanceName').shutdown()
    }

    def 'Locking and unlocking the coordinator lock.'() {
        when: 'try to get a lock'
            assert objectUnderTest.tryLock() == true
        then: 'the lock is acquired'
            assert objectUnderTest.isLocked() == true
        and: 'can get the same lock again (reentrant locking)'
            assert objectUnderTest.tryLock() == true
        when: 'release the lock'
            objectUnderTest.forceUnlock()
        then: 'the lock is released'
            assert objectUnderTest.isLocked() == false
    }

}
