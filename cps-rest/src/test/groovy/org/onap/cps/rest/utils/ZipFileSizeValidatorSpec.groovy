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
import spock.lang.Specification

class ZipFileSizeValidatorSpec extends Specification {

    def static thresholdSize = ZipFileSizeValidator.THRESHOLD_SIZE
    def static thresholdEntries = ZipFileSizeValidator.THRESHOLD_ENTRIES
    def static thresholdRatio = ZipFileSizeValidator.THRESHOLD_RATIO

    def objectUnderTest = new ZipFileSizeValidator()
    def compressedFileSize = 100

    def setup() {
        objectUnderTest.setTotalEntryInArchive(0)
        objectUnderTest.setTotalSizeArchive(0)
        objectUnderTest.setCompressedSize(compressedFileSize)
    }

    def 'Increment the total entries in Archive.'() {
        when: 'the totalEntriesInArchive value is incremented'
            objectUnderTest.incrementTotalEntryInArchive()
        then: 'the totalEntriesInArchive is incremented by 1'
            assert objectUnderTest.totalEntryInArchive == old(objectUnderTest.totalEntryInArchive) + 1
    }

    def 'Update the total size of Archive.'() {
        given: 'the size of an entry of archive'
            def entrySize = 100
        when: 'the totalSizeArchive is to be updated with the latest entry Size'
            objectUnderTest.updateTotalSizeArchive(entrySize)
        then: 'the totalSizeArchive is updated as expected'
            assert objectUnderTest.totalSizeArchive == old(objectUnderTest.totalSizeArchive) + entrySize
    }

    def 'Validate the zip archive for compression ratio less that threshold compression ratio.'() {
        given: 'the totalEntrySize of the archive so that compression ratio is within the threshold'
            int totalEntrySize = compressedFileSize * thresholdRatio - 1
        when: 'the validation is performed against the threshold compression ratio'
            objectUnderTest.validateCompresssionRatio(totalEntrySize)
        then: 'validation passes and no exception is thrown'
            noExceptionThrown()
    }

    def 'Validate the zip archive for compression ratio.'() {
        given: 'the totalEntrySize of the archive so that compression ratio is higher than the threshold'
            int totalEntrySize = compressedFileSize * thresholdRatio + 1
        when: 'the validation is performed against the threshold compression ratio'
            objectUnderTest.validateCompresssionRatio(totalEntrySize)
        then: 'validation fails and exception is thrown'
            thrown ModelValidationException
    }

    def 'Validate the zip archive for thresholdSize and thresholdEntries #caseDescriptor.'() {
        given:
            objectUnderTest.setTotalEntryInArchive(totalEntriesInArchive)
            objectUnderTest.setTotalSizeArchive(totalSizeArchive)
        when: 'the validation is performed against the threshold size and threshold Entries count'
            objectUnderTest.validateSizeAndEntries()
        then: 'validation passes and no exception is thrown'
            noExceptionThrown()
        where: 'following cases are tested'
            caseDescriptor              | totalSizeArchive  | totalEntriesInArchive
            'less than threshold value' | thresholdSize - 1 | thresholdEntries - 1
            'at threshold value'        | thresholdSize     | thresholdEntries
    }

    def 'Validate the zip archive for thresholdSize and thresholdEntries with #caseDescriptor.'() {
        given:
            objectUnderTest.setTotalEntryInArchive(totalEntriesInArchive)
            objectUnderTest.setTotalSizeArchive(totalSizeArchive)
        when: 'the validation is performed against the threshold size and threshold Entries count'
            objectUnderTest.validateSizeAndEntries()
        then: 'validation fails and exception is thrown'
            thrown ModelValidationException
        where: 'following cases are tested'
            caseDescriptor                                  | totalSizeArchive  | totalEntriesInArchive
            'totalEntriesInArchive exceeds threshold value' | thresholdSize     | thresholdEntries + 1
            'totalSizeArchive exceeds threshold value'      | thresholdSize + 1 | thresholdEntries
    }
}
