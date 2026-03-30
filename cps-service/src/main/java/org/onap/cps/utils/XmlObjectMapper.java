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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.exceptions.DataValidationException;
import org.springframework.stereotype.Component;
@Slf4j
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


    public JsonNode convertToXmlNode(final String xmlContent) {
        try {
            return xmlMapper.readTree(xmlContent);
        } catch (final JsonProcessingException e) {
            log.error("Parsing error occurred while converting XML content to Json Node.");
            throw new DataValidationException(
                    String.format(
                            "XML parsing error at line: %d, column: %d",
                            e.getLocation().getLineNr(),
                            e.getLocation().getColumnNr()
                    ),
                    e.getOriginalMessage()
            );
        }
    }
public String convertXmlNodeToString(final JsonNode xmlNode)
{
    try {return xmlMapper.writeValueAsString(xmlNode);
    }catch (final Exception exception) {
        throw new DataValidationException("Data Validation Failed",
                "Failed to build XML: " + exception.getMessage(),
                exception
        );
    }
}
}
