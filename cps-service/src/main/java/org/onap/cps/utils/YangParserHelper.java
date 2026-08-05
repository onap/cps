/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2025-2026 Deutsche Telekom AG
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.cpspath.parser.PathParsingException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveStatementInference;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Slf4j
@Service
@RequiredArgsConstructor
public class YangParserHelper {

    static final String DATA_ROOT_NODE_NAMESPACE = "urn:ietf:params:xml:ns:netconf:base:1.0";
    static final String DATA_ROOT_NODE_TAG_NAME = "data";
    static final String DATA_VALIDATION_FAILURE_MESSAGE = "Data Validation Failed";
    private static final String PATH_DELIMITER = "/";
    private static final String DATA_SCHEMA_NODE = "dataSchemaNode";
    static final boolean VALIDATE_ONLY = true;
    static final boolean VALIDATE_AND_PARSE = false;

    /**
     * Parses data into NormalizedNode according to given schema context.
     *
     * @param contentType     the type of the node data (json or xml)
     * @param nodeData        data string
     * @param schemaContext   schema context describing associated data model
     * @param parentNodeXpath the xpath referencing the parent node current data fragment belong to
     * @return the NormalizedNode object
     */
    public ContainerNode parseData(final ContentType contentType,
                                   final String nodeData,
                                   final SchemaContext schemaContext,
                                   final String parentNodeXpath,
                                   final boolean validateOnly) {
        if (contentType == ContentType.JSON) {
            final ContainerNode validatedAndParsedJson = parseJsonData(nodeData, schemaContext, parentNodeXpath);
            if (validateOnly) {
                return null;
            }
            return validatedAndParsedJson;
        }
        final ContainerNode parsedXmlData = parseXmlData(nodeData, schemaContext, parentNodeXpath);
        if (validateOnly) {
            return null;
        }
        return parsedXmlData;
    }

    private ContainerNode parseJsonData(final String jsonData,
                                        final SchemaContext schemaContext,
                                        final String parentNodeXpath) {
        final JSONCodecFactory jsonCodecFactory =
            JSONCodecFactorySupplier.RFC7951.getShared((EffectiveModelContext) schemaContext);
        final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> dataContainerNodeBuilder =
                ImmutableNodes.builderFactory().newContainerBuilder()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(
                            QName.create(DATA_ROOT_NODE_NAMESPACE, DATA_ROOT_NODE_TAG_NAME)
                        ));
        final NormalizedNodeStreamWriter normalizedNodeStreamWriter = ImmutableNormalizedNodeStreamWriter
                .from(dataContainerNodeBuilder);
        final ValueRecordingJsonReader jsonReader = new ValueRecordingJsonReader(new StringReader(jsonData));
        final JsonParserStream jsonParserStream;

        if (parentNodeXpath.isEmpty()) {
            jsonParserStream = JsonParserStream.create(normalizedNodeStreamWriter, jsonCodecFactory);
        } else {
            final Collection<QName> dataSchemaNodeIdentifiers
                = getDataSchemaNodeIdentifiers(schemaContext, parentNodeXpath);
            final EffectiveModelContext effectiveModelContext = (EffectiveModelContext) schemaContext;
            final EffectiveStatementInference effectiveStatementInference =
                SchemaInferenceStack.of(effectiveModelContext,
                    SchemaNodeIdentifier.Absolute.of(dataSchemaNodeIdentifiers)).toInference();
            jsonParserStream =
                JsonParserStream.create(normalizedNodeStreamWriter, jsonCodecFactory, effectiveStatementInference);
        }

