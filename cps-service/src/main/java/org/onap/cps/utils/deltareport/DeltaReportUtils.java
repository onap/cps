/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 TechMahindra Ltd.
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
import static org.onap.cps.utils.XmlUtils.getTransformerFactory;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
    public static String buildXmlUsingDom(final List<DeltaReport> deltaReports) {
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

                final Element sourceDataElement = convertMapToElement(document, deltaReportElement,
                        "source-data", deltaReport.getSourceData());
                if (sourceDataElement != null) {
                    deltaReportElement.appendChild(sourceDataElement);
                }
                final Element targetDataElement = convertMapToElement(document, deltaReportElement,
                        "target-data", deltaReport.getTargetData());
                if (targetDataElement != null) {
                    deltaReportElement.appendChild(targetDataElement);
                }
                rootElement.appendChild(deltaReportElement);
            }
            return transformDocumentToString(document);

        } catch (final DOMException | ParserConfigurationException
                       | TransformerException exception) {
            throw new DataValidationException("Data Validation Failed", "Failed to build XML deltaReport: "
                    + exception.getMessage(), exception);
        }
    }

    private static Element convertMapToElement(final Document document, final Element parentElement, final String name,
                                               final Map<String, Serializable> data) {
        if (data != null && !data.isEmpty()) {
            final Element element = document.createElement(name);

            for (final Map.Entry<String, Serializable> mapentry : data.entrySet()) {
                final Element childElement = createXmlElement(document, mapentry);
                element.appendChild(childElement);
            }
            parentElement.appendChild(element);
            return element;
        }
        return null;
    }

    private static Element createKeyValueElement(final Document document, final Element parentElement,
                                                 final String name, final String value) {
        if (value != null && !value.isEmpty()) {
            final Element element = document.createElement(name);
            element.appendChild(document.createTextNode(value));
            parentElement.appendChild(element);
            return parentElement;
        }
        return null;
    }

    private static Element createXmlElement(final Document document,
                                            final Map.Entry<String, Serializable> inputMap) {
        final Element element = document.createElement(inputMap.getKey());
        final Serializable mapValue = inputMap.getValue();
        if (mapValue instanceof Collection) {
            final String collectionAsCsvString = ((Collection<?>) mapValue)
                    .stream().map(Object::toString).collect(Collectors.joining(", "));
            element.appendChild(document.createTextNode(collectionAsCsvString));
        } else {
            element.appendChild(document.createTextNode(mapValue.toString()));
        }
        return element;
    }

    @SuppressWarnings("SameReturnValue")
    private static String transformDocumentToString(final Document document) throws TransformerException {
        final Transformer transformer = getTransformerFactory().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        final StringWriter stringWriter = new StringWriter();
        final StreamResult streamResult = new StreamResult(stringWriter);
        transformer.transform(new DOMSource(document), streamResult);
        return stringWriter.toString();
    }
}

