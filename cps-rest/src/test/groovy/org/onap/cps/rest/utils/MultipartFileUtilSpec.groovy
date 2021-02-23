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
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.utils.DataMapUtils
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification
import spock.lang.Unroll

class MultipartFileUtilSpec extends Specification {

    def 'Data node without leaves and without children.'() {
        given: 'a datanode with no leaves and no children'
            def dataNodeWithoutData = new DataNodeBuilder().withXpath('some xpath').build()
        when: 'it is converted to a map'
            def result = DataMapUtils.toDataMap(dataNodeWithoutData)
        then: 'an empty object map is returned'
            result.isEmpty()
    }

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
        then: 'information from yang files is extracted, not yang file (json) is ignored'
            assert result.size() == 2
            assert result["assembly.yang"] == "fake assembly content 1\n"
            assert result["component.yang"] == "fake component content 1\n"
    }

    @Unroll
    def 'Extract resources from zip archive having #caseDescriptor.'() {
        when: 'attempt to extract resources from zip file is performed'
            MultipartFileUtil.extractYangResourcesMap(multipartFile)
        then: 'the validation exception is thrown indicating invalid zip file content'
            thrown(ModelValidationException)
        where: 'following cases are tested'
            caseDescriptor                      | multipartFile
            'text files only'                   | multipartZipFileFromResource("/no-yang-files.zip")
            'multiple yang file with same name' | multipartZipFileFromResource("/yang-files-multiple-sets.zip")
    }

    def 'Extract yang resource from a file with invalid filename extension.'() {
        given: 'uploaded file with unsupported (.doc) exception'
            def multipartFile = new MockMultipartFile("file", "filename.doc", "text/plain", "content".getBytes())
        when: 'attempt to extract resources from the file is performed'
            MultipartFileUtil.extractYangResourcesMap(multipartFile)
        then: 'validation exception is thrown indicating the file type is not supported'
            thrown(ModelValidationException)
    }

    @Unroll
    def 'IOException thrown during yang resources extraction from #fileType file.'() {
        when: 'attempt to extract resources from the file is performed'
            MultipartFileUtil.extractYangResourcesMap(multipartFileForIOException(fileType))
        then: 'CpsException is thrown indicating the internal error occurrence'
            thrown(CpsException)
        where: 'following file types are used'
            fileType << ['YANG', 'ZIP']
    }

    def multipartZipFileFromResource(resourcePath) {
        return new MockMultipartFile("file", "TEST.ZIP", "application/zip",
                getClass().getResource(resourcePath).getBytes())
    }

    def multipartFileForIOException(extension) {
        def multipartFile = Mock(MultipartFile)
        multipartFile.getOriginalFilename() >> "TEST." + extension
        multipartFile.getBytes() >> { throw new IOException() }
        multipartFile.getInputStream() >> { throw new IOException() }
        return multipartFile
    }

}
