/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
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

package org.onap.cps;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Common convenience methods for testing.
 */
public class TestUtils {

    /**
     * Convert a file in the test resource folder to file.
     *
     * @param filename to name of the file in test/resources
     * @return the content of the file as a String
     * @throws IOException when there is an IO issue
     */
    public static File readFile(final String filename) {
        return new File(ClassLoader.getSystemClassLoader().getResource(filename).getFile());
    }

    /**
     * Convert a file in the test resource folder to a string.
     *
     * @param filename to name of the file in test/resources
     * @return the content of the file as a String
     * @throws IOException when there is an IO issue
     */
    public static String getResourceFileContent(final String filename) throws IOException {
        final File file = readFile(filename);
        return new String(Files.readAllBytes(file.toPath()));
    }

    /**
     * Reads yang resources into map.
     *
     * @param resources list of file paths
     * @return yang resource map where key is filename and value is file content
     * @throws IOException when there an I/O issue
     */
    public static Map<String, String> getYangResourcesAsMap(final String... resources) throws IOException {
        final Map<String, String> yangResourcesMap = new HashMap<>();
        for (final String resourcePath : resources) {
            final File file = readFile(resourcePath);
            final String content = new String(Files.readAllBytes(file.toPath()));
            yangResourcesMap.put(file.getName(), content);
        }
        return yangResourcesMap;
    }
}
