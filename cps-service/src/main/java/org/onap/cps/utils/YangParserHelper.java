/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import lombok.RequiredArgsConstructor;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.cpspath.parser.PathParsingException;
import org.onap.cps.spi.exceptions.DataValidationException;
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
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Service
@RequiredArgsConstructor
public class YangParserHelper {

    static final String DATA_ROOT_NODE_NAMESPACE = "urn:ietf:params:xml:ns:netconf:base:1.0";
    static final String DATA_ROOT_NODE_TAG_NAME = "data";

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
                                   final String parentNodeXpath) {
        if (contentType == ContentType.JSON) {
            return parseJsonData(nodeData, schemaContext, parentNodeXpath);
        }
        return parseXmlData(nodeData, schemaContext, parentNodeXpath);
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
        final JsonReader jsonReader = new JsonReader(new StringReader(jsonData));
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
        } catch (final IOException | JsonSyntaxException exception) {
            throw new DataValidationException(
                    "Failed to parse json data: " + jsonData, exception.getMessage(), exception);
        } catch (final IllegalStateException | IllegalArgumentException exception) {
            throw new DataValidationException(
                    "Failed to parse json data. Unsupported xpath or json data:" + jsonData, exception
                    .getMessage(), exception);
        }
        return dataContainerNodeBuilder.build();
    }

    private ContainerNode parseXmlData(final String xmlData,
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
                preparedXmlContent = XmlFileUtils.prepareXmlContent(xmlData, schemaContext);
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
                preparedXmlContent = XmlFileUtils.prepareXmlContent(xmlData, parentSchemaNode, parentNodeXpath);
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
                "Failed to parse xml data: " + xmlData, exception.getMessage(), exception);
        }
        final DataContainerChild dataContainerChild =
            (DataContainerChild) getFirstChildXmlRoot(normalizedNodeResult.getResult());
        final YangInstanceIdentifier.NodeIdentifier nodeIdentifier =
            new YangInstanceIdentifier.NodeIdentifier(dataContainerChild.getIdentifier().getNodeType());
        return Builders.containerBuilder().withChild(dataContainerChild).withNodeIdentifier(nodeIdentifier).build();
    }

    private static Collection<QName> getDataSchemaNodeIdentifiers(final SchemaContext schemaContext,
                                                                  final String parentNodeXpath) {
        return (Collection<QName>) getDataSchemaNodeAndIdentifiersByXpath(parentNodeXpath, schemaContext)
            .get("dataSchemaNodeIdentifiers");
    }

    private static Map<String, Object> getDataSchemaNodeAndIdentifiersByXpath(final String parentNodeXpath,
                                                                              final SchemaContext schemaContext) {
        final String[] xpathNodeIdSequence = xpathToNodeIdSequence(parentNodeXpath);
        return findDataSchemaNodeAndIdentifiersByXpathNodeIdSequence(xpathNodeIdSequence, schemaContext.getChildNodes(),
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

    private static Map<String, Object> findDataSchemaNodeAndIdentifiersByXpathNodeIdSequence(
            final String[] xpathNodeIdSequence,
            final Collection<? extends DataSchemaNode> dataSchemaNodes,
            final Collection<QName> dataSchemaNodeIdentifiers) {
        final String currentXpathNodeId = xpathNodeIdSequence[0];
        final DataSchemaNode currentDataSchemaNode = dataSchemaNodes.stream()
            .filter(dataSchemaNode -> currentXpathNodeId.equals(dataSchemaNode.getQName().getLocalName()))
            .findFirst().orElseThrow(() -> schemaNodeNotFoundException(currentXpathNodeId));
        dataSchemaNodeIdentifiers.add(currentDataSchemaNode.getQName());
        if (xpathNodeIdSequence.length <= 1) {
            final Map<String, Object> dataSchemaNodeAndIdentifiers =
                    new HashMap<>();
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

    private static NormalizedNode getFirstChildXmlRoot(final NormalizedNode parent) {
        final String rootNodeType = parent.getIdentifier().getNodeType().getLocalName();
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
}
