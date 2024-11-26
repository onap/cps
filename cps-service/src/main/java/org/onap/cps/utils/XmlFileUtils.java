/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Deutsche Telekom AG
 *  Modifications Copyright (C) 2023-2024 Nordix Foundation.
 *  Modifications Copyright (C) 2024 TechMahindra Ltd.
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.api.exceptions.DataValidationException;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class XmlFileUtils {

    private static final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private static boolean isNewDocumentBuilderFactoryInstance = true;
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private static boolean isNewTransformerFactoryInstance = true;
    private static final Pattern XPATH_PROPERTY_REGEX =
        Pattern.compile("\\[@(\\S{1,100})=['\\\"](\\S{1,100})['\\\"]\\]");

    /**
     * Prepare XML content.
     *
     * @param xmlContent XML content sent to store
     * @param schemaContext schema context
     *
     * @return XML content wrapped by root node (if needed)
     */
    public static String prepareXmlContent(final String xmlContent, final SchemaContext schemaContext)
        throws IOException, ParserConfigurationException, TransformerException, SAXException {
        return addRootNodeToXmlContent(xmlContent, schemaContext.getModules().iterator().next().getName(),
                YangParserHelper.DATA_ROOT_NODE_NAMESPACE);
    }

    /**
     * Prepare XML content.
     *
     * @param xmlContent XML content sent to store
     * @param parentSchemaNode Parent schema node
     * @param xpath Parent xpath
     *
     * @return XML content wrapped by root node (if needed)
     */
    public static String prepareXmlContent(final String xmlContent,
                                           final DataSchemaNode parentSchemaNode,
                                           final String xpath)
        throws IOException, ParserConfigurationException, TransformerException, SAXException {
        final String namespace = parentSchemaNode.getQName().getNamespace().toString();
        final String parentXpathPart = xpath.substring(xpath.lastIndexOf('/') + 1);
        final Matcher regexMatcher = XPATH_PROPERTY_REGEX.matcher(parentXpathPart);
        if (regexMatcher.find()) {
            final HashMap<String, String> rootNodePropertyMap = new HashMap<>();
            rootNodePropertyMap.put(regexMatcher.group(1), regexMatcher.group(2));
            return addRootNodeToXmlContent(xmlContent, parentSchemaNode.getQName().getLocalName(), namespace,
                    rootNodePropertyMap);
        }

        return addRootNodeToXmlContent(xmlContent, parentSchemaNode.getQName().getLocalName(), namespace);
    }

    private static String addRootNodeToXmlContent(final String xmlContent,
                                                 final String rootNodeTagName,
                                                 final String namespace,
                                                 final Map<String, String> rootNodeProperty)
        throws IOException, SAXException, ParserConfigurationException, TransformerException {
        final DocumentBuilder documentBuilder = getDocumentBuilderFactory().newDocumentBuilder();
        final Document document =
            documentBuilder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
        final Element root = document.getDocumentElement();
        if (!root.getTagName().equals(rootNodeTagName)
            && !root.getTagName().equals(YangParserHelper.DATA_ROOT_NODE_TAG_NAME)) {
            final Document documentWithRootNode = addDataRootNode(root, rootNodeTagName, namespace, rootNodeProperty);
            documentWithRootNode.setXmlStandalone(true);
            final Transformer transformer = getTransformerFactory().newTransformer();
            final StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(documentWithRootNode), new StreamResult(stringWriter));
            return stringWriter.toString();
        }
        return xmlContent;
    }

    /**
     * Add root node to XML content.
     *
     * @param xmlContent XML content to add root node into
     * @param rootNodeTagName Root node tag name
     * @return XML content with root node tag added (if needed)
     */
    public static String addRootNodeToXmlContent(final String xmlContent,
                                                 final String rootNodeTagName,
                                                 final String namespace)
        throws IOException, ParserConfigurationException, TransformerException, SAXException {
        return addRootNodeToXmlContent(xmlContent, rootNodeTagName, namespace, new HashMap<>());
    }

    /**
     * Add root node into DOM element.
     *
     * @param node DOM element to add root node into
     * @param tagName Root tag name to add
     * @return DOM element with a root node
     */
    static Document addDataRootNode(final Element node,
                                    final String tagName,
                                    final String namespace,
                                    final Map<String, String> rootNodeProperty)
        throws ParserConfigurationException {
        final DocumentBuilder documentBuilder = getDocumentBuilderFactory().newDocumentBuilder();
        final Document document = documentBuilder.newDocument();
        final Element rootElement = document.createElementNS(namespace, tagName);
        for (final Map.Entry<String, String> entry : rootNodeProperty.entrySet()) {
            final Element propertyElement = document.createElement(entry.getKey());
            propertyElement.setTextContent(entry.getValue());
            rootElement.appendChild(propertyElement);
        }
        rootElement.appendChild(document.adoptNode(node));
        document.appendChild(rootElement);
        return document;
    }

    /**
     * Convert a list of data maps to XML format.
     *
     * @param dataMaps List of data maps to convert
     * @return XML string representation of the data maps
     */
    @SuppressFBWarnings(value = "DCN_NULLPOINTER_EXCEPTION")
    public static String convertDataMapsToXml(final List<Map<String, Object>> dataMaps) {
        try {
            final DocumentBuilder documentBuilder = getDocumentBuilderFactory().newDocumentBuilder();
            final Document document = documentBuilder.newDocument();
            final DocumentFragment documentFragment = document.createDocumentFragment();
            for (final Map<String, Object> dataMap : dataMaps) {
                createXmlElements(document, documentFragment, dataMap);
            }
            return transformFragmentToString(documentFragment);
        } catch (final DOMException |  NullPointerException | ParserConfigurationException | TransformerException
                exception) {
            throw new DataValidationException(
                    "Data Validation Failed", "Failed to parse xml data: " + exception.getMessage(), exception);
        }
    }

    private static void createXmlElements(final Document document, final Node parentNode,
                                          final Map<String, Object> dataMap) {
        for (final Map.Entry<String, Object> dataNodeMapEntry : dataMap.entrySet()) {
            if (dataNodeMapEntry.getValue() instanceof List) {
                appendList(document, parentNode, dataNodeMapEntry);
            } else if (dataNodeMapEntry.getValue() instanceof Map) {
                appendMap(document, parentNode, dataNodeMapEntry);
            } else {
                appendObject(document, parentNode, dataNodeMapEntry);
            }
        }
    }

    private static void appendList(final Document document, final Node parentNode,
                                   final Map.Entry<String, Object> dataNodeMapEntry) {
        final List<Object> dataNodeMaps = (List<Object>) dataNodeMapEntry.getValue();
        if (dataNodeMaps.isEmpty()) {
            final Element listElement = document.createElement(dataNodeMapEntry.getKey());
            parentNode.appendChild(listElement);
        } else {
            for (final Object dataNodeMap : dataNodeMaps) {
                final Element listElement = document.createElement(dataNodeMapEntry.getKey());
                if (dataNodeMap == null) {
                    parentNode.appendChild(listElement);
                } else if (dataNodeMap instanceof Map) {
                    createXmlElements(document, listElement, (Map<String, Object>) dataNodeMap);
                } else {
                    listElement.appendChild(document.createTextNode(dataNodeMap.toString()));
                }
                parentNode.appendChild(listElement);
            }
        }
    }

    private static void appendMap(final Document document, final Node parentNode,
                                  final Map.Entry<String, Object> dataNodeMapEntry) {
        final Element childElement = document.createElement(dataNodeMapEntry.getKey());
        createXmlElements(document, childElement, (Map<String, Object>) dataNodeMapEntry.getValue());
        parentNode.appendChild(childElement);
    }

    private static void appendObject(final Document document, final Node parentNode,
                                     final Map.Entry<String, Object> dataNodeMapEntry) {
        final Element element = document.createElement(dataNodeMapEntry.getKey());
        if (dataNodeMapEntry.getValue() != null) {
            element.appendChild(document.createTextNode(dataNodeMapEntry.getValue().toString()));
        }
        parentNode.appendChild(element);
    }

    private static String transformFragmentToString(final DocumentFragment documentFragment)
            throws TransformerException {
        final Transformer transformer = getTransformerFactory().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        final StringWriter writer = new StringWriter();
        final StreamResult result = new StreamResult(writer);
        transformer.transform(new DOMSource(documentFragment), result);
        return writer.toString();
    }

    private static DocumentBuilderFactory getDocumentBuilderFactory() {

        if (isNewDocumentBuilderFactoryInstance) {
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            isNewDocumentBuilderFactoryInstance = false;
        }

        return documentBuilderFactory;
    }

    private static TransformerFactory getTransformerFactory() {

        if (isNewTransformerFactoryInstance) {
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            isNewTransformerFactoryInstance = false;
        }

        return transformerFactory;
    }
}
