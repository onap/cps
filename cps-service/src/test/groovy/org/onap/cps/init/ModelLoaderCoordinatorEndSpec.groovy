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

class ModelLoaderCoordinatorEndSpec extends Specification {

    def mockModelLoaderCoordinatorLock = Mock(ModelLoaderCoordinatorLock)
    def spiedSleeper = Spy(new Sleeper())
    def mockReadinessManager = Mock(ReadinessManager)

    def objectUnderTest = new ModelLoaderCoordinatorEnd(mockModelLoaderCoordinatorLock, null, null, null, null, mockReadinessManager, spiedSleeper)

    def 'Model Loader Coordinator End for master instance.'() {
        given: 'instance is master'
            objectUnderTest.isMaster = true
        when: 'model loader is started'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'the model loader coordinator lock is released'
            1 * mockModelLoaderCoordinatorLock.forceUnlock()
        and: 'no checks for locked are made'
            0 * mockModelLoaderCoordinatorLock.isLocked()
    }

    def 'Model Loader Coordinator End for non-master instance.'() {
        given: 'instance is NOT master'
            objectUnderTest.isMaster = false
        and: 'the lock will be unlocked upon the third time checking (so expect 3 calls)'
            3 * mockModelLoaderCoordinatorLock.isLocked() >>> [ true, true, false ]
        when: 'model loader is started'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'the system sleeps twice'
            2 * spiedSleeper.haveALittleRest(_)
    }

    def 'Model Loader Coordinator End for non-master with exception during sleep.'() {
        given: 'instance is NOT master'
            objectUnderTest.isMaster = false
        and: 'attempting the get the lock will succeed on the second attempt'
            mockModelLoaderCoordinatorLock.isLocked() >>> [ true, false ]
        when: 'model loader is started'
            objectUnderTest.onboardOrUpgradeModel()
        then: 'the system sleeps once (but is interrupted)'
            1 * spiedSleeper.haveALittleRest(_) >> { throw new InterruptedException() }
    }

}
