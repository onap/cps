/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 Deutsche Telekom AG
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
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.xml.sax.SAXException;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class YangUtils {

    private static final String XPATH_DELIMITER_REGEX = "\\/";
    private static final String XPATH_NODE_KEY_ATTRIBUTES_REGEX = "\\[.*?\\]";

    /**
     * Parses data into NormalizedNode according to given schema context.
     *
     * @param nodeData      data string
     * @param schemaContext schema context describing associated data model
     * @return the NormalizedNode object
     */
    @SuppressWarnings("squid:S1452")  // Generic type <? ,?> is returned by external librray, opendaylight.yangtools
    public static NormalizedNode<?, ?> parseData(final ContentType contentType, final String nodeData,
                                                 final SchemaContext schemaContext) {
        if (contentType == ContentType.JSON) {
            return parseJsonData(nodeData, schemaContext, Optional.empty());
        }
        return parseXmlData(XmlFileUtils.prepareXmlContent(nodeData, schemaContext), schemaContext,
                Optional.empty());
    }

    /**
     * Parses data into NormalizedNode according to given schema context.
     *
     * @param nodeData      data string
     * @param schemaContext schema context describing associated data model
     * @return the NormalizedNode object
     */
    @SuppressWarnings("squid:S1452")  // Generic type <? ,?> is returned by external librray, opendaylight.yangtools
    public static NormalizedNode<?, ?> parseData(final ContentType contentType, final String nodeData,
                                                 final SchemaContext schemaContext, final String parentNodeXpath) {
        final var parentSchemaNode = getDataSchemaNodeByXpath(parentNodeXpath, schemaContext);
        if (contentType == ContentType.JSON) {
            return parseJsonData(nodeData, schemaContext, Optional.of(parentSchemaNode));
        }
        return parseXmlData(XmlFileUtils.prepareXmlContent(nodeData, parentSchemaNode, parentNodeXpath), schemaContext,
                Optional.of(parentSchemaNode));
    }

    /**
     * Parses jsonData into NormalizedNode according to given schema context.
     *
     * @param jsonData      json data as string
     * @param schemaContext schema context describing associated data model
     * @return the NormalizedNode object
     */
    @SuppressWarnings("squid:S1452")  // Generic type <? ,?> is returned by external librray, opendaylight.yangtools
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
    @SuppressWarnings("squid:S1452")  // Generic type <? ,?> is returned by external librray, opendaylight.yangtools
    public static NormalizedNode<?, ?> parseJsonData(final String jsonData, final SchemaContext schemaContext,
        final String parentNodeXpath) {
        final var parentSchemaNode = getDataSchemaNodeByXpath(parentNodeXpath, schemaContext);
        return parseJsonData(jsonData, schemaContext, Optional.of(parentSchemaNode));
    }

    private static NormalizedNode<?, ?> parseJsonData(final String jsonData, final SchemaContext schemaContext,
        final Optional<DataSchemaNode> optionalParentSchemaNode) {
        final var jsonCodecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02
            .getShared((EffectiveModelContext) schemaContext);
        final var normalizedNodeResult = new NormalizedNodeResult();
        final var normalizedNodeStreamWriter = ImmutableNormalizedNodeStreamWriter
            .from(normalizedNodeResult);

        try (final JsonParserStream jsonParserStream = optionalParentSchemaNode.isPresent()
            ? JsonParserStream.create(normalizedNodeStreamWriter, jsonCodecFactory, optionalParentSchemaNode.get())
            : JsonParserStream.create(normalizedNodeStreamWriter, jsonCodecFactory)
        ) {
            final var jsonReader = new JsonReader(new StringReader(jsonData));
            jsonParserStream.parse(jsonReader);

        } catch (final IOException | JsonSyntaxException exception) {
            throw new DataValidationException(
                "Failed to parse json data: " + jsonData, exception.getMessage(), exception);
        } catch (final IllegalStateException | IllegalArgumentException exception) {
            throw new DataValidationException(
                "Failed to parse json data. Unsupported xpath or json data:" + jsonData, exception
                .getMessage(), exception);
        }
        return normalizedNodeResult.getResult();
    }

    private static NormalizedNode<?, ?> parseXmlData(final String xmlData, final SchemaContext schemaContext,
                                                     final Optional<DataSchemaNode> optionalParentSchemaNode) {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        final NormalizedNodeResult normalizedNodeResult = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter normalizedNodeStreamWriter = ImmutableNormalizedNodeStreamWriter
               .from(normalizedNodeResult);
        final XmlCodecFactory xmlCodecFactory = XmlCodecFactory.create((EffectiveModelContext) schemaContext);
        try (final XmlParserStream xmlParser = optionalParentSchemaNode.isPresent()
                ? XmlParserStream.create(normalizedNodeStreamWriter, (EffectiveModelContext) schemaContext,
                    optionalParentSchemaNode.get())
                : XmlParserStream.create(normalizedNodeStreamWriter, xmlCodecFactory,
                    xmlCodecFactory.getEffectiveModelContext())) {
            final XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xmlData));
            xmlParser.parse(reader);
        } catch (final XMLStreamException | URISyntaxException | IOException
                       | SAXException | NullPointerException exception) {
            throw new DataValidationException(
                "Failed to parse xml data: " + xmlData, exception.getMessage(), exception);
        }
        final NormalizedNode<?, ?> normalizedNode = normalizedNodeResult.getResult();
        return getFirstChildXmlRoot(normalizedNode);
    }

    /**
     * Create an xpath form a Yang Tools NodeIdentifier (i.e. PathArgument).
     *
     * @param nodeIdentifier the NodeIdentifier
     * @return an xpath
     */
    public static String buildXpath(final YangInstanceIdentifier.PathArgument nodeIdentifier) {
        final var xpathBuilder = new StringBuilder();
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
        final String[] xpathNodeIdSequence = Arrays.stream(xpath
                        .replaceAll(XPATH_NODE_KEY_ATTRIBUTES_REGEX, "")
                        .split(XPATH_DELIMITER_REGEX))
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
        final var nextXpathNodeIdSequence = new String[xpathNodeIdSequence.length - 1];
        System.arraycopy(xpathNodeIdSequence, 1, nextXpathNodeIdSequence, 0, nextXpathNodeIdSequence.length);
        return nextXpathNodeIdSequence;
    }

    private static DataValidationException schemaNodeNotFoundException(final String schemaNodeIdentifier) {
        return new DataValidationException("Invalid xpath.",
            String.format("No schema node was found for xpath identifier '%s'.", schemaNodeIdentifier));
    }

    private static NormalizedNode<?, ?> getFirstChildXmlRoot(final NormalizedNode<?, ?> parent) {
        final Collection<DataContainerChild<?, ?>> children = (Collection<DataContainerChild<?, ?>>)
                parent.getValue();
        final String parentNodeType = parent.getNodeType().getLocalName();
        while (children.iterator().hasNext()) {
            final NormalizedNode<?, ?> child = children.iterator().next();
            if (!child.getNodeType().getLocalName().equals(parentNodeType)) {
                return child;
            }
            return getFirstChildXmlRoot(child);
        }
        throw new DataValidationException("Something really unexpected happened!",
                "Parent NormalizedNode has no children");
    }
}
