/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG
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

import lombok.Builder;
import lombok.Getter;

/**
 * Context information for XML parsing operations.
 * This class tracks whether XML content was wrapped by XmlUtils during preparation,
 * which is necessary for proper unwrapping in DataNodeBuilder.
 */
@Getter
@Builder
public class XmlParsingContext {

    /**
     * Indicates whether a parent xpath was provided during XML parsing.
     */
    private final boolean hasParentXpath;

    /**
     * The parent xpath used during XML parsing, if any.
     */
    private final String parentXpath;

    /**
     * Indicates whether the XML content was wrapped by XmlUtils.
     * This is the authoritative flag that determines if unwrapping should occur.
     */
    private final boolean isWrappedByXmlUtils;

    /**
     * Creates a context for XML parsing without a parent xpath.
     *
     * @return XmlParsingContext with no parent xpath
     */
    public static XmlParsingContext noParentXpath() {
        return XmlParsingContext.builder()
                .hasParentXpath(false)
                .parentXpath("")
                .isWrappedByXmlUtils(false)
                .build();
    }

    /**
     * Creates a context for root-level XML that was wrapped with the module name by XmlUtils.
     *
     * @param moduleName the module name used as the wrapper element
     * @return XmlParsingContext indicating the XML was wrapped with the module name
     */
    public static XmlParsingContext wrappedWithModuleName(final String moduleName) {
        return XmlParsingContext.builder()
                .hasParentXpath(false)
                .parentXpath(moduleName)
                .isWrappedByXmlUtils(true)
                .build();
    }

    /**
     * Creates a context for XML parsing with a parent xpath and wrapping.
     *
     * @param parentXpath the parent xpath
     * @return XmlParsingContext with parent xpath and wrapping flag set
     */
    public static XmlParsingContext withParentXpath(final String parentXpath) {
        return XmlParsingContext.builder()
                .hasParentXpath(true)
                .parentXpath(parentXpath)
                .isWrappedByXmlUtils(true)
                .build();
    }
}
