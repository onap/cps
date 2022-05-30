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
import org.onap.cps.spi.model.CmHandleQueryParameters
import org.onap.cps.spi.model.ConditionProperties
import spock.lang.Specification

class CmHandleQueryRestParametersValidatorSpec extends Specification {
    def 'CM Handle Query validation: empty query.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateCmHandleQueryParameters(cmHandleQueryParameters)
        then: 'data validation exception is not thrown'
            noExceptionThrown()
    }

    def 'CM Handle Query validation: normal query.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def condition = new ConditionProperties()
            condition.conditionName = 'hasAllProperties'
            condition.conditionParameters = [[key1:'value1'],[key2:'value2']]
            cmHandleQueryParameters.cmHandleQueryParameters = [condition]
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateCmHandleQueryParameters(cmHandleQueryParameters)
        then: 'data validation exception is not thrown'
            noExceptionThrown()
    }

    def 'CM Handle Query validation: #scenario.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def condition = new ConditionProperties()
            condition.conditionName = conditionName
            condition.conditionParameters = conditionParameters
            cmHandleQueryParameters.cmHandleQueryParameters = [condition]
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateCmHandleQueryParameters(cmHandleQueryParameters)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        where:
            scenario               | conditionName      | conditionParameters
            'empty properties'     | 'hasAllProperties' | [[ : ]]
            'empty conditions'     | 'hasAllProperties' | []
            'wrong condition name' | 'wrong'            | []
            'no condition name'    | ''                 | []
            'too many properties'  | 'hasAllProperties' | [[key1:'value1', key2:'value2']]
            'wrong properties'     | 'hasAllProperties' | [['':'wrong']]
    }

    def 'CM Handle Query validation: validate module name condition properties - valid query.'() {
        given: 'a condition property'
            def conditionProperty = [moduleName: 'value']
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateModuleNameConditionProperties(conditionProperty)
        then: 'data validation exception is not thrown'
            noExceptionThrown()
    }

    def 'CM Handle Query validation: validate module name condition properties - #scenario.'() {
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateModuleNameConditionProperties(conditionProperty)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        where:
            scenario        | conditionProperty
            'invalid value' | [moduleName: '']
            'invalid name'  | [wrongName: 'value']
    }
}
