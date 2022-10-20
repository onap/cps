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

package org.onap.cps.spi.impl.utils

import org.onap.cps.spi.exceptions.DataValidationException
import spock.lang.Specification

class CpsValidatorSpec extends Specification {

    def objectUnderTest = new CpsValidatorImpl()

    def 'Validating a valid string.'() {
        when: 'the string is validated using a valid name'
            objectUnderTest.validateNameCharacters('name-with-no-spaces')
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'Validating an invalid string.'() {
        when: 'the string is validated using an invalid name'
            objectUnderTest.validateNameCharacters(name)
        then: 'a data validation exception is thrown'
            def exceptionThrown = thrown(DataValidationException)
        and: 'the error was encountered at the following index in #scenario'
            assert exceptionThrown.getDetails().contains(expectedErrorMessage)
        where: 'the following names are used'
            scenario     | name               || expectedErrorMessage
            'position 5' | 'name with spaces' || 'name with spaces invalid token encountered at position 5'
            'position 9' | 'nameWith Space'   || 'nameWith Space invalid token encountered at position 9'
    }
}
