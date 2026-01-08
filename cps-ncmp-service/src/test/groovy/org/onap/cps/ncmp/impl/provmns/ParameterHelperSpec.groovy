/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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
package org.onap.cps.ncmp.impl.provmns

import jakarta.servlet.http.HttpServletRequest
import org.onap.cps.ncmp.api.exceptions.ProvMnSException
import spock.lang.Specification

class ParameterHelperSpec extends Specification {

    def objectUnderTest = new ParameterHelper()

    def mockHttpServletRequest = Mock(HttpServletRequest)

    def uriPathAttributeName = 'org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping'

    def 'Extract request parameters with url first part is a FDN with #scenario.'() {
        given: 'a http request with all the required parts'
            mockHttpServletRequest.getAttribute(uriPathAttributeName) >> path
        when: 'the request parameters are extracted'
            def result = objectUnderTest.extractRequestParameters(mockHttpServletRequest)
        then: 'the Uri LDN first part is as expected'
            assert result.uriLdnFirstPart == expectedUriLdnFirstPart
        and: 'the class name and id are mapped correctly'
            assert result.className == 'myClass'
            assert result.id == 'id'
        where: 'The following URIs are used'
            scenario            | path                                                        || expectedUriLdnFirstPart
            '1 segment'         | 'ProvMnS/v1/segment1/myClass=id'                            || '/segment1'
            '2 segments'        | 'ProvMnS/v1/segment1/segment2/myClass=id'                   || '/segment1/segment2'
            'multiple segments' | 'ProvMnS/v1/segment1/segment2/segment3/segment4/myClass=id' || '/segment1/segment2/segment3/segment4'
            'no slash'          | 'ProvMnS/v1/myClass=id'                                     || ''
    }

    def 'Extract request parameters for Patch Path with attributes.'() {
        when: 'the request parameters are extracted from the path'
            def result = objectUnderTest.createRequestParametersForPatch(path)
        then: 'the FDN is as expected'
            assert result.fdn == expectedFdn
        and: 'the class name and id are mapped correctly'
            assert result.className == 'myClass'
            assert result.id == 'id'
        where: 'the following paths are used'
            scenario                 | path                              ||  expectedFdn
            'attributes in path'     | '/myClass=id/attributes'          || '/myClass=id'
            'attributes with parent' | '/parent=p/myClass=id/attributes' || '/parent=p/myClass=id'
            '#/attributes in path'   | '/myClass=id#/attributes'         || '/myClass=id'
    }

    def 'Attempt to extract request parameters with #scenario.'() {
        given: 'a http request with invalid path'
            mockHttpServletRequest.getAttribute(uriPathAttributeName) >> path
            mockHttpServletRequest.getMethod() >> 'GET'
        when: 'attempt to extract the request parameters'
            objectUnderTest.extractRequestParameters(mockHttpServletRequest)
        then: 'a ProvMnS exception is thrown'
            def thrown = thrown(ProvMnSException)
            assert thrown.message == 'GET failed'
        and: 'the title contains the expected error message'
            assert thrown.title == expectedPathInError + ' not a valid path'
        where: 'the following invalid paths are used'
            scenario                       | path                                || expectedPathInError
            'no = After (last) class name' | 'ProvMnS/v1/myClass1=id/Class2'     || '/myClass1=id/Class2'
            'attributes in path'           | 'ProvMnS/v1/myClass=id/attributes'  || '/myClass=id/attributes'
            '#/attributes in path'         | 'ProvMnS/v1/myClass=id#/attributes' || '/myClass=id#/attributes'
            'missing ProvMnS prefix'       | 'v1/segment1/myClass=id'            || 'v1/segment1/myClass=id'
            'wrong version'                | 'ProvMnS/wrongVersion/myClass=id'   || 'ProvMnS/wrongVersion/myClass=id'
            'empty path'                   | ''                                  ||  ''
    }

    def 'Extract Fdn.'() {
        expect: 'Only valid name-id pairs are retuned up to the required index'
            assert objectUnderTest.extractFdn('/a=1/b=2/c=3/d/e/f', indexFromEnd) == expectedResult
        where: 'following fdns are used'
            indexFromEnd || expectedResult
            0            || ''
            1            || '/a=1/b=2/c=3'
            2            || '/a=1/b=2'
            3            || '/a=1'
            4            || ''
    }

    def 'Extract Parent Fdn.'() {
        expect: 'Teh cortect Parent FDN (up to 2nd last name-id pair)) is returned'
            assert objectUnderTest.extractParentFdn('/a=1/b=2/c=3/d/e/f') == '/a=1/b=2'
    }

}
