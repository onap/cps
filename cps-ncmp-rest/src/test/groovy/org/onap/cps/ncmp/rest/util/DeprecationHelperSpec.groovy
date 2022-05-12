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
import org.onap.cps.ncmp.api.models.CmHandleQueryApiParameters
import org.onap.cps.ncmp.api.models.ConditionApiProperties
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters
import org.onap.cps.ncmp.rest.model.ModuleNameAsJsonObject
import org.onap.cps.ncmp.rest.model.OldConditionProperties
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class DeprecationHelperSpec extends Specification {

    DeprecationHelper deprecationHelper = new DeprecationHelper(new JsonObjectMapper(new ObjectMapper()))

    def 'Map deprecated condition properties.'() {
        given: 'a deprecated condition properties'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def oldConditionProperties = new OldConditionProperties()
            def moduleNameAsJsonObject = new ModuleNameAsJsonObject()
            moduleNameAsJsonObject.moduleName = 'module-1'
            oldConditionProperties.name = 'hasAllModule'
            oldConditionProperties.conditionParameters = [moduleNameAsJsonObject]
            cmHandleQueryParameters.conditions = [oldConditionProperties]

            def cmHandleQueryApiParameters = new CmHandleQueryApiParameters()
            def conditionApiProperties = new ConditionApiProperties()
            conditionApiProperties.conditionName = 'hasAllModule'
            conditionApiProperties.conditionParameters = [[moduleName:'module-1']]
            cmHandleQueryApiParameters.cmHandleQueryParameters = [conditionApiProperties]
        when: 'converted into the new format'
            def result = deprecationHelper.mapOldConditionProperties(cmHandleQueryParameters)
        then: 'result is the expected'
            assert result == cmHandleQueryApiParameters
    }
}
