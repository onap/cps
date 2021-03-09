/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.query

import org.onap.cps.spi.exceptions.CpsPathException
import spock.lang.Specification
import spock.lang.Unroll

class CpsPathQuerySpec extends Specification {

    def objectUnderTest = new CpsPathQuery()

    @Unroll
    def 'Parse cps path with valid cps path and a filter with #scenario.'() {
        when: 'the given cps path is parsed'
            def result = objectUnderTest.createFrom(cpsPath)
        then: 'the query has the right type'
            result.cpsPathQueryType == CpsPathQueryType.XPATH_LEAF_VALUE
        and: 'the right query parameters are set'
            result.xpathPrefix == expectedXpathPrefix
            result.leafName == expectedLeafName
            result.leafValue == expectedLeafValue
        where: 'the following data is used'
            scenario               | cpsPath                                                  || expectedXpathPrefix | expectedLeafName       | expectedLeafValue
            'leaf of type String'  | '/parent/child[@common-leaf-name=\'common-leaf-value\']' || '/parent/child'     |'common-leaf-name'      | 'common-leaf-value'
            'leaf of type Integer' | '/parent/child[@common-leaf-name-int=5]'                 || '/parent/child'     |'common-leaf-name-int'  | 5
            'spaces around ='      | '/parent/child[@common-leaf-name-int = 5]'               || '/parent/child'     |'common-leaf-name-int'  | 5
            'key in top container' | '/parent[@common-leaf-name-int = 5]'                     || '/parent'           |'common-leaf-name-int'  | 5
    }

    @Unroll
    def 'Parse cps path of type ends with a #scenario.'() {
        when: 'the given cps path is parsed'
            def result = objectUnderTest.createFrom(cpsPath)
        then: 'the query has the right type'
            result.cpsPathQueryType == CpsPathQueryType.XPATH_ENDS_WITH
        and: 'the right ends with parameters are set'
            result.endsWith == expectedEndsWithValue
        where: 'the following data is used'
            scenario         | cpsPath                   || expectedEndsWithValue
            'yang container' | '///cps-path'             || '/cps-path'
            'yang list'      | '///cps-path[@key=value]' || '/cps-path[@key=value]'
    }

    @Unroll
    def 'Parse cps path with #scenario.'() {
        when: 'the given cps path is parsed'
            objectUnderTest.createFrom(cpsPath)
        then: 'a CpsPathException is thrown'
            thrown(CpsPathException)
        where: 'the following data is used'
            scenario            | cpsPath
            'no / at the start' | 'invalid-cps-path/child'
            'float value'       | '/parent-200/child-202[@common-leaf-name-float=5.0]'
    }
}
