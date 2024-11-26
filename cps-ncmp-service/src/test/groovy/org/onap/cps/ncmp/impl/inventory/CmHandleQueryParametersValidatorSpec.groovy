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

package org.onap.cps.ncmp.impl.inventory

import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryServiceParameters
import org.onap.cps.spi.api.exceptions.DataValidationException
import org.onap.cps.spi.api.model.ConditionProperties
import spock.lang.Specification

class CmHandleQueryParametersValidatorSpec extends Specification {

    def 'CM Handle Query validation: empty query.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
        when: 'validator is invoked'
            CmHandleQueryParametersValidator.validateCmHandleQueryParameters(cmHandleQueryParameters, [])
        then: 'data validation exception is not thrown'
            noExceptionThrown()
    }

    def 'CM Handle Query validation: normal query.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def condition = new ConditionProperties()
            condition.conditionName = 'validConditionName'
            condition.conditionParameters = [['key':'value']]
        cmHandleQueryParameters.cmHandleQueryParameters = [condition]
        when: 'validator is invoked'
            CmHandleQueryParametersValidator.validateCmHandleQueryParameters(cmHandleQueryParameters, ['validConditionName'])
        then: 'data validation exception is not thrown'
            noExceptionThrown()
    }

    def 'CM Handle Query validation: #scenario.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def condition = new ConditionProperties()
            condition.conditionName = conditionName
            condition.conditionParameters = conditionParameters
            cmHandleQueryParameters.cmHandleQueryParameters = [condition]
        when: 'validator is invoked'
            CmHandleQueryParametersValidator.validateCmHandleQueryParameters(cmHandleQueryParameters, ['validConditionName'])
        then: 'a data validation exception is thrown'
            def thrown = thrown(DataValidationException)
        and: 'the exception details contain the correct significant term '
            assert thrown.details.contains(expectedWordInDetails)
        where:
            scenario                 | conditionName        | conditionParameters                || expectedWordInDetails
            'unknown condition name' | 'unknownCondition'   | [['key': 'value']]                 || 'conditionName'
            'no condition name'      | ''                   | [['key': 'value']]                 || 'conditionName'
            'empty conditions'       | 'validConditionName' | []                                 || 'conditionsParameters'
            'empty properties'       | 'validConditionName' | [[:]]                              || 'conditionsParameter'
            'too many properties'    | 'validConditionName' | [[key1: 'value1', key2: 'value2']] || 'conditionsParameter'
            'empty key'              | 'validConditionName' | [['': 'wrong']]                    || 'conditionsParameter'
    }

    def 'CM Handle Query validation: validate module name condition properties - valid query.'() {
        given: 'a condition property'
            def conditionProperty = [moduleName: 'value']
        when: 'validator is invoked'
            CmHandleQueryParametersValidator.validateModuleNameConditionProperties(conditionProperty)
        then: 'data validation exception is not thrown'
            noExceptionThrown()
    }

    def 'CM Handle Query validation: validate module name condition properties - #scenario.'() {
        when: 'validator is invoked'
            CmHandleQueryParametersValidator.validateModuleNameConditionProperties(conditionProperty)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        where:
            scenario        | conditionProperty
            'invalid value' | [moduleName: '']
            'invalid name'  | [wrongName: 'value']
    }

    def 'Validate CmHandle where an exception is thrown due to #scenario.'() {
        when: 'the validator is called on a cps path condition property'
            CmHandleQueryParametersValidator.validateCpsPathConditionProperties(conditionProperty)
        then: 'a data validation exception is thrown'
            def e = thrown(DataValidationException)
        and: 'exception message matches the expected message'
            e.details.contains(exceptionMessage)
        where:
            scenario                              | conditionProperty                               || exceptionMessage
            'more than one condition is supplied' | ['cpsPath':'some-path', 'cpsPath2':'some-path'] || 'Only one condition property is allowed for the CPS path query.'
            'cpsPath key not supplied'            | ['wrong-key':'some-path']                       || 'Wrong CPS path condition property. - expecting "cpsPath" as the condition property.'
            'cpsPath not supplied'                | ['cpsPath':'']                                  || 'Wrong CPS path. - please supply a valid CPS path.'
    }

    def 'No conditions.'() {
        expect: 'no conditions always returns true'
            CmHandleQueryParametersValidator.validateCpsPathConditionProperties([:]) == true
    }

    def 'Validate CmHandle where #scenario.'() {
        when: 'the validator is called on a cps path condition property'
            def result = CmHandleQueryParametersValidator.validateCpsPathConditionProperties(['cpsPath':cpsPath])
        then: 'the expected boolean value is returned'
            result == expectedBoolean
        where:
            scenario                                       | cpsPath                                                || expectedBoolean
            'cpsPath is valid'                             | '/some/valid/path'                                     || true
            'cpsPath attempts to query private properties' | "//additional-properties[@some-property='some-value']" || false
    }
}
