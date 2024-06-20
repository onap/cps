/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.utils.url.builder

import spock.lang.Specification

class DmiServiceUrlTemplateBuilderSpec extends Specification {

    def objectUnderTest = new DmiServiceUrlTemplateBuilder()

    def 'Build URL template parameters with (variable) path segments and query parameters.'() {
        given: 'the URL details are given to the builder'
            objectUnderTest.fixedPathSegment('segment')
            objectUnderTest.variablePathSegment('myVariableSegment','someValue')
            objectUnderTest.fixedPathSegment('segment?with:special&characters')
            objectUnderTest.queryParameter('param1', 'abc')
            objectUnderTest.queryParameter('param2', 'value?with#special:characters')
        when: 'the URL template parameters are created'
            def result = objectUnderTest.createUrlTemplateParameters('myDmiServer', 'myBasePath')
        then: 'the URL template contains variable names instead of value and un-encoded fixed segment'
            assert result.urlTemplate == 'myDmiServer/myBasePath/v1/segment/{myVariableSegment}/segment?with:special&characters?param1={param1}&param2={param2}'
        and: 'URL variables contains name and un-encoded value pairs'
            assert result.urlVariables == ['myVariableSegment': 'someValue', 'param1': 'abc', 'param2': 'value?with#special:characters']
    }

    def 'Build URL template parameters with special characters in query parameters.'() {
        given: 'the query parameter is given to the builder'
           objectUnderTest.queryParameter('my&param', 'special&characters=are?not\\encoded')
        when: 'the URL template parameters are created'
            def result = objectUnderTest.createUrlTemplateParameters('myDmiServer', 'myBasePath')
        then: 'Special characters are not encoded'
            assert result.urlVariables == ['my&param': 'special&characters=are?not\\encoded']
    }

    def 'Build URL template parameters with empty query parameters.'() {
        when: 'the query parameter is given to the builder'
            objectUnderTest.queryParameter('param', value)
        and: 'the URL template parameters are create'
            def result = objectUnderTest.createUrlTemplateParameters('myDmiServer', 'myBasePath')
        then: 'no parameter gets added'
            assert result.urlVariables.isEmpty()
        where: 'the following parameter values are used'
            value << [ null, '', ' ' ]
    }
}