        try (jsonParserStream) {
            jsonParserStream.parse(jsonReader);
        } catch (final IOException | JsonSyntaxException | IllegalStateException | IllegalArgumentException
                       | NullPointerException exception) {
            throw JsonParseFailureTranslator.translate(
                buildJsonDataValidationException(exception, jsonReader, schemaContext, parentNodeXpath));
        }
        return dataContainerNodeBuilder.build();
    }

    private static JsonNodeFailureContext buildJsonDataValidationException(final Exception exception,
                                                                      final ValueRecordingJsonReader jsonReader,
                                                                      final SchemaContext schemaContext,
                                                                      final String parentNodeXpath) {
        final List<String> nodeNames = getFailingNodeName(jsonReader.getPath(), parentNodeXpath);
        if (nodeNames.isEmpty()) {
            return new JsonNodeFailureContext(exception, null, PATH_DELIMITER, "",
                jsonReader.getLastStringValue(), jsonReader.isLastValueNull(),
                Set.copyOf(jsonReader.getSeenFieldNames()));
        }
        final String xpath = PATH_DELIMITER + String.join(PATH_DELIMITER, nodeNames);
        final String nodeName = nodeNames.get(nodeNames.size() - 1);
        DataSchemaNode dataSchemaNode = null;
        try {
            dataSchemaNode = (DataSchemaNode) findDataSchemaNodeAndIdentifiersByXpathNodeIdSequence(
                nodeNames, schemaContext.getChildNodes(), new ArrayList<>()).get(DATA_SCHEMA_NODE);
        } catch (final DataValidationException schemaLookupException) {
            // The JSON path did not resolve to a known schema node (e.g. an
            // unrecognised field name). A null schema node is still forwarded so the
            // FallbackRule can produce a useful error message for the caller.
            log.debug("Schema node not found during JSON parse failure analysis." + schemaLookupException);
        }
        return new JsonNodeFailureContext(exception, dataSchemaNode, xpath, nodeName,
            jsonReader.getLastStringValue(), jsonReader.isLastValueNull(),
            Set.copyOf(jsonReader.getSeenFieldNames()));
    }

    private static List<String> getFailingNodeName(final String jsonPath, final String parentNodeXpath) {
        final List<String> nodeNames = new ArrayList<>();
        if (!StringUtils.isEmpty(parentNodeXpath)) {
            nodeNames.addAll(xpathToNodeIdSequence(parentNodeXpath));
        }
        final StringTokenizer jsonPathTokenizer = new StringTokenizer(jsonPath, ".");
        while (jsonPathTokenizer.hasMoreTokens()) {
            final String jsonPathToken = jsonPathTokenizer.nextToken();
            if (!"$".equals(jsonPathToken)) {
                nodeNames.add(withoutListIndex(jsonPathToken));
            }
        }
        return nodeNames;
    }

    private static String withoutListIndex(final String jsonPathToken) {
        final int startOfListIndex = jsonPathToken.indexOf('[');
        if (startOfListIndex < 0) {
            return jsonPathToken;
        }
        return jsonPathToken.substring(0, startOfListIndex);
    }


    @SuppressFBWarnings(value = "DCN_NULLPOINTER_EXCEPTION", justification = "Problem originates in 3PP code")
    private ContainerNode parseXmlData(final String xmlData,
                                       final SchemaContext schemaContext,
                                       final String parentNodeXpath) {
        final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> dataRootBuilder =
                newDataRootBuilder();
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(dataRootBuilder);
        final boolean hasParentNode = !parentNodeXpath.isEmpty();
        final String prepareXml = prepareXml(xmlData, schemaContext, parentNodeXpath);
        final XmlParserStream xmlParserStream = hasParentNode
                ? XmlParserStream.create(writer, getEffectiveStatementInference(schemaContext, parentNodeXpath))
                : XmlParserStream.create(writer, (EffectiveModelContext) schemaContext);
        parseXmlIntoWriter(prepareXml, xmlParserStream);
        final ContainerNode parsedXml = dataRootBuilder.build();
        final boolean wrappedByRootNode = !prepareXml.equals(xmlData);
        return (hasParentNode || wrappedByRootNode) ? removeWrappedRootNode(parsedXml) : parsedXml;
    }

    private static String prepareXml(final String xmlData,
                                     final SchemaContext schemaContext,
                                     final String parentNodeXpath) {
        try {
            if (parentNodeXpath.isEmpty()) {
                return XmlUtils.parsedXmlContent(xmlData, schemaContext);
            }
            final DataSchemaNode parentSchemaNode =
                    (DataSchemaNode) getDataSchemaNodeAndIdentifiersByXpath(parentNodeXpath, schemaContext)
                            .get("dataSchemaNode");
            return XmlUtils.parsedXmlContent(xmlData, parentSchemaNode, parentNodeXpath);
        } catch (final IOException | ParserConfigurationException | TransformerException | SAXException exception) {
            throw new DataValidationException(
                    DATA_VALIDATION_FAILURE_MESSAGE, "Failed to parse xml data: " + exception.getMessage(), exception);
        }
    }

    private static EffectiveStatementInference getEffectiveStatementInference(final SchemaContext schemaContext,
                                                                              final String parentNodeXpath) {
        return SchemaInferenceStack.of((EffectiveModelContext) schemaContext,
                SchemaNodeIdentifier.Absolute.of(getDataSchemaNodeIdentifiers(schemaContext, parentNodeXpath)))
                .toInference();
    }

    @SuppressFBWarnings(value = "DCN_NULLPOINTER_EXCEPTION", justification = "Problem originates in 3PP code")
    private static void parseXmlIntoWriter(final String xml, final XmlParserStream xmlParserStream) {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        try (xmlParserStream; StringReader stringReader = new StringReader(xml)) {
            xmlParserStream.parse(factory.createXMLStreamReader(stringReader));
        } catch (final XMLStreamException | IOException | NullPointerException exception) {
            throw new DataValidationException(
                    DATA_VALIDATION_FAILURE_MESSAGE, "Failed to parse xml data: " + exception.getMessage(), exception);
        }
    }

    private static DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> newDataRootBuilder() {
        return ImmutableNodes.builderFactory().newContainerBuilder()
                .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(
                        QName.create(DATA_ROOT_NODE_NAMESPACE, DATA_ROOT_NODE_TAG_NAME)));
    }

    private static ContainerNode removeWrappedRootNode(final ContainerNode container) {
        if (container.body().isEmpty()) {
            return container;
        }
        final NormalizedNode firstChildNode = container.body().iterator().next();
        final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> builder =
                ImmutableNodes.builderFactory().newContainerBuilder()
                        .withNodeIdentifier(container.name());
        if (firstChildNode instanceof MapNode mapNode) {
            final MapEntryNode mapEntry = mapNode.body().iterator().next();
            for (final DataContainerChild dataContainerChild : mapEntry.body()) {
                builder.withChild(dataContainerChild);
            }
        } else if (firstChildNode instanceof DataContainerNode dataContainerNode) {
            for (final DataContainerChild dataContainerChild : dataContainerNode.body()) {
                builder.withChild(dataContainerChild);
            }
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static Collection<QName> getDataSchemaNodeIdentifiers(final SchemaContext schemaContext,
                                                                  final String parentNodeXpath) {
        return (Collection<QName>) getDataSchemaNodeAndIdentifiersByXpath(parentNodeXpath, schemaContext)
            .get("dataSchemaNodeIdentifiers");
    }

    private static Map<String, Object> getDataSchemaNodeAndIdentifiersByXpath(final String parentNodeXpath,
                                                                              final SchemaContext schemaContext) {
        final List<String> xpathNodeIdSequence = xpathToNodeIdSequence(parentNodeXpath);
        return findDataSchemaNodeAndIdentifiersByXpathNodeIdSequence(xpathNodeIdSequence, schemaContext.getChildNodes(),
                new ArrayList<>());
    }

    private static List<String> xpathToNodeIdSequence(final String xpath) {
        try {
            return CpsPathUtil.getXpathNodeIdSequence(xpath);
        } catch (final PathParsingException pathParsingException) {
            throw new DataValidationException(pathParsingException.getMessage(), pathParsingException.getDetails(),
                    pathParsingException);
        }
    }

    private static Map<String, Object> findDataSchemaNodeAndIdentifiersByXpathNodeIdSequence(
            final List<String> xpathNodeIdSequence,
            final Collection<? extends DataSchemaNode> dataSchemaNodes,
            final Collection<QName> dataSchemaNodeIdentifiers) {
        final String currentXpathNodeId = xpathNodeIdSequence.get(0);
        final DataSchemaNode currentDataSchemaNode = dataSchemaNodes.stream()
            .filter(dataSchemaNode -> currentXpathNodeId.equals(dataSchemaNode.getQName().getLocalName()))
            .findFirst().orElseThrow(() -> schemaNodeNotFoundException(currentXpathNodeId));
        dataSchemaNodeIdentifiers.add(currentDataSchemaNode.getQName());
        if (xpathNodeIdSequence.size() <= 1) {
            final Map<String, Object> dataSchemaNodeAndIdentifiers = new HashMap<>();
            dataSchemaNodeAndIdentifiers.put("dataSchemaNode", currentDataSchemaNode);
            dataSchemaNodeAndIdentifiers.put("dataSchemaNodeIdentifiers", dataSchemaNodeIdentifiers);
            return dataSchemaNodeAndIdentifiers;
        }
        if (currentDataSchemaNode instanceof DataNodeContainer) {
            return findDataSchemaNodeAndIdentifiersByXpathNodeIdSequence(
                    getNextLevelXpathNodeIdSequence(xpathNodeIdSequence),
                    ((DataNodeContainer) currentDataSchemaNode).getChildNodes(),
                    dataSchemaNodeIdentifiers);
        }
        throw schemaNodeNotFoundException(xpathNodeIdSequence.get(1));
    }

    private static List<String> getNextLevelXpathNodeIdSequence(final List<String> xpathNodeIdSequence) {
        return xpathNodeIdSequence.subList(1, xpathNodeIdSequence.size());
    }

    private static DataValidationException schemaNodeNotFoundException(final String schemaNodeIdentifier) {
        return new DataValidationException("Invalid xpath.",
            String.format("No schema node was found for xpath identifier '%s'.", schemaNodeIdentifier));
    }
}
