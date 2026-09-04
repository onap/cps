/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

/*
 * TEMPORARY - CI QUALITY GATE TEST ONLY - DO NOT MERGE - DELETE BEFORE REVIEW
 * Fully covers SonarGateProbe so the JaCoCo 100% coverage gate passes.
 */

package org.onap.cps.utils

import spock.lang.Specification

class SonarGateProbeSpec extends Specification {

    def objectUnderTest = new SonarGateProbe()

    def 'Generate token of the requested length #length.'() {
        when: 'a token of the given length is generated'
            def token = objectUnderTest.generateToken(length)
        then: 'the token has the expected length'
            token.length() == length
        and: 'the token contains only digits'
            token.every { Character.isDigit(it as char) }
        where: 'the following lengths are used'
            length << [0, 1, 8]
    }
}
