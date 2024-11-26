/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  Modifications Copyright (C) 2021 Bell Canada.
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

import static org.opendaylight.yangtools.yang.common.YangConstants.RFC6020_YANG_FILE_EXTENSION;

import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.api.exceptions.CpsException;
import org.onap.cps.api.exceptions.ModelValidationException;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MultipartFileUtil {

    private static final String ZIP_FILE_EXTENSION = ".zip";
    private static final String YANG_FILE_EXTENSION = RFC6020_YANG_FILE_EXTENSION;
    private static final int READ_BUFFER_SIZE = 1024;

    /**
     * Extracts yang resources from multipart file instance.
     *
     * @param multipartFile the yang file uploaded
     * @return yang resources as {map} where the key is original file name, and the value is file content
     * @throws ModelValidationException if the file name extension is not '.yang' or '.zip'
     *                                  or if zip archive contain no yang files
     * @throws CpsException             if the file content cannot be read
     */

    public static Map<String, String> extractYangResourcesMap(final MultipartFile multipartFile) {
        final String originalFileName = multipartFile.getOriginalFilename();
        if (resourceNameEndsWithExtension(originalFileName, YANG_FILE_EXTENSION)) {
            return Map.of(originalFileName, extractYangResourceContent(multipartFile));
        }
        if (resourceNameEndsWithExtension(originalFileName, ZIP_FILE_EXTENSION)) {
            return extractYangResourcesMapFromZipArchive(multipartFile);
        }
        throw new ModelValidationException("Unsupported file type.",
            String.format("Filename %s matches none of expected extensions: %s", originalFileName,
                Arrays.asList(YANG_FILE_EXTENSION, ZIP_FILE_EXTENSION)));
    }

    private static Map<String, String> extractYangResourcesMapFromZipArchive(final MultipartFile multipartFile) {
        final ImmutableMap.Builder<String, String> yangResourceMapBuilder = ImmutableMap.builder();
        final var zipFileSizeValidator = new ZipFileSizeValidator();
        try (
            final var inputStream = multipartFile.getInputStream();
            final var zipInputStream = new ZipInputStream(inputStream);
        ) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                extractZipEntryToMapIfApplicable(yangResourceMapBuilder, zipEntry, zipInputStream,
                    zipFileSizeValidator);
            }
            zipInputStream.closeEntry();

        } catch (final IOException e) {
            throw new CpsException("Cannot extract resources from zip archive.", e.getMessage(), e);
        }
        zipFileSizeValidator.validateSizeAndEntries();
        try {
            final Map<String, String> yangResourceMap = yangResourceMapBuilder.build();
            if (yangResourceMap.isEmpty()) {
                throw new ModelValidationException("Archive contains no YANG resources.",
                    String.format("Archive contains no files having %s extension.", YANG_FILE_EXTENSION));
            }
            return yangResourceMap;

        } catch (final IllegalArgumentException e) {
            throw new ModelValidationException("Invalid ZIP archive content.",
                "Multiple resources with same name detected.", e);
        }
    }

    private static void extractZipEntryToMapIfApplicable(
        final ImmutableMap.Builder<String, String> yangResourceMapBuilder, final ZipEntry zipEntry,
        final ZipInputStream zipInputStream, final ZipFileSizeValidator zipFileSizeValidator) throws IOException {
        zipFileSizeValidator.setCompressedSize(zipEntry.getCompressedSize());
        final String yangResourceName = extractResourceNameFromPath(zipEntry.getName());
        if (zipEntry.isDirectory() || !resourceNameEndsWithExtension(yangResourceName, YANG_FILE_EXTENSION)) {
            return;
        }
        yangResourceMapBuilder.put(yangResourceName, extractYangResourceContent(zipInputStream, zipFileSizeValidator));
    }

    private static boolean resourceNameEndsWithExtension(final String resourceName, final String extension) {
        return resourceName != null && resourceName.toLowerCase(Locale.ENGLISH).endsWith(extension);
    }

    private static String extractResourceNameFromPath(final String path) {
        return path == null ? "" : path.replaceAll("^.*[\\\\/]", "");
    }

    private static String extractYangResourceContent(final MultipartFile multipartFile) {
        try {
            return new String(multipartFile.getBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new CpsException("Cannot read the resource file.", e.getMessage(), e);
        }
    }

    private static String extractYangResourceContent(final ZipInputStream zipInputStream,
                                                     final ZipFileSizeValidator zipFileSizeValidator)
        throws IOException {
        try (final var byteArrayOutputStream = new ByteArrayOutputStream()) {
            var totalSizeEntry = 0;
            int numberOfBytesRead;
            final var buffer = new byte[READ_BUFFER_SIZE];
            zipFileSizeValidator.incrementTotalYangFileEntryCountInArchive();
            while ((numberOfBytesRead = zipInputStream.read(buffer, 0, READ_BUFFER_SIZE)) > 0) {
                byteArrayOutputStream.write(buffer, 0, numberOfBytesRead);
                totalSizeEntry += numberOfBytesRead;
                zipFileSizeValidator.updateTotalUncompressedSizeOfYangFilesInArchive(numberOfBytesRead);
                zipFileSizeValidator.validateCompresssionRatio(totalSizeEntry);
            }
            return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        }
    }
}
