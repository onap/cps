/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ri.utils


import org.onap.cps.spi.api.PaginationOption
import org.onap.cps.spi.api.exceptions.DataValidationException
import spock.lang.Specification

class CpsValidatorImplSpec extends Specification {

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

    def 'Validating a list of valid names.'() {
        given: 'a list of valid names'
            def names = ['valid-name', 'another-valid-name']
        when: 'a list of strings is validated'
            objectUnderTest.validateNameCharacters(names)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'Validating a list of names with invalid names.'() {
        given: 'a list of names with an invalid name'
            def names = ['valid-name', 'name with spaces']
        when: 'a list of strings is validated'
            objectUnderTest.validateNameCharacters(names)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }

    def 'Validate Pagination option with invalid page index and size.'() {
        when: 'the pagination option is validated using invalid options'
            objectUnderTest.validatePaginationOption(new PaginationOption(-5, -2))
        then: 'a data validation exception is thrown'
            def exceptionThrown = thrown(DataValidationException)
        and: 'the error was encountered at the following index in #scenario'
            assert exceptionThrown.getDetails().contains("Invalid page index or size")
    }
}
