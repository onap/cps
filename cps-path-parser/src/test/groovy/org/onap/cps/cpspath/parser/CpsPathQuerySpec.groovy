/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd
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

import static org.onap.cps.cpspath.parser.CpsPathPrefixType.ABSOLUTE
import static org.onap.cps.cpspath.parser.CpsPathPrefixType.DESCENDANT

class CpsPathQuerySpec extends Specification {

    def 'Default values for the most basic cps query.'() {
        when: 'the cps path is parsed'
            def result = CpsPathQuery.createFrom('/parent')
        then: 'the query has the correct default properties'
            assert result.cpsPathPrefixType == ABSOLUTE
            assert result.hasAncestorAxis() == false
            assert result.hasLeafConditions() == false
            assert result.hasTextFunctionCondition() == false
            assert result.hasContainsFunctionCondition() == false
            assert result.isPathToListElement() == false
    }

    def 'Parse cps path with valid cps path and a filter with #scenario.'() {
        when: 'the given cps path is parsed'
            def result = CpsPathQuery.createFrom(cpsPath)
        then: 'the query has the right xpath type'
            assert result.cpsPathPrefixType == ABSOLUTE
        and: 'the right query parameters are set'
            assert result.xpathPrefix == expectedXpathPrefix
            assert result.hasLeafConditions()
            assert result.leafConditions[0].name() == expectedLeafName
            assert result.leafConditions[0].value() == expectedLeafValue
        where: 'the following data is used'
            scenario               | cpsPath                                                    || expectedXpathPrefix                             | expectedLeafName       | expectedLeafValue
            'leaf of type String'  | '/parent/child[@common-leaf-name="common-leaf-value"]'     || '/parent/child'                                 | 'common-leaf-name'     | 'common-leaf-value'
            'leaf of type String'  | '/parent/child[@common-leaf-name=\'common-leaf-value\']'   || '/parent/child'                                 | 'common-leaf-name'     | 'common-leaf-value'
            'leaf of type Integer' | '/parent/child[@common-leaf-name-int=5]'                   || '/parent/child'                                 | 'common-leaf-name-int' | 5
            'spaces around ='      | '/parent/child[@common-leaf-name-int = 5]'                 || '/parent/child'                                 | 'common-leaf-name-int' | 5
            'key in top container' | '/parent[@common-leaf-name-int=5]'                         || '/parent'                                       | 'common-leaf-name-int' | 5
            'parent list'          | '/shops/shop[@id=1]/categories[@id=1]/book[@title="Dune"]' || "/shops/shop[@id='1']/categories[@id='1']/book" | 'title'                | 'Dune'
            "' in double quote"    | '/parent[@common-leaf-name="leaf\'value"]'                 || '/parent'                                       | 'common-leaf-name'     | "leaf'value"
            "' in single quote"    | "/parent[@common-leaf-name='leaf''value']"                 || '/parent'                                       | 'common-leaf-name'     | "leaf'value"
            '" in double quote'    | '/parent[@common-leaf-name="leaf""value"]'                 || '/parent'                                       | 'common-leaf-name'     | 'leaf"value'
            '" in single quote'    | '/parent[@common-leaf-name=\'leaf"value\']'                || '/parent'                                       | 'common-leaf-name'     | 'leaf"value'
    }

    def 'Parse cps path of type ends with a #scenario.'() {
        when: 'the given cps path is parsed'
            def result = CpsPathQuery.createFrom(cpsPath)
        then: 'the query has the right xpath type'
            result.cpsPathPrefixType == DESCENDANT
        and: 'the right ends with parameters are set'
            result.descendantName == expectedDescendantName
        where: 'the following data is used'
            scenario         | cpsPath          || expectedDescendantName
            'yang container' | '//cps-path'     || 'cps-path'
            'parent & child' | '//parent/child' || 'parent/child'
    }

