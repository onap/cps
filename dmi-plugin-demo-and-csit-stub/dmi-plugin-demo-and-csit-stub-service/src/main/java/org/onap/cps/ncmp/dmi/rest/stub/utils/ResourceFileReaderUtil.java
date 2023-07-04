/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.dmi.rest.stub.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/**
 * Common convenience methods for reading resource file content.
 */
@Slf4j
public class ResourceFileReaderUtil {

    /**
     * Converts a resource file content into string.
     *
     * @param fileClasspath to name of the file in test/resources
     * @return the content of the file as a String
     * @throws IOException when there is an IO issue
     */
    public static String getResourceFileContent(final Resource fileClasspath) {
        String fileContent = null;
        try {
            fileContent = StreamUtils.copyToString(fileClasspath.getInputStream(), StandardCharsets.UTF_8);
        } catch (final IOException ioException) {
            log.debug("unable to read resource file content. cause : {}", ioException.getMessage());
        }
        return fileContent;
    }
}
