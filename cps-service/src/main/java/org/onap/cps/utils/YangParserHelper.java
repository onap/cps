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
import com.google.gson.stream.JsonReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.cpspath.parser.PathParsingException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.codec.xml.XmlParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.EffectiveStatementInference;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Service
@RequiredArgsConstructor
public class YangParserHelper {

    static final String DATA_ROOT_NODE_NAMESPACE = "urn:ietf:params:xml:ns:netconf:base:1.0";
    static final String DATA_ROOT_NODE_TAG_NAME = "data";
    static final String DATA_VALIDATION_FAILURE_MESSAGE = "Data Validation Failed";
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
        final NormalizedNodeResult normalizedNodeResult = parseXmlData(nodeData, schemaContext, parentNodeXpath);
        if (validateOnly) {
            return null;
        }
        return buildContainerNodeFormNormalizedNodeResult(normalizedNodeResult);
    }

    private ContainerNode parseJsonData(final String jsonData,
                                        final SchemaContext schemaContext,
                                        final String parentNodeXpath) {
        final JSONCodecFactory jsonCodecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02
            .getShared((EffectiveModelContext) schemaContext);
        final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> dataContainerNodeBuilder =
                Builders.containerBuilder()
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
            final EffectiveModelContext effectiveModelContext = ((EffectiveModelContext) schemaContext);
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
            throw jsonDataValidationException(exception, jsonReader, schemaContext, parentNodeXpath);
        }
        return dataContainerNodeBuilder.build();
    }

    /**
     * Converts a json parser failure into a user friendly exception, reporting the xpath of the offending node.
     * Falls back to the original parser message when the failure is not a recognised validation problem.
     */
    private static DataValidationException jsonDataValidationException(final Exception exception,
                                                                      final ValueRecordingJsonReader jsonReader,
                                                                      final SchemaContext schemaContext,
                                                                      final String parentNodeXpath) {
        final List<String> nodeNames = nodeNamesOfFailingNode(jsonReader.getPath(), parentNodeXpath);
        if (isLeaf(findDataSchemaNode(nodeNames, schemaContext))) {
            final String xpath = "/" + String.join("/", nodeNames);
            final String nodeName = nodeNames.get(nodeNames.size() - 1);
            if (jsonReader.isLastValueNull()) {
                return new DataValidationException(DATA_VALIDATION_FAILURE_MESSAGE,
                    String.format("Validation failed at xpath '%s' : Mandatory field '%s' must not be null.",
                        xpath, nodeName));
            }
            if (isInvalidIntegerValue(exception)) {
                return new DataValidationException(DATA_VALIDATION_FAILURE_MESSAGE,
                    String.format("Validation failed at xpath '%s' : Invalid integer value '%s'. "
                        + "Expected an integer type.", xpath, jsonReader.getLastStringValue()));
            }
        }
        return new DataValidationException(
                DATA_VALIDATION_FAILURE_MESSAGE, "Failed to parse json data. " + exception.getMessage(), exception);
    }

    /**
     * Tells whether the parser rejected the value because it is not a valid number for the type of the node.
     * The cause of the failure is used to recognise this, so the message of the 3PP exception is not relied upon.
     */
    private static boolean isInvalidIntegerValue(final Exception exception) {
        return exception.getCause() instanceof NumberFormatException;
    }

    private static boolean isLeaf(final DataSchemaNode dataSchemaNode) {
        return dataSchemaNode instanceof TypedDataSchemaNode;
    }

    /**
     * Lists the node names leading to the node the json reader stopped on, starting from the root of the model.
     * The json reader reports a path like "$.bookstore.premises.addresses[0].postal-code", which holds the node
     * names relative to the parent node the data fragment belongs to.
     */
    private static List<String> nodeNamesOfFailingNode(final String jsonPath, final String parentNodeXpath) {
        final List<String> nodeNames = new ArrayList<>();
        if (!parentNodeXpath.isEmpty()) {
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

    /**
     * Finds the schema node for the given sequence of node names, returning null when any of them is not modelled.
     */
    private static DataSchemaNode findDataSchemaNode(final List<String> nodeNames,
                                                     final SchemaContext schemaContext) {
        Collection<? extends DataSchemaNode> candidateDataSchemaNodes = schemaContext.getChildNodes();
        DataSchemaNode matchingDataSchemaNode = null;
        for (final String nodeName : nodeNames) {
            matchingDataSchemaNode = candidateDataSchemaNodes.stream()
                .filter(dataSchemaNode -> nodeName.equals(dataSchemaNode.getQName().getLocalName()))
                .findFirst().orElse(null);
            if (matchingDataSchemaNode == null) {
                return null;
            }
            candidateDataSchemaNodes = matchingDataSchemaNode instanceof DataNodeContainer dataNodeContainer
                ? dataNodeContainer.getChildNodes() : List.of();
        }
        return matchingDataSchemaNode;
    }

    @SuppressFBWarnings(value = "DCN_NULLPOINTER_EXCEPTION", justification = "Problem originates in 3PP code")
    private NormalizedNodeResult parseXmlData(final String xmlData,
                                       final SchemaContext schemaContext,
                                       final String parentNodeXpath) {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        final NormalizedNodeResult normalizedNodeResult = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter normalizedNodeStreamWriter = ImmutableNormalizedNodeStreamWriter
                .from(normalizedNodeResult);

        final EffectiveModelContext effectiveModelContext = (EffectiveModelContext) schemaContext;
        final XmlParserStream xmlParserStream;
        final String preparedXmlContent;
        try {
            if (parentNodeXpath.isEmpty()) {
                preparedXmlContent = XmlUtils.prepareXmlContent(xmlData, schemaContext);
                xmlParserStream = XmlParserStream.create(normalizedNodeStreamWriter, effectiveModelContext);
            } else {
                final DataSchemaNode parentSchemaNode =
                    (DataSchemaNode) getDataSchemaNodeAndIdentifiersByXpath(parentNodeXpath, schemaContext)
                        .get("dataSchemaNode");
                final Collection<QName> dataSchemaNodeIdentifiers =
                    getDataSchemaNodeIdentifiers(schemaContext, parentNodeXpath);
                final EffectiveStatementInference effectiveStatementInference =
                    SchemaInferenceStack.of(effectiveModelContext,
                        SchemaNodeIdentifier.Absolute.of(dataSchemaNodeIdentifiers)).toInference();
                preparedXmlContent = XmlUtils.prepareXmlContent(xmlData, parentSchemaNode, parentNodeXpath);
                xmlParserStream = XmlParserStream.create(normalizedNodeStreamWriter, effectiveStatementInference);
            }

            try (xmlParserStream;
                 StringReader stringReader = new StringReader(preparedXmlContent)) {
                final XMLStreamReader xmlStreamReader = factory.createXMLStreamReader(stringReader);
                xmlParserStream.parse(xmlStreamReader);
            }
        } catch (final XMLStreamException | URISyntaxException | IOException | SAXException | NullPointerException
                       | ParserConfigurationException | TransformerException exception) {
            throw new DataValidationException(
                    DATA_VALIDATION_FAILURE_MESSAGE, "Failed to parse xml data: " + exception.getMessage(), exception);
        }
        return normalizedNodeResult;
    }

    private ContainerNode buildContainerNodeFormNormalizedNodeResult(final NormalizedNodeResult normalizedNodeResult) {

        final DataContainerChild dataContainerChild =
                (DataContainerChild) getFirstChildXmlRoot(normalizedNodeResult.getResult());
        final YangInstanceIdentifier.NodeIdentifier nodeIdentifier =
                new YangInstanceIdentifier.NodeIdentifier(dataContainerChild.getIdentifier().getNodeType());
        return Builders.containerBuilder().withChild(dataContainerChild).withNodeIdentifier(nodeIdentifier).build();
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

    private static NormalizedNode getFirstChildXmlRoot(final NormalizedNode parent) {
        final String rootNodeType = parent.getIdentifier().getNodeType().getLocalName();
        @SuppressWarnings("unchecked")
        final Collection<DataContainerChild> children = (Collection<DataContainerChild>) parent.body();
        final Iterator<DataContainerChild> iterator = children.iterator();
        NormalizedNode child = null;
        while (iterator.hasNext()) {
            child = iterator.next();
            if (!child.getIdentifier().getNodeType().getLocalName().equals(rootNodeType)
                    && !(child instanceof LeafNode)) {
                return child;
            }
        }
        return getFirstChildXmlRoot(child);
    }

    /**
     * Json reader that remembers the last value read, so it can be reported when validation fails on it.
     */
    private static class ValueRecordingJsonReader extends JsonReader {

        private String lastStringValue;
        private boolean lastValueNull;

        ValueRecordingJsonReader(final Reader reader) {
            super(reader);
        }

        String getLastStringValue() {
            return lastStringValue;
        }

        boolean isLastValueNull() {
            return lastValueNull;
        }

        @Override
        public String nextString() throws IOException {
            lastStringValue = super.nextString();
            lastValueNull = false;
            return lastStringValue;
        }

        @Override
        public void nextNull() throws IOException {
            lastStringValue = null;
            lastValueNull = true;
            super.nextNull();
        }
    }
}
