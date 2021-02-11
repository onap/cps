/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
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
    def 'Parse cps path with valid cps path : #scenario.'()
    {   when: 'the given cps path is parsed'
        def result = objectUnderTest.createFrom(cpsPath)
        then: 'object has the expected attribute'
            result.xpathPrefix == '/parent-200/child-202'
            result.leafName == expectedLeafName
            result.leafValue == expectedLeafValue
        where: 'the following data is used'
            scenario  | cpsPath                                                          || expectedLeafName       | expectedLeafValue
            'String'  | '/parent-200/child-202[@common-leaf-name=\'common-leaf-value\']' || 'common-leaf-name'     | 'common-leaf-value'
            'Integer' | '/parent-200/child-202[@common-leaf-name-int=5]'                 || 'common-leaf-name-int' |  5
    }

    @Unroll
    def 'Parse cps path with CpsPathException : #scenario.'()
    {   when: 'the given cps path is parsed'
            objectUnderTest.createFrom(cpsPath)
        then: 'a CpsPathException is thrown'
            thrown(CpsPathException)
        where: 'the following data is used'
            scenario                    | cpsPath
            'Invalid cps path'          | 'invalid-cps-path'
            'Cps path with float value' | '/parent-200/child-202[@common-leaf-name-float=5.0]'
    }
}
