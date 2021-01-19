/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.rest.utils

import org.onap.cps.spi.exceptions.ModelValidationException
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Specification

class MultipartFileUtilSpec extends Specification {

    def 'Extract yang resource from yang file.'() {
        given:
            def multipartFile = new MockMultipartFile("file", "filename.yang", "text/plain", "content".getBytes())
        when:
            def result = MultipartFileUtil.extractYangResourcesMap(multipartFile)
        then:
            assert result != null
            assert result.size() == 1
            assert result.get("filename.yang") == "content"
    }

    def 'Extract yang resources from zip archive.'() {
        given:
            def multipartFile = new MockMultipartFile("file", "TEST.ZIP", "application/zip",
                    getClass().getResource("/assembly.zip").getBytes())
        when:
            def result = MultipartFileUtil.extractYangResourcesMap(multipartFile)
        then:
            assert result != null
            assert result.size() == 2
            assert result.containsKey("assembly.yang")
            assert result.containsKey("component.yang")
    }

    def 'Extract resources from zip archive having no yang files.'() {
        given:
            def multipartFile = new MockMultipartFile("file", "TEST.ZIP", "application/zip",
                    getClass().getResource("/no-yang-files.zip").getBytes())
        when:
            MultipartFileUtil.extractYangResourcesMap(multipartFile)
        then:
            thrown(ModelValidationException)
    }

    def 'Extract yang resource from  file with invalid filename extension.'() {
        given:
            def multipartFile = new MockMultipartFile("file", "filename.doc", "text/plain", "content".getBytes())
        when:
            MultipartFileUtil.extractYangResourcesMap(multipartFile)
        then:
            thrown(ModelValidationException)
    }

}
