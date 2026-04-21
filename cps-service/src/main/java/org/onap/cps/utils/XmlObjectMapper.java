/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 Deutsche Telekom AG.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.util.List;
import lombok.NoArgsConstructor;
import org.onap.cps.api.exceptions.DataValidationException;
import org.springframework.stereotype.Component;


@NoArgsConstructor
@Component
public class XmlObjectMapper {
    private final XmlMapper xmlMapper = new XmlMapper();

    /**
     * Serializing generic java object to XML using Jackson.
     *
     * @param object any java object value
     * @param rootName the name of the XML root name
     * @return the generated XML as a string.
     */

    public String asXmlString(final Object object, final String rootName) {
        try {
            return xmlMapper.writer().withRootName(rootName).writeValueAsString(object);
        } catch (final Exception exception) {
            throw new DataValidationException("Data Validation Failed",
                    "Failed to build XML: " + exception.getMessage(),
                    exception
            );
        }
    }

    /**
     * Deserialize XML content from given XML content String to List of specific class type.
     *
     * @param xmlContent          JSON content
     * @param collectionEntryType compatible Object class type
     * @param <T>                 type parameter
     * @return a list of specific class type 'T'
     */

    public <T> List<T> convertToXmlArray(final String xmlContent,
                                         final Class<T> collectionEntryType) {
        try {
            final CollectionType collectionType =
                    xmlMapper.getTypeFactory()
                            .constructCollectionType(List.class, collectionEntryType);
            return xmlMapper.readValue(xmlContent, collectionType);
        } catch (final JsonProcessingException e) {
            throw new DataValidationException(
                    String.format("XML parsing error at line: %d, column: %d",
                            e.getLocation().getLineNr(),
                            e.getLocation().getColumnNr()),
                    e.getOriginalMessage()
            );
        }
    }

    /**
     * Serializing generic java object to XML using Jackson.
     *
     * @param object the name of the XML
     * @return the generated XML as a String.
     */
    @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
    public String asXmlString(final Object object) {
        try {
            String xmlString = xmlMapper.writeValueAsString(object);
            xmlString = xmlString.replaceFirst("<LinkedHashMap>", "")
                    .replaceFirst("</LinkedHashMap>", "");
            return xmlString;
        } catch (final Exception exception) {
            throw new DataValidationException("Data Validation Failed",
                    "Failed to build XML: " + exception.getMessage(),
                    exception
            );
        }
    }

    /**
     * Serializing generic java object to XML using Jackson.
     *
     * @param object the name of the XML
     * @param rootName the name of the XML root name
     * @param namespaceUri the name of namespace
     * @return the generated XML as a String.
     */
    public String asXmlStringWithRoot(final Object object, final String rootName,
                               final String namespaceUri) {
        try {
            xmlMapper.setDefaultUseWrapper(false);
            // Convert Object to XML with root name
            String xml = xmlMapper.writer()
                    .withRootName(rootName)
                    .writeValueAsString(object);

            // Remove unwanted LinkedHashMap wrapper (if present)
            xml = xml.replaceFirst("<LinkedHashMap>", "")
                    .replaceFirst("</LinkedHashMap>", "");

            // Add namespace to root element
            xml = xml.replaceFirst(
                    "<" + rootName + ">",
                    "<" + rootName + " xmlns=\"" + namespaceUri + "\">"
            );
            return xml;
        } catch (final Exception exception) {
            throw new DataValidationException(
                    "Data Validation Failed",
                    "Failed to build XML: " + exception.getMessage(),
                    exception
            );
        }
    }
}