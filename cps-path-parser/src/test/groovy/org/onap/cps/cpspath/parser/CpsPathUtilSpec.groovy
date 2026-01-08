/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
 *  Modifications Copyright (C) 2025 Deutsche Telekom AG
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

import spock.lang.Specification

class CpsPathUtilSpec extends Specification {

    def 'Normalized xpath for root.'() {
        expect: 'root node xpath is parsed'
            assert CpsPathUtil.getNormalizedXpath('/') == ''
    }

    def 'Normalized xpaths for list index values using #scenario'() {
        when: 'xpath with #scenario is parsed'
            def result = CpsPathUtil.getNormalizedXpath(xpath)
        then: 'normalized path uses single quotes for leave values'
            assert result == "/parent/child[@common-leaf-name='123']"
        where: 'the following xpaths are used'
            scenario        | xpath
            'no quotes'     | '/parent/child[@common-leaf-name=123]'
            'double quotes' | '/parent/child[@common-leaf-name="123"]'
            'single quotes' | "/parent/child[@common-leaf-name='123']"
    }

    def 'Normalized parent paths of absolute paths.'() {
        when: 'a given cps path is parsed'
            def result = CpsPathUtil.getNormalizedParentXpath(cpsPath)
        then: 'the result is the expected parent path'
            assert result == expectedParentPath
        where: 'the following absolute cps paths are used'
            cpsPath                               || expectedParentPath
            '/parent'                             || ''
            '/parent/child'                       || '/parent'
            '/parent/child/grandChild'            || '/parent/child'
            '/parent[@id=1]/child'                || "/parent[@id='1']"
            '/parent/child[@id=1]/grandChild'     || "/parent/child[@id='1']"
            '/parent/child/grandChild[@id="x"]'   || "/parent/child"
            '/parent/ancestor::grandparent'       || ''
            '/parent/child/ancestor::grandparent' || '/parent'
            '/parent/child/name[text()="value"]'  || '/parent'
    }

    def 'Normalized parent paths of descendant paths.'() {
        when: 'a given cps path is parsed'
            def result = CpsPathUtil.getNormalizedParentXpath(cpsPath)
        then: 'the result is the expected parent path'
            assert result == expectedParentPath
        where: 'the following descendant cps paths are used'
            cpsPath                                || expectedParentPath
            '//parent'                             || ''
            '//parent/child'                       || '//parent'
            '//parent/child/grandChild'            || '//parent/child'
            '//parent[@id=1]/child'                || "//parent[@id='1']"
            '//parent/child[@id=1]/grandChild'     || "//parent/child[@id='1']"
            '//parent/child/grandChild[@id="x"]'   || "//parent/child"
            '//parent/ancestor::grandparent'       || ''
            '//parent/child/ancestor::grandparent' || '//parent'
            '//parent/child/name[text()="value"]'  || '//parent'
    }

    def 'Get node ID sequence for given xpath with #scenario.'() {
        when: 'a given xpath with #scenario is parsed'
            def result = CpsPathUtil.getXpathNodeIdSequence(xpath)
        then: 'the result is the expected node ID sequence'
            assert result == expectedNodeIdSequence
        where: 'the following xpaths are used'
            scenario                         | xpath                                 || expectedNodeIdSequence
            'no child'                       | '/parent'                             || ["parent"]
            'child and parent'               | '/parent/child'                       || ["parent","child"]
            'grand child'                    | '/parent/child/grandChild'            || ["parent","child","grandChild"]
            'parent & top is list element'   | '/parent[@id=1]/child'                || ["parent","child"]
            'parent is list element'         | '/parent/child[@id=1]/grandChild'     || ["parent","child","grandChild"]
            'parent is list element with /'  | "/parent/child[@id='a/b']/grandChild" || ["parent","child","grandChild"]
            'parent is list element with ['  | "/parent/child[@id='a[b']/grandChild" || ["parent","child","grandChild"]
            'does not include ancestor node' | '/parent/child/ancestor::grandparent' || ["parent","child"]
    }

    def 'Recognizing (absolute) xpaths to List elements.'() {
        expect: 'check for list returns the correct values'
            assert CpsPathUtil.isPathToListElement(xpath) == expectList
        where: 'the following xpaths are used'
            xpath                                  || expectList
            '/parent[@id=1]'                       || true
            '/parent[@id=1]/child'                 || false
            '/parent/child[@id=1]'                 || true
            '//child[@id=1]'                       || false
            '/parent/ancestor::grandparent[@id=1]' || false
    }

    def 'Parsing Exception.'() {
        when: 'a invalid xpath is parsed'
            CpsPathUtil.getNormalizedXpath('///')
        then: 'a path parsing exception is thrown'
            thrown(PathParsingException)
    }

}
