/*
 *  ============LICENSE_START=======================================================
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
package org.onap.cps.ncmp.impl.provmns

import spock.lang.Specification

class RequestParametersSpec extends Specification {

    def objectUnderTest = new RequestParameters()

    def 'Generate target FDN #scenario.'() {
        given: 'request parameters with URI LDN first part, class name and id'
            objectUnderTest.uriLdnFirstPart = uriLdnFirstPart
            objectUnderTest.className = 'myClass'
            objectUnderTest.id = 'myId'
        when: 'target FDN is generated'
            def result = objectUnderTest.toTargetFdn()
        then: 'the target FDN is as expected'
            result == expectedTargetFdn
        where: 'the following uri first part is used'
            scenario           | uriLdnFirstPart || expectedTargetFdn
            'with segments'    | '/segment1'     || '/segment1/myClass=myId'
            'empty first part' | ''              || '/myClass=myId'
    }
}
