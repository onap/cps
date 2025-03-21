/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.config

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import spock.lang.Specification

class MicroMeterConfigSpec extends Specification {

    def objectUnderTest = new MicroMeterConfig()
    def simpleMeterRegistry = new SimpleMeterRegistry()

    def 'Creating a timed aspect.'() {
        expect: 'a timed aspect can be created'
            assert objectUnderTest.timedAspect(simpleMeterRegistry) != null
    }

    def 'Creating JVM process metrics.'() {
        expect: 'process memory metrics can be created'
            assert objectUnderTest.processMemoryMetrics() != null
        and: 'process thread metrics can be created'
            assert objectUnderTest.processThreadMetrics() != null
    }

}
