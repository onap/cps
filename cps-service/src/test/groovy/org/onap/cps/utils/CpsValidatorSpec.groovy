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

import org.onap.cps.spi.exceptions.CpsException
import org.onap.cps.spi.exceptions.DataValidationException
import spock.lang.Specification

class CpsValidatorSpec extends Specification {


    def 'Validating a valid string.'() {
        when: 'the string is validated using a valid name'
            CpsValidator.validateNameCharacters('name-with-no-spaces')
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'Validating an invalid string.'() {
        when: 'the string is validated using an invalid name'
            CpsValidator.validateNameCharacters(name)
        then: 'a data validation exception is thrown'
            def exceptionThrown = thrown(DataValidationException)
        and: 'the error was encountered at the following index in #scenario'
            assert exceptionThrown.getDetails().contains(expectedErrorMessage)
        where: 'the following names are used'
            scenario     | name               || expectedErrorMessage
            'position 5' | 'name with spaces' || 'name with spaces invalid token encountered at position 5'
            'position 9' | 'nameWith Space'   || 'nameWith Space invalid token encountered at position 9'
    }

    def 'Validating topic names.'() {
        when: 'the topic name is validated'
            def isValidTopicName = CpsValidator.validateTopicName(topicName)
        then: 'boolean response will be returned for #scenario'
            assert isValidTopicName == booleanResponse
        where: 'the following names are used'
            scenario                  | topicName       || booleanResponse
            'valid topic'             | 'my-topic-name' || true
            'empty topic'             | ''              || false
            'blank topic'             | ' '             || false
            'null topic'              | null            || false
            'invalid non empty topic' | '1_5_*_#'       || false
    }

    def 'Validating Cm Handle READY State'() {
        when: 'the cm handle state is READY'
            CpsValidator.isCmHandleStateReady("READY")
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'Validating Cm Handle with #scenario State' () {
        when: 'the cm handle state is #scenario'
            CpsValidator.isCmHandleStateReady(cmHandleState)
        then: 'a cps exception is thrown'
            def exceptionThrown = thrown(CpsException)
        and: 'the error was encountered at the following index in #scenario'
            assert exceptionThrown.getDetails().contains(expectedErrorMessage)
        where: 'the following states are used'
            scenario   | cmHandleState || expectedErrorMessage
            'LOCKED'   | 'LOCKED'      || 'Cm-Handle state is LOCKED must be "READY"'
            'ADVISED'  | 'ADVISED'     || 'Cm-Handle state is ADVISED must be "READY"'
            'DELETING' | 'DELETING'    || 'Cm-Handle state is DELETING must be "READY"'
    }
}
