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

package org.onap.cps.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static org.opendaylight.yangtools.yang.common.YangConstants.RFC6020_YANG_FILE_EXTENSION;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.impl.Fragment;
import org.onap.cps.yang.YangTextSchemaSourceSet;
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.ValueNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;

@Slf4j
public class YangUtils {

    private YangUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Parse a file containing yang modules.
     *
     * @param yangModelFiles list of files containing one or more yang modules. The file has to have a .yang extension.
     * @return a SchemaContext representing the yang model
     * @throws IOException         when the system as an IO issue
     * @throws YangParserException when the file does not contain a valid yang structure
     */
    @Deprecated
    public static YangTextSchemaSourceSet parseYangModelFiles(final List<File> yangModelFiles)
            throws IOException, YangParserException, ReactorException {
        final YangTextSchemaSourceSetBuilder yangModelsMapBuilder = new YangTextSchemaSourceSetBuilder();
        for (final File file :yangModelFiles) {
            final String fileNameWithExtension = file.getName();
            checkArgument(fileNameWithExtension.endsWith(RFC6020_YANG_FILE_EXTENSION),
                    "Filename %s does not end with '%s'", RFC6020_YANG_FILE_EXTENSION,
                    fileNameWithExtension);
            final String content = Files.asCharSource(file, Charsets.UTF_8).read();
            yangModelsMapBuilder.put(fileNameWithExtension, content);
        }
        return yangModelsMapBuilder.build();
    }

    /**
     * Parse a file containing json data for a certain model (schemaContext).
     *
     * @param jsonData      a string containing json data for the given model
     * @param schemaContext the SchemaContext for the given data
     * @return the NormalizedNode representing the json data
     */
    public static NormalizedNode<?, ?> parseJsonData(final String jsonData, final SchemaContext schemaContext)
            throws IOException {
        final JSONCodecFactory jsonCodecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02
                .getShared(schemaContext);
        final NormalizedNodeResult normalizedNodeResult = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter normalizedNodeStreamWriter = ImmutableNormalizedNodeStreamWriter
                .from(normalizedNodeResult);
        try (final JsonParserStream jsonParserStream = JsonParserStream
                .create(normalizedNodeStreamWriter, jsonCodecFactory)) {
            final JsonReader jsonReader = new JsonReader(new StringReader(jsonData));
            jsonParserStream.parse(jsonReader);
        }
        return normalizedNodeResult.getResult();
    }

    /**
     * Break a Normalized Node tree into fragments that can be stored by the persistence service.
     *
     * @param tree   the normalized node tree
     * @param module the module applicable for the data in the normalized node
     * @return the 'root' Fragment for the tree contain all relevant children etc.
     */
    public static Fragment fragmentNormalizedNode(
            final NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?> tree,
            final Module module) {
        final QName[] nodeTypes = {tree.getNodeType()};
        final String xpath = buildXpathId(tree.getIdentifier());
        final Fragment rootFragment = Fragment.createRootFragment(module, nodeTypes, xpath);
        fragmentNormalizedNode(rootFragment, tree);
        return rootFragment;
    }

    private static void fragmentNormalizedNode(final Fragment currentFragment,
                                               final NormalizedNode normalizedNode) {
        if (normalizedNode instanceof DataContainerNode) {
            inspectContainer(currentFragment, (DataContainerNode) normalizedNode);
        } else if (normalizedNode instanceof MapNode) {
            inspectKeyedList(currentFragment, (MapNode) normalizedNode);
        } else if (normalizedNode instanceof ValueNode) {
            inspectLeaf(currentFragment, (ValueNode) normalizedNode);
        } else if (normalizedNode instanceof LeafSetNode) {
            inspectLeafList(currentFragment, (LeafSetNode) normalizedNode);
        } else {
            log.warn("Cannot normalize " + normalizedNode.getClass());
        }
    }

    private static void inspectLeaf(final Fragment currentFragment,
                                    final ValueNode valueNode) {
        final Object value = valueNode.getValue();
        currentFragment.addLeafValue(valueNode.getNodeType().getLocalName(), value);
    }

    private static void inspectLeafList(final Fragment currentFragment,
                                        final LeafSetNode leafSetNode) {
        currentFragment.addLeafListName(leafSetNode.getNodeType().getLocalName());
        for (final NormalizedNode value : (Collection<NormalizedNode>) leafSetNode.getValue()) {
            fragmentNormalizedNode(currentFragment, value);
        }
    }

    private static void inspectContainer(final Fragment currentFragment,
                                         final DataContainerNode dataContainerNode) {
        final Collection<NormalizedNode> leaves = (Collection) dataContainerNode.getValue();
        for (final NormalizedNode leaf : leaves) {
            fragmentNormalizedNode(currentFragment, leaf);
        }
    }

    private static void inspectKeyedList(final Fragment currentFragment,
                                         final MapNode mapNode) {
        createNodeForEachListElement(currentFragment, mapNode);
    }

    private static void createNodeForEachListElement(final Fragment currentFragment, final MapNode mapNode) {
        final Collection<MapEntryNode> mapEntryNodes = mapNode.getValue();
        for (final MapEntryNode mapEntryNode : mapEntryNodes) {
            final String xpathId = buildXpathId(mapEntryNode.getIdentifier());
            final Fragment listElementFragment =
                currentFragment.createChildFragment(mapNode.getNodeType(), xpathId);
            fragmentNormalizedNode(listElementFragment, mapEntryNode);
        }
    }

    private static String buildXpathId(final YangInstanceIdentifier.PathArgument nodeIdentifier) {
        final StringBuilder xpathIdBuilder = new StringBuilder();
        xpathIdBuilder.append("/").append(nodeIdentifier.getNodeType().getLocalName());

        if (nodeIdentifier instanceof NodeIdentifierWithPredicates) {
            xpathIdBuilder.append(getKeyAttributesStatement((NodeIdentifierWithPredicates) nodeIdentifier));
        }
        return xpathIdBuilder.toString();
    }

    private static String getKeyAttributesStatement(final NodeIdentifierWithPredicates nodeIdentifier) {
        final List<String> keyAttributes = nodeIdentifier.entrySet().stream().map(
            entry -> {
                final String name = entry.getKey().getLocalName();
                final String value = String.valueOf(entry.getValue()).replace("'", "\\'");
                return String.format("@%s='%s'", name, value);
            }
        ).collect(Collectors.toList());

        if (keyAttributes.isEmpty()) {
            return "";
        } else {
            Collections.sort(keyAttributes);
            return "[" + String.join(" and ", keyAttributes) + "]";
        }
    }
}
