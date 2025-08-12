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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.onap.cps.api.exceptions.DataValidationException;
import org.springframework.stereotype.Component;

@Component
public class XmlObjectMapper {

    private final XmlMapper xmlMapper;

    public XmlObjectMapper() {
        this.xmlMapper = new XmlMapper();
    }

    /**
     * Serializing generic java object to XML using Jackson.
     *
     * @param object any java object value
     * @return the generated XML as a string.
     */

    public <T> String toXml(final T object, final String rootName) {
        try {
            return xmlMapper.writer().withRootName(rootName).writeValueAsString(object);
        } catch (final Exception exception) {
            throw new DataValidationException("Data Validation Failed",
                    "Failed to build XML: " + exception.getMessage(),
                    exception
            );
        }
    }
}
