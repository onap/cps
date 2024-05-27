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

package org.onap.cps.ncmp.api.impl.utils

import spock.lang.Specification

class DmiServiceUrlBuilderSpec extends Specification {

    def objectUnderTest = new DmiServiceUrlBuilder()

    def 'Build URI with (variable) path segments and parameters.'() {
        given: 'the URI details are given to the builder'
            objectUnderTest.pathSegment(segment1)
            objectUnderTest.variablePathSegment('myVariableSegment','someValue')
            objectUnderTest.pathSegment(segment2)
            objectUnderTest.queryParameter('param1', paramValue1)
            objectUnderTest.queryParameter('param2', paramValue2)
            objectUnderTest.queryParameter('param3', null)
            objectUnderTest.queryParameter('param4', '')
        when: 'the URI (string) is build'
            def result = objectUnderTest.build('myDmiServer', 'myBasePath')
        then: 'the URI is correct (segments are in correct order) '
            assert result == expectedUri
        where: 'following URI details are used'
            segment1   | segment2   | paramValue1 | paramValue2 || expectedUri
            'segment1' | 'segment2' | '123'       | 'abc'       || 'myDmiServer/myBasePath/v1/segment1/someValue/segment2?param1=123&param2=abc'
            'segment2' | 'segment1' | 'abc'       | '123'       || 'myDmiServer/myBasePath/v1/segment2/someValue/segment1?param1=abc&param2=123'
    }

    def 'Build URI with special characters in path segments.'() {
        given: 'the path segments are given to the builder'
            objectUnderTest.pathSegment(segment)
            objectUnderTest.variablePathSegment('myVariableSegment', variableSegmentValue)
        when: 'the URI (string) is build'
            def result = objectUnderTest.build('myDmiServer', 'myBasePath')
        then: 'Only teh characters that cause issues in path segments issues are encoded'
            assert result == expectedUri
        where: 'following variable path segments are used'
            segment                                | variableSegmentValue || expectedUri
            'some/special?characters=are\\encoded' | 'my/variable/segment'  || 'myDmiServer/myBasePath/v1/some%2Fspecial%3Fcharacters=are%5Cencoded/my%2Fvariable%2Fsegment'
            'but=some&are:not-!'                   | 'my&variable:segment'  || 'myDmiServer/myBasePath/v1/but=some&are:not-!/my&variable:segment'
    }

    def 'Build URI with special characters in query parameters.'() {
        given: 'the query parameter is given to the builder'
           objectUnderTest.queryParameter(paramName, value)
        when: 'the URI (string) is build'
            def result = objectUnderTest.build('myDmiServer', 'myBasePath')
        then: 'Only the characters (in the name and value) that cause issues in query parameters are encoded'
            assert result == expectedUri
        where: 'the following query parameters are used'
            paramName  | value                                  || expectedUri
            'my&param' | 'some?special&characters=are\\encoded' || 'myDmiServer/myBasePath/v1?my%26param=some?special%26characters%3Dare%5Cencoded'
            'my-param' | 'but/some:are-not-!'                   || 'myDmiServer/myBasePath/v1?my-param=but/some:are-not-!'
    }

    def 'Build URI with empty query parameters.'() {
        when: 'the query parameter is given to the builder'
            objectUnderTest.queryParameter('param', value)
        and: 'the URI (string) is build'
            def result = objectUnderTest.build('myDmiServer', 'myBasePath')
        then: 'no parameter gets added'
            assert result == 'myDmiServer/myBasePath/v1'
        where: 'the following parameter values are used'
            value << [ null, '', ' ' ]
    }

}
