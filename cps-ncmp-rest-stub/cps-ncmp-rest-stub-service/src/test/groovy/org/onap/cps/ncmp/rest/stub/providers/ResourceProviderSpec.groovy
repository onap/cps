/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.rest.stub.providers

import java.nio.file.Files
import java.nio.file.Path
import org.springframework.util.FileSystemUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

class ResourceProviderSpec extends Specification {

    @TempDir
    @Shared
    def tempDirectory

    def setupSpec() {
        tempDirectory = Files.createTempDirectory('spock-test')
        Files.write(tempDirectory.resolve('file.txt'), 'Dummy file content'.getBytes())
    }

    def cleanupSpec() {
        if(Files.exists(tempDirectory)) {
            FileSystemUtils.deleteRecursively(tempDirectory)
        }
    }

    def 'Resource Provider with existing file on #scenario'() {

        given: 'a resource provider with base stub folder defined on #scenario' 
            def resourceProvider = new ResourceProviderImpl(dir)
        when: 'attempting to access that file #filename'
            def optional= resourceProvider.getResourceInputStream(filename)
        then: 'it is present'
            assert optional.isPresent()
        where:
        scenario          | dir                      | filename
        'classpath'       | '/stubs/'                | 'passthrough-operational-example.json'
        'file system'     | tempDirectory.toString() | 'file.txt'
    }

    def 'Resource Provider without required resource file on #scenario'() {

        given: 'a resource provider with base stub folder defined on #scenario'
            def resourceProvider = new ResourceProviderImpl(dir)
        when: 'attempting to access that file #filename'
            def optional= resourceProvider.getResourceInputStream(filename)
        then: 'it is empty'
            assert optional.isEmpty()
        where:
            scenario      | dir                      | filename
            'classpath'   | '/stubs/'                | 'unknown-file.txt'
            'file system' | tempDirectory.toString() | 'unknown-file.txt'
    }

}
