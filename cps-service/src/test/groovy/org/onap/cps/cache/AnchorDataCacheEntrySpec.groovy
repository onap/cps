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

package org.onap.cps.cache


import spock.lang.Specification

class AnchorDataCacheEntrySpec extends Specification {

    def objectUnderTest = new AnchorDataCacheEntry()

    def 'Anchor Data Cache Properties Management.'() {
        when: 'a property named "sample" is added to the cache'
            objectUnderTest.setProperty('sample', 123)
        then: 'the cache has that property'
            assert objectUnderTest.hasProperty('sample')
        and: 'the value is correct'
            assert objectUnderTest.getProperty('sample') == 123
        and: 'the cache does not have an an object called "something else"'
            assert objectUnderTest.hasProperty('something else') == false
    }
}
