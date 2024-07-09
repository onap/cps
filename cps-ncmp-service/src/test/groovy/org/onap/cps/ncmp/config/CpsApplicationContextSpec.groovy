/*
 * ============LICENSE_START========================================================
 * Copyright (c) 2023-2024 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification;

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
@ContextConfiguration(classes = [CpsApplicationContext.class])
class CpsApplicationContextSpec extends Specification {

    def 'Verify if cps application context contains a requested bean.'() {
        when: 'cps bean is requested from application context'
            def jsonObjectMapper = CpsApplicationContext.getCpsBean(JsonObjectMapper.class)
        then: 'requested bean of JsonObjectMapper is not null'
            assert jsonObjectMapper != null
    }
}
