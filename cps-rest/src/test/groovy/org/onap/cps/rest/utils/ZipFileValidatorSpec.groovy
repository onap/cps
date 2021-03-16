/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada.
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
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(properties="spring.main.lazy-initialization=true")
class ZipFileValidatorSpec extends Specification {

    def objectUnderTest = new ZipFileValidator()

    def setup() {
        objectUnderTest.setTotalEntryInArchive(10000)
        objectUnderTest.setTotalSizeArchive(1000000000)
        objectUnderTest.setCompressedSize(200)
    }

    def 'Increment the total entries in Archive.'() {
        when: 'the totalEntriesInArchive value is incremented'
            objectUnderTest.incrementTotalEntryInArchive()
        then: 'the totalEntriesInArchive is incremented by 1'
            assert objectUnderTest.totalEntryInArchive == 10001
    }

    def 'Update the total size of Archive.'() {
        given: 'the size of an entry of archive'
            def entrySize = 1024
        when: 'the totalSizeArchive is to be updated with the latest entry Size'
            objectUnderTest.updateTotalSizeArchive(entrySize)
        then: 'the totalSizeArchive is updated as expected'
            assert objectUnderTest.totalSizeArchive == 1000001024
    }

    def 'Validate the zip archive for compression ratio .'() {
        given: 'the totalEntrySize of the archive so that compression ratio is higher than the threshold'
            def totalEntrySize = 2048
        when: 'the validation is performed against the threshold compression ratio'
            objectUnderTest.validateCompresssionRatio(totalEntrySize)
        then: 'validation fails and exception is thrown'
            thrown ModelValidationException
    }

    def 'Validate the zip archive for thresholdSize and thresholdEntries .'() {
        when: 'the validation is performed against the threshold size and threshold Entries count'
            objectUnderTest.validateSizeAndEntries()
        then: 'validation fails and exception is thrown'
            thrown ModelValidationException
    }

}
