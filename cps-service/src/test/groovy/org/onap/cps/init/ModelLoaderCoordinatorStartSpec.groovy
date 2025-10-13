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
import spock.lang.Specification

class ModelLoaderCoordinatorStartSpec extends Specification {

    def mockModelLoaderCoordinatorLock = Mock(ModelLoaderCoordinatorLock)
    def mockReadinessManager = Mock(ReadinessManager)

    def objectUnderTest = new ModelLoaderCoordinatorStart(mockModelLoaderCoordinatorLock, mockReadinessManager)

    def 'Model Loader Coordinator Master check.'() {
        given: 'can get lock is: #canLock'
            mockModelLoaderCoordinatorLock.tryLock() >> canLock
        when: 'application is started event triggers the start coordinator'
            objectUnderTest.onApplicationEvent(null)
        then: 'the master flag state is equals to can-lock result'
            assert objectUnderTest.isMaster() == canLock
        and: 'the coordinator END process is registered with the readiness manager'
            1 * mockReadinessManager.registerStartupProcess('ModelLoaderCoordinatorEnd')
        where: 'both can-lock options are tested'
            canLock << [ true, false ]
    }

}
