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

class DmiServiceUrlBuilderSpec extends Specification {

    def objectUnderTest = new DmiServiceUrlBuilder()

    def 'Build URI with (variable) path segments and parameters.'() {
        given: 'the URI details are given to the builder'
            objectUnderTest.fixedPathSegment(segment1)
            objectUnderTest.variablePathSegment('myVariableSegment','someValue')
            objectUnderTest.fixedPathSegment(segment2)
            objectUnderTest.queryParameter('param1', paramValue1)
            objectUnderTest.queryParameter('param2', paramValue2)
            objectUnderTest.queryParameter('param3', null)
            objectUnderTest.queryParameter('param4', '')
        when: 'the URI (string) is build'
            def result = objectUnderTest.createUrlTemplateParameters('myDmiServer', 'myBasePath')
        then: 'the URI is correct (segments are in correct order) '
            assert result.urlTemplate == expectedUrlTemplate
            assert result.urlVariables == expectedUrlVariables
        where: 'following URI details are used'
            segment1   | segment2   | paramValue1 | paramValue2 || expectedUrlTemplate                                                                                  | expectedUrlVariables
            'segment1' | 'segment2' | '123'       | 'abc'       || 'myDmiServer/{dmiBasePath}/v1/segment1/{myVariableSegment}/segment2?param1={param1}&param2={param2}' | ['myVariableSegment': 'someValue', 'dmiBasePath': 'myBasePath', 'param1': '123', 'param2': 'abc']
            'segment2' | 'segment1' | 'abc'       | '123'       || 'myDmiServer/{dmiBasePath}/v1/segment2/{myVariableSegment}/segment1?param1={param1}&param2={param2}' | ['myVariableSegment': 'someValue', 'dmiBasePath': 'myBasePath', 'param1': 'abc', 'param2': '123']
    }

    def 'Build URI with special characters in path segments.'() {
        given: 'the path segments are given to the builder'
            objectUnderTest.fixedPathSegment(segment)
            objectUnderTest.variablePathSegment('myVariableSegment', variableSegmentValue)
        when: 'the URI (string) is build'
            def result = objectUnderTest.createUrlTemplateParameters('myDmiServer', 'myBasePath')
        then: 'Only the characters that cause issues in path segments issues are encoded'
            assert result.urlTemplate == expectedUrlTemplate
            assert result.urlVariables == expectedUrlVariables
        where: 'following variable path segments are used'
            segment                                | variableSegmentValue  || expectedUrlTemplate                                                                     | expectedUrlVariables
            'some/special?characters=are\\encoded' | 'my/variable/segment' || 'myDmiServer/{dmiBasePath}/v1/some/special?characters=are\\encoded/{myVariableSegment}' | ['myVariableSegment': 'my/variable/segment', 'dmiBasePath': 'myBasePath']
            'but=some&are:not-!'                   | 'my&variable:segment' || 'myDmiServer/{dmiBasePath}/v1/but=some&are:not-!/{myVariableSegment}'                   | ['myVariableSegment': 'my&variable:segment', 'dmiBasePath': 'myBasePath']
    }

    def 'Build URI with special characters in query parameters.'() {
        given: 'the query parameter is given to the builder'
           objectUnderTest.queryParameter(paramName, value)
        when: 'the URI (string) is build'
            def result = objectUnderTest.createUrlTemplateParameters('myDmiServer', 'myBasePath')
        then: 'Only the characters (in the name and value) that cause issues in query parameters are encoded'
            assert result.urlTemplate == expectedUrlTemplate
            assert result.urlVariables == expectedUrlVariables
        where: 'the following query parameters are used'
            paramName  | value                                  || expectedUrlTemplate                                | expectedUrlVariables
            'my&param' | 'some?special&characters=are\\encoded' || 'myDmiServer/{dmiBasePath}/v1?my&param={my&param}' | ['my&param': 'some?special&characters=are\\encoded', 'dmiBasePath': 'myBasePath']
            'my-param' | 'but/some:are-not-!'                   || 'myDmiServer/{dmiBasePath}/v1?my-param={my-param}' | ['my-param': 'but/some:are-not-!', 'dmiBasePath': 'myBasePath']
    }

    def 'Build URI with empty query parameters.'() {
        when: 'the query parameter is given to the builder'
            objectUnderTest.queryParameter('param', value)
        and: 'the URI (string) is build'
            def result = objectUnderTest.createUrlTemplateParameters('myDmiServer', 'myBasePath')
        then: 'no parameter gets added'
            assert result.urlTemplate == 'myDmiServer/{dmiBasePath}/v1'
            assert result.urlVariables.dmiBasePath == 'myBasePath'
        where: 'the following parameter values are used'
            value << [ null, '', ' ' ]
    }
}
