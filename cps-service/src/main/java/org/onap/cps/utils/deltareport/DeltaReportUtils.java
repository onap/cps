/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Deutsche Telekom AG.
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

package org.onap.cps.utils.deltareport;

import static org.onap.cps.utils.XmlUtils.getDocumentBuilderFactory;
import static org.onap.cps.utils.XmlUtils.transformNodeToString;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.DeltaReport;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeltaReportUtils {
    private static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        return getDocumentBuilderFactory().newDocumentBuilder();
    }

    /**
     * Converts a collection of DeltaReport objects to XML format using DOM.
     * @param deltaReports The collection of DeltaReport objects.
     * @return The XML string representation of the delta reports.
     */
    public static String buildXml(final List<DeltaReport> deltaReports) {
        try {
            final DocumentBuilder documentBuilder = getDocumentBuilder();
            final Document document = documentBuilder.newDocument();
            final Element rootElement = document.createElement("deltaReports");
            document.appendChild(rootElement);
            int id = 1;
            for (final DeltaReport deltaReport : deltaReports) {
                final Element deltaReportElement = document.createElement("deltaReport");
                deltaReportElement.setAttribute("id", String.valueOf(id++));
                createKeyValueElement(document, deltaReportElement, "action", deltaReport.getAction());
                createKeyValueElement(document, deltaReportElement, "xpath", deltaReport.getXpath());
                convertMapToElement(document, deltaReportElement,
                        "source-data", deltaReport.getSourceData());
                convertMapToElement(document, deltaReportElement,
                        "target-data", deltaReport.getTargetData());
                rootElement.appendChild(deltaReportElement);
            }
            return transformDocumentToString(document);
        } catch (final DOMException | ParserConfigurationException
                       | TransformerException exception) {
            throw new DataValidationException("Data Validation Failed", "Failed to build XML deltaReport: "
                    + exception.getMessage(), exception);
        }
    }

    private static void convertMapToElement(final Document document, final Element parentElement, final String name,
                                               final Map<String, Serializable> data) {
        if (data != null && !data.isEmpty()) {
            final Element element = document.createElement(name);
            for (final Map.Entry<String, Serializable> mapentry : data.entrySet()) {
                final Element childElement = createXmlElement(document, mapentry);
                element.appendChild(childElement);
            }
            parentElement.appendChild(element);
        }
    }

    private static void createKeyValueElement(final Document document, final Element parentElement,
                                                 final String name, final String value) {
        if (value != null && !value.isEmpty()) {
            final Element element = document.createElement(name);
            element.appendChild(document.createTextNode(value));
            parentElement.appendChild(element);
        }
    }

    private static Element createXmlElement(final Document document, final Map.Entry<String, Serializable> entry) {
        final Element element = document.createElement(entry.getKey());
        final Serializable mapValue = entry.getValue();
        if (mapValue instanceof Map) {
            final Map<String, Serializable> nestedMap = (Map<String, Serializable>) mapValue;
            for (final Map.Entry<String, Serializable> nestedEntry : nestedMap.entrySet()) {
                element.appendChild(createXmlElement(document, nestedEntry));
            }
        } else if (mapValue instanceof Collection) {
            final Collection<?> collection = (Collection<?>) mapValue;
            final boolean containsMap = collection.stream().anyMatch(item -> item instanceof Map);
            if (containsMap) {
                for (final Object item : collection) {
                    if (item instanceof Map) {
                        final Map<String, Serializable> itemMap = (Map<String, Serializable>) item;
                        for (final Map.Entry<String, Serializable> itemEntry : itemMap.entrySet()) {
                            element.appendChild(createXmlElement(document, itemEntry));
                        }
                    }
                }
            } else {
                final String joinedValues = collection.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                element.appendChild(document.createTextNode(joinedValues));
            }
        } else {
            element.appendChild(document.createTextNode(mapValue.toString()));
        }
        return element;
    }

    private static String transformDocumentToString(final Document document) throws TransformerException {
        return transformNodeToString(document);
    }
}