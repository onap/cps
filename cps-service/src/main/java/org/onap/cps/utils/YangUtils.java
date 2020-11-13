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

import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;
import org.onap.cps.api.impl.Fragment;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
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
import org.opendaylight.yangtools.yang.model.parser.api.YangParser;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.model.repo.api.StatementParserMode;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;

public class YangUtils {

    private static final YangParserFactory PARSER_FACTORY;

    private YangUtils() {
        throw new IllegalStateException("Utility class");
    }

    static {
        final Iterator<YangParserFactory> it = ServiceLoader.load(YangParserFactory.class).iterator();
        if (!it.hasNext()) {
            throw new IllegalStateException("No YangParserFactory found");
        }
        PARSER_FACTORY = it.next();
    }

    /**
     * Parse a file containing yang modules.
     * @param yangModelFile  a file containing one or more yang modules
     *                   (please note the file has to have a .yang extension if not an exception will be thrown)
     * @return a SchemaContext representing the yang model
     * @throws IOException when the system as an IO issue
     * @throws YangParserException when the file does not contain a valid yang structure
     */
    public static SchemaContext parseYangModelFile(final File yangModelFile) throws IOException, YangParserException {
        final YangTextSchemaSource yangTextSchemaSource = YangTextSchemaSource.forFile(yangModelFile);
        final YangParser yangParser = PARSER_FACTORY
                .createParser(StatementParserMode.DEFAULT_MODE);
        yangParser.addSource(yangTextSchemaSource);
        return yangParser.buildEffectiveModel();
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
            final NormalizedNode<? extends PathArgument, ?> tree,
            final Module module) {
        final Fragment rootFragment = new Fragment(null, module, tree.getNodeType());
        rootFragment.setXpath(buildXpathNodeIdentifier(tree.getIdentifier()));
        fragmentNormalizedNode(rootFragment, tree);
        return rootFragment;
    }

    private static void fragmentNormalizedNode(final Fragment currentFragment,
                                               final NormalizedNode normalizedNode) {
        if (normalizedNode instanceof ValueNode) {
            inspectLeaf(currentFragment, (ValueNode) normalizedNode);
        } else if (normalizedNode instanceof LeafSetNode) {
            inspectLeafList(currentFragment, (LeafSetNode) normalizedNode);
        } else if (normalizedNode instanceof DataContainerNode) {
            inspectContainer(currentFragment, (DataContainerNode) normalizedNode);
        } else if (normalizedNode instanceof MapNode) {
            inspectKeyedList(currentFragment, (MapNode) normalizedNode);
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
        // Instead of one child, we have a collection of List Elements
        final Collection<MapEntryNode> mapEntryNodes = (Collection) mapNode.getValue();
        for (final MapEntryNode mapEntryNode : mapEntryNodes) {
            final String xPathNodeId = buildXpathNodeIdentifier(mapEntryNode.getIdentifier());
            final Fragment listElementFragment = currentFragment.createChildFragment(mapNode.getNodeType(), xPathNodeId);
            fragmentNormalizedNode(listElementFragment, mapEntryNode);
        }
    }

    private static String buildXpathNodeIdentifier(PathArgument nodeIdentifier) {
        StringBuilder xPathIdBuilder = new StringBuilder();

        QName nodeType = nodeIdentifier.getNodeType();
        xPathIdBuilder.append("/").append(nodeType.getLocalName());

        if (nodeIdentifier instanceof NodeIdentifierWithPredicates) {
            // set unique node identifiers as name-value pairs of key attributes (leaves)
            StringBuilder nvpBuilder= new StringBuilder();
            ((NodeIdentifierWithPredicates) nodeIdentifier).entrySet().forEach(
                entry -> {
                    String name = entry.getKey().getLocalName();
                    String value = String.valueOf(entry.getValue()).replaceAll("'", "\\'");
                    nvpBuilder
                        .append(nvpBuilder.length() > 0 ? " and " : "")
                        .append(String.format("@%s='%s'", name, value));
                }
            );
            if(nvpBuilder.length() >0) {
                xPathIdBuilder.append("[").append(nvpBuilder.toString()).append("]");
            }
        }
        return xPathIdBuilder.toString();
    }
}