    def 'Parse cps path to form the Normalized cps path containing #scenario.'() {
        when: 'the given cps path is parsed'
            def result = CpsPathUtil.getCpsPathQuery(cpsPath)
        then: 'the query has the right normalized xpath type'
            assert result.normalizedXpath == expectedNormalizedXPath
        where: 'the following data is used'
            scenario                                                      | cpsPath                                                        || expectedNormalizedXPath
            'yang container'                                              | '/cps-path'                                                    || '/cps-path'
            'descendant anywhere'                                         | '//cps-path'                                                   || '//cps-path'
            'descendant with leaf condition'                              | '//cps-path[@key=1]'                                           || "//cps-path[@key='1']"
            'descendant with leaf condition has ">" operator'             | '//cps-path[@key>9]'                                           || "//cps-path[@key>9]"
            'descendant with leaf condition has "<" operator'             | '//cps-path[@key<10]'                                          || "//cps-path[@key<10]"
            'descendant with leaf condition has ">=" operator'            | '//cps-path[@key>=8]'                                          || "//cps-path[@key>=8]"
            'descendant with leaf condition has "<=" operator'            | '//cps-path[@key<=12]'                                         || "//cps-path[@key<=12]"
            'descendant with leaf value and ancestor'                     | '//cps-path[@key=1]/ancestor::parent[@key=1]'                  || "//cps-path[@key='1']/ancestor::parent[@key='1']"
            'parent & child'                                              | '/parent/child'                                                || '/parent/child'
            'parent leaf of type Integer & child'                         | '/parent/child[@code=1]/child2'                                || "/parent/child[@code='1']/child2"
            'parent leaf with double quotes'                              | '/parent/child[@code="1"]/child2'                              || "/parent/child[@code='1']/child2"
            'parent leaf with double quotes inside single quotes'         | '/parent/child[@code=\'"1"\']/child2'                          || "/parent/child[@code='\"1\"']/child2"
            'parent leaf with single quotes inside double quotes'         | '/parent/child[@code="\'1\'"]/child2'                          || "/parent/child[@code='''1''']/child2"
            'leaf with single quotes inside double quotes'                | '/parent/child[@code="\'1\'"]'                                 || "/parent/child[@code='''1''']"
            'leaf with single quotes inside single quotes'                | "/parent/child[@code='I''m quoted']"                           || "/parent/child[@code='I''m quoted']"
            'leaf with more than one attribute'                           | '/parent/child[@key1=1 and @key2="abc"]'                       || "/parent/child[@key1='1' and @key2='abc']"
            'parent & child with more than one attribute'                 | '/parent/child[@key1=1 and @key2="abc"]/child2'                || "/parent/child[@key1='1' and @key2='abc']/child2"
            'leaf with more than one attribute has OR operator'           | '/parent/child[@key1=1 or @key2="abc"]'                        || "/parent/child[@key1='1' or @key2='abc']"
            'parent & child with more than one attribute has OR operator' | '/parent/child[@key1=1 or @key2="abc"]/child2'                 || "/parent/child[@key1='1' or @key2='abc']/child2"
            'parent & child with multiple AND  operators'                 | '/parent/child[@key1=1 and @key2="abc" and @key="xyz"]/child2' || "/parent/child[@key1='1' and @key2='abc' and @key='xyz']/child2"
            'parent & child with multiple OR  operators'                  | '/parent/child[@key1=1 or @key2="abc" or @key="xyz"]/child2'   || "/parent/child[@key1='1' or @key2='abc' or @key='xyz']/child2"
            'parent & child with multiple AND/OR combination'             | '/parent/child[@key1=1 and @key2="abc" or @key="xyz"]/child2'  || "/parent/child[@key1='1' and @key2='abc' or @key='xyz']/child2"
            'parent & child with multiple OR/AND combination'             | '/parent/child[@key1=1 or @key2="abc" and @key="xyz"]/child2'  || "/parent/child[@key1='1' or @key2='abc' and @key='xyz']/child2"
    }

    def 'Parse xpath to form the Normalized xpath containing #scenario.'() {
        when: 'the given xpath is parsed'
            def result = CpsPathUtil.getNormalizedXpath(xPath)
        then: 'the query has the right normalized xpath type'
            assert result == expectedNormalizedXPath
        where: 'the following data is used'
            scenario               | xPath      || expectedNormalizedXPath
            'yang container'       | '/xpath'   || '/xpath'
            'descendant anywhere'  | '//xpath'  || '//xpath'
    }

