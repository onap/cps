/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.dmi


import spock.lang.Specification

class DmiPropertiesSpec extends Specification {

    def objectUnderTest = new DmiProperties()

    def 'Geting dmi base path.'() {
        given: 'base path of #dmiBasePath'
            objectUnderTest.dmiBasePath = dmiBasePath
        expect: 'Preceding and trailing slash wil be removed'
            assert objectUnderTest.getDmiBasePath() == 'test'
        where: 'the following dmi base paths are used'
            dmiBasePath << [ 'test' , '/test', 'test/', '/test/' ]
    }
}
