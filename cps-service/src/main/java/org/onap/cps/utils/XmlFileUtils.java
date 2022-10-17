/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Deutsche Telekom AG
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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class XmlFileUtils {

    private static DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    private static final String DATA_ROOT_NODE_TAG_NAME = "data";
    private static final String ROOT_NODE_NAMESPACE = "urn:ietf:params:xml:ns:netconf:base:1.0";
    private static final Pattern XPATH_PROPERTY_REGEX = Pattern.compile(".*\\[@(\\S*)=['\\\"](\\S*)['\\\"]\\]");

    /**
     * Prepare XML content.
     *
     * @param xmlContent XML content sent to store
     * @param schemaContext schema context
     * @return XML content wrapped by root node (if needed)
     */
    public static String prepareXmlContent(final String xmlContent, final SchemaContext schemaContext) {

        return addRootNodeToXmlContent(xmlContent, schemaContext.getModules().iterator().next().getName(),
                ROOT_NODE_NAMESPACE);

    }

    /**
     * Prepare XML content.
     *
     * @param xmlContent XML content sent to store
     * @param parentSchemaNode Parent schema node
     * @return XML content wrapped by root node (if needed)
     */
    public static String prepareXmlContent(final String xmlContent, final DataSchemaNode parentSchemaNode,
                                           final String xpath) {
        final String namespace = parentSchemaNode.getQName().getNamespace().toString();
        final String parentXpathPart = xpath.substring(xpath.lastIndexOf('/') + 1);
        final Matcher regexMatcher = XPATH_PROPERTY_REGEX.matcher(parentXpathPart);
        if (regexMatcher.find()) {
            final HashMap<String, String> rootNodePropertyMap = new HashMap<String, String>();
            rootNodePropertyMap.put(regexMatcher.group(1), regexMatcher.group(2));
            return addRootNodeToXmlContent(xmlContent, parentSchemaNode.getQName().getLocalName(), namespace,
                    rootNodePropertyMap);
        }

        return addRootNodeToXmlContent(xmlContent, parentSchemaNode.getQName().getLocalName(), namespace);
    }

    /**
     * Add root node to XML content.
     *
     * @param xmlContent xml content to add root node
     * @param rootNodeTagName root node tag name
     * @param namespace root node namespace
     * @param rootNodeProperty root node properites map
     * @return An edited content with added root node (if needed)
     */
    public static String addRootNodeToXmlContent(final String xmlContent, final String rootNodeTagName,
                                                 final String namespace,
                                                 final HashMap<String, String> rootNodeProperty) {
        try {
            final DocumentBuilder builder = dbFactory.newDocumentBuilder();
            final StringBuilder xmlStringBuilder = new StringBuilder();
            xmlStringBuilder.append(xmlContent);
            Document xmlDoc = builder.parse(
                    new ByteArrayInputStream(xmlStringBuilder.toString().getBytes("utf-8")));
            final Element root = xmlDoc.getDocumentElement();
            if (!root.getTagName().equals(rootNodeTagName) && !root.getTagName().equals(DATA_ROOT_NODE_TAG_NAME)) {
                xmlDoc = addDataRootNode(root, rootNodeTagName, namespace, rootNodeProperty);
                xmlDoc.setXmlStandalone(true);
                final TransformerFactory transformerFactory = TransformerFactory.newInstance();
                final Transformer transformer = transformerFactory.newTransformer();
                final StringWriter stringWriter = new StringWriter();
                transformer.transform(new DOMSource(xmlDoc), new StreamResult(stringWriter));
                return stringWriter.toString();
            }
            return xmlContent;
        } catch (SAXException | IOException | ParserConfigurationException | TransformerException exception) {
            throw new DataValidationException("Failed to parse XML data", "Invalid xml input " + exception.getMessage(),
                    exception);
        }
    }

    /**
     * Add root node to XML content.
     *
     * @param xmlContent XML content to add root node into
     * @param rootNodeTagName Root node tag name
     * @return XML content with root node tag added (if needed)
     */
    public static String addRootNodeToXmlContent(final String xmlContent, final String rootNodeTagName,
                                                 final String namespace) {
        return addRootNodeToXmlContent(xmlContent, rootNodeTagName, namespace, new HashMap<String, String>());
    }

    /**
     * Add root node into DOM element.
     *
     * @param node DOM element to add root node into
     * @param tagName Root tag name to add
     * @return DOM element with a root node
     */
    static Document addDataRootNode(final Element node, final String tagName, final String namespace,
                                    final HashMap<String, String> rootNodeProperty) {
        try {
            final DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
            final Document doc = docBuilder.newDocument();
            final Element root = doc.createElementNS(namespace, tagName);
            for (final Map.Entry<String, String> entry : rootNodeProperty.entrySet()) {
                final Element propertyNode = doc.createElement(entry.getKey());
                propertyNode.setTextContent(entry.getValue());
                root.appendChild(propertyNode);
            }
            root.appendChild(doc.adoptNode(node));
            doc.appendChild(root);
            return doc;
        } catch (final ParserConfigurationException exception) {
            throw new DataValidationException("Can't parse XML", "XML can't be parsed", exception);
        }
    }
}
