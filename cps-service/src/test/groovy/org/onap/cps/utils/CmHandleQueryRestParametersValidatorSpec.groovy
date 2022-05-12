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
            cmHandleQueryParameters.cmHandleQueryRestParameters = [condition]
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateCmHandleQueryParameters(cmHandleQueryParameters)
        then: 'data validation exception is not thrown'
            noExceptionThrown()
    }

    def 'CM Handle Query validation: wrong properties.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def condition = new ConditionProperties()
            condition.conditionName = 'hasAllProperties'
            condition.conditionParameters = [['':'wrong']]
            cmHandleQueryParameters.cmHandleQueryRestParameters = [condition]
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateCmHandleQueryParameters(cmHandleQueryParameters)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }

    def 'CM Handle Query validation: too many properties.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def condition = new ConditionProperties()
            condition.conditionName = 'hasAllProperties'
            condition.conditionParameters = [[key1:'value1', key2:'value2']]
            cmHandleQueryParameters.cmHandleQueryRestParameters = [condition]
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateCmHandleQueryParameters(cmHandleQueryParameters)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }

    def 'CM Handle Query validation: no condition name.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def condition = new ConditionProperties()
            condition.conditionName = ''
            cmHandleQueryParameters.cmHandleQueryRestParameters = [condition]
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateCmHandleQueryParameters(cmHandleQueryParameters)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }

    def 'CM Handle Query validation: wrong condition name.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def condition = new ConditionProperties()
            condition.conditionName = 'wrong'
            cmHandleQueryParameters.cmHandleQueryRestParameters = [condition]
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateCmHandleQueryParameters(cmHandleQueryParameters)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }

    def 'CM Handle Query validation: empty conditions.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def condition = new ConditionProperties()
            condition.conditionName = 'hasAllProperties'
            condition.conditionParameters = []
            cmHandleQueryParameters.cmHandleQueryRestParameters = [condition]
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateCmHandleQueryParameters(cmHandleQueryParameters)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }

    def 'CM Handle Query validation: empty properties.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def condition = new ConditionProperties()
            condition.conditionName = 'hasAllProperties'
            condition.conditionParameters = [Collections.emptyMap()]
            cmHandleQueryParameters.cmHandleQueryRestParameters = [condition]
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateCmHandleQueryParameters(cmHandleQueryParameters)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }

    def 'CM Handle Query validation: validate module name condition properties - valid query.'() {
        given: 'a condition property'
            def conditionProperty = [moduleName: 'value']
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateModuleNameConditionProperties(conditionProperty)
        then: 'data validation exception is not thrown'
            noExceptionThrown()
    }

    def 'CM Handle Query validation: validate module name condition properties - invalid name.'() {
        given: 'a condition property'
            def conditionProperty = [wrongName: 'value']
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateModuleNameConditionProperties(conditionProperty)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }

    def 'CM Handle Query validation: validate module name condition properties - invalid value.'() {
        given: 'a condition property'
            def conditionProperty = [moduleName: '']
        when: 'validator is invoked'
            CmHandleQueryRestParametersValidator.validateModuleNameConditionProperties(conditionProperty)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }
}
