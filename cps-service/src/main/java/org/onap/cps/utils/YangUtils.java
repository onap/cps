/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
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

package org.onap.cps.utils;

import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.cpspath.parser.PathParsingException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveStatementInference;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class YangUtils {

    /**
     * Parses jsonData into Collection of NormalizedNode according to given schema context.
     *
     * @param jsonData      json data as string
     * @param schemaContext schema context describing associated data model
     * @return the Collection of NormalizedNode object
     */
    public static ContainerNode parseJsonData(final String jsonData, final SchemaContext schemaContext) {
        return parseJsonData(jsonData, schemaContext, Optional.empty());
    }

    /**
     * Parses jsonData into Collection of NormalizedNode according to given schema context.
     *
     * @param jsonData        json data fragment as string
     * @param schemaContext   schema context describing associated data model
     * @param parentNodeXpath the xpath referencing the parent node current data fragment belong to
     * @return the NormalizedNode object
     */
    public static ContainerNode parseJsonData(final String jsonData, final SchemaContext schemaContext,
        final String parentNodeXpath) {
        final Collection<QName> dataSchemaNodeIdentifiers =
                getDataSchemaNodeIdentifiersByXpath(parentNodeXpath, schemaContext);
        return parseJsonData(jsonData, schemaContext, Optional.of(dataSchemaNodeIdentifiers));
    }

    private static ContainerNode parseJsonData(final String jsonData, final SchemaContext schemaContext,
        final Optional<Collection<QName>> dataSchemaNodeIdentifiers) {
        final JSONCodecFactory jsonCodecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02
            .getShared((EffectiveModelContext) schemaContext);
        final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> dataContainerNodeBuilder =
                Builders.containerBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(schemaContext.getQName()));
        final NormalizedNodeStreamWriter normalizedNodeStreamWriter = ImmutableNormalizedNodeStreamWriter
                .from(dataContainerNodeBuilder);
        final JsonReader jsonReader = new JsonReader(new StringReader(jsonData));
        final JsonParserStream jsonParserStream;

        if (dataSchemaNodeIdentifiers.isPresent()) {
            final EffectiveModelContext effectiveModelContext = ((EffectiveModelContext) schemaContext);
            final EffectiveStatementInference effectiveStatementInference =
                    SchemaInferenceStack.of(effectiveModelContext,
                    SchemaNodeIdentifier.Absolute.of(dataSchemaNodeIdentifiers.get())).toInference();
            jsonParserStream =
                    JsonParserStream.create(normalizedNodeStreamWriter, jsonCodecFactory, effectiveStatementInference);
        } else {
            jsonParserStream = JsonParserStream.create(normalizedNodeStreamWriter, jsonCodecFactory);
        }

        try {
            jsonParserStream.parse(jsonReader);
            jsonParserStream.close();
        } catch (final JsonSyntaxException exception) {
            throw new DataValidationException(
                "Failed to parse json data: " + jsonData, exception.getMessage(), exception);
        } catch (final IOException | IllegalStateException illegalStateException) {
            throw new DataValidationException(
                "Failed to parse json data. Unsupported xpath or json data:" + jsonData, illegalStateException
                .getMessage(), illegalStateException);
        }
        return dataContainerNodeBuilder.build();
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

    private static Collection<QName> getDataSchemaNodeIdentifiersByXpath(final String parentNodeXpath,
                                                                      final SchemaContext schemaContext) {
        final String[] xpathNodeIdSequence = xpathToNodeIdSequence(parentNodeXpath);
        return findDataSchemaNodeIdentifiersByXpathNodeIdSequence(xpathNodeIdSequence, schemaContext.getChildNodes(),
                new ArrayList<>());
    }

    private static String[] xpathToNodeIdSequence(final String xpath) {
        try {
            return CpsPathUtil.getXpathNodeIdSequence(xpath);
        } catch (final PathParsingException pathParsingException) {
            throw new DataValidationException(pathParsingException.getMessage(), pathParsingException.getDetails(),
                    pathParsingException);
        }
    }

    private static Collection<QName> findDataSchemaNodeIdentifiersByXpathNodeIdSequence(
            final String[] xpathNodeIdSequence,
            final Collection<? extends DataSchemaNode> dataSchemaNodes,
            final Collection<QName> dataSchemaNodeIdentifiers) {
        final String currentXpathNodeId = xpathNodeIdSequence[0];
        final DataSchemaNode currentDataSchemaNode = dataSchemaNodes.stream()
            .filter(dataSchemaNode -> currentXpathNodeId.equals(dataSchemaNode.getQName().getLocalName()))
            .findFirst().orElseThrow(() -> schemaNodeNotFoundException(currentXpathNodeId));
        dataSchemaNodeIdentifiers.add(currentDataSchemaNode.getQName());
        if (xpathNodeIdSequence.length <= 1) {
            return dataSchemaNodeIdentifiers;
        }
        if (currentDataSchemaNode instanceof DataNodeContainer) {
            return findDataSchemaNodeIdentifiersByXpathNodeIdSequence(
                getNextLevelXpathNodeIdSequence(xpathNodeIdSequence),
                    ((DataNodeContainer) currentDataSchemaNode).getChildNodes(),
                    dataSchemaNodeIdentifiers);
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
