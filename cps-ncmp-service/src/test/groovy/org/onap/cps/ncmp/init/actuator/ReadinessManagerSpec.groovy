/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.init.actuator;

import spock.lang.Specification;

class ReadinessManagerSpec extends Specification {

    def readinessManager = new ReadinessManager()

    def 'Default state of CPS service is not ready'() {
        expect: 'by default, isReady() must return false without any state change'
            assert readinessManager.isReady() == false
    }

    def 'CPS service is ready'() {
        when: 'when the readiness is explicitly marked as ready'
            readinessManager.markReady()
        then: 'then readiness state should be true'
            assert readinessManager.isReady() == true
    }

    def 'CPS service is not ready'() {
        given: 'CPS service has already been marked ready'
            readinessManager.markReady()
        when: 'when the readiness is marked as not ready'
            readinessManager.markNotReady()
        then: 'then readiness state should be false'
            assert readinessManager.isReady() == false
    }
}
