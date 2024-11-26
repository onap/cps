/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

package org.onap.cps;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import org.onap.cps.api.model.DataNode;

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

    /**
     * Reads yang resources into map.
     *
     * @param resources list of file paths
     * @return yang resource map where key is filename and value is file content
     * @throws IOException when there an I/O issue
     */
    public static Map<String, String> getYangResourcesAsMap(final String... resources) throws IOException {
        final ImmutableMap.Builder<String, String> yangResourceNameToContentBuilder = new ImmutableMap.Builder<>();
        for (final String resourcePath : resources) {
            final File file = readFile(resourcePath);
            final String content = new String(Files.readAllBytes(file.toPath()));
            yangResourceNameToContentBuilder.put(file.getName(), content);
        }
        return yangResourceNameToContentBuilder.build();
    }

    /**
     * Represents given data node object as flatten map by xpath.
     * For easy finding child node within hierarchy.
     *
     * @param dataNode data node representing a root of tree structure
     * @return the map containing all the data nodes from given structure where key is xpath, value is datanode object
     */
    public static Map<String, DataNode> getFlattenMapByXpath(final DataNode dataNode) {
        final ImmutableMap.Builder<String, DataNode> dataNodeMapBuilder = ImmutableMap.builder();
        buildFlattenMapByXpath(dataNode, dataNodeMapBuilder);
        return dataNodeMapBuilder.build();
    }

    private static void buildFlattenMapByXpath(final DataNode dataNode,
        final ImmutableMap.Builder<String, DataNode> dataNodeMapBuilder) {
        dataNodeMapBuilder.put(dataNode.getXpath(), dataNode);
        dataNode.getChildDataNodes()
            .forEach(childDataNode -> buildFlattenMapByXpath(childDataNode, dataNodeMapBuilder));
    }
}
