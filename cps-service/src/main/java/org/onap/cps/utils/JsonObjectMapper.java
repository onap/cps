/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.api.exceptions.DataValidationException;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class JsonObjectMapper {

    private final ObjectMapper objectMapper;

    /**
     * Serializing generic java object to JSON using Jackson.
     *
     * @param object any java object value
     * @return the generated JSON as a string.
     */
    public String asJsonString(final Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (final JsonProcessingException e) {
            log.error("Parsing error occurred while converting Object to JSON string.");
            throw new DataValidationException("Parsing error occurred while converting given object to JSON string.",
                    e.getMessage());
        }
    }

    /**
     * Constructing JavaType out of given type (typically java.lang.Class).
     * Allow efficient value conversions for structurally compatible json objects,
     * according to standard Jackson configuration.
     *
     * @param jsonObject   structurally compatible json object
     * @param valueType compatible Object class type
     * @param <T>         type parameter
     * @return a class object of specific class type 'T'
     */
    public <T> T convertToValueType(final Object jsonObject, final Class<T> valueType) {
        try {
            return objectMapper.convertValue(jsonObject, valueType);
        } catch (final IllegalArgumentException e) {
            log.error("Found structurally incompatible object while converting into value type.");
            throw new DataValidationException("Found structurally incompatible object while converting "
                    + "into value type.", e.getMessage());
        }
    }

    /**
     * Deserialize JSON content from given JSON content String.
     *
     * @param jsonContent   JSON content
     * @param valueType compatible Object class type
     * @param <T>       type parameter
     * @return a class object of specific class type 'T'
     */
    public <T> T convertJsonString(final String jsonContent, final Class<T> valueType) {
        try {
            return objectMapper.readValue(jsonContent, valueType);
        } catch (final JsonProcessingException e) {
            log.error("Parsing error occurred while converting JSON content to specific class type.");
            throw new DataValidationException("Parsing error occurred while converting "
                    + "JSON content to specific class type.", e.getMessage());
        }
    }

    /**
     * Serializing generic json object to bytes using Jackson.
     *
     * @param jsonObject any json object value
     * @return the generated JSON as a byte array.
     */
    public byte[] asJsonBytes(final Object jsonObject) {
        try {
            return objectMapper.writeValueAsBytes(jsonObject);
        } catch (final JsonProcessingException jsonProcessingException) {
            log.error("Parsing error occurred while converting JSON object to bytes.");
            throw new DataValidationException("Parsing error occurred while converting given JSON object to bytes.",
                    jsonProcessingException.getMessage());
        }
    }

    /**
     * Deserialize JSON content from given JSON content String to JsonNode.
     *
     * @param jsonContent   JSON content
     * @return a json node
     */
    public JsonNode convertToJsonNode(final String jsonContent) {
        try {
            return objectMapper.readTree(jsonContent);
        } catch (final JsonProcessingException e) {
            log.error("Parsing error occurred while converting JSON content to Json Node.");
            throw new DataValidationException("Parsing error occurred while converting "
                    + "JSON content to Json Node.", e.getMessage());
        }
    }
}
