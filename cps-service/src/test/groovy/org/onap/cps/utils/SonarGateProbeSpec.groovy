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
 * Covers every branch of SonarGateProbe so the JaCoCo 100% coverage gate passes.
 */

package org.onap.cps.utils

import spock.lang.Specification

class SonarGateProbeSpec extends Specification {

    def objectUnderTest = new SonarGateProbe()

    def 'Classify covers every branch for #value.'() {
        expect: 'the expected classification is returned'
            objectUnderTest.classify(value) == expectedClassification
        where: 'the following values exercise all branches'
            value || expectedClassification
            5000  || 'gigantic'
            500   || 'huge'
            75    || 'large'
            25    || 'big'
            7     || 'medium'
            3     || 'small'
            -5000 || 'hugely-negative'
            -500  || 'very-negative'
            -50   || 'quite-negative'
            -5    || 'negative'
            0     || 'zero'
    }
}