    def 'Parse cps path that ends with a yang list containing multiple leaf conditions.'() {
        when: 'the given cps path is parsed'
            def result = CpsPathQuery.createFrom(cpsPath)
        then: 'the expected number of leaves are returned'
            result.leafConditions.size() == expectedNumberOfLeaves
        and: 'the given operator(s) returns in the correct order'
            result.booleanOperators == expectedOperators
        and: 'the given comparativeOperator(s) returns in the correct order'
            result.leafConditions.operator == expectedComparativeOperator
        where: 'the following data is used'
            cpsPath                                                                                   || expectedNumberOfLeaves || expectedOperators || expectedComparativeOperator
            '/parent[@code=1]/child[@common-leaf-name-int=5]'                                         || 1                      || []                || ['=']
            '//child[@int-leaf>15 and @leaf-name="leaf value"]'                                       || 2                      || ['and']           || ['>', '=']
            '//child[@int-leaf<5 or @leaf-name="leaf value"]'                                         || 2                      || ['or']            || ['<', '=']
            '//child[@int-leaf=5 and @common-leaf-name="leaf value" or @leaf-name="leaf value1" ]'    || 3                      || ['and', 'or']     || ['=', '=', '=']
            '//child[@int-leaf=5 or @common-leaf-name="leaf value" and @leaf-name="leaf value1" ]'    || 3                      || ['or', 'and']     || ['=', '=', '=']
            '//child[@int-leaf>=18 and @common-leaf-name="leaf value" and @leaf-name="leaf value1" ]' || 3                      || ['and', 'and']    || ['>=', '=', '=']
            '//child[@int-leaf<=25 or @common-leaf-name="leaf value" or @leaf-name="leaf value1" ]'   || 3                      || ['or', 'or']      || ['<=', '=', '=']
    }

    def 'Parse #scenario cps path with text function condition'() {
        when: 'the given cps path is parsed'
            def result = CpsPathQuery.createFrom(cpsPath)
        then: 'the query has the right xpath type'
            assert result.cpsPathPrefixType == DESCENDANT
        and: 'leaf conditions are only present when expected'
            assert result.hasLeafConditions() == expectLeafConditions
        and: 'the right text function condition is set'
            assert result.hasTextFunctionCondition()
            assert result.textFunctionConditionLeafName == 'leaf-name'
            assert result.textFunctionConditionValue == 'search'
        and: 'the ancestor is only present when expected'
            assert result.hasAncestorAxis() == expectHasAncestorAxis
        where: 'the following data is used'
            scenario                                  | cpsPath                                                              || expectLeafConditions | expectHasAncestorAxis
            'descendant anywhere'                     | '//someContainer/leaf-name[text()="search"]'                         || false                | false
            'descendant with leaf value'              | '//child[@other-leaf=1]/leaf-name[text()="search"]'                  || true                 | false
            'descendant anywhere and ancestor'        | '//someContainer/leaf-name[text()="search"]/ancestor::parent'        || false                | true
            'descendant with leaf value and ancestor' | '//child[@other-leaf=1]/leaf-name[text()="search"]/ancestor::parent' || true                 | true
    }

    def 'Parse cps path with contains function condition'() {
        when: 'the given cps path is parsed'
            def result = CpsPathQuery.createFrom('//someContainer[contains(@lang,"en")]')
        then: 'the query has the right xpath type'
            assert result.cpsPathPrefixType == DESCENDANT
        and: 'the right contains function condition is set'
            assert result.hasContainsFunctionCondition()
            assert result.containsFunctionConditionLeafName == 'lang'
            assert result.containsFunctionConditionValue == 'en'
    }

