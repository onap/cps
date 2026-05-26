/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 Deutsche Telekom AG
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.impl.query

import org.onap.cps.api.exceptions.DataValidationException
import spock.lang.Shared
import spock.lang.Specification

class CompositeQueryOperatorSpec extends Specification {

    @Shared def AND = CompositeQueryOperator.AND
    @Shared def OR  = CompositeQueryOperator.OR

    @Shared
    def objectUnderTest = CompositeQueryOperator

    def 'Resolve operator from string for #scenario.'() {
        expect: 'the correct operator enum value is returned'
            objectUnderTest.toCompositeQueryOperator(operatorString) == expectedOperator
        where: 'the following variations of operators are used'
            scenario          | operatorString || expectedOperator
            'and lowercase'   | 'and'          || AND
            'and uppercase'   | 'AND'          || AND
            'and mixed case'  | 'And'          || AND
            'and with spaces' | ' and '        || AND
            'or lowercase'    | 'or'           || OR
            'or uppercase'    | 'OR'           || OR
            'or mixed case'   | 'Or'           || OR
    }

    def 'Resolve operator from #scenario string.'() {
        expect: 'AND is returned as the default operator'
            objectUnderTest.toCompositeQueryOperator(operatorString) == AND
        where: 'the following null/blank values are used'
            scenario | operatorString
            'null'   | null
            'empty'  | ''
            'blank'  | '   '
    }

    def 'Resolve operator from unsupported string.'() {
        when: 'an unsupported operator string is provided'
            objectUnderTest.toCompositeQueryOperator('invalid')
        then: 'a DataValidationException is thrown'
            def exception = thrown(DataValidationException)
        and: 'exception has correct message'
            exception.message.contains('Invalid operator: invalid')
    }
}
