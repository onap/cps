/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.utils

import org.onap.cps.spi.exceptions.DataValidationException
import spock.lang.Specification

class ValidationUtilsSpec extends Specification {


    def 'Validating a valid string.'() {
        given: 'An valid String'
            def validString = 'name-with-no-spaces'
        when: 'the string is validated'
            ValidationUtils.validateFunctionIds(validString)
        then: 'an exception is not thrown'
            noExceptionThrown()
    }

    def 'Validating an invalid string.'() {
        given: 'An Invalid String'
            def invalidString = 'name with spaces'
        when: 'the string is validated'
            ValidationUtils.validateFunctionIds(invalidString)
        then: 'an exception is thrown'
            thrown(DataValidationException)
    }
}
