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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Thread-local holder for XML parsing context.
 * This allows passing context from YangParserHelper to DataNodeBuilder without changing method signatures.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class XmlParsingContextHolder {

    private static final ThreadLocal<XmlParsingContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * Sets the XML parsing context for the current thread.
     *
     * @param context the XML parsing context
     */
    public static void set(final XmlParsingContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * Gets the XML parsing context for the current thread.
     *
     * @return the XML parsing context, or null if not set
     */
    public static XmlParsingContext get() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * Clears the XML parsing context for the current thread.
     * Should be called after DataNode building is complete.
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
