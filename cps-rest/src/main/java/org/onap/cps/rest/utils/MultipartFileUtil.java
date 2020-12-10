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

package org.onap.cps.rest.utils;

import static org.opendaylight.yangtools.yang.common.YangConstants.RFC6020_YANG_FILE_EXTENSION;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.exceptions.ModelValidationException;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MultipartFileUtil {

    /**
     * Extracts yang resources from multipart file instance.
     *
     * @param multipartFile the yang file uploaded
     * @return yang resources as {map} where the key is original file name, and the value is file content
     * @throws ModelValidationException if the file name extension is not '.yang'
     * @throws CpsException             if the file content cannot be read
     */

    public static Map<String, String> extractYangResourcesMap(final MultipartFile multipartFile) {
        return ImmutableMap.of(extractYangResourceName(multipartFile), extractYangResourceContent(multipartFile));
    }

    private static String extractYangResourceName(final MultipartFile multipartFile) {
        final String fileName = multipartFile.getOriginalFilename();
        if (!fileName.endsWith(RFC6020_YANG_FILE_EXTENSION)) {
            throw new ModelValidationException("Unsupported file type.",
                String.format("Filename %s does not end with '%s'", fileName, RFC6020_YANG_FILE_EXTENSION));
        }
        return fileName;
    }

    private static String extractYangResourceContent(final MultipartFile multipartFile) {
        try {
            final String content = new String(multipartFile.getBytes());
            return content;
        } catch (final IOException e) {
            throw new CpsException("Cannot read the resource file.", e.getMessage(), e);
        }
    }

}
