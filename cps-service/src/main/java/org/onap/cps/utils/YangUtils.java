/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class YangUtils {

    private static final String XPATH_DELIMITER_REGEX = "\\/";
    private static final String XPATH_NODE_KEY_ATTRIBUTES_REGEX = "\\[.+";

    /**
     * Parses jsonData into NormalizedNode according to given schema context.
     *
     * @param jsonData      json data as string
     * @param schemaContext schema context describing associated data model
     * @return the NormalizedNode object
     */
    public static NormalizedNode<?, ?> parseJsonData(final String jsonData, final SchemaContext schemaContext) {
        return parseJsonData(jsonData, schemaContext, Optional.empty());
    }

    /**
     * Parses jsonData into NormalizedNode according to given schema context.
     *
     * @param jsonData        json data fragment as string
     * @param schemaContext   schema context describing associated data model
     * @param parentNodeXpath the xpath referencing the parent node current data fragment belong to
     * @return the NormalizedNode object
     */
    public static NormalizedNode<?, ?> parseJsonData(final String jsonData, final SchemaContext schemaContext,
        final String parentNodeXpath) {
        final DataSchemaNode parentSchemaNode = getDataSchemaNodeByXpath(parentNodeXpath, schemaContext);
        return parseJsonData(jsonData, schemaContext, Optional.of(parentSchemaNode));
    }

    private static NormalizedNode<?, ?> parseJsonData(final String jsonData, final SchemaContext schemaContext,
        final Optional<DataSchemaNode> optionalParentSchemaNode) {
        final JSONCodecFactory jsonCodecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02
            .getShared(schemaContext);
        final NormalizedNodeResult normalizedNodeResult = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter normalizedNodeStreamWriter = ImmutableNormalizedNodeStreamWriter
            .from(normalizedNodeResult);

        try (final JsonParserStream jsonParserStream = optionalParentSchemaNode.isPresent()
            ? JsonParserStream.create(normalizedNodeStreamWriter, jsonCodecFactory, optionalParentSchemaNode.get())
            : JsonParserStream.create(normalizedNodeStreamWriter, jsonCodecFactory)
        ) {
            final JsonReader jsonReader = new JsonReader(new StringReader(jsonData));
            jsonParserStream.parse(jsonReader);

        } catch (final IOException | IllegalStateException | JsonSyntaxException exception) {
            throw new DataValidationException("Failed to parse json data.", String
                .format("Exception occurred on parsing string %s.", jsonData), exception);
        }
        return normalizedNodeResult.getResult();
    }

    /**
     * Create an xpath form a Yang Tools NodeIdentifier (i.e. PathArgument).
     *
     * @param nodeIdentifier the NodeIdentifier
     * @return an xpath
     */
    public static String buildXpath(final YangInstanceIdentifier.PathArgument nodeIdentifier) {
        final StringBuilder xpathBuilder = new StringBuilder();
        xpathBuilder.append("/").append(nodeIdentifier.getNodeType().getLocalName());

        if (nodeIdentifier instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates) {
            xpathBuilder.append(getKeyAttributesStatement(
                (YangInstanceIdentifier.NodeIdentifierWithPredicates) nodeIdentifier));
        }
        return xpathBuilder.toString();
    }

    private static String getKeyAttributesStatement(
        final YangInstanceIdentifier.NodeIdentifierWithPredicates nodeIdentifier) {
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

    private static DataSchemaNode getDataSchemaNodeByXpath(final String parentNodeXpath,
        final SchemaContext schemaContext) {
        final String[] xpathNodeIdSequence = xpathToNodeIdSequence(parentNodeXpath);
        return findDataSchemaNodeByXpathNodeIdSequence(xpathNodeIdSequence, schemaContext.getChildNodes());
    }

    private static String[] xpathToNodeIdSequence(final String xpath) {
        final String[] xpathNodeIdSequence = Arrays.stream(xpath.split(XPATH_DELIMITER_REGEX))
            .map(identifier -> identifier.replaceFirst(XPATH_NODE_KEY_ATTRIBUTES_REGEX, ""))
            .filter(identifier -> !identifier.isEmpty())
            .toArray(String[]::new);
        if (xpathNodeIdSequence.length < 1) {
            throw new DataValidationException("Invalid xpath.", "Xpath contains no node identifiers.");
        }
        return xpathNodeIdSequence;
    }

    private static DataSchemaNode findDataSchemaNodeByXpathNodeIdSequence(final String[] xpathNodeIdSequence,
        final Collection<? extends DataSchemaNode> dataSchemaNodes) {
        final String currentXpathNodeId = xpathNodeIdSequence[0];
        final DataSchemaNode currentDataSchemaNode = dataSchemaNodes.stream()
            .filter(dataSchemaNode -> currentXpathNodeId.equals(dataSchemaNode.getQName().getLocalName()))
            .findFirst().orElseThrow(() -> schemaNodeNotFoundException(currentXpathNodeId));
        if (xpathNodeIdSequence.length <= 1) {
            return currentDataSchemaNode;
        }
        if (currentDataSchemaNode instanceof DataNodeContainer) {
            return findDataSchemaNodeByXpathNodeIdSequence(
                getNextLevelXpathNodeIdSequence(xpathNodeIdSequence),
                ((DataNodeContainer) currentDataSchemaNode).getChildNodes());
        }
        throw schemaNodeNotFoundException(xpathNodeIdSequence[1]);
    }

    private static String[] getNextLevelXpathNodeIdSequence(final String[] xpathNodeIdSequence) {
        final String[] nextXpathNodeIdSequence = new String[xpathNodeIdSequence.length - 1];
        System.arraycopy(xpathNodeIdSequence, 1, nextXpathNodeIdSequence, 0, nextXpathNodeIdSequence.length);
        return nextXpathNodeIdSequence;
    }

    private static DataValidationException schemaNodeNotFoundException(final String schemaNodeIdentifier) {
        return new DataValidationException("Invalid xpath.",
            String.format("No schema node was found for xpath identifier '%s'.", schemaNodeIdentifier));
    }
}
