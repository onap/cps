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

package org.onap.cps.cpspath.parser

import org.springframework.util.StopWatch
import spock.lang.Specification

class CpsPathUtilSpec extends Specification {

    def 'Normalized xpaths for list index values using #scenario'() {
        when: 'xpath with #scenario is parsed'
            def result = CpsPathUtil.getNormalizedXpath(xpath)
        then: 'normalized path uses single quotes for leave values'
            result == "/parent/child[@common-leaf-name='123']"
        where: 'the following xpaths are used'
            scenario        | xpath
            'no quotes'     | '/parent/child[@common-leaf-name=123]'
            'double quotes' | '/parent/child[@common-leaf-name="123"]'
            'single quotes' | "/parent/child[@common-leaf-name='123']"
    }

    def 'Normalized parent xpaths'() {
        when: 'a given xpath with #scenario is parsed'
            def result = CpsPathUtil.getNormalizedParentXpath(xpath)
        then: 'the result is the expected parent path'
            result == expectedParentPath
        where: 'the following xpaths are used'
            scenario                         | xpath                                 || expectedParentPath
            'no child'                       | '/parent'                             || ''
            'child and parent'               | '/parent/child'                       || '/parent'
            'grand child'                    | '/parent/child/grandChild'            || '/parent/child'
            'parent & top is list element'   | '/parent[@id=1]/child'                || "/parent[@id='1']"
            'parent is list element'         | '/parent/child[@id=1]/grandChild'     || "/parent/child[@id='1']"
            'parent is list element with /'  | "/parent/child[@id='a/b']/grandChild" || "/parent/child[@id='a/b']"
            'parent is list element with ['  | "/parent/child[@id='a[b']/grandChild" || "/parent/child[@id='a[b']"
            'parent is list element using "' | '/parent/child[@id="x"]/grandChild'   || "/parent/child[@id='x']"
    }

    def 'Recognizing (absolute) xpaths to List elements'() {
        expect: 'check for list returns the correct values'
            assert CpsPathUtil.isPathToListElement(xpath) == expectList
        where: 'the following xpaths are used'
            xpath                  || expectList
            '/parent[@id=1]'       || true
            '/parent[@id=1]/child' || false
            '/parent/child[@id=1]' || true
            '//child[@id=1]'       || false
    }

    def 'Parsing Exception'() {
        when: 'a invalid xpath is parsed'
            CpsPathUtil.getNormalizedXpath('///')
        then: 'a path parsing exception is thrown'
            thrown(PathParsingException)
    }

    def 'CPS Path Processing Performance Test.'() {
        when: '200,000 paths are processed'
            def setupStopWatch = new StopWatch()
            setupStopWatch.start()
            (1..100000).each {
                CpsPathUtil.getNormalizedXpath('/long/path/to/see/if/it/adds/paring/time/significantly/parent/child[@common-leaf-name="123"]')
                CpsPathUtil.getNormalizedXpath('//child[@other-leaf=1]/leaf-name[text()="search"]/ancestor::parent')
            }
            setupStopWatch.stop()
        then: 'it takes less then 5,000 milliseconds'
            // In CI this actually takes about 2.7 sec  which  is approx. 70+ parser executions per millisecond!
            assert setupStopWatch.getTotalTimeMillis() < 5000
    }
}
