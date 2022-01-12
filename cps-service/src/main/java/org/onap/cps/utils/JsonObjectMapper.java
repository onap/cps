/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.exceptions.CpsException;
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
    public String parseObjectAsJsonString(final Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (final JsonProcessingException jpe) {
            log.error("Parsing error occurred while converting Object to JSON string.");
            throw new CpsException("Parsing error occurred while converting given object to JSON string.",
                    jpe.getMessage());
        }
    }

    /**
     * Constructing JavaType out of given type (typically java.lang.Class).
     * Allow efficient value conversions for structurally compatible Objects,
     * according to standard Jackson configuration.
     *
     * @param fromValue   structurally compatible Object
     * @param toValueType compatible Object class type
     * @param <T>         type parameter
     * @return a class object of specific class type 'T'
     */
    public <T> T convertFromValueToValueType(final Object fromValue, final Class<T> toValueType) {
        try {
            return objectMapper.convertValue(fromValue, toValueType);
        } catch (final IllegalArgumentException iae) {
            log.error("Found structurally incompatible object while converting into JSON string.");
            throw new CpsException("Found structurally incompatible object while converting into JSON string.",
                    iae.getMessage());
        }
    }

    /**
     * Deserialize JSON content from given JSON content String.
     *
     * @param content   JSON content
     * @param valueType compatible Object class type
     * @param <T>       type parameter
     * @return a class object of specific class type 'T'
     */
    public <T> T convertStringContentToValueType(final String content, final Class<T> valueType) {
        try {
            return objectMapper.readValue(content, valueType);
        } catch (final JsonProcessingException jpe) {
            log.error("Parsing error occurred while converting JSON content to specific class type.");
            throw new CpsException("Parsing error occurred while converting JSON content to specific class type.",
                    jpe.getMessage());
        }
    }
}