    def 'Parse cps path with error: #scenario.'() {
        when: 'the given cps path is parsed'
            CpsPathQuery.createFrom(cpsPath)
        then: 'a CpsPathException is thrown'
            thrown(PathParsingException)
        where: 'the following data is used'
            scenario                                 | cpsPath
            'no / at the start'                      | 'invalid-cps-path/child'
            'additional / after descendant option'   | '///cps-path'
            'float value'                            | '/parent/child[@someFloat=5.0]'
            'unmatched quotes, double quote first '  | '/parent/child[@someString="value with unmatched quotes\']'
            'unmatched quotes, single quote first'   | '/parent/child[@someString=\'value with unmatched quotes"]'
            'missing attribute value'                | '//child[@int-leaf=5 and @name]'
            'incomplete ancestor value'              | '//books/ancestor::'
            'invalid list element with missing ['    | '/parent-206/child-206/grand-child-206@key="A"]'
            'invalid list element with incorrect ]'  | '/parent-206/child-206/grand-child-206]@key="A"]'
            'invalid list element with incorrect ::' | '/parent-206/child-206/grand-child-206::@key"A"]'
    }

    def 'Parse cps path using ancestor by schema node identifier with a #scenario.'() {
        when: 'the given cps path is parsed'
            def result = CpsPathQuery.createFrom('//descendant/ancestor::' + ancestorPath)
        then: 'the query has the right type'
            result.cpsPathPrefixType == DESCENDANT
        and: 'the result has ancestor axis'
            result.hasAncestorAxis()
        and: 'the correct ancestor schema node identifier is set'
            result.ancestorSchemaNodeIdentifier == ancestorPath
        and: 'there are no leaves conditions'
            assert result.hasLeafConditions() == false
        where:
            scenario                                    | ancestorPath
            'basic container'                           | 'someContainer'
            'container with parent'                     | 'parent/child'
            'ancestor that is a list'                   | "categories[@code='1']"
            'ancestor that is a list with compound key' | "categories[@key1='1' and @key2='2']"
            'parent that is a list'                     | "parent[@id='1']/child"
    }

    def 'Combinations #scenario.'() {
        when: 'the given cps path is parsed'
            def result = CpsPathQuery.createFrom(cpsPath + '/ancestor::someAncestor')
        then: 'the query has the right type'
            assert result.cpsPathPrefixType == DESCENDANT
        and: 'leaf conditions are only present when expected'
            assert result.hasLeafConditions() == expectLeafConditions
        and: 'the result has ancestor axis'
            assert result.hasAncestorAxis()
        and: 'the correct ancestor schema node identifier is set'
            assert result.ancestorSchemaNodeIdentifier == 'someAncestor'
            assert result.descendantName == expectedDescendantName
        where:
            scenario                     | cpsPath                               || expectedDescendantName   | expectLeafConditions
            'basic container'            | '//someContainer'                     || 'someContainer'          | false
            'container with parent'      | '//parent/child'                      || 'parent/child'           | false
            'container with list-parent' | '//parent[@id=1]/child'               || "parent[@id='1']/child"  | false
            'container with list-parent' | '//parent[@id=1]/child[@name="test"]' || "parent[@id='1']/child"  | true
    }

    def 'Parse cps path with multiple conditions on same leaf.'() {
        when: 'the given cps path is parsed using multiple conditions on same leaf'
            def result = CpsPathQuery.createFrom('/test[@same-name="value1" or @same-name="value2"]')
        then: 'two leaves are present with correct values'
            assert result.leafConditions.size() == 2
            assert result.leafConditions[0].name == "same-name"
            assert result.leafConditions[0].value == "value1"
            assert result.leafConditions[1].name == "same-name"
            assert result.leafConditions[1].value == "value2"
    }

    def 'Ordering of data leaves is preserved.'() {
        when: 'the given cps path is parsed'
            def result = CpsPathQuery.createFrom(cpsPath)
        then: 'the order of the data leaves is preserved'
            assert result.leafConditions[0].name == expectedFirstLeafName
            assert result.leafConditions[1].name == expectedSecondLeafName
        where: 'the following data is used'
            cpsPath                                      || expectedFirstLeafName | expectedSecondLeafName
            '/test[@name1="value1" and @name2="value2"]' || 'name1'               | 'name2'
            '/test[@name2="value2" and @name1="value1"]' || 'name2'               | 'name1'
    }

}
