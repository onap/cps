/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.spi.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.model.ConditionProperties
import spock.lang.Specification

class ConditionPropertiesSpec extends Specification {

    ObjectMapper objectMapper = new ObjectMapper()

    def 'Condition Properties JSON conversion.'() {
        given: 'a condition properties'
            def objectUnderTest = new ConditionProperties(conditionName: 'test', conditionParameters: [[key: 'value' ] ])
        expect: 'the name is blank'
            assert objectMapper.writeValueAsString(objectUnderTest) == '{"conditionName":"test","conditionParameters":[{"key":"value"}]}'
    }

}
