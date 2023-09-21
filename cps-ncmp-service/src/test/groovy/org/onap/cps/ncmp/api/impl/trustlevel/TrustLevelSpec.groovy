/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.trustlevel

import spock.lang.Specification

class TrustLevelSpec extends Specification {

    def 'Obtain TrustLevel enum by a string value'() {
        when: 'TrustLevel is obtained by per #stringValue'
            def result = TrustLevel.fromString(stringValue)
        then: 'the result is equal to expected result'
            result == expectedResult
        where: 'below scenarios are applicable'
            scenario                         |   stringValue     ||  expectedResult
            'the string value is NONE'       |   'NONE'          ||  TrustLevel.NONE
            'the string value is COMPLETE'   |   'COMPLETE'      ||  TrustLevel.COMPLETE
            'the string value is some value' |   'some-value'    ||  null
    }

}
