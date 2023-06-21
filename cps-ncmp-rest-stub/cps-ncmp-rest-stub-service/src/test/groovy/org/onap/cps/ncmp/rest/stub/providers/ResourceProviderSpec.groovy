/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
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
import spock.lang.Specification
import spock.lang.TempDir

class ResourceProviderSpec extends Specification {

    @TempDir
    Path tempDirectory

    def setup() {
        tempDirectory = Files.createTempDirectory("spock-test")
    }

    def cleanup() {
        if(Files.exists(tempDirectory)) {
            FileSystemUtils.deleteRecursively(tempDirectory);
        }
    }

    def "Should return optional input stream if file exists on file system"() {

        given:
            def resourceProvider = new ResourceProviderImpl(tempDirectory.toString())

        and:
            Files.write(tempDirectory.resolve("file.txt"), "Dummy file content".getBytes());

        when:
            def optional= resourceProvider.getResourceInputStream("file.txt")

        then:
            assert optional.isPresent()
    }

    def "Should return #scenario"() {

        given:
            def resourceProvider = new ResourceProviderImpl("/stubs/")

        when:
            def optional= resourceProvider.getResourceInputStream(filename)

        then:
            assert optional.isPresent() == expected
        where:
            scenario                                                                           | filename                               | expected
            'optional input stream if file exists on class path'                               | 'passthrough-operational-example.json' | true
            'empty optional input stream if file does not exists on class path or file system' | 'unknown-file.txt'                     | false
    }

}
