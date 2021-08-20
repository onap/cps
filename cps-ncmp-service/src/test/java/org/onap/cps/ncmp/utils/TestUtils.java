/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

package org.onap.cps.ncmp.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Common convenience methods for testing.
 */
public class TestUtils {

    /**
     * Convert a file in the test resource folder to file.
     *
     * @param filename to name of the file in test/resources
     * @return the file
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
}
