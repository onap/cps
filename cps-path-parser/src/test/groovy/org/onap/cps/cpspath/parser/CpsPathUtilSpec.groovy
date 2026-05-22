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

    def 'Multi-key predicate conditions are sorted alphabetically in normalized xpath #scenario.'() {
        when: 'a multi-key xpath is normalized'
            def result = CpsPathUtil.getNormalizedXpath(xpath)
        then: 'predicate conditions are sorted alphabetically'
            assert result == expectedNormalizedXpath
        where: 'the following xpaths are used'
            scenario                                   | xpath                                          || expectedNormalizedXpath
            'single key unchanged'                     | '/a[@CN=0]'                                    || "/a[@CN='0']"
            'two keys already in order'                | '/a[@MCC=230 and @MNC=3]'                      || "/a[@MCC='230' and @MNC='3']"
            'two keys out of order'                    | '/a[@SRN=61 and @CN=0]'                        || "/a[@CN='0' and @SRN='61']"
            'three keys out of order'                  | '/a[@CN=0 and @SRN=61 and @SN=0]'             || "/a[@CN='0' and @SN='0' and @SRN='61']"
            'multi-level with unsorted multi-key'      | '/p[@b=2 and @a=1]/c[@z=9 and @m=5]'          || "/p[@a='1' and @b='2']/c[@m='5' and @z='9']"
            'exact bug report case'                    | '/SECTOREQMANTENNAREF[@CN=0 and @SRN=61 and @SN=0]' || "/SECTOREQMANTENNAREF[@CN='0' and @SN='0' and @SRN='61']"
    }

    def 'Normalized xpath keys are sorted alphabetically #scenario'() {
        when: 'a given xpath is normalized'
            def result = CpsPathUtil.getNormalizedXpathWithSortedKeys(xpath)
        then: 'the result has sorted predicate keys'
            assert result == expectedXpath
        where: 'the following xpaths are used'
            scenario                           | xpath                                                                                || expectedXpath
            'single level'                     | '/addresses [@No=2 and @street="abc" and @pin-code=123]'                             || "/addresses[@No='2' and @pin-code='123' and @street='abc']"
            'multiple levels'                  | '/premises [@num=1 and @code=2]/addresses[@No=2 and @street="abc" and @pin-code=123]'|| "/premises[@code='2' and @num='1']/addresses[@No='2' and @pin-code='123' and @street='abc']"
            'root path is returned unchanged'  | '/'                                                                                  || '/'
            'empty path is treated as root'    | ''                                                                                   || '/'
    }

    def "Invalid xpath inputs throw appropriate exceptions #scenario"() {
        when: 'an invalid xpath is normalized'
            CpsPathUtil.getNormalizedXpathWithSortedKeys(xpath)
        then: 'a PathParsingException is thrown'
            thrown(PathParsingException)
        where: 'invalid xpath scenarios are tested'
            scenario                             | xpath
            'unsupported operator'               | '/bookstore/book[@No!=23]'
            'attribute without value'            | '/bookstore/book[@id]'
            'missing closing bracket'            | '/bookstore/book[@No=2'
            'misplaced bracket'                  | '/bookstore/add[ress]/book[@No=2]'
            'extra closing bracket'              | '/bookstore/book[@No=2]]'
    }

    def 'getNormalizedXpath and getNormalizedXpathWithSortedKeys return identical results for multi-key xpath.'() {
        given: 'the exact xpath from the bug report with keys in non-alphabetical order'
            def xpath = "/NE[@neid='Praha_Mezilesi-A-10313']/NodeModule/NODE/SECTOREQM[@SECTOREQMID='1']/SECTOREQMANTENNAREF[@CN=0 and @SRN=61 and @SN=0]"
        when: 'both normalization methods are called'
            def resultFromNormalizedXpath = CpsPathUtil.getNormalizedXpath(xpath)
            def resultFromSortedKeys = CpsPathUtil.getNormalizedXpathWithSortedKeys(xpath)
        then: 'both return identical sorted results'
            assert resultFromNormalizedXpath == resultFromSortedKeys
        and: 'the keys are in alphabetical order matching the stored xpath'
            assert resultFromNormalizedXpath.contains("[@CN='0' and @SN='0' and @SRN='61']")
    }
}
