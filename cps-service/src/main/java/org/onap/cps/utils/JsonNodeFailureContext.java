/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG. All rights reserved.
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

import java.util.Set;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;

/**
 * Holds the context captured at the point of a JSON parse failure.
 * Passed to each rule in {@link JsonParseFailureTranslator} to determine whether the rule applies
 * and to produce the appropriate exception message.
 *
 * @param exception       the exception thrown by the YANG JSON codec
 * @param dataSchemaNode  the resolved schema node for the failing path, or {@code null} when not found
 * @param xpath           the full xpath to the failing node, e.g. {@code /bookstore/bookstore-name}
 * @param nodeName        the local name of the failing node, e.g. {@code bookstore-name}
 * @param lastStringValue the last string value read by the JSON reader before the failure
 * @param lastValueNull   {@code true} when the last value read was a JSON {@code null}
 * @param seenFieldNames  field names encountered in the current JSON object before the parsing failure occurred
 */
record JsonNodeFailureContext(
        Exception exception,
        DataSchemaNode dataSchemaNode,
        String xpath,
        String nodeName,
        String lastStringValue,
        boolean lastValueNull,
        Set<String> seenFieldNames) {

    boolean isLeaf() {
        return dataSchemaNode instanceof TypedDataSchemaNode;
    }
}


