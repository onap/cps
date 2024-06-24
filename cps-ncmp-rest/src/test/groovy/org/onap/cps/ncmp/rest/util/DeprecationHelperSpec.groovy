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

package org.onap.cps.ncmp.rest.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryApiParameters
import org.onap.cps.ncmp.api.inventory.models.ConditionApiProperties
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters
import org.onap.cps.ncmp.rest.model.ConditionProperties
import org.onap.cps.ncmp.rest.model.ModuleNameAsJsonObject
import org.onap.cps.ncmp.rest.model.OldConditionProperties
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class DeprecationHelperSpec extends Specification {

    DeprecationHelper deprecationHelper = new DeprecationHelper(new JsonObjectMapper(new ObjectMapper()))

    def 'Map deprecated condition properties - #scenario.'() {
        given: 'a deprecated condition properties'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            cmHandleQueryParameters.conditions = oldConditionPropertiesArray
            cmHandleQueryParameters.cmHandleQueryParameters = cmHandleQueryParametersArray
        when: 'converted into the new format'
            def result = deprecationHelper.mapOldConditionProperties(cmHandleQueryParameters)
        then: 'result is the expected'
            assert result == new CmHandleQueryApiParameters(cmHandleQueryParameters: expectedCmHandleQueryApiParametersArray)
        where:
            scenario                           | oldConditionPropertiesArray                                                                                                   | cmHandleQueryParametersArray                                                                           || expectedCmHandleQueryApiParametersArray
            'mapping old query'                | [new OldConditionProperties(name: 'hasAllModule', conditionParameters: [new ModuleNameAsJsonObject(moduleName: 'module-1')])] | []                                                                                                     || [new ConditionApiProperties(conditionName: 'hasAllModule', conditionParameters: [[moduleName:'module-1']])]
            'old condition is null'            | null                                                                                                                          | []                                                                                                     || []
            'old condition parameters is null' | [new OldConditionProperties(name: 'hasAllModule', conditionParameters: null)]                                                 | []                                                                                                     || []
            'old condition name is null'       | [new OldConditionProperties(name: null, conditionParameters: [new ModuleNameAsJsonObject(moduleName: 'module-1')])]           | []                                                                                                     || []
            'new query parameters are filled'  | [new OldConditionProperties(name: 'hasAllModule', conditionParameters: [new ModuleNameAsJsonObject(moduleName: 'module-1')])] | [new ConditionProperties(conditionName: 'hasAllModule', conditionParameters: [[moduleName:'module-2']])] || [new ConditionApiProperties(conditionName: 'hasAllModule', conditionParameters: [[moduleName:'module-2']])]
    }
}
