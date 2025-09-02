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

package org.onap.cps.init.actuator

import spock.lang.Specification;

class ReadinessManagerSpec extends Specification {

    def readinessManager = new ReadinessManager()

    def "Default state of CPS service is ready when no module loader is registered"() {
        expect: "by default, cps service is Up and running"
            assert readinessManager.isReady() == true
    }

    def "CPS service is not ready when module any loader is running"() {
        when: "a module loader process is registered"
            readinessManager.registerProcess("someModuleLoader")
        then: "cps health check should return DOWN"
            assert readinessManager.isReady() == false
            assert readinessManager.getProcessesAsString() == "someModuleLoader"
    }

    def "CPS service becomes ready after all the module loaders are completes"() {
        given: "a module loader is registered"
            readinessManager.registerProcess("someModuleLoader")
        when: "all the module loaders are marked as completed"
            readinessManager.markProcessComplete("someModuleLoader")
        then: "cps health check returns UP as no module loader is in processes"
            assert readinessManager.isReady() == true
    }
}
