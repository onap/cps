/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  Modifications Copyright (C) 2023 Nordix Foundation.
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

import org.onap.cps.spi.api.exceptions.CpsException
import org.onap.cps.spi.api.exceptions.ModelValidationException
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
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
        then: 'information from yang files is extracted, not yang file (json) is ignored'
            assert result.size() == 2
            assert result["assembly.yang"] == "fake assembly content 1\n"
            assert result["component.yang"] == "fake component content 1\n"
    }

    def 'Yang file limits in zip archive: #scenario for the bug reported in CPS-1477'() {
        given: 'a yang file size (uncompressed) limit of #threshold bytes'
            ZipFileSizeValidator.thresholdSize = threshold
        and: 'an archive with a yang file of 1083 bytes'
            def multipartFile = multipartZipFileFromResource('/yang-files-set-total-1083-bytes.zip')
        when: 'attempt to extract yang files'
            def thrownException = null
            try {
                MultipartFileUtil.extractYangResourcesMap(multipartFile)
            } catch (Exception e) {
                thrownException  = e
            }
        then: 'ModelValidationException indicating size limit is only thrown when threshold exceeded'
            if (thresholdExceeded) {
                assert thrownException instanceof ModelValidationException
                assert thrownException.details.contains('limit of ' + threshold + ' bytes')
            } else {
                assert thrownException == null
            }
        where:
            scenario          | threshold || thresholdExceeded
            'exceed limit'    | 1082      || true
            'equals to limit' | 1083      || false
            'within limit'    | 1084      || false
    }

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

    def 'IOException thrown during yang resources extraction from #fileType file.'() {
        when: 'attempt to extract resources from the file is performed'
            MultipartFileUtil.extractYangResourcesMap(multipartFileForIOException(fileType))
        then: 'CpsException is thrown indicating the internal error occurrence'
            thrown(CpsException)
        where: 'following file types are used'
            fileType << ['YANG', 'ZIP']
    }

    def 'Resource name extension checks, with #scenario.'() {
        expect: 'extension check returns expected result'
            assert MultipartFileUtil.resourceNameEndsWithExtension(resourceName, '.test') == expectedResult
        where: 'following resource names are tested'
            scenario           | resourceName  || expectedResult
            'correct extension'| 'file.test'   || true
            'mixed case'       | 'file.TesT'   || true
            'other extension'  | 'file.other'  || false
            'no extension'     | 'file'        || false
            'null'             | null          || false
    }

    def 'Extract resourcename, with #scenario.'() {
        expect: 'extension check returns expected result'
            assert MultipartFileUtil.extractResourceNameFromPath(path) == expectedResoureName
        where: 'following resource names are tested'
            scenario           | path                || expectedResoureName
            'no folder'        | 'file.test'         || 'file.test'
            'single folder'    | 'folder/file.test'  || 'file.test'
            'multiple folders' | 'f1/f2/file.test'   || 'file.test'
            'with root'        | '/f1/f2/file.test'  || 'file.test'
            'windows notation' | 'c:\\f2\\file.test' || 'file.test'
            'empty path'       | ''                  || ''
            'null path'        | null                || ''
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
