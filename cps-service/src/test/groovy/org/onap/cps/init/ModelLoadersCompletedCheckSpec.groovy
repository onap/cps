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

import org.onap.cps.init.actuator.ReadinessManager
import org.onap.cps.utils.Sleeper
import spock.lang.Specification

class ModelLoadersCompletedCheckSpec extends Specification {

    def mockModelLoaderLock = Mock(ModelLoaderLock)
    def spiedSleeper = Spy(new Sleeper())
    def mockReadinessManager = Mock(ReadinessManager)

    def objectUnderTest = new ModelLoadersCompletedCheck(mockModelLoaderLock, null, null, null, null, mockReadinessManager, spiedSleeper)

    def 'Model Loaders Completed Check for master instance.'() {
        given: 'instance is master'
            objectUnderTest.isMaster = true
        when: 'model loader is started'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'the model loader lock is released'
            1 * mockModelLoaderLock.forceUnlock()
        and: 'no checks for locked are made'
            0 * mockModelLoaderLock.isLocked()
    }

    def 'Model Loaders Completed Check for non-master instance.'() {
        given: 'instance is NOT master'
            objectUnderTest.isMaster = false
        and: 'the lock will be unlocked upon the third time checking (so expect 3 calls)'
            3 * mockModelLoaderLock.isLocked() >>> [true, true, false ]
        when: 'model loader is started'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'the system sleeps twice'
            2 * spiedSleeper.haveALittleRest(_)
    }

    def 'Model Loaders Completed Check for non-master with exception during sleep.'() {
        given: 'instance is NOT master'
            objectUnderTest.isMaster = false
        and: 'attempting the get the lock will succeed on the second attempt'
            mockModelLoaderLock.isLocked() >>> [true, false ]
        when: 'model loader is started'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'the system sleeps once (but is interrupted)'
            1 * spiedSleeper.haveALittleRest(_) >> { throw new InterruptedException() }
    }

}
