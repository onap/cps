/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

class ParameterMapperSpec extends Specification {

    def objectUnderTest = new ParameterMapper()

    def mockHttpServletRequest = Mock(HttpServletRequest)

    def uriPathAttributeName = 'org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping'

    def 'Extract request parameters with url first part is a FDN with #scenario.'() {
        given: 'a http request with all the required parts'
            mockHttpServletRequest.getAttribute(uriPathAttributeName) >> (String) "ProvMnS/v1/${fdnPrefix}/myClass=myId"
        when: 'the request parameters are extracted'
            def result = objectUnderTest.extractRequestParameters(mockHttpServletRequest)
        then: 'the Uri LDN first part is the fdnPrefix preceded with an extra  "/"'
            assert result.uriLdnFirstPart == '/' + fdnPrefix
        and: 'the class name and id are mapped correctly'
            assert result.className == 'myClass'
            assert result.id == 'myId'
        where: 'The following FDN prefixes are used'
            scenario            | fdnPrefix
            '1 segment'         | 'somePrefix'
            'multiple segments' | 'some/prefix'
            'empty segment'     | ''
    }

    def 'Attempt to extract request parameters with #scenario.'() {
        given: 'a http request with all the required parts'
            mockHttpServletRequest.getAttribute(uriPathAttributeName) >> path
        when: 'attempt to extract the request parameters'
            objectUnderTest.extractRequestParameters(mockHttpServletRequest)
        then: 'a ProvMnS exception is thrown with message about the path being invalid'
            def thrown = thrown(ProvMnSException)
            assert thrown.message == 'not a valid path'
        then: 'the details contain the faulty path'
            assert thrown.details.contains(path)
        where: 'the following paths are used'
            scenario                       | path
            'No / in URI first part'       | 'ProvMnS/v1/myClass=myId'
            'No = After (last) class name' | 'ProvMnS/v1/someOtherClass=someId/myClass'
    }

}
