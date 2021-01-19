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

import org.onap.cps.spi.exceptions.CpsException
import org.onap.cps.spi.exceptions.ModelValidationException
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Specification

class MultipartFileUtilSpec extends Specification {

    def 'Extract yang resource from yang file.'() {
        given: 'uploaded yang file'
            def multipartFile = new MockMultipartFile("file", "filename.yang", "text/plain", "content".getBytes())
        when: 'resources are extracted from the file'
            def result = MultipartFileUtil.extractYangResourcesMap(multipartFile)
        then: 'the expected name and content are extracted as result'
            assert result.size() == 1
            assert result.get("filename.yang") == "content"
    }

    def 'Extract yang resources from zip archive.'() {
        given: 'uploaded zip archive containing 2 yang files and 1 not yang (json) file'
            def multipartFile = new MockMultipartFile("file", "TEST.ZIP", "application/zip",
                    getClass().getResource("/yang-files-set.zip").getBytes())
        when: 'resources are extracted from zip file'
            def result = MultipartFileUtil.extractYangResourcesMap(multipartFile)
        then: 'information from yang files is extracted, not jang file is ignored'
            assert result.size() == 2
            assert result.containsKey("assembly.yang")
            assert result.containsKey("component.yang")
    }

    def 'Extract resources from zip archive having no yang files.'() {
        given: 'uploaded zip archive containing text files only'
            def multipartFile = new MockMultipartFile("file", "TEST.ZIP", "application/zip",
                    getClass().getResource("/no-yang-files.zip").getBytes())
        when: 'attempt to extract resources from zip file is performed'
            MultipartFileUtil.extractYangResourcesMap(multipartFile)
        then: 'the validation exception is thrown indicating no resources to extract'
            thrown(ModelValidationException)
    }

    def 'Extract yang resource from  file with invalid filename extension.'() {
        given: 'uploaded file with unsupported (.doc) exception'
            def multipartFile = new MockMultipartFile("file", "filename.doc", "text/plain", "content".getBytes())
        when: 'attempt to extract resources from the file is performed'
            MultipartFileUtil.extractYangResourcesMap(multipartFile)
        then: 'validation exception is thrown indicating the file type is not supported'
            thrown(ModelValidationException)
    }

}
