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

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Container for prepared XML content and its parsing context.
 * This class bundles the XML string with metadata about how it was prepared.
 */
@Getter
@AllArgsConstructor
public class ContextualXml {

    /**
     * The prepared XML content as a string.
     */
    private final String xmlContent;

    /**
     * The parsing context that describes how the XML was prepared.
     */
    private final XmlParsingContext context;
}
