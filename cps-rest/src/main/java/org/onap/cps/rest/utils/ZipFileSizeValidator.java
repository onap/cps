/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada.
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

package org.onap.cps.rest.utils;

import lombok.Getter;
import lombok.Setter;
import org.onap.cps.api.exceptions.ModelValidationException;

@Setter
@Getter
public class ZipFileSizeValidator {

    private static final int THRESHOLD_ENTRIES = 10000;
    private static int thresholdSize = 100000000;
    private static final double THRESHOLD_RATIO = 40;
    private static final String INVALID_ZIP = "Invalid ZIP archive content.";

    private int totalUncompressedSizeOfYangFilesInArchive = 0;
    private int totalYangFileEntriesInArchive = 0;
    private long compressedSize = 0;

    /**
     * Increment the totalEntryInArchive by 1.
     */
    public void incrementTotalYangFileEntryCountInArchive() {
        totalYangFileEntriesInArchive++;
    }

    /**
     * Update the totalSizeArchive by numberOfBytesRead.
     *
     * @param numberOfBytesRead the number of bytes of each entry
     */
    public void updateTotalUncompressedSizeOfYangFilesInArchive(final int numberOfBytesRead) {
        totalUncompressedSizeOfYangFilesInArchive += numberOfBytesRead;
    }

    /**
     * Validate the total Compression size of the zip.
     *
     * @param totalEntrySize the size of the unzipped entry.
     */
    public void validateCompresssionRatio(final int totalEntrySize) {
        final double compressionRatio = (double) totalEntrySize / compressedSize;
        if (compressionRatio > THRESHOLD_RATIO) {
            throw new ModelValidationException(INVALID_ZIP,
                String.format("Ratio between compressed and uncompressed data exceeds the CPS limit"
                    + " %s.", THRESHOLD_RATIO));
        }
    }

    /**
     * Validate the total Size and number of entries in the zip.
     */
    public void validateSizeAndEntries() {
        if (totalUncompressedSizeOfYangFilesInArchive > thresholdSize) {
            throw new ModelValidationException(INVALID_ZIP,
                String.format("The total size of uncompressed yang files exceeds the CPS limit of %s bytes.",
                        thresholdSize));
        }
        if (totalYangFileEntriesInArchive > THRESHOLD_ENTRIES) {
            throw new ModelValidationException(INVALID_ZIP,
                String.format("The number of yang file entries in the archive exceeds the CPS limit %s.",
                    THRESHOLD_ENTRIES));
        }
    }
}
